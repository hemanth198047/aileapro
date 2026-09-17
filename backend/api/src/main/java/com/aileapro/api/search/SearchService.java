package com.aileapro.api.search;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Learner-facing search and tutor retrieval over the same corpus.
 */
@Service
public class SearchService {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;
    public static final int RETRIEVE_LIMIT = 6;

    private final ContentIndex index;

    public SearchService(ContentIndex index) {
        this.index = index;
    }

    public SearchPage search(String q, String types, Integer limit) {
        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            return new SearchPage(query, List.of(), Set.of(), 0, "Type something to search the published curriculum.");
        }
        if (query.length() < 2) {
            return new SearchPage(query, List.of(), Set.of(), 0, "Use at least two characters.");
        }
        Set<String> typeSet = parseTypes(types);
        int capped = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<ContentIndex.SearchHit> hits = index.search(query, typeSet, capped);
        String note = hits.isEmpty()
                ? "No published unit or block matched. Try a title word from the curriculum, or a concept name."
                : hits.size() + (hits.size() == 1 ? " result." : " results.");
        return new SearchPage(query, hits, typeSet, hits.size(), note);
    }

    public List<ContentHit> retrieveForTutor(String ask, UUID preferUnitId) {
        if (ask == null || ask.isBlank()) {
            return List.of();
        }
        return index.retrieve(ask, preferUnitId, RETRIEVE_LIMIT);
    }

    public long chunkCount() {
        return index.chunkCount();
    }

    private static Set<String> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Set.of("unit", "block");
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : types.split(",")) {
            String cleaned = part.trim().toLowerCase(Locale.ROOT);
            if (cleaned.equals("unit") || cleaned.equals("block") || cleaned.equals("path")) {
                out.add(cleaned);
            }
        }
        return out.isEmpty() ? Set.of("unit", "block") : out;
    }

    public record SearchPage(
            String query,
            List<ContentIndex.SearchHit> results,
            Set<String> types,
            int total,
            String note) {
    }
}
