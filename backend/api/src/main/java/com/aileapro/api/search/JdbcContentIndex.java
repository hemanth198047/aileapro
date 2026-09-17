package com.aileapro.api.search;

import com.aileapro.ai.Chunker;
import com.aileapro.ai.EmbeddingPort;
import com.aileapro.ai.Embeddings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Postgres-backed corpus: {@code tsvector} for keywords, {@code pgvector} for semantics.
 *
 * <p>Not a Spring bean by default — {@link ContentIndexConfig} wires it only when
 * {@code aileapro.search.backend=jdbc}. That keeps H2 tests from trying to speak {@code vector}.
 */
public class JdbcContentIndex implements ContentIndex {

    private static final Logger log = LoggerFactory.getLogger(JdbcContentIndex.class);

    private final JdbcTemplate jdbc;
    private final EmbeddingPort embeddings;

    public JdbcContentIndex(JdbcTemplate jdbc, EmbeddingPort embeddings) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
    }

    @Override
    public void reindexUnit(UnitDocument unit, List<BlockDocument> blocks) {
        String unitTsv = unit.title() + " " + nullToEmpty(unit.description());
        jdbc.update(
                """
                update learning_units
                   set search_tsv = to_tsvector('english', coalesce(?, ''))
                 where id = ?
                """,
                unitTsv,
                unit.id());

        jdbc.update("delete from content_chunks where unit_id = ?", unit.id());

        for (BlockDocument block : blocks) {
            jdbc.update(
                    """
                    update unit_blocks
                       set search_tsv = to_tsvector('english', coalesce(?, ''))
                     where id = ?
                    """,
                    block.text(),
                    block.id());

            String knowledge = block.knowledgeClass() == null ? unit.knowledgeClass() : block.knowledgeClass();
            for (Chunker.Chunk piece : Chunker.chunk(block.text())) {
                float[] vector = embeddings.embed(piece.text());
                jdbc.update(
                        """
                        insert into content_chunks (
                            id, unit_id, block_id, chunk_index, text, content_hash,
                            token_estimate, embedding, model_slug, knowledge_class,
                            block_updated_at, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, cast(? as vector), ?, ?, ?, now())
                        """,
                        UUID.randomUUID(),
                        unit.id(),
                        block.id(),
                        piece.index(),
                        piece.text(),
                        piece.contentHash(),
                        piece.tokenEstimate(),
                        Embeddings.toPgVector(vector),
                        embeddings.modelSlug(),
                        knowledge,
                        Timestamp.from(block.updatedAt()));
            }
        }
        log.debug("Reindexed unit {} with {} blocks", unit.slug(), blocks.size());
    }

    @Override
    public void removeUnit(UUID unitId) {
        jdbc.update("delete from content_chunks where unit_id = ?", unitId);
        jdbc.update("update learning_units set search_tsv = null where id = ?", unitId);
        jdbc.update("update unit_blocks set search_tsv = null where unit_id = ?", unitId);
    }

    @Override
    public List<SearchHit> search(String query, Set<String> types, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> want = types == null || types.isEmpty()
                ? Set.of("unit", "block")
                : types.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        String tsQuery = toTsQuery(query);
        List<SearchHit> hits = new ArrayList<>();

        if (want.contains("unit")) {
            hits.addAll(jdbc.query(
                    """
                    select id, slug, title, description, knowledge_class, updated_at,
                           ts_rank(search_tsv, to_tsquery('english', ?)) as rank
                      from learning_units
                     where status = 'published'
                       and search_tsv @@ to_tsquery('english', ?)
                     order by rank desc
                     limit ?
                    """,
                    unitSearchMapper(query),
                    tsQuery,
                    tsQuery,
                    limit));
        }

        if (want.contains("block")) {
            hits.addAll(jdbc.query(
                    """
                    select b.id, u.id as unit_id, u.slug, u.title, b.block_type,
                           u.knowledge_class, b.updated_at,
                           ts_headline('english', coalesce(b.payload->>'markdown', ''),
                                       to_tsquery('english', ?),
                                       'MaxWords=32, MinWords=12') as headline,
                           ts_rank(b.search_tsv, to_tsquery('english', ?)) as rank
                      from unit_blocks b
                      join learning_units u on u.id = b.unit_id
                     where u.status = 'published'
                       and b.search_tsv @@ to_tsquery('english', ?)
                     order by rank desc
                     limit ?
                    """,
                    blockSearchMapper(),
                    tsQuery,
                    tsQuery,
                    tsQuery,
                    limit));
        }

        return hits.stream()
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public List<ContentHit> retrieve(String query, UUID preferUnitId, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String tsQuery = toTsQuery(query);
        String vector = Embeddings.toPgVector(embeddings.embed(query));
        int fetch = Math.max(limit * 4, 20);

        List<Ranked> fts = jdbc.query(
                """
                select c.id, c.block_id, c.unit_id, u.slug, u.title, b.block_type,
                       left(c.text, 280) as snippet, c.knowledge_class, c.block_updated_at,
                       ts_rank(to_tsvector('english', c.text), to_tsquery('english', ?)) as rank
                  from content_chunks c
                  join learning_units u on u.id = c.unit_id
                  join unit_blocks b on b.id = c.block_id
                 where u.status = 'published'
                   and to_tsvector('english', c.text) @@ to_tsquery('english', ?)
                 order by rank desc
                 limit ?
                """,
                rankedMapper("keyword"),
                tsQuery,
                tsQuery,
                fetch);

        List<Ranked> vec = jdbc.query(
                """
                select c.id, c.block_id, c.unit_id, u.slug, u.title, b.block_type,
                       left(c.text, 280) as snippet, c.knowledge_class, c.block_updated_at,
                       1 - (c.embedding <=> cast(? as vector)) as rank
                  from content_chunks c
                  join learning_units u on u.id = c.unit_id
                  join unit_blocks b on b.id = c.block_id
                 where u.status = 'published'
                 order by c.embedding <=> cast(? as vector)
                 limit ?
                """,
                rankedMapper("vector"),
                vector,
                vector,
                fetch);

        Map<UUID, Double> rrf = new HashMap<>();
        Map<UUID, Ranked> byId = new HashMap<>();
        for (int i = 0; i < fts.size(); i++) {
            Ranked row = fts.get(i);
            byId.put(row.chunkId(), row);
            rrf.merge(row.chunkId(), 1.0 / (60 + i), Double::sum);
        }
        for (int i = 0; i < vec.size(); i++) {
            Ranked row = vec.get(i);
            if (row.rank() < 0.05) {
                continue;
            }
            byId.put(row.chunkId(), row);
            rrf.merge(row.chunkId(), 1.0 / (60 + i), Double::sum);
        }

        Set<UUID> ftsIds = fts.stream().map(Ranked::chunkId).collect(Collectors.toSet());
        Set<UUID> vecIds = vec.stream().map(Ranked::chunkId).collect(Collectors.toSet());

        List<ContentHit> hits = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : rrf.entrySet()) {
            Ranked row = byId.get(entry.getKey());
            double score = entry.getValue();
            if (preferUnitId != null && preferUnitId.equals(row.unitId())) {
                score += 0.02;
            }
            String match = ftsIds.contains(row.chunkId()) && vecIds.contains(row.chunkId())
                    ? "hybrid"
                    : ftsIds.contains(row.chunkId()) ? "keyword" : "vector";
            hits.add(new ContentHit(
                    row.chunkId(),
                    row.blockId(),
                    row.unitId(),
                    row.unitSlug(),
                    row.unitTitle(),
                    row.blockType(),
                    row.snippet(),
                    row.knowledgeClass(),
                    row.updatedAt(),
                    score,
                    match));
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(ContentHit::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public long chunkCount() {
        Long count = jdbc.queryForObject("select count(*) from content_chunks", Long.class);
        return count == null ? 0 : count;
    }

    private record Ranked(
            UUID chunkId,
            UUID blockId,
            UUID unitId,
            String unitSlug,
            String unitTitle,
            String blockType,
            String snippet,
            String knowledgeClass,
            Instant updatedAt,
            double rank,
            String match) {
    }

    private RowMapper<Ranked> rankedMapper(String match) {
        return (rs, rowNum) -> new Ranked(
                uuid(rs, "id"),
                uuid(rs, "block_id"),
                uuid(rs, "unit_id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("block_type"),
                rs.getString("snippet"),
                rs.getString("knowledge_class"),
                instant(rs, "block_updated_at"),
                rs.getDouble("rank"),
                match);
    }

    private RowMapper<SearchHit> unitSearchMapper(String query) {
        return (rs, rowNum) -> {
            String description = rs.getString("description");
            return new SearchHit(
                    "unit",
                    uuid(rs, "id"),
                    rs.getString("slug"),
                    rs.getString("title"),
                    snippet(description, query),
                    "/app/learn/" + rs.getString("slug"),
                    rs.getString("knowledge_class"),
                    instant(rs, "updated_at"),
                    rs.getDouble("rank"),
                    uuid(rs, "id"),
                    rs.getString("slug"),
                    null);
        };
    }

    private RowMapper<SearchHit> blockSearchMapper() {
        return (rs, rowNum) -> new SearchHit(
                "block",
                uuid(rs, "id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("headline"),
                "/app/learn/" + rs.getString("slug"),
                rs.getString("knowledge_class"),
                instant(rs, "updated_at"),
                rs.getDouble("rank"),
                uuid(rs, "unit_id"),
                rs.getString("slug"),
                rs.getString("block_type"));
    }

    /**
     * Turns free text into a safe {@code to_tsquery} expression: tokens joined with {@code &},
     * non-alnum stripped. An empty result means "match nothing" rather than a syntax error.
     */
    static String toTsQuery(String query) {
        List<String> parts = new ArrayList<>();
        for (String part : query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (part.length() >= 2) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) {
            return "___nomatch___";
        }
        return String.join(" & ", parts);
    }

    private static String snippet(String text, String query) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String flat = text.replace('\n', ' ').trim();
        return flat.length() <= 220 ? flat : flat.substring(0, 220).trim() + "…";
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
