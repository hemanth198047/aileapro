package com.aileapro.domain;

/**
 * The P0 item types from ASM-01. Deliberately a short list: five types that can be marked
 * honestly are worth more than nine where four of them are marked by string comparison and
 * pretend otherwise.
 */
public enum ItemType {

    /** One correct choice out of several. */
    MCQ("mcq"),

    /**
     * Several correct choices. Scored with partial credit, because "select all that apply"
     * marked all-or-nothing tells the learner nothing about which half they understood.
     */
    MULTI_SELECT("multi_select"),

    TRUE_FALSE("true_false"),

    /**
     * A short typed answer, marked against authored accepted forms after normalisation.
     * Only used where the answer space is genuinely small — a number, a shape, a term.
     */
    SHORT_TEXT("short_text"),

    /**
     * A code answer marked by comparing normalised text to accepted solutions. This is a
     * stub and says so in the UI: real execution against tests is the lab in Phase 9. It
     * exists because a coding item that is honest about being unrun is more useful than no
     * coding item at all.
     */
    CODING_STUB("coding_stub");

    private final String code;

    ItemType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** True when marking is exact rather than approximate, which the UI discloses. */
    public boolean isObjective() {
        return this != CODING_STUB;
    }

    public static ItemType fromCode(String code) {
        for (ItemType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown item type: " + code);
    }
}
