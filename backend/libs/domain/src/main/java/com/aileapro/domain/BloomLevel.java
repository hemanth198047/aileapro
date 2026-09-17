package com.aileapro.domain;

/**
 * The cognitive dimension an item asks for (ASM-02).
 *
 * <p>This is tagged for a specific reason: a learner who answers every {@code REMEMBER} item
 * and no {@code APPLY} item has a recognisable and nameable gap, and the report is required
 * to name it rather than average it away into a percentage.
 */
public enum BloomLevel {

    REMEMBER("remember", "recall"),
    UNDERSTAND("understand", "explanation"),
    APPLY("apply", "use on a new case"),
    ANALYZE("analyze", "taking something apart"),
    EVALUATE("evaluate", "judging between options"),
    CREATE("create", "producing something new");

    private final String code;
    private final String humanName;

    BloomLevel(String code, String humanName) {
        this.code = code;
        this.humanName = humanName;
    }

    public String code() {
        return code;
    }

    /** Wording for the report, which talks to learners and not to curriculum designers. */
    public String humanName() {
        return humanName;
    }

    /** Recall-only items cannot demonstrate understanding, however many of them are right. */
    public boolean isRecallOnly() {
        return this == REMEMBER;
    }

    public static BloomLevel fromCode(String code) {
        for (BloomLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown Bloom level: " + code);
    }
}
