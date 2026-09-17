package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.LearningViews.NextAction;

import java.util.List;
import java.util.Optional;

/**
 * Which one thing to do next, as a pure function over the learner's standing.
 *
 * <p>The priority order comes from 07-MASTERY_AND_ADAPTIVE_LEARNING, minus the rules whose
 * inputs do not exist yet. Overdue spaced reviews rank above everything in the spec, but
 * there is no review schedule until the assessment engine lands, so the highest rule that
 * can currently fire is unblocking a prerequisite. That absence is stated in the reason text
 * rather than papered over with a fake review.
 *
 * <p>"Done for now" here means <em>read as far as reading counts</em>, not understood. A unit
 * whose waypoint demands UNDERSTOOD cannot reach it by reading, so the policy stops asking
 * the learner to re-read it and says plainly what the next real step would be.
 */
public final class NextActionPolicy {

    private NextActionPolicy() {
    }

    /** A path waypoint plus what the learner has shown on it. */
    public record Stop(int sort, String requiredMasteryState, UnitRef unit, Mastery mastery) {
    }

    /** A prerequisite of a stop, in study order: the earliest thing to study comes first. */
    public record Prereq(UnitRef unit, Mastery mastery) {
    }

    public static final String RULE_ONBOARDING = "onboarding";
    public static final String RULE_PREREQUISITE_GAP = "prerequisite_gap";
    public static final String RULE_CURRENT_WAYPOINT = "current_waypoint";
    public static final String RULE_READING_EXHAUSTED = "reading_exhausted";
    public static final String RULE_EMPTY_PATH = "empty_path";

    /** Reading is finished on a unit once its Read chip is solid. */
    public static boolean readingFinished(Mastery mastery) {
        return "solid".equals(mastery.evidence().read().state());
    }

    /** The waypoint the learner is actually on: the first one reading has not finished. */
    public static Optional<Stop> currentStop(List<Stop> stops) {
        return stops.stream().filter(stop -> !readingFinished(stop.mastery())).findFirst();
    }

    /** The earliest prerequisite that is still unread, which is what blocks the stop. */
    public static Optional<Prereq> blockingPrerequisite(List<Prereq> requiredInStudyOrder) {
        return requiredInStudyOrder.stream()
                .filter(prereq -> !readingFinished(prereq.mastery()))
                .findFirst();
    }

    /** Nothing can be recommended sensibly before we know what the learner is aiming at. */
    public static NextAction onboarding() {
        return new NextAction(
                "setup",
                "Choose",
                "your goal and weekly hours",
                3,
                "Recommendations are based on a path and the time you have, and neither is set yet.",
                RULE_ONBOARDING,
                "/app/onboarding",
                null,
                null);
    }

    public static NextAction readPrerequisite(Prereq prereq, UnitRef blockedUnit) {
        return read(
                prereq.unit(),
                prereq.mastery(),
                "It is a prerequisite of " + blockedUnit.title() + ", and you have not read it yet.",
                RULE_PREREQUISITE_GAP);
    }

    /**
     * @param prerequisitesCleared true when this waypoint had prerequisites and all of them are
     *                             read, which is worth saying; false when it had none to clear
     */
    public static NextAction readWaypoint(Stop stop, String pathTitle, boolean prerequisitesCleared) {
        String reason;
        if (stop.mastery().openedCount() > 0) {
            reason = "You started it and stopped partway, and it is the next waypoint on " + pathTitle + ".";
        } else if (prerequisitesCleared) {
            reason = "It is the next unread waypoint on " + pathTitle + ", and its prerequisites are read.";
        } else {
            reason = "It is the next unread waypoint on " + pathTitle + ", and nothing has to come before it.";
        }
        return read(stop.unit(), stop.mastery(), reason, RULE_CURRENT_WAYPOINT);
    }

    /**
     * Every waypoint has been read, which is the ceiling of what this phase can measure. The
     * honest next step is graded work, so the action says so instead of inventing one.
     */
    public static NextAction readingExhausted(String pathTitle) {
        return new NextAction(
                "blocked",
                "Wait for",
                "graded practice and assessment",
                null,
                "You have read every waypoint on " + pathTitle
                        + " as far as reading counts as evidence. Moving past INTRODUCED needs work you produce,"
                        + " which arrives with the assessment engine.",
                RULE_READING_EXHAUSTED,
                "/app/map",
                null,
                null);
    }

    public static NextAction emptyPath(String pathTitle) {
        return new NextAction(
                "blocked",
                "Wait for",
                "content on " + pathTitle,
                null,
                "This path has no published waypoints yet, so there is nothing to recommend.",
                RULE_EMPTY_PATH,
                "/app/learn",
                null,
                null);
    }

    private static NextAction read(UnitRef unit, Mastery mastery, String reason, String rule) {
        boolean resuming = mastery.openedCount() > 0;
        return new NextAction(
                "read",
                resuming ? "Finish reading" : "Read",
                unit.title(),
                minutes(unit, mastery),
                reason,
                rule,
                "/app/learn/" + unit.slug(),
                unit,
                mastery);
    }

    /**
     * Minutes for a fresh unit is the author's estimate. For one already started it is the
     * reading time still outstanding, so the number shrinks as the learner works instead of
     * repeating the same estimate every visit.
     */
    private static Integer minutes(UnitRef unit, Mastery mastery) {
        if (mastery.openedCount() == 0) {
            return unit.estimateMinutes();
        }
        int remaining = mastery.readFloorSeconds() - mastery.dwellSeconds();
        if (remaining <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(remaining / 60.0));
    }
}
