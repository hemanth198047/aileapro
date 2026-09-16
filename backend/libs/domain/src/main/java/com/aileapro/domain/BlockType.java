package com.aileapro.domain;

/**
 * Typed content blocks from 06-CURRICULUM_MODEL. A learning mode is a view over these
 * blocks, which is why explanation levels are separate types rather than one body field.
 */
public enum BlockType {
    WHY("why"),
    OBJECTIVES("objectives"),
    PREREQ_CHECK("prereq_check"),
    ELI5("eli5"),
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced"),
    EXPERT("expert"),
    RESEARCH("research"),
    FORMAL_DEFINITION("formal_definition"),
    MATH_INTUITION("math_intuition"),
    MATH_FORMAL("math_formal"),
    VISUAL("visual"),
    CODE("code"),
    INTERACTIVE("interactive"),
    EXAMPLE("example"),
    COUNTEREXAMPLE("counterexample"),
    APPLICATION("application"),
    COMMON_MISTAKE("common_mistake"),
    MISCONCEPTION("misconception"),
    EXERCISE_SET("exercise_set"),
    QUIZ_SET("quiz_set"),
    CHALLENGE("challenge"),
    PROJECT_LINK("project_link"),
    INTERVIEW("interview"),
    ASSESSMENT_CRITERIA("assessment_criteria"),
    MASTERY_CRITERIA("mastery_criteria"),
    REFERENCES("references"),
    RELATED("related");

    private final String code;

    BlockType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** True for the blocks that carry an explanation the learner reads at a chosen level. */
    public boolean isExplanationLevel() {
        return this == ELI5 || this == BEGINNER || this == INTERMEDIATE
                || this == ADVANCED || this == EXPERT || this == RESEARCH;
    }

    public static BlockType fromCode(String code) {
        for (BlockType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown block type: " + code);
    }
}
