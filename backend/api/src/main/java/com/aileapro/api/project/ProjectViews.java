package com.aileapro.api.project;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectViews {

    private ProjectViews() {
    }

    public record Summary(
            UUID id,
            String slug,
            String title,
            String summary,
            String status,
            int milestoneCount,
            int passedMilestones,
            boolean completed,
            boolean acceptsSubmissions) {
    }

    public record Milestone(
            UUID id,
            String slug,
            String title,
            String description,
            String criteria,
            String hint,
            int sortOrder,
            String checkKey,
            String starterSource,
            boolean passed,
            Instant lastPassedAt) {
    }

    public record Detail(
            UUID id,
            String slug,
            String title,
            String summary,
            String objective,
            String status,
            String starterSource,
            List<String> applyUnitSlugs,
            List<Milestone> milestones,
            int passedMilestones,
            boolean completed,
            boolean acceptsSubmissions,
            UUID portfolioItemId) {
    }

    public record SubmissionResult(
            UUID id,
            String projectSlug,
            String milestoneSlug,
            boolean passed,
            String review,
            String mentorComment,
            String stdout,
            boolean development,
            boolean projectCompleted,
            UUID portfolioItemId,
            List<String> applyUnitsUpdated) {
    }

    public record PortfolioItem(
            UUID id,
            String projectSlug,
            String title,
            String summary,
            String reflection,
            String visibility,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record Portfolio(List<PortfolioItem> items, String note) {
    }
}
