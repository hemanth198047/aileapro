package com.aileapro.api.graph;

import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.graph.GraphViews.Neighborhood;
import com.aileapro.api.graph.GraphViews.Prerequisites;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.EdgeType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Graph reads. Authenticated like the rest of {@code /v1}, and published-only, so the map
 * can never be used to enumerate drafts.
 */
@RestController
@RequestMapping("/v1")
public class GraphController {

    private final GraphQueryService graph;
    private final GraphViews.MasteryLookup mastery;

    public GraphController(GraphQueryService graph, GraphViews.MasteryLookup mastery) {
        this.graph = graph;
        this.mastery = mastery;
    }

    /**
     * @param hops  clamped to {@link GraphQueryService#MAX_HOPS}; the response reports both the
     *              radius used and the node budget, so a client cannot mistake a trimmed
     *              neighbourhood for the whole graph
     * @param types optional edge type filter
     */
    @GetMapping("/units/{idOrSlug}/graph")
    public Neighborhood graph(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String idOrSlug,
            @RequestParam(required = false) Integer hops,
            @RequestParam(required = false) List<String> types) {
        Neighborhood neighborhood = graph.neighborhood(idOrSlug, hops, parseTypes(types));
        return GraphViews.withMastery(
                neighborhood,
                mastery.statesFor(
                        principal.getId(), neighborhood.nodes().stream().map(node -> node.unit()).toList()));
    }

    @GetMapping("/units/{idOrSlug}/prerequisites")
    public Prerequisites prerequisites(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String idOrSlug) {
        Prerequisites prerequisites = graph.prerequisites(idOrSlug);
        var units = java.util.stream.Stream.concat(
                        prerequisites.required().stream(), prerequisites.advisory().stream())
                .map(step -> step.unit())
                .toList();
        return GraphViews.withMastery(prerequisites, mastery.statesFor(principal.getId(), units));
    }

    /** Parsed by hand so a typo is a 400 that names the valid types, not a 500. */
    private static Set<EdgeType> parseTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return EnumSet.noneOf(EdgeType.class);
        }
        Set<EdgeType> parsed = EnumSet.noneOf(EdgeType.class);
        for (String raw : types) {
            for (String candidate : raw.split(",")) {
                String name = candidate.strip();
                if (name.isEmpty()) {
                    continue;
                }
                try {
                    parsed.add(EdgeType.valueOf(name.toUpperCase()));
                } catch (IllegalArgumentException unknown) {
                    throw ApiException.validation("Unknown edge type '" + name + "'. Valid types: "
                            + Arrays.toString(EdgeType.values()));
                }
            }
        }
        return parsed;
    }
}
