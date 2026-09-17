package com.aileapro.api;

import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.graph.GraphQueryService;
import com.aileapro.api.graph.GraphViews.Neighborhood;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GraphApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder seeder;

    @Autowired
    ObjectMapper json;

    @BeforeEach
    void seedOnce() {
        seeder.seed();
    }

    @Test
    void prerequisiteChainIsTransitiveAndInStudyOrder() throws Exception {
        mockMvc.perform(get("/v1/units/how-a-neuron-learns/prerequisites").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                // vectors -> matrices -> matmul, so the furthest ancestor is what to study first.
                .andExpect(jsonPath("$.required[0].unit.slug").value("vectors"))
                .andExpect(jsonPath("$.required[0].depth").value(3))
                .andExpect(jsonPath("$.required[0].direct").value(false))
                .andExpect(jsonPath("$.required[1].unit.slug").value("matrices"))
                .andExpect(jsonPath("$.required[2].unit.slug").value("matrix-multiplication"))
                .andExpect(jsonPath("$.required[2].direct").value(true))
                // DEPENDS_ON helps but does not block, so it is reported separately.
                .andExpect(jsonPath("$.advisory[*].unit.slug").value(hasItem("python-functions")))
                .andExpect(jsonPath("$.advisory[*].unit.slug").value(everyItem(not("matrix-multiplication"))))
                // Nothing here claims to know what the learner has mastered.
                .andExpect(jsonPath("$.masteryTracked").value(false))
                .andExpect(jsonPath("$.required[*].masteryState").value(everyItem(equalTo("unknown"))));
    }

    @Test
    void neighbourhoodExcludesDraftUnitsAndTheEdgesThatTouchThem() throws Exception {
        mockMvc.perform(get("/v1/units/matrix-multiplication/graph?hops=1").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[*].unit.slug").value(hasItem("matrices")))
                .andExpect(jsonPath("$.nodes[*].unit.slug").value(hasItem("how-a-neuron-learns")))
                // The seed authors two edges into the draft attention unit on purpose.
                .andExpect(jsonPath("$.nodes[*].unit.slug")
                        .value(everyItem(not("scaled-dot-product-attention"))));

        Neighborhood neighborhood = neighborhood("/v1/units/matrix-multiplication/graph?hops=1");
        Set<UUID> nodeIds = neighborhood.nodes().stream()
                .map(node -> node.unit().id())
                .collect(Collectors.toSet());
        assertThat(neighborhood.edges())
                .as("no edge may point at a node the client was not given")
                .allSatisfy(edge -> assertThat(nodeIds).contains(edge.fromId(), edge.toId()));
    }

    @Test
    void mapDoesNotDownloadTheWholeGraph() throws Exception {
        Neighborhood wide = neighborhood("/v1/units/matrix-multiplication/graph?hops=99");

        assertThat(wide.hops())
                .as("the server clamps the radius rather than honouring any hop count")
                .isEqualTo(GraphQueryService.MAX_HOPS);
        assertThat(wide.maxHops()).isEqualTo(GraphQueryService.MAX_HOPS);
        assertThat(wide.nodeBudget()).isEqualTo(GraphQueryService.NODE_BUDGET);
        assertThat(wide.nodes()).hasSizeLessThanOrEqualTo(GraphQueryService.NODE_BUDGET);
        assertThat(wide.nodes().size())
                .as("a neighbourhood is a slice, not the 71 seeded units")
                .isLessThan(20);
    }

    @Test
    void oneHopIsNarrowerThanThree() throws Exception {
        int near = neighborhood("/v1/units/matrix-multiplication/graph?hops=1").nodes().size();
        int far = neighborhood("/v1/units/matrix-multiplication/graph?hops=3").nodes().size();

        assertThat(near).isLessThan(far);
    }

    @Test
    void edgeTypeFilterIsApplied() throws Exception {
        Neighborhood onlyPrerequisites =
                neighborhood("/v1/units/matrix-multiplication/graph?hops=2&types=PREREQUISITE_OF");

        assertThat(onlyPrerequisites.edges()).isNotEmpty().allSatisfy(edge -> {
            assertThat(edge.type()).isEqualTo("PREREQUISITE_OF");
            assertThat(edge.gating()).isTrue();
        });
    }

    @Test
    void unknownEdgeTypeIsARequestErrorNotAServerError() throws Exception {
        mockMvc.perform(get("/v1/units/matrix-multiplication/graph?types=IS_FRIENDS_WITH")
                        .with(user("ada").roles("LEARNER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"))
                .andExpect(jsonPath("$.error.message").value(containsString("PREREQUISITE_OF")));
    }

    @Test
    void draftUnitHasNoGraphOfItsOwn() throws Exception {
        mockMvc.perform(get("/v1/units/scaled-dot-product-attention/graph").with(user("ada").roles("LEARNER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/units/scaled-dot-product-attention/prerequisites")
                        .with(user("ada").roles("LEARNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unitWithNoPrerequisitesSaysSoWithoutInventingAny() throws Exception {
        mockMvc.perform(get("/v1/units/instructions-and-state/prerequisites").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").isEmpty())
                .andExpect(jsonPath("$.advisory").isEmpty());
    }

    @Test
    void graphRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/units/matrix-multiplication/graph")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/units/matrix-multiplication/prerequisites")).andExpect(status().isUnauthorized());
    }

    private Neighborhood neighborhood(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url).with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andReturn();
        return json.readValue(result.getResponse().getContentAsString(), Neighborhood.class);
    }
}
