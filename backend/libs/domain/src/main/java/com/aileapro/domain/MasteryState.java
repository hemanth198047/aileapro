package com.aileapro.domain;

/**
 * Where a learner stands on one unit (07-MASTERY_AND_ADAPTIVE_LEARNING).
 *
 * <p>The order is the progression, and it is one-way except for decay. Each step names the
 * evidence it needs, because the point of this model is that a state is a claim someone
 * could be asked to back up.
 */
public enum MasteryState {

    /** Nothing has been shown. The honest default, and where everything starts. */
    UNKNOWN(0),

    /** The unit was opened and at least one explanation was read for long enough to be plausible. */
    INTRODUCED(1),

    /** Easy recall answered correctly. Needs graded items, so it waits for Phase 6. */
    FAMILIAR(2),

    /** Medium items plus misconception checks passed. */
    UNDERSTOOD(3),

    /** Exercises done, and for a code unit, something actually run. */
    PRACTICED(4),

    /** An assessment set and a transfer item, consistent across more than one day. */
    PROFICIENT(5),

    /** Applied in a project or taught back, and still there after a retention interval. */
    MASTERED(6);

    private final int rank;

    MasteryState(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean atLeast(MasteryState other) {
        return rank >= other.rank;
    }

    /**
     * The ceiling that reading alone can reach. Everything above this needs evidence the
     * learner produced rather than consumed, which is why a page view can never approach it.
     */
    public static MasteryState readingCeiling() {
        return INTRODUCED;
    }
}
