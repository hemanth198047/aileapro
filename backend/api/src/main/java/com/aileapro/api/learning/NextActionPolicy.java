package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.LearningViews.NextAction;

import java.util.List;
import java.util.Optional;

/**
 * Which one thing to do next, as a pure function over the learner's standing.
 *
 * <p>The priority order comes from 07-MASTERY_AND_ADAPTIVE_LEARNING:
 *
 * <ol>
 *   <li><strong>Overdue reviews.</strong> Knowledge already paid for is decaying, and
 *       recovering it is cheaper than acquiring anything new.
 *   <li><strong>Open misconceptions.</strong> A named false belief actively corrupts
 *       whatever is studied next, so new material on top of one is wasted.
 *   <li><strong>Unmet prerequisites.</strong> The current waypoint is unreachable until
 *       these are cleared.
 *   <li><strong>Reading the current waypoint.</strong>
 *   <li><strong>Practice on what has been read.</strong> Reading fills one chip and
 *       nothing else, so this is where most learners actually get stuck.
 *   <li><strong>A cold assessment.</strong>
 *   <li><strong>The ceiling.</strong> Said out loud, with the reason.
 * </ol>
 *
 * <p>"Done for now" here means <em>read as far as reading counts</em>, not understood. A unit
 * whose waypoint demands UNDERSTOOD cannot reach it by reading, so the policy stops asking
 * the learner to re-read it and offers the graded work instead.
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
    public static final String RULE_OVERDUE_REVIEW = "overdue_review";
    public static final String RULE_OPEN_MISCONCEPTION = "open_misconception";
    public static final String RULE_PREREQUISITE_GAP = "prerequisite_gap";
    public static final String RULE_CURRENT_WAYPOINT = "current_waypoint";
    public static final String RULE_PRACTICE_DUE = "practice_due";
    public static final String RULE_ASSESS_DUE = "assess_due";
    public static final String RULE_CONTENT_CEILING = "content_ceiling";
    public static final String RULE_READING_EXHAUSTED = "reading_exhausted";
    public static final String RULE_EMPTY_PATH = "empty_path";

    /** Reading is finished on a unit once its Read chip is solid. */
    public static boolean readingFinished(Mastery mastery) {
        return "solid".equals(mastery.evidence().read().state());
    }

    public static boolean practiceFinished(Mastery mastery) {
        return "solid".equals(mastery.evidence().practice().state());
    }

    public static boolean assessFinished(Mastery mastery) {
        return "solid".equals(mastery.evidence().assess().state());
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
     * Overdue cards outrank everything. This is the one rule that is about not losing ground
     * rather than gaining it, and the arithmetic favours it: a card recovered in twenty
     * seconds is knowledge that would otherwise have to be relearned from the page.
     */
    public static NextAction review(AssessmentSignals.DueReviews reviews) {
        String reason = reviews.overdueCount() > 0
                ? "%d card%s overdue, the oldest by %d day%s. Recall that is slipping is cheaper to recover now "
                                .formatted(reviews.overdueCount(), reviews.overdueCount() == 1 ? " is" : "s are",
                                        reviews.daysOverdue(), reviews.daysOverdue() == 1 ? "" : "s")
                        + "than to relearn later."
                : "%d card%s due today. Spaced recall is the cheapest evidence in the system: it costs a minute "
                                .formatted(reviews.dueCount(), reviews.dueCount() == 1 ? " is" : "s are")
                        + "and it is the only thing that shows you still know something.";
        return new NextAction(
                "review",
                "Review",
                reviews.dueCount() + (reviews.dueCount() == 1 ? " card" : " cards"),
                Math.max(1, (int) Math.ceil(reviews.dueCount() * 0.4)),
                reason,
                RULE_OVERDUE_REVIEW,
                "/app/review",
                null,
                null);
    }

    /**
     * A named false belief is cleared before new material goes on top of it. Not because it is
     * a failure, but because studying the next unit while holding it means encoding the next
     * unit wrong too.
     */
    public static NextAction clearMisconception(AssessmentSignals.OpenMisconception misconception) {
        return new NextAction(
                "reteach",
                "Clear up",
                misconception.claim(),
                8,
                "This belief came up in your answers, and it is on " + misconception.unitTitle()
                        + ". Anything built on top of it inherits the error, so it goes first. Read the block that"
                        + " addresses it, then answer the item that raised it — reading alone does not clear the flag.",
                RULE_OPEN_MISCONCEPTION,
                misconception.href(),
                null,
                null);
    }

    /**
     * Reading is finished and the practice set is not. This is the most common real position a
     * learner is in, and the reason says why re-reading is not the answer.
     */
    public static NextAction practise(Stop stop, AssessmentSignals.GradedOffer offer) {
        boolean started = "partial".equals(stop.mastery().evidence().practice().state());
        return new NextAction(
                "practice",
                started ? "Finish practising" : "Practise",
                stop.unit().title(),
                Math.max(3, offer.practiceItemCount() * 2),
                started
                        ? "You started the practice set on this unit and stopped partway. The chip fills when most "
                                + "of it is right, not when it has been opened."
                        : "You have read it, which fills the Read chip and nothing else. The graded practice set is "
                                + "what moves this past INTRODUCED — reading it again will not.",
                RULE_PRACTICE_DUE,
                "/app/learn/" + stop.unit().slug() + "#practice",
                stop.unit(),
                stop.mastery());
    }

    /** Practice is solid and the quiz has not been passed cold. */
    public static NextAction assess(Stop stop, AssessmentSignals.GradedOffer offer) {
        boolean attempted = "partial".equals(stop.mastery().evidence().assess().state());
        return new NextAction(
                "assess",
                attempted ? "Retake the quiz on" : "Take the quiz on",
                stop.unit().title(),
                Math.max(5, offer.quizItemCount() * 2),
                attempted
                        ? "You have attempted this quiz without reaching the pass mark. The practice set is solid, "
                                + "so the gap is between practising with feedback and answering cold."
                        : "Your practice on this unit is solid. A quiz taken without notes is the difference "
                                + "between PRACTICED and evidence that it stuck.",
                RULE_ASSESS_DUE,
                "/app/quiz/" + offer.quizSlug(),
                stop.unit(),
                stop.mastery());
    }

    /**
     * The learner has done everything this unit's content can measure. Naming the content's
     * limit is the difference between an honest ceiling and an unexplained wall — the learner
     * has not fallen short of anything.
     */
    public static NextAction ceiling(Stop stop, AssessmentSignals.GradedOffer offer, String pathTitle) {
        return new NextAction(
                "blocked",
                "Nothing more on",
                stop.unit().title(),
                null,
                offer.ceilingReason().isBlank()
                        ? "You are at " + stop.mastery().state() + " on this unit, which is as far as its authored "
                                + "content can currently evidence."
                        : offer.ceilingReason(),
                RULE_CONTENT_CEILING,
                "/app/learn/" + stop.unit().slug(),
                stop.unit(),
                stop.mastery());
    }

    /**
     * Every waypoint has been read and nothing graded is authored for any of them. The honest
     * answer is that the content is missing, not that the learner is finished.
     */
    public static NextAction readingExhausted(String pathTitle) {
        return new NextAction(
                "blocked",
                "Wait for",
                "graded practice and assessment",
                null,
                "You have read every waypoint on " + pathTitle
                        + " as far as reading counts as evidence, and none of them has a practice set or quiz"
                        + " authored yet. Moving past INTRODUCED needs work you produce.",
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
