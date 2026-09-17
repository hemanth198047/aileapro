package com.aileapro.api.graph;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.graph.GraphViews.Edge;
import com.aileapro.api.graph.GraphViews.Neighborhood;
import com.aileapro.api.graph.GraphViews.Node;
import com.aileapro.api.graph.GraphViews.PrerequisiteStep;
import com.aileapro.api.graph.GraphViews.Prerequisites;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.EdgeType;
import com.aileapro.persistence.ConceptEdgeEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reads of the typed graph, for the map and the prerequisite panel.
 *
 * <p>Two rules shape everything here. A neighbourhood is bounded — by hops and by a node
 * budget — because the map must stay usable and must never turn into a download of the whole
 * graph. And a node only exists if its unit is published: an edge pointing at a draft is
 * dropped along with the draft, since a dangling arrow is itself a disclosure.
 *
 * <p>The walk runs in Java rather than as a recursive query. The authored graph is small and
 * the node budget caps the work, so the cost is a handful of indexed lookups, and the same
 * traversal serves both the map and the prerequisite chain.
 */
@Service
@Transactional(readOnly = true)
public class GraphQueryService {

    /** Two hops is the readable default from the spec; three is as far as the server will go. */
    public static final int DEFAULT_HOPS = 2;
    public static final int MAX_HOPS = 3;

    /** A map with more nodes than this is a hairball, not an explanation. */
    public static final int NODE_BUDGET = 60;

    /** Only a broken graph would nest prerequisites this deep; the guard stops a runaway walk. */
    private static final int MAX_PREREQUISITE_DEPTH = 20;

    private final ConceptEdgeRepository edges;
    private final CurriculumQueryService curriculum;

    public GraphQueryService(ConceptEdgeRepository edges, CurriculumQueryService curriculum) {
        this.edges = edges;
        this.curriculum = curriculum;
    }

    public Neighborhood neighborhood(String idOrSlug, Integer requestedHops, Collection<EdgeType> requestedTypes) {
        UnitRef focus = curriculum.publishedRef(idOrSlug);
        int hops = clampHops(requestedHops);
        Set<EdgeType> types = requestedTypes == null || requestedTypes.isEmpty()
                ? EnumSet.allOf(EdgeType.class)
                : EnumSet.copyOf(requestedTypes);

        Map<UUID, Node> nodes = new LinkedHashMap<>();
        nodes.put(focus.id(), new Node(focus, 0, GraphViews.MASTERY_UNKNOWN));
        List<UUID> frontier = List.of(focus.id());
        boolean truncated = false;

        for (int hop = 1; hop <= hops && !frontier.isEmpty(); hop++) {
            List<UUID> ordered = candidatesBeyond(frontier, types, nodes.keySet());
            Map<UUID, UnitRef> refs = curriculum.publishedRefs(ordered);
            List<UUID> next = new ArrayList<>();
            for (UUID id : ordered) {
                UnitRef ref = refs.get(id);
                if (ref == null) {
                    continue;
                }
                if (nodes.size() >= NODE_BUDGET) {
                    truncated = true;
                    break;
                }
                nodes.put(id, new Node(ref, hop, GraphViews.MASTERY_UNKNOWN));
                next.add(id);
            }
            frontier = next;
        }

        return new Neighborhood(
                focus,
                hops,
                MAX_HOPS,
                NODE_BUDGET,
                truncated,
                false,
                types.stream().map(Enum::name).sorted().toList(),
                List.copyOf(nodes.values()),
                edgesWithin(nodes.keySet(), types));
    }

    public Prerequisites prerequisites(String idOrSlug) {
        UnitRef target = curriculum.publishedRef(idOrSlug);

        Map<UUID, Integer> depthById = new LinkedHashMap<>();
        List<UUID> frontier = List.of(target.id());
        for (int depth = 1; depth <= MAX_PREREQUISITE_DEPTH && !frontier.isEmpty(); depth++) {
            List<UUID> next = new ArrayList<>();
            for (ConceptEdgeEntity edge : edges.findByToIdInAndEdgeTypeAndStatus(
                    frontier, EdgeType.PREREQUISITE_OF, ContentStatus.PUBLISHED)) {
                UUID ancestor = edge.getFromId();
                // The first depth at which a unit appears is the one that matters, and the
                // guard also makes the walk safe if a cycle ever reaches the database.
                if (ancestor.equals(target.id()) || depthById.containsKey(ancestor)) {
                    continue;
                }
                depthById.put(ancestor, depth);
                next.add(ancestor);
            }
            frontier = next;
        }

        // A draft in the middle of a chain hides itself, not its published ancestors: they are
        // still genuinely required, so the walk passes through it and the display skips it.
        Map<UUID, UnitRef> refs = curriculum.publishedRefs(depthById.keySet());
        List<PrerequisiteStep> required = depthById.entrySet().stream()
                .filter(entry -> refs.containsKey(entry.getKey()))
                .map(entry -> new PrerequisiteStep(
                        refs.get(entry.getKey()),
                        entry.getValue(),
                        entry.getValue() == 1,
                        GraphViews.MASTERY_UNKNOWN))
                // Deepest first: that is study order, not the order the walk found them in.
                .sorted(Comparator.comparingInt(PrerequisiteStep::depth).reversed()
                        .thenComparing(step -> step.unit().title()))
                .toList();

        List<UUID> advisoryIds = edges.findByToIdInAndEdgeTypeAndStatus(
                        List.of(target.id()), EdgeType.DEPENDS_ON, ContentStatus.PUBLISHED).stream()
                .map(ConceptEdgeEntity::getFromId)
                .toList();
        Map<UUID, UnitRef> advisoryRefs = curriculum.publishedRefs(advisoryIds);
        List<PrerequisiteStep> advisory = advisoryIds.stream()
                .filter(advisoryRefs::containsKey)
                .map(id -> new PrerequisiteStep(advisoryRefs.get(id), 1, true, GraphViews.MASTERY_UNKNOWN))
                .sorted(Comparator.comparing(step -> step.unit().title()))
                .toList();

        return new Prerequisites(target, false, required, advisory);
    }

    /**
     * Ids one hop out from the frontier, best first. Ordering matters: when the budget runs
     * out, the nodes that survive should be the strongest links rather than whichever rows
     * the database happened to return first.
     */
    private List<UUID> candidatesBeyond(
            Collection<UUID> frontier, Set<EdgeType> types, Set<UUID> alreadyPlaced) {
        Map<UUID, Double> bestWeight = new LinkedHashMap<>();
        for (ConceptEdgeEntity edge : edges.findTouching(frontier, types, ContentStatus.PUBLISHED)) {
            UUID other = frontier.contains(edge.getFromId()) ? edge.getToId() : edge.getFromId();
            if (alreadyPlaced.contains(other)) {
                continue;
            }
            double weight = edge.getWeight() == null ? 1.0 : edge.getWeight().doubleValue();
            bestWeight.merge(other, weight, Math::max);
        }
        return bestWeight.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Edges are collected once the node set is final, so the response can never contain an
     * edge that points at a node the client was not given.
     */
    private List<Edge> edgesWithin(Set<UUID> nodeIds, Set<EdgeType> types) {
        Set<String> seen = new LinkedHashSet<>();
        List<Edge> out = new ArrayList<>();
        for (ConceptEdgeEntity edge : edges.findTouching(nodeIds, types, ContentStatus.PUBLISHED)) {
            if (!nodeIds.contains(edge.getFromId()) || !nodeIds.contains(edge.getToId())) {
                continue;
            }
            EdgeType type = edge.getEdgeType();
            if (!seen.add(dedupeKey(edge, type))) {
                continue;
            }
            out.add(new Edge(
                    edge.getId(),
                    edge.getFromId(),
                    edge.getToId(),
                    type.name(),
                    edge.getWeight() == null ? 1.0 : edge.getWeight().doubleValue(),
                    type.isSymmetric(),
                    type.isGating(),
                    edge.getNote()));
        }
        return out;
    }

    /** A symmetric edge authored in both directions is one relationship, so it is drawn once. */
    private static String dedupeKey(ConceptEdgeEntity edge, EdgeType type) {
        String from = edge.getFromId().toString();
        String to = edge.getToId().toString();
        if (type.isSymmetric() && from.compareTo(to) > 0) {
            return type.name() + ':' + to + ':' + from;
        }
        return type.name() + ':' + from + ':' + to;
    }

    private static int clampHops(Integer requested) {
        if (requested == null) {
            return DEFAULT_HOPS;
        }
        return Math.max(1, Math.min(MAX_HOPS, requested));
    }
}
