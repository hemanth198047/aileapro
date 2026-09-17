package com.aileapro.api.tutor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** What the tutor endpoints return. */
public final class TutorViews {

    private TutorViews() {
    }

    /** A conversation in a list. */
    public record ConversationSummary(
            UUID id,
            String unitSlug,
            String unitTitle,
            String persona,
            String title,
            int messageCount,
            Instant createdAt,
            Instant lastMessageAt) {
    }

    /**
     * One stored turn.
     *
     * @param development true when a fixture wrote it, which the UI must show and never hide
     * @param sources     block ids the answer was grounded in; empty until retrieval exists
     * @param refusalCode set when the tutor declined, so the UI can present it as a decision
     */
    public record Message(
            UUID id,
            String role,
            String content,
            String action,
            boolean development,
            String refusalCode,
            List<UUID> sources,
            String model,
            Integer promptVersion,
            Instant createdAt) {
    }

    public record ConversationDetail(
            UUID id,
            String unitSlug,
            String unitTitle,
            String persona,
            String title,
            List<Message> messages) {
    }

    /**
     * What the tutor knows about the learner on this unit, shown in the dock.
     *
     * <p>Exposed deliberately. A tutor that silently reads your mastery record and your open
     * misconceptions is doing something the learner has a right to see, and showing it also
     * makes the answers less uncanny: "it knows I got the aliasing question wrong" is fine when
     * the panel says so.
     */
    public record Context(
            String unitSlug,
            String unitTitle,
            String masteryState,
            boolean readSolid,
            List<String> openMisconceptions,
            List<String> recentMistakes) {
    }

    /**
     * Whether the tutor can answer, and on what.
     *
     * @param development  true when fixtures are serving, which drives the banner
     * @param retrieval    false until Phase 8; the UI says so rather than showing empty sources
     * @param usdSpent     today's spend against {@code usdCap}
     */
    public record Status(
            boolean available,
            boolean development,
            boolean retrieval,
            String providerSlug,
            String modelSlug,
            BigDecimal usdSpent,
            BigDecimal usdCap,
            long turnsUsed,
            int turnsCap,
            List<Action> actions,
            List<PersonaOption> personas) {
    }

    public record Action(String slug, String label, String hint) {
    }

    public record PersonaOption(String slug, String title, String hint) {
    }

    /**
     * One cited curriculum block in the Sources drawer.
     *
     * @param updatedAt knowledge timestamp — when the authored block last changed
     */
    public record SourceBlock(
            UUID blockId,
            String unitSlug,
            String unitTitle,
            String blockType,
            String knowledgeClass,
            Instant updatedAt,
            String href) {
    }
}
