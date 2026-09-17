package com.aileapro.domain;

/** How much evidence one chip rests on. Stored lowercase because it is part of the API surface. */
public enum EvidenceState {

    EMPTY("empty"),
    PARTIAL("partial"),
    SOLID("solid");

    private final String code;

    EvidenceState(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static EvidenceState fromCode(String code) {
        for (EvidenceState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown evidence state: " + code);
    }

    public boolean atLeast(EvidenceState other) {
        return ordinal() >= other.ordinal();
    }
}
