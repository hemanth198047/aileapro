package com.aileapro.api.ai;

import com.aileapro.ai.ChatMessage;
import com.aileapro.ai.ChatRequest;
import com.aileapro.ai.ChatResult;
import com.aileapro.ai.ProviderException;
import com.aileapro.api.platform.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * The single door every model call goes through.
 *
 * <p>Budget first, then routing, then the call, then the meter — in that order, every time, for
 * every feature. Centralising it is what makes the cost table complete: a feature that could
 * call a provider directly would be invisible to the budget and to the invoice, and it would
 * take exactly one deadline for somebody to add one.
 */
@Service
public class AiGateway {

    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    /** One retry per model. Two failures from the same provider is a pattern, not bad luck. */
    private static final int ATTEMPTS_PER_ROUTE = 2;

    private final ModelRouter router;
    private final UsageMeter meter;
    private final AiBudget budget;

    public AiGateway(ModelRouter router, UsageMeter meter, AiBudget budget) {
        this.router = router;
        this.meter = meter;
        this.budget = budget;
    }

    public record Request(
            UUID userId,
            String feature,
            String profileSlug,
            List<ChatMessage> messages,
            UUID promptVersionId,
            UUID conversationId) {
    }

    public record Answer(
            String text,
            String providerSlug,
            String modelSlug,
            UUID modelId,
            boolean development,
            int tokensIn,
            int tokensOut,
            int latencyMs,
            boolean truncated) {
    }

    public Answer complete(Request request) {
        return run(request, null);
    }

    /**
     * Streams an answer to the consumer and returns the whole of it.
     *
     * <p>Once any text has reached the learner, this stops falling back. A second model's
     * answer appended to the first model's opening sentence is not a degraded answer, it is two
     * answers spliced together, and the learner has no way to tell that is what happened.
     */
    public Answer stream(Request request, Consumer<String> onChunk) {
        return run(request, onChunk);
    }

    private Answer run(Request request, Consumer<String> onChunk) {
        budget.require(request.userId());

        List<ModelRouter.Route> chain = router.chain(request.profileSlug());
        if (chain.isEmpty()) {
            throw new ApiException(
                    503,
                    "provider_unavailable",
                    "No model is configured for this kind of question, so the tutor cannot answer. "
                            + "Everything else on the page works without one.");
        }

        ProviderException last = null;
        for (ModelRouter.Route route : chain) {
            for (int attempt = 1; attempt <= ATTEMPTS_PER_ROUTE; attempt++) {
                Emitted emitted = new Emitted();
                long startedAt = System.nanoTime();
                try {
                    ChatRequest chatRequest = new ChatRequest(
                            route.wireName(),
                            request.messages(),
                            route.temperature(),
                            route.maxOutputTokens());

                    ChatResult result = onChunk == null
                            ? route.port().complete(chatRequest)
                            : route.port().stream(chatRequest, chunk -> {
                                emitted.any = true;
                                onChunk.accept(chunk);
                            });

                    int latencyMs = millisSince(startedAt);
                    meter.record(new UsageMeter.Call(
                            request.userId(),
                            request.feature(),
                            request.profileSlug(),
                            route.providerSlug(),
                            route.modelSlug(),
                            route.model(),
                            request.promptVersionId(),
                            request.conversationId(),
                            result.tokensIn(),
                            result.tokensOut(),
                            latencyMs,
                            UsageMeter.Outcome.OK));

                    return new Answer(
                            result.text(),
                            route.providerSlug(),
                            route.modelSlug(),
                            route.model() == null ? null : route.model().getId(),
                            route.port().development(),
                            result.tokensIn(),
                            result.tokensOut(),
                            latencyMs,
                            result.truncated());

                } catch (ProviderException failed) {
                    last = failed;
                    meter.record(new UsageMeter.Call(
                            request.userId(),
                            request.feature(),
                            request.profileSlug(),
                            route.providerSlug(),
                            route.modelSlug(),
                            route.model(),
                            request.promptVersionId(),
                            request.conversationId(),
                            0,
                            0,
                            millisSince(startedAt),
                            UsageMeter.Outcome.PROVIDER_ERROR));
                    log.warn(
                            "{} on {} failed ({}): {}",
                            route.modelSlug(),
                            request.profileSlug(),
                            failed.retryable() ? "retryable" : "final",
                            failed.getMessage());

                    if (emitted.any) {
                        throw new ApiException(
                                503,
                                "provider_unavailable",
                                "The answer stopped partway through. Nothing was saved, so asking again "
                                        + "starts cleanly rather than continuing a half-finished thought.");
                    }
                    if (!failed.retryable()) {
                        break;
                    }
                    if (attempt < ATTEMPTS_PER_ROUTE) {
                        backOff(attempt);
                    }
                }
            }
        }

        throw new ApiException(
                503,
                "provider_unavailable",
                "Every model we could ask is failing right now"
                        + (last == null ? "." : ": " + last.getMessage())
                        + " The unit, the practice set, and your reviews do not need one.");
    }

    /** Jittered, so a provider recovering from an outage is not hit by a synchronised retry. */
    private static void backOff(int attempt) {
        long base = 150L * attempt;
        long jitter = ThreadLocalRandom.current().nextLong(50, 250);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static int millisSince(long startedAtNanos) {
        return (int) ((System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    /** Whether anything reached the learner, which decides if falling back is still honest. */
    private static final class Emitted {
        private boolean any;
    }
}
