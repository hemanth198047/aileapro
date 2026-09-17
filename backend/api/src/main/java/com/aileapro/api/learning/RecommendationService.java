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
 * the rules need, including the graded ones it reads through {@link AssessmentSignals} rather
 * than by importing the assessment engine.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final CurriculumQueryService curriculum;
    private final GraphQueryService graph;
    private final MasteryService mastery;
    private final LearnerProfileService profiles;
    private final AssessmentSignals signals;

    public RecommendationService(
            CurriculumQueryService curriculum,
            GraphQueryService graph,
            MasteryService mastery,
            LearnerProfileService profiles,
            AssessmentSignals signals) {
        this.curriculum = curriculum;
        this.graph = graph;
        this.mastery = mastery;
        this.profiles = profiles;
        this.signals = signals;
    }

    public NextAction next(UUID userId) {
        boolean onboarded = profiles.profile(userId).map(p -> p.onboardedAt() != null).orElse(false);
        if (!onboarded) {
            return NextActionPolicy.onboarding();
        }

        // The two highest rules do not depend on a path at all: decaying recall and a held
        // false belief are worth fixing whether or not the learner has picked a destination.
        Optional<NextAction> maintenance = maintenance(userId);
        if (maintenance.isPresent()) {
            return maintenance.get();
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
        AssessmentSignals.DueReviews reviews = signals.dueReviews(userId);
        List<NextAction> reviewActions =
                reviews.dueCount() == 0 ? List.of() : List.of(NextActionPolicy.review(reviews));

        if (target.isEmpty()) {
            return new Today(
                    onboarded ? NextActionPolicy.emptyPath("the catalog") : NextActionPolicy.onboarding(),
                    null, null, 0, 0, 0, reviewActions, reviewsNote(userId, reviews), onboarded);
        }

        PathDetail path = curriculum.path(target.get().slug());
        List<Stop> stops = stops(userId, path);
        NextAction next = onboarded ? next(userId) : NextActionPolicy.onboarding();

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
                reviewActions,
                reviewsNote(userId, reviews),
                onboarded);
    }

    /** Overdue recall first, then a held false belief. Both outrank anything new. */
    private Optional<NextAction> maintenance(UUID userId) {
        AssessmentSignals.DueReviews reviews = signals.dueReviews(userId);
        if (reviews.dueCount() > 0) {
            return Optional.of(NextActionPolicy.review(reviews));
        }
        return signals.openMisconceptions(userId).stream()
                .findFirst()
                .map(NextActionPolicy::clearMisconception);
    }

    private String reviewsNote(UUID userId, AssessmentSignals.DueReviews reviews) {
        if (reviews.dueCount() > 0) {
            return "";
        }
        return "Nothing is due. Cards are enrolled when you finish reading a unit, and answering one early does"
                + " not count towards retention — that would make remembering something and seeing it again the"
                + " same fact.";
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

        Optional<Stop> unread = NextActionPolicy.currentStop(stops);
        if (unread.isPresent()) {
            Stop stop = unread.get();
            // Only the blocked waypoint's own chain is queried: one walk, and it is the only
            // one that can change the answer.
            List<Prereq> required = prerequisitesOf(userId, stop.unit());
            Optional<Prereq> blocking = NextActionPolicy.blockingPrerequisite(required);
            return blocking
                    .map(prereq -> NextActionPolicy.readPrerequisite(prereq, stop.unit()))
                    .orElseGet(() -> NextActionPolicy.readWaypoint(stop, path.title(), !required.isEmpty()));
        }

        // Everything is read, which is where Phase 5 stopped. Graded work takes over: the
        // first waypoint whose practice or assessment is outstanding, in path order.
        for (Stop stop : stops) {
            Optional<AssessmentSignals.GradedOffer> offered = signals.offerFor(userId, stop.unit().id());
            if (offered.isEmpty()) {
                continue;
            }
            AssessmentSignals.GradedOffer offer = offered.get();
            if (offer.practiceSlug() != null && !NextActionPolicy.practiceFinished(stop.mastery())) {
                return NextActionPolicy.practise(stop, offer);
            }
            if (offer.quizSlug() != null && !NextActionPolicy.assessFinished(stop.mastery())) {
                return NextActionPolicy.assess(stop, offer);
            }
            if (!stateOf(stop.mastery()).atLeast(MasteryState.valueOf(offer.ceiling()))) {
                continue;
            }
            return NextActionPolicy.ceiling(stop, offer, path.title());
        }
        return NextActionPolicy.readingExhausted(path.title());
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
