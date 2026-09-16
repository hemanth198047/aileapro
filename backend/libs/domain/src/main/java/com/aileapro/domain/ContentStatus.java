package com.aileapro.domain;

/** Publication lifecycle shared by units and paths. Learners only ever see PUBLISHED. */
public enum ContentStatus {
    DRAFT("draft"),
    REVIEW("review"),
    VALIDATE("validate"),
    PUBLISHED("published"),
    DEPRECATED("deprecated");

    private final String code;

    ContentStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ContentStatus fromCode(String code) {
        for (ContentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown content status: " + code);
    }
}
