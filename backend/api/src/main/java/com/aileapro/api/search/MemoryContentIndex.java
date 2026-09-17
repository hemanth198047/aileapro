package com.aileapro.api.search;

import com.aileapro.ai.Chunker;
import com.aileapro.ai.EmbeddingPort;
import com.aileapro.ai.Embeddings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory corpus for tests and for any environment that cannot speak pgvector.
 *
 * <p>Token overlap stands in for FTS; cosine on {@link EmbeddingPort} vectors stands in for
 * pgvector. Ranking is reciprocal-rank fusion of the two, the same shape as the JDBC path.
 */
public class MemoryContentIndex implements ContentIndex {

    private final EmbeddingPort embeddings;
    private final Map<UUID, UnitDocument> units = new ConcurrentHashMap<>();
    private final Map<UUID, BlockDocument> blocks = new ConcurrentHashMap<>();
    private final Map<UUID, List<StoredChunk>> chunksByUnit = new ConcurrentHashMap<>();

    public MemoryContentIndex(EmbeddingPort embeddings) {
        this.embeddings = embeddings;
    }

    private record StoredChunk(
            UUID chunkId,
            UUID blockId,
            UUID unitId,
            int index,
            String text,
            float[] vector,
            String knowledgeClass,
            java.time.Instant updatedAt) {
    }

    @Override
    public synchronized void reindexUnit(UnitDocument unit, List<BlockDocument> blocksForUnit) {
        removeUnit(unit.id());
        units.put(unit.id(), unit);
        List<StoredChunk> stored = new ArrayList<>();
        for (BlockDocument block : blocksForUnit) {
            blocks.put(block.id(), block);
            for (Chunker.Chunk piece : Chunker.chunk(block.text())) {
                stored.add(new StoredChunk(
                        UUID.randomUUID(),
                        block.id(),
                        unit.id(),
                        piece.index(),
                        piece.text(),
                        embeddings.embed(piece.text()),
                        block.knowledgeClass() == null ? unit.knowledgeClass() : block.knowledgeClass(),
                        block.updatedAt()));
            }
        }
        chunksByUnit.put(unit.id(), List.copyOf(stored));
    }

    @Override
    public synchronized void removeUnit(UUID unitId) {
        List<StoredChunk> existing = chunksByUnit.remove(unitId);
        if (existing != null) {
            for (StoredChunk chunk : existing) {
                blocks.remove(chunk.blockId());
            }
        }
        units.remove(unitId);
    }

    @Override
    public List<SearchHit> search(String query, Set<String> types, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> want = types == null || types.isEmpty()
                ? Set.of("unit", "block")
                : types.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> tokens = tokens(query);
        List<SearchHit> hits = new ArrayList<>();

        if (want.contains("unit")) {
            for (UnitDocument unit : units.values()) {
                double score = overlap(tokens, tokens(unit.title() + " " + nullToEmpty(unit.description())));
                if (score <= 0) {
                    continue;
                }
                hits.add(new SearchHit(
                        "unit",
                        unit.id(),
                        unit.slug(),
                        unit.title(),
                        snippet(nullToEmpty(unit.description()), query),
                        "/app/learn/" + unit.slug(),
                        unit.knowledgeClass(),
                        unit.updatedAt(),
                        score,
                        unit.id(),
                        unit.slug(),
                        null));
            }
        }

        if (want.contains("block")) {
            for (List<StoredChunk> group : chunksByUnit.values()) {
                for (StoredChunk chunk : group) {
                    double score = overlap(tokens, tokens(chunk.text()));
                    if (score <= 0) {
                        continue;
                    }
                    UnitDocument unit = units.get(chunk.unitId());
                    BlockDocument block = blocks.get(chunk.blockId());
                    if (unit == null || block == null) {
                        continue;
                    }
                    hits.add(new SearchHit(
                            "block",
                            chunk.blockId(),
                            unit.slug(),
                            unit.title(),
                            snippet(chunk.text(), query),
                            "/app/learn/" + unit.slug(),
                            chunk.knowledgeClass(),
                            chunk.updatedAt(),
                            score,
                            unit.id(),
                            unit.slug(),
                            block.blockType()));
                }
            }
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
        float[] needle = embeddings.embed(query);
        Set<String> tokens = tokens(query);

        List<StoredChunk> all = chunksByUnit.values().stream().flatMap(List::stream).toList();
        Map<UUID, Double> ftsRank = new HashMap<>();
        Map<UUID, Double> vecRank = new HashMap<>();
        Map<UUID, StoredChunk> byId = new HashMap<>();

        List<StoredChunk> ftsOrdered = all.stream()
                .map(chunk -> Map.entry(chunk, overlap(tokens, tokens(chunk.text()))))
                .filter(e -> e.getValue() > 0)
                .sorted(Comparator.<Map.Entry<StoredChunk, Double>>comparingDouble(Map.Entry::getValue).reversed())
                .map(Map.Entry::getKey)
                .toList();
        for (int i = 0; i < ftsOrdered.size(); i++) {
            StoredChunk chunk = ftsOrdered.get(i);
            byId.put(chunk.chunkId(), chunk);
            ftsRank.put(chunk.chunkId(), 1.0 / (60 + i));
        }

        List<StoredChunk> vecOrdered = all.stream()
                .sorted(Comparator.comparingDouble((StoredChunk c) -> Embeddings.cosine(needle, c.vector())).reversed())
                .toList();
        for (int i = 0; i < vecOrdered.size(); i++) {
            StoredChunk chunk = vecOrdered.get(i);
            byId.put(chunk.chunkId(), chunk);
            double cosine = Embeddings.cosine(needle, chunk.vector());
            if (cosine < 0.05) {
                continue;
            }
            vecRank.put(chunk.chunkId(), 1.0 / (60 + i));
        }

        Set<UUID> ids = new HashSet<>();
        ids.addAll(ftsRank.keySet());
        ids.addAll(vecRank.keySet());

        List<ContentHit> hits = new ArrayList<>();
        for (UUID id : ids) {
            StoredChunk chunk = byId.get(id);
            UnitDocument unit = units.get(chunk.unitId());
            BlockDocument block = blocks.get(chunk.blockId());
            if (unit == null || block == null) {
                continue;
            }
            double score = ftsRank.getOrDefault(id, 0.0) + vecRank.getOrDefault(id, 0.0);
            if (preferUnitId != null && preferUnitId.equals(chunk.unitId())) {
                score += 0.02;
            }
            String match = ftsRank.containsKey(id) && vecRank.containsKey(id)
                    ? "hybrid"
                    : ftsRank.containsKey(id) ? "keyword" : "vector";
            hits.add(new ContentHit(
                    chunk.chunkId(),
                    chunk.blockId(),
                    chunk.unitId(),
                    unit.slug(),
                    unit.title(),
                    block.blockType(),
                    snippet(chunk.text(), query),
                    chunk.knowledgeClass(),
                    chunk.updatedAt(),
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
        return chunksByUnit.values().stream().mapToLong(List::size).sum();
    }

    /** Test helper: wipe everything. */
    public void clear() {
        units.clear();
        blocks.clear();
        chunksByUnit.clear();
    }

    private static Set<String> tokens(String text) {
        Set<String> out = new HashSet<>();
        for (String part : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (part.length() >= 2) {
                out.add(part);
            }
        }
        return out;
    }

    private static double overlap(Set<String> query, Set<String> document) {
        if (query.isEmpty() || document.isEmpty()) {
            return 0;
        }
        long shared = query.stream().filter(document::contains).count();
        return shared == 0 ? 0 : (double) shared / query.size();
    }

    private static String snippet(String text, String query) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String flat = text.replace('\n', ' ').trim();
        if (flat.length() <= 220) {
            return flat;
        }
        String lower = flat.toLowerCase(Locale.ROOT);
        String first = tokens(query).stream().findFirst().orElse("");
        int at = first.isBlank() ? 0 : Math.max(0, lower.indexOf(first) - 40);
        int end = Math.min(flat.length(), at + 220);
        String slice = flat.substring(at, end).trim();
        return (at > 0 ? "…" : "") + slice + (end < flat.length() ? "…" : "");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
