package com.aileapro.api.search;

import com.aileapro.ai.Chunker;
import com.aileapro.ai.Embeddings;
import com.aileapro.ai.FakeEmbeddingClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure retrieval pieces that do not need Spring. */
class ChunkerAndEmbeddingTest {

    @Test
    void chunkingIsIdempotentAndParagraphAware() {
        String text = "First paragraph about vectors.\n\nSecond paragraph about matrices.\n\nThird.";
        List<Chunker.Chunk> first = Chunker.chunk(text);
        List<Chunker.Chunk> second = Chunker.chunk(text);

        assertThat(first).isNotEmpty();
        assertThat(second).isEqualTo(first);
        assertThat(first.get(0).contentHash()).hasSize(64);
        assertThat(first.get(0).text()).contains("vectors");
    }

    @Test
    void emptyTextProducesNoChunks() {
        assertThat(Chunker.chunk("")).isEmpty();
        assertThat(Chunker.chunk("   ")).isEmpty();
    }

    @Test
    void fakeEmbeddingsAreDeterministicAndNormalised() {
        FakeEmbeddingClient client = new FakeEmbeddingClient();
        float[] a = client.embed("aliasing and mutable lists");
        float[] b = client.embed("aliasing and mutable lists");
        float[] c = client.embed("completely unrelated astronomy");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(FakeEmbeddingClient.DIMENSIONS);
        double norm = 0;
        for (float v : a) {
            norm += v * v;
        }
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
        assertThat(Embeddings.cosine(a, b)).isGreaterThan(Embeddings.cosine(a, c));
    }

    @Test
    void memoryIndexFindsASeedTitleAndNeverFabricatesBlockIds() {
        MemoryContentIndex index = new MemoryContentIndex(new FakeEmbeddingClient());
        UUID unitId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-17T12:00:00Z");

        index.reindexUnit(
                new ContentIndex.UnitDocument(
                        unitId,
                        "python-values-and-types",
                        "Values and types",
                        "How Python names point at objects",
                        "knowledge_unit",
                        "FOUNDATIONAL",
                        now),
                List.of(new ContentIndex.BlockDocument(
                        blockId,
                        unitId,
                        "beginner",
                        "Changing b also changes a when both names refer to the same list.",
                        "FOUNDATIONAL",
                        now)));

        List<ContentIndex.SearchHit> hits = index.search("Values and types", Set.of("unit"), 10);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).slug()).isEqualTo("python-values-and-types");
        assertThat(hits.get(0).updatedAt()).isEqualTo(now);

        List<ContentHit> retrieved = index.retrieve("why does changing b change a", unitId, 5);
        assertThat(retrieved).isNotEmpty();
        assertThat(retrieved).allSatisfy(hit -> {
            assertThat(hit.blockId()).isEqualTo(blockId);
            assertThat(hit.updatedAt()).isEqualTo(now);
        });
    }
}
