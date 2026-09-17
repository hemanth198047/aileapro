package com.aileapro.api.search;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The searchable corpus.
 *
 * <p>Two implementations: {@link JdbcContentIndex} for Postgres (FTS + pgvector) and
 * {@link MemoryContentIndex} for tests (token overlap + cosine on fake embeddings). Features
 * never care which one is wired.
 */
public interface ContentIndex {

    /** Replace every indexed row for one published unit. */
    void reindexUnit(UnitDocument unit, List<BlockDocument> blocks);

    /** Drop a unit that is no longer published. */
    void removeUnit(UUID unitId);

    /** Ranked search across published units and blocks. */
    List<SearchHit> search(String query, Set<String> types, int limit);

    /** Hybrid retrieval for the tutor, optionally preferring the open unit. */
    List<ContentHit> retrieve(String query, UUID preferUnitId, int limit);

    /** How many chunks are currently indexed. */
    long chunkCount();

    record UnitDocument(
            UUID id,
            String slug,
            String title,
            String description,
            String kind,
            String knowledgeClass,
            Instant updatedAt) {
    }

    record BlockDocument(
            UUID id,
            UUID unitId,
            String blockType,
            String text,
            String knowledgeClass,
            Instant updatedAt) {
    }

    /**
     * A search result the UI can render.
     *
     * @param type {@code unit}, {@code block}, or {@code path}
     */
    record SearchHit(
            String type,
            UUID id,
            String slug,
            String title,
            String snippet,
            String href,
            String knowledgeClass,
            Instant updatedAt,
            double score,
            UUID unitId,
            String unitSlug,
            String blockType) {
    }
}
