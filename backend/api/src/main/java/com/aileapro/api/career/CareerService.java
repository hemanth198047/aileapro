package com.aileapro.api.career;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PathDetail;
import com.aileapro.api.curriculum.CurriculumViews.PathSummary;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearnerProfileService;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.MasteryService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.MasteryState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Career readiness from demonstrated mastery only.
 *
 * <p>Gaps are path waypoints whose mastery is below {@link MasteryState#PROFICIENT}.
 * Onboarding self-report never enters this calculation.
 */
@Service
public class CareerService {

    /** The bar Phase 12 uses for "ready enough on this unit" — fixed, not the waypoint label. */
    public static final MasteryState READY_BAR = MasteryState.PROFICIENT;

    private final LearnerProfileService profiles;
    private final CurriculumQueryService curriculum;
    private final MasteryService mastery;

    public CareerService(
            LearnerProfileService profiles, CurriculumQueryService curriculum, MasteryService mastery) {
        this.profiles = profiles;
        this.curriculum = curriculum;
        this.mastery = mastery;
    }

    @Transactional(readOnly = true)
    public CareerViews.Gaps gaps(UUID userId, String pathIdOrSlug) {
        PathDetail path = resolvePath(userId, pathIdOrSlug);
        List<UnitRef> refs = path.waypoints().stream().map(w -> w.unit()).toList();
        Map<UUID, Mastery> byUnit = mastery.masteryFor(userId, refs);

        List<CareerViews.Gap> gaps = new ArrayList<>();
        int ready = 0;
        for (var waypoint : path.waypoints()) {
            UnitRef unit = waypoint.unit();
            Mastery standing = byUnit.get(unit.id());
            MasteryState state = stateOf(standing);
            boolean met = state.atLeast(READY_BAR);
            if (met) {
                ready++;
            } else {
                gaps.add(new CareerViews.Gap(
                        unit.id(),
                        unit.slug(),
                        unit.title(),
                        state.name(),
                        READY_BAR.name(),
                        waypoint.requiredMasteryState(),
                        "/app/learn/" + unit.slug()));
            }
        }

        boolean readyForRole = !refs.isEmpty() && gaps.isEmpty();
        String honesty = readyForRole
                ? "Every required unit on this path is at least " + READY_BAR.name()
                        + " from demonstrated evidence. That is readiness on this path — not a job offer."
                : "Not job-ready on this path until every required unit reaches "
                        + READY_BAR.name()
                        + " from graded evidence. Self-reported experience does not count.";

        return new CareerViews.Gaps(
                path.slug(),
                path.title(),
                path.roleTarget(),
                READY_BAR.name(),
                refs.size(),
                ready,
                gaps.size(),
                readyForRole,
                honesty,
                gaps,
                "interview_coach",
                "Gaps shrink only when mastery events raise a unit to "
                        + READY_BAR.name()
                        + ". Onboarding claims never appear here as mastered.");
    }

    private PathDetail resolvePath(UUID userId, String pathIdOrSlug) {
        if (pathIdOrSlug != null && !pathIdOrSlug.isBlank()) {
            return curriculum.path(pathIdOrSlug);
        }
        return profiles
                .targetPath(userId)
                .map(PathSummary::slug)
                .map(curriculum::path)
                .orElseThrow(() -> ApiException.notFound(
                        "No target path set. Finish onboarding or pass ?path= to choose one."));
    }

    private static MasteryState stateOf(Mastery mastery) {
        if (mastery == null || mastery.state() == null) {
            return MasteryState.UNKNOWN;
        }
        try {
            return MasteryState.valueOf(mastery.state());
        } catch (IllegalArgumentException unknown) {
            return MasteryState.UNKNOWN;
        }
    }
}
