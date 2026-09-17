package com.aileapro.api.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cycle detection for gating edges.
 *
 * <p>A prerequisite cycle is not a cosmetic problem: it describes a curriculum nobody can
 * ever start, and the recommendation in Phase 5 would loop on it. So this runs before
 * prerequisite edges are committed, and it reports the offending ring rather than just
 * refusing, because the author needs to know which link to cut.
 *
 * <p>Generic in the node type so the seed loader can check slugs and get an error a human
 * can read, while the API can check identifiers.
 */
public final class PrerequisiteDag {

    private PrerequisiteDag() {
    }

    /**
     * @param outgoing adjacency of "must come before": {@code A -> [B]} means A precedes B
     * @return the cycle as a walk that returns to its first node, or empty when acyclic
     */
    public static <T> Optional<List<T>> findCycle(Map<T, ? extends Collection<T>> outgoing) {
        Set<T> settled = new HashSet<>();
        Set<T> onPath = new HashSet<>();
        Map<T, T> cameFrom = new HashMap<>();

        for (T root : outgoing.keySet()) {
            if (settled.contains(root)) {
                continue;
            }
            // Iterative so a deep chain cannot overflow the stack on a large authored graph.
            Deque<Step<T>> stack = new ArrayDeque<>();
            stack.push(new Step<>(root, next(outgoing, root)));
            onPath.add(root);

            while (!stack.isEmpty()) {
                Step<T> step = stack.peek();
                if (step.remaining.isEmpty()) {
                    stack.pop();
                    onPath.remove(step.node);
                    settled.add(step.node);
                    continue;
                }
                T child = step.remaining.remove(step.remaining.size() - 1);
                if (onPath.contains(child)) {
                    return Optional.of(ring(child, step.node, cameFrom));
                }
                if (settled.contains(child)) {
                    continue;
                }
                cameFrom.put(child, step.node);
                onPath.add(child);
                stack.push(new Step<>(child, next(outgoing, child)));
            }
        }
        return Optional.empty();
    }

    private static <T> List<T> next(Map<T, ? extends Collection<T>> outgoing, T node) {
        Collection<T> children = outgoing.get(node);
        return children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    /** Walks back up the path from the repeated node to close the ring. */
    private static <T> List<T> ring(T repeated, T lastNode, Map<T, T> cameFrom) {
        List<T> backwards = new ArrayList<>();
        backwards.add(repeated);
        T current = lastNode;
        while (current != null && !current.equals(repeated)) {
            backwards.add(current);
            current = cameFrom.get(current);
        }
        backwards.add(repeated);
        return backwards.reversed();
    }

    private record Step<T>(T node, List<T> remaining) {
    }
}
