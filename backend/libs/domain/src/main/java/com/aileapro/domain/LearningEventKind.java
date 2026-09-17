package com.aileapro.domain;

/**
 * Things a learner can be observed doing while reading. All of them are consumption
 * signals, which is exactly why none of them can fill anything but the Read chip.
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
     * The exercises were opened. Also credited to nothing — Practice needs a submission,
     * which arrives with grading in Phase 6.
     */
    EXERCISE_VIEWED("exercise_viewed");

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
