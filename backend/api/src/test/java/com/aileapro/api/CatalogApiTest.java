package com.aileapro.api;

import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.domain.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
class CatalogApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder seeder;

    @Autowired
    LearningUnitRepository units;

    @BeforeEach
    void seedOnce() {
        seeder.seed();
    }

    @Test
    void seedIsIdempotent() {
        CurriculumSeeder.SeedReport first = seeder.seed();
        long unitsAfterFirst = units.count();
        long publishedAfterFirst = units.countByStatus(ContentStatus.PUBLISHED);

        CurriculumSeeder.SeedReport second = seeder.seed();

        assertThat(second).isEqualTo(first);
        assertThat(units.count()).isEqualTo(unitsAfterFirst);
        assertThat(units.countByStatus(ContentStatus.PUBLISHED)).isEqualTo(publishedAfterFirst);
        // The seed is authored, not generated: it must stay small enough to review by hand.
        assertThat(units.count()).isLessThan(300);
    }

    @Test
    void learnerReadsAPublishedUnitBySlug() throws Exception {
        mockMvc.perform(get("/v1/units/matrix-multiplication").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Matrix Multiplication"))
                .andExpect(jsonPath("$.kind").value("knowledge_unit"))
                .andExpect(jsonPath("$.volatility").value("LOW"))
                .andExpect(jsonPath("$.blocks[*].type").value(hasItem("why")))
                .andExpect(jsonPath("$.blocks[*].type").value(hasItem("mastery_criteria")))
                // An explanation the learner can read without any AI involvement.
                .andExpect(jsonPath("$.blocks[?(@.type == 'beginner')].payload.markdown").exists())
                .andExpect(jsonPath("$.objectives").isNotEmpty())
                .andExpect(jsonPath("$.breadcrumb[0].slug").value("linear-algebra"))
                // Provenance is attached, with a verification date.
                .andExpect(jsonPath("$.resources[0].url").exists())
                .andExpect(jsonPath("$.resources[0].lastVerifiedAt").exists());
    }

    @Test
    void draftUnitIsInvisibleToLearners() throws Exception {
        assertThat(units.findBySlugAndLocale("mixture-of-experts-draft", "en"))
                .as("the draft unit exists in the database")
                .isPresent();

        mockMvc.perform(get("/v1/units/mixture-of-experts-draft").with(user("ada").roles("LEARNER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("not_found"));
    }

    @Test
    void draftWaypointIsDroppedFromThePublishedPath() throws Exception {
        mockMvc.perform(get("/v1/paths").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug").value(hasItem("ai-engineer-abridged")));

        mockMvc.perform(get("/v1/paths/ai-engineer-abridged").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleTarget").value("AI Engineer"))
                .andExpect(jsonPath("$.waypoints.length()").value(10))
                .andExpect(jsonPath("$.waypoints[0].unit.slug").value("instructions-and-state"))
                .andExpect(jsonPath("$.waypoints[*].unit.slug")
                        .value(everyItem(not("mixture-of-experts-draft"))))
                .andExpect(jsonPath("$.estimateMinutes").value(300));
    }

    @Test
    void domainWithoutContentIsHonestAboutIt() throws Exception {
        mockMvc.perform(get("/v1/units/ai-entrepreneurship").with(user("ada").roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("domain"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                // Mapped, described, and empty. The UI has to say so rather than imply depth.
                .andExpect(jsonPath("$.children").isEmpty())
                .andExpect(jsonPath("$.blocks").isEmpty());
    }

    @Test
    void catalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/units/matrix-multiplication"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/paths"))
                .andExpect(status().isUnauthorized());
    }
}
