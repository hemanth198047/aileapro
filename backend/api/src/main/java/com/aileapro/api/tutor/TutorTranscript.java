package com.aileapro.api.tutor;

import com.aileapro.ai.ChatMessage;
import com.aileapro.ai.Injection;
import com.aileapro.ai.Persona;
import com.aileapro.ai.TutorAction;
import com.aileapro.api.ai.ModelRepository;
import com.aileapro.api.ai.PromptVersionRepository;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.platform.ApiException;
import com.aileapro.persistence.ConversationEntity;
import com.aileapro.persistence.MessageEntity;
import com.aileapro.persistence.ModelEntity;
import com.aileapro.persistence.PromptVersionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conversations and messages: everything that touches the transcript.
 *
 * <p>Separate from {@link TutorService} because of how transactions actually work. A turn holds
 * no transaction — it can take twenty seconds and a database connection held that long is one
 * nobody else can have — so every write it needs is a short call into this bean. Had these
 * methods lived on the same class, calling them from inside the turn would have bypassed the
 * proxy and quietly run them with no transaction at all.
 */
@Service
public class TutorTranscript {

    /**
     * How much of the conversation goes back to the model, in learner/tutor pairs.
     *
     * <p>Six is enough to keep a thread of reasoning and short enough that a long conversation
     * does not cost progressively more. Sending everything makes the twentieth question ten
     * times the price of the first, for a gain nobody can demonstrate.
     */
    private static final int HISTORY_TURNS = 6;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final CurriculumQueryService curriculum;
    private final ModelRepository models;
    private final PromptVersionRepository promptVersions;

    public TutorTranscript(
            ConversationRepository conversations,
            MessageRepository messages,
            CurriculumQueryService curriculum,
            ModelRepository models,
            PromptVersionRepository promptVersions) {
        this.conversations = conversations;
        this.messages = messages;
        this.curriculum = curriculum;
        this.models = models;
        this.promptVersions = promptVersions;
    }

    // ------------------------------------------------------------------
    // Conversations
    // ------------------------------------------------------------------

    @Transactional
    public TutorViews.ConversationDetail start(UUID userId, String unitSlug, String personaSlug) {
        UnitRef unit = null;
        if (unitSlug != null && !unitSlug.isBlank()) {
            unit = curriculum.publishedRef(unitSlug);
        }
        Persona persona;
        try {
            persona = Persona.fromSlug(personaSlug);
        } catch (IllegalArgumentException unknown) {
            throw ApiException.validation(unknown.getMessage());
        }

        ConversationEntity row = new ConversationEntity();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        row.setUnitId(unit == null ? null : unit.id());
        row.setPersona(persona.slug());
        conversations.save(row);

        return new TutorViews.ConversationDetail(
                row.getId(),
                unit == null ? null : unit.slug(),
                unit == null ? null : unit.title(),
                persona.slug(),
                row.getTitle(),
                List.of());
    }

    /** Always loaded with the owner in the query. A transcript is the most private row here. */
    @Transactional(readOnly = true)
    public ConversationEntity owned(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound("No such conversation"));
    }

    @Transactional(readOnly = true)
    public TutorViews.ConversationDetail detail(UUID userId, UUID conversationId) {
        ConversationEntity row = owned(userId, conversationId);
        UnitRef unit = unitOf(row);
        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(row.getId()).stream()
                .filter(message -> !"system".equals(message.getRole()))
                .toList();
        return new TutorViews.ConversationDetail(
                row.getId(),
                unit == null ? null : unit.slug(),
                unit == null ? null : unit.title(),
                row.getPersona(),
                row.getTitle(),
                views(stored));
    }

    @Transactional(readOnly = true)
    public List<TutorViews.ConversationSummary> list(UUID userId, String unitSlug) {
        List<ConversationEntity> rows;
        if (unitSlug == null || unitSlug.isBlank()) {
            rows = conversations.findTop20ByUserIdOrderByLastMessageAtDescCreatedAtDesc(userId);
        } else {
            rows = conversations.findByUserIdAndUnitIdOrderByCreatedAtDesc(
                    userId, curriculum.publishedRef(unitSlug).id());
        }
        List<TutorViews.ConversationSummary> out = new ArrayList<>(rows.size());
        for (ConversationEntity row : rows) {
            UnitRef unit = unitOf(row);
            out.add(new TutorViews.ConversationSummary(
                    row.getId(),
                    unit == null ? null : unit.slug(),
                    unit == null ? null : unit.title(),
                    row.getPersona(),
                    row.getTitle(),
                    messages.findByConversationIdOrderBySortAsc(row.getId()).size(),
                    row.getCreatedAt(),
                    row.getLastMessageAt()));
        }
        return out;
    }

    /**
     * Deletes a conversation and its messages.
     *
     * <p>Here from the first version rather than promised for later. A learner who cannot delete
     * the record of being confused will learn not to be candid in front of the tutor, and a
     * tutor nobody is candid with cannot teach anything.
     */
    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        ConversationEntity row = owned(userId, conversationId);
        messages.deleteByConversationId(row.getId());
        conversations.delete(row);
    }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    /** Prior learner turns, which is what the reveal ladder counts as effort. */
    @Transactional(readOnly = true)
    public int priorLearnerTurns(UUID conversationId) {
        return messages.countLearnerTurns(conversationId);
    }

    /**
     * Stores the learner's message before the answer is attempted.
     *
     * <p>Order matters: a provider failure then leaves a transcript showing what was asked,
     * rather than a gap the learner has to reconstruct from memory.
     */
    @Transactional
    public UUID storeLearner(UUID conversationId, String text, TutorAction action, String title) {
        MessageEntity row = new MessageEntity();
        row.setId(UUID.randomUUID());
        row.setConversationId(conversationId);
        row.setRole("user");
        row.setContent(text);
        row.setAction(action == null ? null : action.slug());
        row.setSort(messages.maxSort(conversationId) + 1);
        messages.save(row);

        conversations.findById(conversationId).ifPresent(conversation -> {
            if (conversation.getTitle() == null || conversation.getTitle().isBlank()) {
                conversation.setTitle(title);
            }
            conversation.setLastMessageAt(Instant.now());
            conversations.save(conversation);
        });
        return row.getId();
    }

    /** What produced an answer, recorded with it. */
    public record Attribution(UUID modelId, UUID promptVersionId, boolean development, String reveal) {

        /** A refusal is authored text: no model, no prompt, and no reveal stage. */
        public static Attribution refusal() {
            return new Attribution(null, null, false, null);
        }
    }

    @Transactional
    public UUID storeAssistant(UUID conversationId, String text, Attribution about, String refusalCode) {
        MessageEntity row = new MessageEntity();
        row.setId(UUID.randomUUID());
        row.setConversationId(conversationId);
        row.setRole("assistant");
        row.setContent(text);
        row.setModelId(about.modelId());
        row.setPromptVersionId(about.promptVersionId());
        row.setDevelopment(about.development());
        row.setReveal(about.reveal());
        row.setRefusalCode(refusalCode);
        row.setSort(messages.maxSort(conversationId) + 1);
        messages.save(row);

        conversations.findById(conversationId).ifPresent(conversation -> {
            conversation.setLastMessageAt(Instant.now());
            conversations.save(conversation);
        });
        return row.getId();
    }

    /**
     * The tail of the conversation for the model, oldest first.
     *
     * <p>Refusals are left out. Sending them back would have the model treating "I will not give
     * you the answer" as part of the material it is teaching from.
     *
     * @param excludeMessageId the message just stored, which the caller appends as the question
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> history(UUID conversationId, UUID excludeMessageId) {
        List<MessageEntity> usable = messages.findByConversationIdOrderBySortAsc(conversationId).stream()
                .filter(message -> message.getRefusalCode() == null)
                .filter(message -> !"system".equals(message.getRole()))
                .filter(message -> !message.getId().equals(excludeMessageId))
                .toList();
        List<MessageEntity> window = usable.size() <= HISTORY_TURNS * 2
                ? usable
                : usable.subList(usable.size() - HISTORY_TURNS * 2, usable.size());

        List<ChatMessage> wire = new ArrayList<>(window.size());
        for (MessageEntity message : window) {
            wire.add("assistant".equals(message.getRole())
                    ? ChatMessage.assistant(message.getContent())
                    : ChatMessage.user(Injection.fenceSafe(message.getContent())));
        }
        return wire;
    }

    /** Every block id any answer in this conversation cited. Empty until retrieval exists. */
    @Transactional(readOnly = true)
    public List<UUID> citedBlocks(UUID conversationId) {
        return messages.findByConversationIdOrderBySortAsc(conversationId).stream()
                .flatMap(message -> Arrays.stream(message.getRetrievalIds()))
                .distinct()
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Resolves model and prompt version so the transcript can say what wrote each line.
     *
     * <p>Two batched lookups rather than one per message: a long conversation should not cost a
     * query per turn to display.
     */
    private List<TutorViews.Message> views(List<MessageEntity> stored) {
        Set<UUID> modelIds = new HashSet<>();
        Set<UUID> versionIds = new HashSet<>();
        for (MessageEntity message : stored) {
            if (message.getModelId() != null) {
                modelIds.add(message.getModelId());
            }
            if (message.getPromptVersionId() != null) {
                versionIds.add(message.getPromptVersionId());
            }
        }
        Map<UUID, String> modelSlugs = new HashMap<>();
        for (ModelEntity model : models.findAllById(modelIds)) {
            modelSlugs.put(model.getId(), model.getSlug());
        }
        Map<UUID, Integer> versions = new HashMap<>();
        for (PromptVersionEntity version : promptVersions.findAllById(versionIds)) {
            versions.put(version.getId(), version.getVersion());
        }

        List<TutorViews.Message> out = new ArrayList<>(stored.size());
        for (MessageEntity message : stored) {
            out.add(new TutorViews.Message(
                    message.getId(),
                    message.getRole(),
                    message.getContent(),
                    message.getAction(),
                    message.isDevelopment(),
                    message.getRefusalCode(),
                    Arrays.asList(message.getRetrievalIds()),
                    modelSlugs.get(message.getModelId()),
                    versions.get(message.getPromptVersionId()),
                    message.getCreatedAt()));
        }
        return out;
    }

    UnitRef unitOf(ConversationEntity conversation) {
        if (conversation.getUnitId() == null) {
            return null;
        }
        return curriculum.publishedRefs(List.of(conversation.getUnitId())).get(conversation.getUnitId());
    }
}
