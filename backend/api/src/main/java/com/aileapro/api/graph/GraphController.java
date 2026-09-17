package com.aileapro.api.graph;

import com.aileapro.api.graph.GraphViews.Neighborhood;
import com.aileapro.api.graph.GraphViews.Prerequisites;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.EdgeType;
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

    public GraphController(GraphQueryService graph) {
        this.graph = graph;
    }

    /**
     * @param hops  clamped to {@link GraphQueryService#MAX_HOPS}; the response reports both the
     *              radius used and the node budget, so a client cannot mistake a trimmed
     *              neighbourhood for the whole graph
     * @param types optional edge type filter
     */
    @GetMapping("/units/{idOrSlug}/graph")
    public Neighborhood graph(
            @PathVariable String idOrSlug,
            @RequestParam(required = false) Integer hops,
            @RequestParam(required = false) List<String> types) {
        return graph.neighborhood(idOrSlug, hops, parseTypes(types));
    }

    @GetMapping("/units/{idOrSlug}/prerequisites")
    public Prerequisites prerequisites(@PathVariable String idOrSlug) {
        return graph.prerequisites(idOrSlug);
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
