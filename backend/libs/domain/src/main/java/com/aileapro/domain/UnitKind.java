package com.aileapro.domain;

/**
 * The taxonomy is a recommendation, not a schema constraint: a unit of any kind may
 * contain a unit of any other kind. Depth is cached, never enforced.
 */
public enum UnitKind {
    DOMAIN("domain"),
    PATH("path"),
    MODULE("module"),
    CHAPTER("chapter"),
    SECTION("section"),
    SUBSECTION("subsection"),
    TOPIC("topic"),
    SUBTOPIC("subtopic"),
    MICRO_CONCEPT("micro_concept"),
    KNOWLEDGE_UNIT("knowledge_unit"),
    CUSTOM("custom");

    private final String code;

    UnitKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static UnitKind fromCode(String code) {
        for (UnitKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown unit kind: " + code);
    }
}
