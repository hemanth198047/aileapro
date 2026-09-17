package com.aileapro.domain;

/** The four kinds of evidence a unit can carry, in the order the UI shows them. */
public enum EvidenceChip {

    /** Filled by reading. Cannot take a unit past INTRODUCED. */
    READ("read"),

    /** Filled by answering items with the unit open. */
    PRACTICE("practice"),

    /** Filled by answering items cold, in a quiz or an exam. */
    ASSESS("assess"),

    /**
     * Filled by producing something: a lab run, a project, a teach-back. Nothing can fill it
     * yet, and the UI says so rather than leaving a chip that looks broken.
     */
    APPLY("apply");

    private final String code;

    EvidenceChip(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static EvidenceChip fromCode(String code) {
        for (EvidenceChip chip : values()) {
            if (chip.code.equals(code)) {
                return chip;
            }
        }
        throw new IllegalArgumentException("Unknown evidence chip: " + code);
    }
}
