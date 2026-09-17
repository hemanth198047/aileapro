package com.aileapro.api.graph;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;

import java.util.List;
import java.util.UUID;

/** Wire shapes for the map and the prerequisite panel. */
public final class GraphViews {

    /**
     * Mastery is not recorded anywhere yet, so every node reports this. Phase 5 replaces it
     * with real states; until then the map must colour nodes neutrally rather than implying
     * that nothing has been learnt.
     */
    public static final String MASTERY_UNKNOWN = "unknown";

    private GraphViews() {
    }

    /** A published unit in the neighbourhood, with its distance from the focus. */
    public record Node(UnitRef unit, int hops, String masteryState) {
    }

    public record Edge(
            UUID id,
            UUID fromId,
            UUID toId,
            String type,
            double weight,
            boolean symmetric,
            boolean gating,
            String note) {
    }

    /**
     * @param hops        how far the walk actually went
     * @param maxHops     the ceiling the server enforces, whatever the client asked for
     * @param nodeBudget  the node ceiling for one response
     * @param truncated   true when the budget stopped the walk, so the client can say so
     * @param masteryTracked false until Phase 5; the legend depends on it
     */
    public record Neighborhood(
            UnitRef focus,
            int hops,
            int maxHops,
            int nodeBudget,
            boolean truncated,
            boolean masteryTracked,
            List<String> edgeTypes,
            List<Node> nodes,
            List<Edge> edges) {
    }

    /**
     * @param depth  hops back from the target: the largest depth is the earliest thing to study
     * @param direct true when the edge points straight at the target
     */
    public record PrerequisiteStep(UnitRef unit, int depth, boolean direct, String masteryState) {
    }

    /**
     * @param required transitive PREREQUISITE_OF ancestors, in study order
     * @param advisory direct DEPENDS_ON units, which help but do not block
     */
    public record Prerequisites(
            UnitRef unit,
            boolean masteryTracked,
            List<PrerequisiteStep> required,
            List<PrerequisiteStep> advisory) {
    }
}
