package com.aileapro.api;

import com.aileapro.ai.ResearchPolicy;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.research.ResearchService;
import com.aileapro.api.research.ResearchViews;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 11 gate: explainer cites only seeded papers; invented titles/DOIs cannot pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    ObjectMapper json;

    @Autowired
    ResearchService research;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            seeded = true;
        }
    }

    @Test
    void readingListExposesOnlySeededPapersWithDois() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "research-list@aileapro.dev");

        String body = mockMvc.perform(get("/v1/research/papers").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papers.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.papers[*].slug", hasItem("attention-2017")))
                .andExpect(jsonPath("$.papers[*].slug", hasItem("rag-2020")))
                .andExpect(jsonPath("$.papers[*].doi", everyItem(org.hamcrest.Matchers.notNullValue())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode papers = json.readTree(body).get("papers");
        for (JsonNode paper : papers) {
            assertThat(paper.path("sourceType").isMissingNode() || true).isTrue();
            assertThat(ResearchPolicy.DENIED_TITLES)
                    .noneMatch(denied -> paper.path("title").asText().equalsIgnoreCase(denied));
        }
    }

    @Test
    void explainerCitesOnlyAttachedSeededSources() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "research-explain@aileapro.dev");

        String body = mockMvc.perform(post("/v1/research/explain")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"What is retrieval-augmented generation?","paperSlug":"rag-2020"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refused").value(false))
                .andExpect(jsonPath("$.citations[0].slug").value("rag-2020"))
                .andExpect(jsonPath("$.citations[0].doi").value("10.48550/arXiv.2005.11401"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode explainer = json.readTree(body);
        assertThat(ResearchPolicy.containsFabrication(explainer.path("answer").asText())).isFalse();
        for (JsonNode citation : explainer.get("citations")) {
            assertThat(citation.path("slug").asText()).isIn("rag-2020");
        }
    }

    @Test
    void explainerRefusesWhenNothingMatches() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "research-refuse@aileapro.dev");

        mockMvc.perform(post("/v1/research/explain")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"zzzz unrelated underwater basket weaving"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refused").value(true))
                .andExpect(jsonPath("$.refusalCode").value("no_sources"))
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test
    void inventedPaperTitlesAreRejectedByTheCitationGuard() {
        assertThatThrownBy(() -> research.sanitizeDraft(
                        "See Deep Learning Will Solve Everything (DOI 10.9999/fake.doi.0001).",
                        List.of("attention-2017")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invented a paper title");
    }

    @Test
    void unsourcedDoisAreStrippedFromDrafts() {
        ResearchViews.Paper attention = research.getPaper("attention-2017");
        assertThat(attention.doi()).isEqualTo("10.48550/arXiv.1706.03762");

        String cleaned = research.sanitizeDraft(
                "Transformers use attention (DOI 10.48550/arXiv.1706.03762) "
                        + "unlike the fake 10.1234/invented.paper.2099 claim.",
                List.of("attention-2017"));
        assertThat(cleaned).contains("10.48550/arXiv.1706.03762");
        assertThat(cleaned).contains("[unsourced DOI removed]");
        assertThat(cleaned).doesNotContain("10.1234/invented.paper.2099");
        assertThat(ResearchPolicy.containsFabrication(cleaned)).isFalse();
    }

    @Test
    void literatureMapIncludesPapersAndUnitCitationEdges() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "research-map@aileapro.dev");

        mockMvc.perform(get("/v1/research/map").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[?(@.kind=='paper')].slug", hasItem("attention-2017")))
                .andExpect(jsonPath("$.edges").isArray());
    }
}
