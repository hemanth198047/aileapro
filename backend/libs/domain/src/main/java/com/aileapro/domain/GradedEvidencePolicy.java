package com.aileapro.domain;

/**
 * What graded work is worth, as a pure function.
 *
 * <p>{@link MasteryPolicy} handles reading and cannot raise anything but the Read chip. This
 * class is its counterpart: it is the only place where Practice and Assess can move, and the
 * only place that can take a unit past {@link MasteryState#INTRODUCED}. Keeping them apart
 * means neither can accidentally acquire the other's power.
 *
 * <p>The ladder in 07-MASTERY_AND_ADAPTIVE_LEARNING is a table of evidence, not a scale of
 * numbers, so each rung here is a named gate:
 *
 * <ul>
 *   <li><strong>FAMILIAR</strong> — easy items answered correctly. Recall, nothing more.
 *   <li><strong>UNDERSTOOD</strong> — items above recall answered correctly, and no
 *       misconception left open on the unit. Someone who can recite the rule and still holds
 *       a named false belief about it does not understand it.
 *   <li><strong>PRACTICED</strong> — the authored practice set essentially completed.
 *   <li><strong>PROFICIENT</strong> — a cold assessment passed, on more than one day. One
 *       good afternoon is not consistency.
 *   <li><strong>MASTERED</strong> — application plus a retention check after a real interval.
 *       Nothing can currently fill the Apply chip, so this rung is unreachable by design
 *       rather than by omission: "never jump to MASTERED from a single quiz."
 * </ul>
 */
public final class GradedEvidencePolicy {

    private GradedEvidencePolicy() {
    }

    /** Fewer correct answers than this cannot establish recall, however good the ratio. */
    public static final int RECALL_MIN_CORRECT = 2;

    /** Accuracy needed on answered items before recall counts as demonstrated. */
    public static final double RECALL_ACCURACY = 0.70;

    /** Share of the unit's authored practice items that must be right for a solid chip. */
    public static final double PRACTICE_SOLID_FRACTION = 0.80;

    /** Distinct days carrying graded evidence before consistency counts (07, w2). */
    public static final int CONSISTENCY_DAYS = 2;

    /** Successful spaced recalls, on or after the due date, for a retention check. */
    public static final int RETENTION_MIN_PASSES = 2;

    /** How fast the moving accuracy follows new evidence. */
    private static final double ACCURACY_ALPHA = 0.40;

    /**
     * What the rules need to know about the unit's authored assessment content.
     *
     * @param practiceItemCount   published practice items authored for this unit
     * @param assessItemCount     published quiz items authored for this unit
     * @param hasUnderstandItems  whether any authored item asks for more than recall, which
     *                            decides if UNDERSTOOD is reachable on this unit at all
     */
    public record UnitFacts(int practiceItemCount, int assessItemCount, boolean hasUnderstandItems) {

        public static UnitFacts none() {
            return new UnitFacts(0, 0, false);
        }
    }

    /**
     * The graded half of the projection: counts, not a history.
     *
     * @param assessBestFraction best submitted fraction on a cold assessment, null if none
     * @param understandCorrect  correct answers on items above recall level
     * @param gradedDays         distinct days on which anything was graded
     * @param openMisconceptions named false beliefs currently held open on this unit
     * @param reviewsDuePassed   cards recalled correctly on or after their due date
     */
    public record Graded(
            int practiceCorrect,
            int practiceAnswered,
            int assessCorrect,
            int assessAnswered,
            Double assessBestFraction,
            int assessmentsPassed,
            int understandCorrect,
            int gradedDays,
            int openMisconceptions,
            int reviewsDuePassed,
            int reviewsDueFailed,
            Double accuracyEma) {

        public static Graded empty() {
            return new Graded(0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, null);
        }

        public int totalCorrect() {
            return practiceCorrect + assessCorrect;
        }

        public int totalAnswered() {
            return practiceAnswered + assessAnswered;
        }
    }

    /**
     * One marked item, as the projection needs it.
     *
     * @param fraction  share of the item's points earned, so partial credit is not rounded
     *                  up into a right answer
     * @param marked    false for an item that could not be marked, which changes nothing
     */
    public record Marked(
            AssessmentKind kind, BloomLevel bloom, short difficulty, double fraction, boolean correct, boolean marked) {
    }

    /** Folds one marked item into the graded counts. Unmarked items change nothing. */
    public static Graded apply(Graded current, Marked item, boolean onANewDay) {
        if (!item.marked()) {
            return current;
        }

        int practiceCorrect = current.practiceCorrect();
        int practiceAnswered = current.practiceAnswered();
        int assessCorrect = current.assessCorrect();
        int assessAnswered = current.assessAnswered();

        EvidenceChip chip = item.kind().chip();
        if (chip == EvidenceChip.PRACTICE) {
            practiceAnswered++;
            if (item.correct()) {
                practiceCorrect++;
            }
        } else if (chip == EvidenceChip.ASSESS) {
            assessAnswered++;
            if (item.correct()) {
                assessCorrect++;
            }
        } else {
            // The diagnostic grades nothing. It is recorded as history and used to choose a
            // starting point, and it must not appear in any count that feeds a chip.
            return current;
        }

        int understandCorrect = current.understandCorrect();
        if (item.correct() && !item.bloom().isRecallOnly()) {
            understandCorrect++;
        }

        return new Graded(
                practiceCorrect,
                practiceAnswered,
                assessCorrect,
                assessAnswered,
                current.assessBestFraction(),
                current.assessmentsPassed(),
                understandCorrect,
                current.gradedDays() + (onANewDay ? 1 : 0),
                current.openMisconceptions(),
                current.reviewsDuePassed(),
                current.reviewsDueFailed(),
                nextAccuracy(current.accuracyEma(), item));
    }

    /**
     * Difficulty-weighted moving accuracy: the P0 stand-in for item response theory.
     *
     * <p>Each item carries an expected pass rate for its difficulty. The observation is
     * scored against that expectation, so getting a hard item right moves the estimate up
     * more than getting a definition right, and missing an easy item moves it down more than
     * missing a transfer item. It is crude, and it is honest about being crude: a real model
     * needs item statistics across many learners, which do not exist yet.
     */
    private static double nextAccuracy(Double current, Marked item) {
        double expected = expectedPassRate(item.difficulty());
        double observed = item.fraction();
        // Centre on 0.5 and let the surprise move it: performing exactly as expected for
        // this difficulty is average, not excellent.
        double signal = clamp(0.5 + (observed - expected) / 2.0);
        if (current == null) {
            return signal;
        }
        return clamp(current + ACCURACY_ALPHA * (signal - current));
    }

    /** Authored priors, not measured ones, and replaced as soon as there is data. */
    private static double expectedPassRate(short difficulty) {
        return switch (difficulty) {
            case 1 -> 0.90;
            case 2 -> 0.80;
            case 3 -> 0.65;
            case 4 -> 0.50;
            default -> 0.35;
        };
    }

    /** Records a submitted assessment: the best cold fraction and whether it passed. */
    public static Graded applySubmission(Graded current, AssessmentKind kind, double fraction, boolean passed) {
        if (kind.chip() != EvidenceChip.ASSESS) {
            return current;
        }
        Double best = current.assessBestFraction() == null
                ? fraction
                : Math.max(current.assessBestFraction(), fraction);
        return new Graded(
                current.practiceCorrect(),
                current.practiceAnswered(),
                current.assessCorrect(),
                current.assessAnswered(),
                best,
                current.assessmentsPassed() + (passed ? 1 : 0),
                current.understandCorrect(),
                current.gradedDays(),
                current.openMisconceptions(),
                current.reviewsDuePassed(),
                current.reviewsDueFailed(),
                current.accuracyEma());
    }

    /**
     * Records a graded card. Only a card answered on or after its due date says anything
     * about retention; answering one early is re-reading with extra steps.
     */
    public static Graded applyReview(Graded current, boolean wasDue, boolean passed) {
        if (!wasDue) {
            return current;
        }
        return new Graded(
                current.practiceCorrect(),
                current.practiceAnswered(),
                current.assessCorrect(),
                current.assessAnswered(),
                current.assessBestFraction(),
                current.assessmentsPassed(),
                current.understandCorrect(),
                current.gradedDays(),
                current.openMisconceptions(),
                current.reviewsDuePassed() + (passed ? 1 : 0),
                current.reviewsDueFailed() + (passed ? 0 : 1),
                current.accuracyEma());
    }

    public static Graded withOpenMisconceptions(Graded current, int open) {
        return new Graded(
                current.practiceCorrect(),
                current.practiceAnswered(),
                current.assessCorrect(),
                current.assessAnswered(),
                current.assessBestFraction(),
                current.assessmentsPassed(),
                current.understandCorrect(),
                current.gradedDays(),
                Math.max(0, open),
                current.reviewsDuePassed(),
                current.reviewsDueFailed(),
                current.accuracyEma());
    }

    /**
     * The Practice chip. Solid means the authored set was essentially completed correctly,
     * measured against what the author wrote rather than against how much the learner chose
     * to attempt — otherwise answering one item of twelve would fill the chip.
     */
    public static EvidenceState practiceState(Graded graded, UnitFacts facts) {
        if (graded.practiceAnswered() == 0) {
            return EvidenceState.EMPTY;
        }
        if (graded.practiceCorrect() == 0) {
            return EvidenceState.PARTIAL;
        }
        int target = facts.practiceItemCount() > 0
                ? (int) Math.ceil(facts.practiceItemCount() * PRACTICE_SOLID_FRACTION)
                : Math.max(RECALL_MIN_CORRECT, graded.practiceAnswered());
        return graded.practiceCorrect() >= target ? EvidenceState.SOLID : EvidenceState.PARTIAL;
    }

    /**
     * The Assess chip. Solid requires a submitted assessment at or above its own pass mark;
     * attempting one and falling short is real evidence of something, but it is not this.
     */
    public static EvidenceState assessState(Graded graded) {
        if (graded.assessAnswered() == 0 && graded.assessmentsPassed() == 0) {
            return EvidenceState.EMPTY;
        }
        return graded.assessmentsPassed() > 0 ? EvidenceState.SOLID : EvidenceState.PARTIAL;
    }

    /** True when enough items were answered correctly, at a good enough rate, to claim recall. */
    public static boolean recallDemonstrated(Graded graded) {
        if (graded.totalCorrect() < RECALL_MIN_CORRECT || graded.totalAnswered() == 0) {
            return false;
        }
        return graded.totalCorrect() / (double) graded.totalAnswered() >= RECALL_ACCURACY;
    }

    /**
     * The ladder, climbed one rung at a time.
     *
     * <p>Each rung requires every rung below it, which is the property that makes the state
     * mean something. Without it a unit whose authored practice set is all definitions could
     * reach PRACTICED without anyone ever having shown they understood the material — the
     * learner would have completed the exercises, and the exercises would not have asked.
     *
     * <p>The consequence is deliberate: on a unit where no item asks for more than recall,
     * the ladder stops at FAMILIAR no matter how much work is done. That is a gap in the
     * content, {@link #reachableCeiling} names it, and the UI reports it as the unit's
     * ceiling rather than as the learner falling short.
     */
    public static MasteryState stateFor(Evidence evidence, Graded graded, UnitFacts facts) {
        boolean clear = graded.openMisconceptions() == 0;

        if (evidence.read() != EvidenceState.SOLID) {
            return MasteryState.UNKNOWN;
        }
        if (!recallDemonstrated(graded)) {
            return MasteryState.INTRODUCED;
        }
        // A named false belief holds the state at recall. Someone can repeat the rule and
        // still be wrong about what it means, and promoting them would bake that in.
        if (!clear || !facts.hasUnderstandItems() || graded.understandCorrect() < RECALL_MIN_CORRECT) {
            return MasteryState.FAMILIAR;
        }
        if (evidence.practice() != EvidenceState.SOLID) {
            return MasteryState.UNDERSTOOD;
        }
        if (evidence.assess() != EvidenceState.SOLID || graded.gradedDays() < CONSISTENCY_DAYS) {
            return MasteryState.PRACTICED;
        }
        if (evidence.apply() != EvidenceState.SOLID || graded.reviewsDuePassed() < RETENTION_MIN_PASSES) {
            return MasteryState.PROFICIENT;
        }
        return MasteryState.MASTERED;
    }

    /**
     * The single requirement standing between the learner and the next rung.
     *
     * <p>The ladder is only useful if a learner can see what the next step asks of them. This
     * returns the first unmet gate rather than a list, because the gates are ordered and
     * naming all of them at once invites working on the fourth while the first is unmet.
     */
    public enum Gate {
        READ_THE_UNIT,
        DEMONSTRATE_RECALL,
        CLEAR_A_MISCONCEPTION,
        ANSWER_ABOVE_RECALL,
        COMPLETE_THE_PRACTICE_SET,
        PASS_THE_QUIZ,
        RETURN_ON_ANOTHER_DAY,
        BUILD_SOMETHING,
        PASS_A_SPACED_REVIEW,
        /** Nothing is outstanding: the learner is at the top of the ladder. */
        NONE,
        /** The content, not the learner, is what stops here. */
        CONTENT_CEILING
    }

    public static Gate nextGate(Evidence evidence, Graded graded, UnitFacts facts) {
        MasteryState state = stateFor(evidence, graded, facts);
        if (state == MasteryState.MASTERED) {
            return Gate.NONE;
        }
        if (state.atLeast(reachableCeiling(facts))) {
            return Gate.CONTENT_CEILING;
        }
        if (evidence.read() != EvidenceState.SOLID) {
            return Gate.READ_THE_UNIT;
        }
        if (!recallDemonstrated(graded)) {
            return Gate.DEMONSTRATE_RECALL;
        }
        if (graded.openMisconceptions() > 0) {
            return Gate.CLEAR_A_MISCONCEPTION;
        }
        if (graded.understandCorrect() < RECALL_MIN_CORRECT) {
            return Gate.ANSWER_ABOVE_RECALL;
        }
        if (evidence.practice() != EvidenceState.SOLID) {
            return Gate.COMPLETE_THE_PRACTICE_SET;
        }
        if (evidence.assess() != EvidenceState.SOLID) {
            return Gate.PASS_THE_QUIZ;
        }
        if (graded.gradedDays() < CONSISTENCY_DAYS) {
            return Gate.RETURN_ON_ANOTHER_DAY;
        }
        if (evidence.apply() != EvidenceState.SOLID) {
            return Gate.BUILD_SOMETHING;
        }
        return Gate.PASS_A_SPACED_REVIEW;
    }

    /**
     * The highest state this unit's authored content can currently support. A unit with no
     * practice items cannot show PRACTICED however good the learner is, and saying so is the
     * difference between an honest ceiling and an unexplained wall.
     */
    public static MasteryState reachableCeiling(UnitFacts facts) {
        if (!facts.hasUnderstandItems()) {
            return facts.practiceItemCount() + facts.assessItemCount() > 0
                    ? MasteryState.FAMILIAR
                    : MasteryState.INTRODUCED;
        }
        if (facts.practiceItemCount() == 0) {
            return MasteryState.UNDERSTOOD;
        }
        if (facts.assessItemCount() == 0) {
            return MasteryState.PRACTICED;
        }
        // Apply is unfillable for now, so PROFICIENT is the top of the product, not of the
        // ladder. MASTERED waits for the lab and projects.
        return MasteryState.PROFICIENT;
    }

    /**
     * The four chips together. Read is passed through from the reading policy untouched, and
     * Apply stays empty because nothing in the product can fill it yet.
     */
    public static Evidence evidenceFor(Evidence current, Graded graded, UnitFacts facts) {
        return new Evidence(
                current.read(),
                practiceState(graded, facts),
                assessState(graded),
                current.apply());
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
