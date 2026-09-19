package com.aileapro.api.intelligence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IntelligenceViews {

    private IntelligenceViews() {
    }

    public record FeedItem(
            String slug,
            String title,
            String summary,
            Instant occurredAt,
            String lifecycle,
            Instant verifiedAt,
            int verifiedSourceCount,
            String currentnessNote,
            List<String> sourceSlugs) {
    }

    public record Feed(List<FeedItem> items, String honesty) {
    }

    public record UpdateSummary(
            UUID id,
            String slug,
            String title,
            String status,
            String lifecycle,
            Instant occurredAt,
            Instant verifiedAt,
            int verifiedSourceCount) {
    }

    public record Proposal(
            UUID id,
            UUID updateId,
            String updateSlug,
            String title,
            BigDecimal impactScore,
            List<String> impactReasons,
            String status,
            String rejectReason,
            Instant createdAt,
            Instant reviewedAt) {
    }

    public record RejectBody(String reason) {
    }

    public record CreateUpdateBody(
            String slug,
            String title,
            String summary,
            String lifecycle,
            List<String> sourceSlugs,
            boolean publish) {
    }

    public record CreateProposalBody(
            String updateSlug,
            String title,
            String unitSlug,
            boolean newConcept,
            boolean changeExisting,
            boolean obsoleteTechnique,
            boolean toolChange,
            boolean apiChange,
            boolean patternChange,
            boolean newPrereq,
            boolean newSpecialization,
            boolean industryExpectation,
            boolean newExerciseOrAssessment,
            boolean careerSkillChange) {
    }

    public record LinkCheckRow(String sourceSlug, boolean ok, Integer httpStatus, String error, int durationMs) {
    }

    public record LinkCheckReport(int checked, int ok, int failed, List<LinkCheckRow> rows, String note) {
    }

    public record UnitChange(
            UUID changeId,
            String action,
            String fromVersion,
            String toVersion,
            Instant at,
            String reason) {
    }

    public record ChangesSince(
            String unitSlug,
            Instant since,
            List<UnitChange> changes,
            String note) {
    }
}
