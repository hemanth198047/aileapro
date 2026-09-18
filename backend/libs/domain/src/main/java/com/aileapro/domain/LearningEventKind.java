package com.aileapro.domain;

/**
 * Things a learner can be observed doing on a unit.
 *
 * <p>The first five are consumption signals, which is exactly why none of them can fill
 * anything but the Read chip. The rest are production: an answer submitted, an attempt
 * marked, a card recalled after a gap. Those are the only events that can move a unit past
 * INTRODUCED, and they are handled by a different policy class for that reason.
 */
public enum LearningEventKind {

    /** The unit page was opened. On its own this is worth nothing. */
    UNIT_OPENED("unit_opened"),

    /** A visible-tab heartbeat carrying seconds since the last one. */
    DWELL("dwell"),

    /** An explanation level was selected and shown. */
    EXPLANATION_LEVEL("explanation_level"),

    /**
     * A self-check answer was revealed. Recorded because it is useful history, and
     * deliberately credited to nothing: revealing an answer demonstrates no recall.
     */
    SELF_CHECK_REVEALED("self_check_revealed"),

    /**
     * The exercises were opened. Also credited to nothing: looking at a question is not
     * answering it. Practice needs a submission, which is {@link #ITEM_ANSWERED}.
     */
    EXERCISE_VIEWED("exercise_viewed"),

    /** One graded item was answered. Carries the item, correctness, and difficulty. */
    ITEM_ANSWERED("item_answered"),

    /** An attempt was submitted and scored. Carries the fraction and whether it passed. */
    ATTEMPT_SUBMITTED("attempt_submitted"),

    /** A flashcard was graded in review. Only counts as retention when it was actually due. */
    REVIEW_ANSWERED("review_answered"),

    /** A wrong answer matched a named misconception. */
    MISCONCEPTION_RAISED("misconception_raised"),

    /** A misconception was answered correctly after the reteach block was read. */
    MISCONCEPTION_CLEARED("misconception_cleared"),

    /**
     * A published project linked to this unit was completed. This is the only event that can
     * fill the Apply chip — quizzes and labs do not count as application on their own.
     */
    PROJECT_COMPLETED("project_completed");

    private final String code;

    LearningEventKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static LearningEventKind fromCode(String code) {
        for (LearningEventKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown learning event kind: " + code);
    }
}
