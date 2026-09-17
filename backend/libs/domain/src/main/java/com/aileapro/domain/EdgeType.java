package com.aileapro.domain;

/**
 * Typed relationships between learning units (08-KNOWLEDGE_GRAPH).
 *
 * <p>Containment is absent on purpose: the parent link and {@code unit_closure} already
 * answer "what is this part of". So is GENERALIZATION_OF — it is the inverse of
 * {@link #SPECIALIZATION_OF}, and only one direction is stored.
 */
public enum EdgeType {

    /** A must be understood before B. Acyclic: a cycle here would make learning impossible. */
    PREREQUISITE_OF(false, true),

    /** B leans on A, but a learner can start B without A. Advisory, not gating. */
    DEPENDS_ON(false, false),

    /** Worth reading together, in either direction. */
    RELATED_TO(true, false),

    /** A is a specific case of B. */
    SPECIALIZATION_OF(false, false),

    /** A technique A is used by system or technology B. */
    USED_BY(false, false),

    /** Concept A is realised by code or pattern B. */
    IMPLEMENTED_BY(false, false),

    /** Concept A shows up in domain or project B. */
    APPLIED_IN(false, false),

    /** Misconception A attaches to concept B. */
    COMMON_MISCONCEPTION_OF(false, false),

    /** Competing approaches, in either direction. */
    ALTERNATIVE_TO(true, false),

    /** B historically succeeds A. */
    EVOLUTION_OF(false, false);

    private final boolean symmetric;
    private final boolean gating;

    EdgeType(boolean symmetric, boolean gating) {
        this.symmetric = symmetric;
        this.gating = gating;
    }

    /** True when the edge reads the same both ways, so the graph shows it once, undirected. */
    public boolean isSymmetric() {
        return symmetric;
    }

    /** True when the edge can block progress, and therefore must stay acyclic. */
    public boolean isGating() {
        return gating;
    }
}
