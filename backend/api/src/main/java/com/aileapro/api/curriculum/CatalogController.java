package com.aileapro.api.curriculum;

import com.aileapro.api.curriculum.CurriculumViews.PathDetail;
import com.aileapro.api.curriculum.CurriculumViews.PathSummary;
import com.aileapro.api.curriculum.CurriculumViews.Resource;
import com.aileapro.api.curriculum.CurriculumViews.UnitDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalog reads. Authentication is required by the filter chain
 * ({@code anyRequest().authenticated()}); these routes hold no learner state of their own.
 * Both UUIDs and slugs are accepted as identifiers.
 */
@RestController
@RequestMapping("/v1")
public class CatalogController {

    private final CurriculumQueryService curriculum;

    public CatalogController(CurriculumQueryService curriculum) {
        this.curriculum = curriculum;
    }

    @GetMapping("/paths")
    public List<PathSummary> paths() {
        return curriculum.listPaths();
    }

    @GetMapping("/paths/{idOrSlug}")
    public PathDetail path(@PathVariable String idOrSlug) {
        return curriculum.path(idOrSlug);
    }

    @GetMapping("/units/{idOrSlug}")
    public UnitDetail unit(@PathVariable String idOrSlug) {
        return curriculum.unit(idOrSlug);
    }

    @GetMapping("/units/{idOrSlug}/resources")
    public List<Resource> resources(@PathVariable String idOrSlug) {
        return curriculum.resources(idOrSlug);
    }
}
