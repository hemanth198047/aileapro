package com.aileapro.domain;

/**
 * What an attempt was for, which decides what its answers are worth.
 *
 * <p>The same item can appear in a practice set and in a quiz. What changes is the claim:
 * practice shows you can do it with the page in front of you, a quiz shows you can do it
 * when asked cold, and the diagnostic shows neither — it is a guess used to decide where to
 * start, taken before any teaching has happened.
 */
public enum AssessmentKind {

    /** Worked with the unit open. Fills the Practice chip. */
    PRACTICE("practice"),

    /** Closed-book on one unit. Fills the Assess chip. */
    QUIZ("quiz"),

    /** Wider than one unit, and the only kind that is timed by default. Fills Assess. */
    EXAM("exam"),

    /**
     * Placement. Fills nothing: it is one or two items per domain, taken before the learner
     * has been taught anything, and calling that evidence of competence would be the exact
     * dishonesty this product is built against.
     */
    DIAGNOSTIC("diagnostic"),

    /**
     * Career interview rehearsal (text). Fills nothing: practising interview answers is not
     * the same as demonstrated mastery on a unit, and must not shrink skill gaps.
     */
    INTERVIEW("interview");

    private final String code;

    AssessmentKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Which chip an attempt of this kind can fill, or null when it can fill none. */
    public EvidenceChip chip() {
        return switch (this) {
            case PRACTICE -> EvidenceChip.PRACTICE;
            case QUIZ, EXAM -> EvidenceChip.ASSESS;
            case DIAGNOSTIC, INTERVIEW -> null;
        };
    }

    public boolean gradesEvidence() {
        return chip() != null;
    }

    public static AssessmentKind fromCode(String code) {
        for (AssessmentKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown assessment kind: " + code);
    }
}
