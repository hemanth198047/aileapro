package com.aileapro.api.career;

import java.util.List;
import java.util.UUID;

public final class CareerViews {

    private CareerViews() {
    }

    public record Gap(
            UUID unitId,
            String unitSlug,
            String unitTitle,
            String currentState,
            String requiredBar,
            String pathRequiredState,
            String href) {
    }

    public record Gaps(
            String pathSlug,
            String pathTitle,
            String roleTarget,
            String readyBar,
            int requiredUnitCount,
            int readyUnitCount,
            int gapCount,
            boolean readyForRole,
            String honesty,
            List<Gap> gaps,
            String interviewPersona,
            String note) {
    }
}
