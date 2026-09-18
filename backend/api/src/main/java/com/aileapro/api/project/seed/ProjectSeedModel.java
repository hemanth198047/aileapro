package com.aileapro.api.project.seed;

import java.util.List;

/** Jackson binding for {@code seed/projects/*.yml}. */
public record ProjectSeedModel(List<SeedProject> projects) {

    public record SeedProject(
            String slug,
            String title,
            String summary,
            String objective,
            String status,
            int sortOrder,
            String unit,
            List<String> applyUnits,
            String starterSource,
            List<SeedMilestone> milestones) {
    }

    public record SeedMilestone(
            String slug,
            String title,
            String description,
            String criteria,
            String hint,
            int sortOrder,
            String checkKey,
            String starterSource) {
    }
}
