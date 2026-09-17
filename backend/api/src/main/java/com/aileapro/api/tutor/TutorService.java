package com.aileapro.api.tutor;

import com.aileapro.ai.ChatMessage;
import com.aileapro.ai.Injection;
import com.aileapro.ai.Persona;
import com.aileapro.ai.TutorAction;
import com.aileapro.ai.TutorPolicy;
import com.aileapro.api.ai.AiBudget;
import com.aileapro.api.ai.AiGateway;
import com.aileapro.api.ai.ModelRouter;
import com.aileapro.api.ai.PromptCatalog;
import com.aileapro.api.ai.UsageMeter;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.Block;
import com.aileapro.api.curriculum.CurriculumViews.UnitDetail;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.platform.ApiException;
import com.aileapro.persistence.ConversationEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One tutor turn, in a fixed order that the rest of the class exists to preserve.
 *
 * <p>The policy decides first, before anything is called: a refused turn therefore costs
 * nothing, cannot be argued round, and is identical every time. The learner's message is
 * stored next, so a provider outage leaves a transcript with the question in it. Only then is
 * a model asked, and what it produced is stored with the model and prompt version that
 * produced it.
 *
 * <p>Nothing here is transactional. A turn can take twenty seconds, and a database connection
 * held for twenty seconds is a connection nobody else can use; the writes are short calls into
 * {@link TutorTranscript}.
 */
@Service
public class TutorService {

    private static final String FEATURE = "tutor";

    /** The unit page is the tutor's material. Capped so one long unit cannot eat the window. */
    private static final int UNIT_TEXT_BUDGET = 6000;

    /** Block kinds that are teaching material. Exercises and their keys are deliberately out. */
    private static final List<String> TEACHING_BLOCKS =
            List.of("prose", "definition", "example", "analogy", "misconception", "visual");

    private final TutorTranscript transcript;
    private final TutorContextService context;
    private final CurriculumQueryService curriculum;
    private final PromptCatalog prompts;
    private final AiGateway gateway;
    private final ModelRouter router;
    private final AiBudget budget;
    private final UsageMeter meter;

    public TutorService(
            TutorTranscript transcript,
            TutorContextService context,
            CurriculumQueryService curriculum,
            PromptCatalog prompts,
            AiGateway gateway,
            ModelRouter router,
            AiBudget budget,
            UsageMeter meter) {
        this.transcript = transcript;
        this.context = context;
        this.curriculum = curriculum;
        this.prompts = prompts;
        this.gateway = gateway;
        this.router = router;
        this.budget = budget;
        this.meter = meter;
    }

    /** What the caller needs once a turn is done. */
    public record TurnResult(UUID messageId, String text, boolean development, String refusalCode) {
    }

    public TurnResult turn(
            UUID userId, UUID conversationId, String text, String actionSlug, Consumer<String> onChunk) {
        String askText = text == null ? "" : text.strip();
        TutorAction action = action(actionSlug);
        if (askText.isEmpty() && action == null) {
            throw ApiException.validation("A message or an action is required");
        }

        ConversationEntity conversation = transcript.owned(userId, conversationId);

        // Checked here as well as inside the gateway. Not redundant: this one runs before the
        // learner's message is stored, so someone out of budget is told so without a
        // half-finished exchange left in their transcript.
        budget.require(userId);

        TutorPolicy.UnitContext unitContext = context.contextFor(userId, conversation.getUnitId());
        TutorPolicy.Turn decision = TutorPolicy.decide(new TutorPolicy.Ask(
                askText,
                action,
                Persona.fromSlug(conversation.getPersona()),
                unitContext,
                transcript.priorLearnerTurns(conversationId),
                TutorPolicy.showsWork(askText)));

        String asked = askText.isEmpty() ? actionLabel(action) : askText;
        UUID learnerMessageId = transcript.storeLearner(conversationId, asked, action, titleFrom(asked));

        if (decision.refused()) {
            String reply = decision.refusal().reply();
            onChunk.accept(reply);
            // Metered even though it is free. A learner repeatedly asking for the answer key is
            // a fact about the product, and a row with cost zero is how that stays visible.
            meter.record(new UsageMeter.Call(
                    userId, FEATURE, "teach", "none", "none", null, null, conversationId,
                    0, 0, 0, UsageMeter.Outcome.REFUSED));
            UUID messageId = transcript.storeAssistant(
                    conversationId, reply, TutorTranscript.Attribution.refusal(), decision.refusal().code());
            return new TurnResult(messageId, reply, false, decision.refusal().code());
        }

        PromptCatalog.Rendered prompt = prompts.render(decision.promptName(), decision.slots());
        List<ChatMessage> wire = new ArrayList<>();
        wire.add(ChatMessage.system(prompt.text()));
        String unitText = unitText(conversation.getUnitId());
        if (unitText != null) {
            wire.add(ChatMessage.data("unit:" + unitContext.slug(), unitText));
        }
        wire.addAll(transcript.history(conversationId, learnerMessageId));
        wire.add(ChatMessage.user(Injection.fenceSafe(asked)));

        AiGateway.Answer answer = gateway.stream(
                new AiGateway.Request(
                        userId, FEATURE, decision.profileSlug(), wire, prompt.versionId(), conversationId),
                onChunk);

        UUID messageId = transcript.storeAssistant(
                conversationId,
                answer.text(),
                new TutorTranscript.Attribution(
                        answer.modelId(), prompt.versionId(), answer.development(), decision.reveal().name()),
                null);
        return new TurnResult(messageId, answer.text(), answer.development(), null);
    }

    /**
     * The authored page, handed over as data rather than as instructions.
     *
     * <p>This is the page the learner already has open, not a retrieved source, which is why it
     * produces no citation and why answers stay marked as ungrounded. Prose and misconception
     * blocks only: an exercise's answer key has no business in the context of a tutor that is
     * forbidden from handing answers over.
     */
    private String unitText(UUID unitId) {
        if (unitId == null) {
            return null;
        }
        UnitRef ref = curriculum.publishedRefs(List.of(unitId)).get(unitId);
        if (ref == null) {
            return null;
        }
        UnitDetail detail = curriculum.unit(ref.slug());

        StringBuilder out = new StringBuilder();
        out.append("# ").append(detail.title()).append('\n');
        if (detail.description() != null && !detail.description().isBlank()) {
            out.append(detail.description()).append('\n');
        }
        if (!detail.objectives().isEmpty()) {
            out.append("\nWhat this page is for:\n");
            for (String objective : detail.objectives()) {
                out.append("- ").append(objective).append('\n');
            }
        }
        for (Block block : detail.blocks()) {
            if (!TEACHING_BLOCKS.contains(block.type())) {
                continue;
            }
            String claim = block.payload() == null ? "" : block.payload().path("claim").asText("");
            String markdown = block.payload() == null ? "" : block.payload().path("markdown").asText("");
            if (!claim.isBlank()) {
                out.append("\nCommon false belief this page argues against: ").append(claim).append('\n');
            }
            if (!markdown.isBlank()) {
                out.append('\n').append(markdown).append('\n');
            }
            if (out.length() > UNIT_TEXT_BUDGET) {
                out.setLength(UNIT_TEXT_BUDGET);
                out.append("\n…(page truncated for length)");
                break;
            }
        }
        return Injection.fenceSafe(out.toString());
    }

    /**
     * What the dock needs before the learner types.
     *
     * <p>{@code development} and {@code retrieval} are part of the contract, not diagnostics.
     * Without them the panel would look like a finished tutor while answering from fixtures
     * with nothing behind it, and the learner would have no way to know.
     */
    public TutorViews.Status status(UUID userId) {
        List<ModelRouter.Route> chain = router.chain("teach");
        ModelRouter.Route best = chain.isEmpty() ? null : chain.get(0);
        AiBudget.Remaining remaining = budget.remaining(userId);

        return new TutorViews.Status(
                best != null,
                best != null && best.port().development(),
                false,
                best == null ? null : best.providerSlug(),
                best == null ? null : best.modelSlug(),
                remaining.usdSpent(),
                remaining.usdCap() == null ? BigDecimal.ZERO : remaining.usdCap(),
                remaining.turnsUsed(),
                remaining.turnsCap(),
                Arrays.stream(TutorAction.values())
                        .map(action -> new TutorViews.Action(
                                action.slug(), actionLabel(action), actionHint(action)))
                        .toList(),
                Arrays.stream(Persona.values())
                        .map(persona -> new TutorViews.PersonaOption(
                                persona.slug(), persona.title(), persona.instruction()))
                        .toList());
    }

    /**
     * What the answers in a conversation were grounded in.
     *
     * <p>Empty, and said plainly, until retrieval exists. The unit page is in context and is
     * named here, but a page the learner is already reading is not a citation, and listing it
     * as one would put a source list under answers that had no sources.
     */
    public Map<String, Object> sources(UUID userId, UUID conversationId) {
        ConversationEntity conversation = transcript.owned(userId, conversationId);
        UnitRef unit = transcript.unitOf(conversation);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("retrieval", false);
        out.put("unitInContext", unit == null ? "" : unit.slug());
        out.put("blocks", transcript.citedBlocks(conversationId));
        out.put(
                "note",
                "Retrieval arrives in the next phase. Until then the tutor reads the page you are "
                        + "on and nothing else, so it has no sources to cite — and it is told not to "
                        + "invent any.");
        return out;
    }

    private static TutorAction action(String slug) {
        try {
            return TutorAction.fromSlug(slug);
        } catch (IllegalArgumentException unknown) {
            throw ApiException.validation(unknown.getMessage());
        }
    }

    /**
     * A title from the learner's first message, with no model call.
     *
     * <p>Naming every conversation with a model would double the calls and the latency for a
     * label nobody reads twice. Six words of what they actually said is more accurate anyway.
     */
    static String titleFrom(String text) {
        String[] words = text.replace('\n', ' ').trim().split("\\s+");
        String title = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(6, words.length)));
        if (title.isBlank()) {
            return "Tutor";
        }
        return title.length() > 60 ? title.substring(0, 57) + "…" : title;
    }

    static String actionLabel(TutorAction action) {
        if (action == null) {
            return "";
        }
        return switch (action) {
            case SIMPLER -> "Explain simpler";
            case DEEPER -> "Go deeper";
            case MATH -> "Show the maths";
            case CODE -> "Show it in code";
            case ANALOGY -> "Give an analogy";
            case QUIZ_ME -> "Quiz me";
            case CHALLENGE_ME -> "Challenge me";
            case REVIEW_CODE -> "Review my code";
        };
    }

    private static String actionHint(TutorAction action) {
        return switch (action) {
            case SIMPLER -> "Fewer moving parts, same truth.";
            case DEEPER -> "The mechanism under the explanation.";
            case MATH -> "The formal statement, every symbol defined.";
            case CODE -> "Runnable code, with the line that matters marked.";
            case ANALOGY -> "One comparison, and where it breaks down.";
            case QUIZ_ME -> "It asks, you answer, one question at a time.";
            case CHALLENGE_ME -> "One case harder than the page covers.";
            case REVIEW_CODE -> "Paste your code; it reads before it advises.";
        };
    }
}
