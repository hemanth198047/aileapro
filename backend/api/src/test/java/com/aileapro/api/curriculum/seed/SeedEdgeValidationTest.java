package com.aileapro.api.curriculum.seed;

import com.aileapro.api.curriculum.seed.SeedModel.SeedEdge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The seed loader is the only way edges get written in Phase 4, so it is where a prerequisite
 * cycle has to be stopped — before anything is saved, not after.
 */
class SeedEdgeValidationTest {

    private static SeedEdge prerequisite(String from, String to) {
        return new SeedEdge(from, to, "PREREQUISITE_OF", null, null, null, null);
    }

    private static SeedEdge advisory(String from, String to) {
        return new SeedEdge(from, to, "DEPENDS_ON", null, null, null, null);
    }

    @Test
    void prerequisiteCycleIsRejectedAndNamesTheRing() {
        List<SeedEdge> edges = List.of(
                prerequisite("vectors", "matrices"),
                prerequisite("matrices", "matmul"),
                prerequisite("matmul", "vectors"));

        assertThatThrownBy(() -> CurriculumSeeder.rejectPrerequisiteCycles(edges))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prerequisite cycle")
                .hasMessageContaining("vectors")
                .hasMessageContaining("matrices")
                .hasMessageContaining("matmul");
    }

    @Test
    void advisoryEdgesMayFormALoop() {
        // DEPENDS_ON never blocks anything, so a mutual "this helps" pair is allowed to exist.
        List<SeedEdge> edges = List.of(
                advisory("vectors", "rag"),
                advisory("rag", "vectors"));

        assertThatCode(() -> CurriculumSeeder.rejectPrerequisiteCycles(edges)).doesNotThrowAnyException();
    }

    @Test
    void unknownEdgeTypeIsRejectedWithTheOffendingPair() {
        List<SeedEdge> edges = List.of(new SeedEdge("vectors", "matrices", "PREREQUISITE", null, null, null, null));

        assertThatThrownBy(() -> CurriculumSeeder.rejectPrerequisiteCycles(edges))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown edge type 'PREREQUISITE'")
                .hasMessageContaining("vectors");
    }
}
