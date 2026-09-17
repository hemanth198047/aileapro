package com.aileapro.api.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /v1/search?q=&types=&limit=}.
 *
 * <p>Authenticated like the rest of the learner surface. Draft units never appear: the index
 * only holds published content.
 */
@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    @GetMapping
    public SearchService.SearchPage search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "types", required = false) String types,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return search.search(q, types, limit);
    }
}
