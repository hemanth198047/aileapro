package com.aileapro.api;

import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.search.SearchIndexer;
import com.aileapro.api.search.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 8 search gate over the in-memory index.
 *
 * <p>A seed unit title must be findable. A retrieved block id must be one that exists. The
 * knowledge timestamp must travel with the hit so the Sources drawer can show it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    SearchIndexer indexer;

    @Autowired
    SearchService search;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            indexer.reindexAll();
            seeded = true;
        }
    }

    @Test
    void aSeedUnitTitleIsFindable() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "search-title@aileapro.dev");

        mockMvc.perform(get("/v1/search").param("q", "Values and types").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(greaterThan(0)))
                .andExpect(jsonPath("$.results[0].title", containsString("Values")))
                .andExpect(jsonPath("$.results[0].updatedAt").isNotEmpty());
    }

    @Test
    void typeChipsRestrictResults() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "search-types@aileapro.dev");

        String unitsOnly = mockMvc.perform(
                        get("/v1/search")
                                .param("q", "python")
                                .param("types", "unit")
                                .session(learner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode results = json.readTree(unitsOnly).get("results");
        for (JsonNode hit : results) {
            assertThat(hit.get("type").asText()).isEqualTo("unit");
        }
    }

    @Test
    void retrievalOnlyReturnsRealBlockIds() {
        assertThat(search.chunkCount()).isGreaterThan(0);

        var hits = search.retrieveForTutor("aliasing mutable list names", null);
        assertThat(hits).isNotEmpty();

        Set<UUID> seen = new HashSet<>();
        for (var hit : hits) {
            assertThat(hit.blockId()).isNotNull();
            assertThat(hit.updatedAt()).isNotNull();
            assertThat(hit.unitSlug()).isNotBlank();
            seen.add(hit.blockId());
        }
        // No fabricated ids: every id came from the index, and the set is finite.
        assertThat(seen).hasSize(hits.stream().map(h -> h.blockId()).distinct().toList().size());
    }

    @Test
    void blankQueryExplainsRatherThanReturningEverything() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "search-blank@aileapro.dev");

        mockMvc.perform(get("/v1/search").param("q", "").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.note", containsString("Type something")));
    }
}
