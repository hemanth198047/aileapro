package com.aileapro.api.assessment;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Mastery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Wire shapes for assessments, attempts, reports, and reviews.
 *
 * <p>One rule governs all of them: an item being served never carries its answer key. The
 * key appears in {@link Marked} only after the answer is recorded, which is what makes the
 * feedback a teaching moment rather than a spoiler.
 */
public final class AssessmentViews {

    private AssessmentViews() {
    }

    /** An assessment as offered, without its items. */
    public record AssessmentSummary(
            UUID id,
            String slug,
            String title,
            String kind,
            String description,
            Integer timeLimitSeconds,
            double passMark,
            int itemCount,
            UnitRef unit,
            UUID openAttemptId) {
    }

    /**
     * What is authored for one unit, for the practice dock and the quiz button.
     *
     * @param ceiling       highest state this unit's content can support, named rather than
     *                      left as an unexplained wall
     * @param ceilingReason why the ceiling is where it is
     * @param stateNote     the one thing standing between the learner and their next rung
     */
    public record UnitAssessments(
            UnitRef unit,
            AssessmentSummary practice,
            AssessmentSummary quiz,
            String ceiling,
            String ceilingReason,
            String stateNote,
            Mastery mastery) {
    }

    /** One choice, as shown. Correctness is absent by construction, not by omission. */
    public record Choice(String id, String text) {
    }

    /**
     * An item as served.
     *
     * @param sort     position within the attempt, so a client can resume in order
     * @param answered true once an answer has been recorded, which makes it read-only
     */
    public record ServedItem(
            UUID id,
            String type,
            String stem,
            List<Choice> choices,
            short difficulty,
            String bloom,
            Short estimateSeconds,
            int sort,
            boolean answered,
            UnitRef unit) {
    }

    /**
     * The result of marking one item, returned the instant it is answered.
     *
     * @param correct     null for an item the system cannot mark, which is not the same as wrong
     * @param unverified  true when marking was not possible and the learner must judge it
     * @param explanation the teaching, shown whether right or wrong
     * @param raised      misconception codes this answer raised
     */
    public record Marked(
            UUID itemId,
            Boolean correct,
            boolean unverified,
            double fraction,
            List<String> correctIds,
            String explanation,
            List<Misconception> raised,
            int answeredCount,
            int servedCount,
            boolean complete) {
    }

    /** A named false belief, with the page that teaches against it. */
    public record Misconception(String code, String claim, UnitRef unit, String href) {
    }

    /**
     * An attempt in progress.
     *
     * @param nextItem   the next item to answer, absent once the attempt is complete
     * @param remaining  how many more items this attempt will serve, null when adaptive
     */
    public record AttemptState(
            UUID id,
            String kind,
            String status,
            String assessmentSlug,
            String assessmentTitle,
            UnitRef unit,
            Instant startedAt,
            Integer timeLimitSeconds,
            Integer secondsRemaining,
            int answeredCount,
            int servedCount,
            Integer remaining,
            ServedItem nextItem) {
    }

    /** One line of a report's narrative sections. */
    public record ReportLine(String label, String detail, String href) {
    }

    /**
     * The report for a submitted attempt.
     *
     * <p>Every narrative field is populated. A report that is only a score is not a report,
     * and this record has no shape that could express one.
     */
    public record Report(
            UUID attemptId,
            String kind,
            String assessmentTitle,
            UnitRef unit,
            Instant submittedAt,
            double fraction,
            double passMark,
            Boolean passed,
            int answered,
            int correct,
            int unmarked,
            String summary,
            String readiness,
            String readinessReason,
            List<ReportLine> strengths,
            List<ReportLine> weaknesses,
            List<Misconception> misconceptions,
            List<ReportLine> missingPrereqs,
            List<ReportLine> revision,
            List<ReportLine> nextSteps,
            String masteryState,
            String masteryNote) {
    }

    /** A card waiting to be reviewed. The back is withheld until the learner commits. */
    public record DueCard(
            UUID scheduleId,
            UUID cardId,
            String front,
            UnitRef unit,
            LocalDate due,
            int daysOverdue,
            int intervalDays,
            int reps,
            int lapses) {
    }

    public record ReviewQueue(
            int dueCount, int overdueCount, int leechCount, List<DueCard> cards, String note) {
    }

    /** What one graded card did to the schedule, shown so the interval is never a mystery. */
    public record ReviewResult(
            UUID scheduleId,
            UUID cardId,
            String back,
            int grade,
            boolean passed,
            boolean wasDue,
            int intervalBefore,
            int intervalAfter,
            LocalDate nextDue,
            double ease,
            boolean leech,
            String note,
            int remaining) {
    }

    /** The diagnostic's placement guess for one domain. */
    public record Prior(
            UnitRef domain, int itemsServed, int itemsCorrect, double estimate, String confidence, String reason) {
    }

    public record PlanLine(String kind, UnitRef unit, String reason, LocalDate due, String href) {
    }

    public record StudyPlan(
            UUID id,
            int version,
            String horizon,
            String reason,
            String source,
            Instant generatedAt,
            List<Prior> priors,
            List<PlanLine> items) {
    }
}
