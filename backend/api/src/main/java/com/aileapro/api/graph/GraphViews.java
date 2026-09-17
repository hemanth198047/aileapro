package com.aileapro.api.graph;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wire shapes for the map and the prerequisite panel. */
public final class GraphViews {

    /**
     * What a node reports before the learner's evidence is laid over it, and what it keeps
     * reporting for a unit with no evidence at all. UNKNOWN is a real answer here: it means
     * nothing has been shown, not that the learner failed. Spelt exactly like the mastery
     * state it stands for, so the wire format has one vocabulary rather than two.
     */
    public static final String MASTERY_UNKNOWN = "UNKNOWN";

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

    /**
     * Lays a learner's mastery states over a graph read. Kept as an interface in this package
     * so the graph never has to know how mastery is recorded, and the learner code can supply
     * it without the two packages depending on each other in both directions.
     */
    public interface MasteryLookup {

        /** States keyed by unit id. Ids with no evidence may be absent. */
        java.util.Map<UUID, String> statesFor(UUID userId, java.util.Collection<UnitRef> units);
    }

    public static Neighborhood withMastery(Neighborhood neighborhood, Map<UUID, String> states) {
        return new Neighborhood(
                neighborhood.focus(),
                neighborhood.hops(),
                neighborhood.maxHops(),
                neighborhood.nodeBudget(),
                neighborhood.truncated(),
                true,
                neighborhood.edgeTypes(),
                neighborhood.nodes().stream()
                        .map(node -> new Node(node.unit(), node.hops(), stateOf(states, node.unit().id())))
                        .toList(),
                neighborhood.edges());
    }

    public static Prerequisites withMastery(Prerequisites prerequisites, Map<UUID, String> states) {
        return new Prerequisites(
                prerequisites.unit(),
                true,
                prerequisites.required().stream().map(step -> withMastery(step, states)).toList(),
                prerequisites.advisory().stream().map(step -> withMastery(step, states)).toList());
    }

    private static PrerequisiteStep withMastery(PrerequisiteStep step, Map<UUID, String> states) {
        return new PrerequisiteStep(
                step.unit(), step.depth(), step.direct(), stateOf(states, step.unit().id()));
    }

    private static String stateOf(Map<UUID, String> states, UUID unitId) {
        String state = states.get(unitId);
        return state == null ? MASTERY_UNKNOWN : state;
    }
}
