package com.aileapro.domain;

/**
 * The four evidence chips for one learner on one unit.
 *
 * <p>Shared by the reading policy and the grading policy so there is exactly one
 * representation of "what this person has shown". Two records with the same four fields
 * would eventually disagree, and the disagreement would be a claim about a learner.
 */
public record Evidence(
        EvidenceState read, EvidenceState practice, EvidenceState assess, EvidenceState apply) {

    public static Evidence none() {
        return new Evidence(
                EvidenceState.EMPTY, EvidenceState.EMPTY, EvidenceState.EMPTY, EvidenceState.EMPTY);
    }

    public EvidenceState of(EvidenceChip chip) {
        return switch (chip) {
            case READ -> read;
            case PRACTICE -> practice;
            case ASSESS -> assess;
            case APPLY -> apply;
        };
    }

    public Evidence with(EvidenceChip chip, EvidenceState state) {
        return switch (chip) {
            case READ -> new Evidence(state, practice, assess, apply);
            case PRACTICE -> new Evidence(read, state, assess, apply);
            case ASSESS -> new Evidence(read, practice, state, apply);
            case APPLY -> new Evidence(read, practice, assess, state);
        };
    }
}
