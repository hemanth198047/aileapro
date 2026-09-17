package com.aileapro.api.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PrerequisiteDagTest {

    @Test
    void chainIsAcyclic() {
        Map<String, List<String>> precedes = Map.of(
                "vectors", List.of("matrices"),
                "matrices", List.of("matmul"),
                "matmul", List.of("neuron"));

        assertThat(PrerequisiteDag.findCycle(precedes)).isEmpty();
    }

    @Test
    void diamondIsAcyclic() {
        // Two routes into the same unit is normal curriculum shape, not a cycle.
        Map<String, List<String>> precedes = Map.of(
                "python", List.of("numpy", "pandas"),
                "numpy", List.of("neuron"),
                "pandas", List.of("neuron"));

        assertThat(PrerequisiteDag.findCycle(precedes)).isEmpty();
    }

    @Test
    void cycleIsReportedAsAWalkThatClosesOnItself() {
        Map<String, List<String>> precedes = Map.of(
                "vectors", List.of("matrices"),
                "matrices", List.of("matmul"),
                "matmul", List.of("vectors"));

        Optional<List<String>> cycle = PrerequisiteDag.findCycle(precedes);

        assertThat(cycle).isPresent();
        List<String> ring = cycle.orElseThrow();
        assertThat(ring.getFirst())
                .as("the report names the unit the ring returns to")
                .isEqualTo(ring.getLast());
        assertThat(ring).hasSize(4).containsExactlyInAnyOrder(
                ring.getFirst(), "vectors", "matrices", "matmul");
    }

    @Test
    void twoNodeCycleIsFound() {
        Map<String, List<String>> precedes = Map.of(
                "a", List.of("b"),
                "b", List.of("a"));

        assertThat(PrerequisiteDag.findCycle(precedes)).isPresent();
    }

    @Test
    void cycleIsFoundWhenItSitsInASecondComponent() {
        // The walk has to keep going after finishing a clean component.
        Map<String, List<String>> precedes = Map.of(
                "clean-1", List.of("clean-2"),
                "ring-1", List.of("ring-2"),
                "ring-2", List.of("ring-3"),
                "ring-3", List.of("ring-1"));

        assertThat(PrerequisiteDag.findCycle(precedes))
                .isPresent()
                .get()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                .contains("ring-1", "ring-2", "ring-3")
                .doesNotContain("clean-1");
    }

    @Test
    void emptyGraphIsAcyclic() {
        assertThat(PrerequisiteDag.<String>findCycle(Map.of())).isEmpty();
    }
}
