package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PathDetail;
import com.aileapro.api.curriculum.CurriculumViews.PathSummary;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.graph.GraphQueryService;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.LearningViews.NextAction;
import com.aileapro.api.learning.LearningViews.Roadmap;
import com.aileapro.api.learning.LearningViews.RoadmapStop;
import com.aileapro.api.learning.LearningViews.Today;
import com.aileapro.api.learning.NextActionPolicy.Prereq;
import com.aileapro.api.learning.NextActionPolicy.Stop;
import com.aileapro.domain.MasteryState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns the learner's evidence into one recommendation and the Today screen around it.
 *
 * <p>The ranking itself lives in {@link NextActionPolicy}; this class only gathers the facts
 * the rules need. Reviews are absent rather than faked: there is no schedule until the
 * assessment engine exists, and {@link Today#reviewsNote()} says as much instead of showing
 * an empty queue with no explanation.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final String REVIEWS_NOTE =
            "Spaced review needs graded items to schedule from, so nothing is due yet. "
                    + "The queue starts filling once the assessment engine lands.";

    private final CurriculumQueryService curriculum;
    private final GraphQueryService graph;
    private final MasteryService mastery;
    private final LearnerProfileService profiles;

    public RecommendationService(
            CurriculumQueryService curriculum,
            GraphQueryService graph,
            MasteryService mastery,
            LearnerProfileService profiles) {
        this.curriculum = curriculum;
        this.graph = graph;
        this.mastery = mastery;
        this.profiles = profiles;
    }

    public NextAction next(UUID userId) {
        boolean onboarded = profiles.profile(userId).map(p -> p.onboardedAt() != null).orElse(false);
        if (!onboarded) {
            return NextActionPolicy.onboarding();
        }

        Optional<PathSummary> target = profiles.targetPath(userId);
        if (target.isEmpty()) {
            return NextActionPolicy.emptyPath("the catalog");
        }
        return nextOnPath(userId, curriculum.path(target.get().slug()));
    }

    public Today today(UUID userId) {
        boolean onboarded = profiles.profile(userId).map(p -> p.onboardedAt() != null).orElse(false);
        Optional<PathSummary> target = profiles.targetPath(userId);

        if (target.isEmpty()) {
            return new Today(
                    onboarded ? NextActionPolicy.emptyPath("the catalog") : NextActionPolicy.onboarding(),
                    null, null, 0, 0, 0, List.of(), REVIEWS_NOTE, onboarded);
        }

        PathDetail path = curriculum.path(target.get().slug());
        List<Stop> stops = stops(userId, path);
        NextAction next = onboarded ? nextOnPath(userId, path) : NextActionPolicy.onboarding();

        int started = (int) stops.stream().filter(stop -> stop.mastery().openedCount() > 0).count();
        int introduced = (int) stops.stream()
                .filter(stop -> stateOf(stop.mastery()).atLeast(MasteryState.INTRODUCED))
                .count();

        return new Today(
                next,
                path.slug(),
                path.title(),
                stops.size(),
                started,
                introduced,
                List.of(),
                REVIEWS_NOTE,
                onboarded);
    }

    /** The path waypoints with the learner's evidence laid over them. */
    public Roadmap roadmap(UUID userId, String pathIdOrSlug) {
        PathDetail path = pathIdOrSlug != null && !pathIdOrSlug.isBlank()
                ? curriculum.path(pathIdOrSlug)
                : profiles.targetPath(userId)
                        .map(summary -> curriculum.path(summary.slug()))
                        .orElseThrow(() -> com.aileapro.api.platform.ApiException.notFound(
                                "No published paths to build a roadmap from"));

        List<RoadmapStop> stops = new ArrayList<>();
        for (Stop stop : stops(userId, path)) {
            stops.add(new RoadmapStop(
                    stop.sort(),
                    stop.requiredMasteryState(),
                    requirementMet(stop),
                    stop.unit(),
                    stop.mastery()));
        }
        return new Roadmap(path.slug(), path.title(), path.version(), path.estimateMinutes(), stops);
    }

    private NextAction nextOnPath(UUID userId, PathDetail path) {
        List<Stop> stops = stops(userId, path);
        if (stops.isEmpty()) {
            return NextActionPolicy.emptyPath(path.title());
        }

        Optional<Stop> current = NextActionPolicy.currentStop(stops);
        if (current.isEmpty()) {
            return NextActionPolicy.readingExhausted(path.title());
        }

        Stop stop = current.get();
        // Only the blocked waypoint's own chain is queried: one walk, and it is the only one
        // that can change the answer.
        List<Prereq> required = prerequisitesOf(userId, stop.unit());
        Optional<Prereq> blocking = NextActionPolicy.blockingPrerequisite(required);
        return blocking
                .map(prereq -> NextActionPolicy.readPrerequisite(prereq, stop.unit()))
                .orElseGet(() -> NextActionPolicy.readWaypoint(stop, path.title(), !required.isEmpty()));
    }

    private List<Stop> stops(UUID userId, PathDetail path) {
        List<UnitRef> refs = path.waypoints().stream().map(waypoint -> waypoint.unit()).toList();
        Map<UUID, Mastery> byUnit = mastery.masteryFor(userId, refs);
        return path.waypoints().stream()
                .map(waypoint -> new Stop(
                        waypoint.sort(),
                        waypoint.requiredMasteryState(),
                        waypoint.unit(),
                        byUnit.get(waypoint.unit().id())))
                .toList();
    }

    /** Required prerequisites in study order — deepest first, which is where to start. */
    private List<Prereq> prerequisitesOf(UUID userId, UnitRef unit) {
        var steps = graph.prerequisites(unit.slug()).required();
        if (steps.isEmpty()) {
            return List.of();
        }
        List<UnitRef> refs = steps.stream().map(step -> step.unit()).toList();
        Map<UUID, Mastery> byUnit = mastery.masteryFor(userId, refs);
        return refs.stream().map(ref -> new Prereq(ref, byUnit.get(ref.id()))).toList();
    }

    private static boolean requirementMet(Stop stop) {
        MasteryState required;
        try {
            required = MasteryState.valueOf(stop.requiredMasteryState());
        } catch (IllegalArgumentException unknown) {
            // An unreadable requirement is treated as unmet: the safe direction is "not yet".
            return false;
        }
        return stateOf(stop.mastery()).atLeast(required);
    }

    private static MasteryState stateOf(Mastery mastery) {
        return MasteryState.valueOf(mastery.state());
    }
}
