package com.aileapro.api;

import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.project.seed.ProjectSeeder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 10 gate: finish the beginner project, portfolio shows an artifact, Apply becomes solid.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectApiTest {

    private static final String PROJECT = "linear-regression-beginner";

    private static final String MSE = """
            def mse(y_true, y_pred):
                n = len(y_true)
                return sum((a - b) ** 2 for a, b in zip(y_true, y_pred)) / n
            """;

    private static final String FIT = """
            def fit(xs, ys):
                n = len(xs)
                mean_x = sum(xs) / n
                mean_y = sum(ys) / n
                num = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
                den = sum((x - mean_x) ** 2 for x in xs)
                slope = num / den
                intercept = mean_y - slope * mean_x
                return slope, intercept
            """;

    private static final String PREDICT = """
            def fit(xs, ys):
                n = len(xs)
                mean_x = sum(xs) / n
                mean_y = sum(ys) / n
                num = sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, ys))
                den = sum((x - mean_x) ** 2 for x in xs)
                slope = num / den
                intercept = mean_y - slope * mean_x
                return slope, intercept

            def predict(x, slope, intercept):
                return slope * x + intercept
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    ProjectSeeder projectSeeder;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            projectSeeder.seed();
            seeded = true;
        }
    }

    @Test
    void listShowsPublishedAndOutlineProjects() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "studio-list@aileapro.dev");

        String body = mockMvc.perform(get("/v1/projects").session(learner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode list = json.readTree(body);
        assertThat(list.isArray()).isTrue();
        boolean sawLr = false;
        boolean sawRag = false;
        for (JsonNode node : list) {
            if ("linear-regression-beginner".equals(node.path("slug").asText())) {
                sawLr = true;
                assertThat(node.path("acceptsSubmissions").asBoolean()).isTrue();
            }
            if ("production-rag".equals(node.path("slug").asText())) {
                sawRag = true;
                assertThat(node.path("status").asText()).isEqualTo("outline");
                assertThat(node.path("acceptsSubmissions").asBoolean()).isFalse();
            }
        }
        assertThat(sawLr).isTrue();
        assertThat(sawRag).isTrue();
    }

    @Test
    void outlineProjectRejectsSubmissions() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "studio-outline@aileapro.dev");

        mockMvc.perform(post("/v1/projects/production-rag/milestones/anything/submit")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"print(1)\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFailingMilestoneDoesNotFillApply() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "studio-fail@aileapro.dev");

        mockMvc.perform(post("/v1/projects/" + PROJECT + "/milestones/mean-squared-error/submit")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"def other():\\n    return 1\\n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.projectCompleted").value(false))
                .andExpect(jsonPath("$.mentorComment", containsString("did not pass")));

        mockMvc.perform(get("/v1/units/python-functions/mastery").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.apply.state").value("empty"));
    }

    @Test
    void completingTheBeginnerProjectFillsApplyAndCreatesAPrivatePortfolioItem() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "studio-complete@aileapro.dev");

        submit(learner, "mean-squared-error", MSE);
        submit(learner, "fit-line", FIT);
        String body = mockMvc.perform(post("/v1/projects/" + PROJECT + "/milestones/predict-and-report/submit")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("source", PREDICT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.projectCompleted").value(true))
                .andExpect(jsonPath("$.portfolioItemId").isNotEmpty())
                .andExpect(jsonPath("$.applyUnitsUpdated").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode result = json.readTree(body);
        assertThat(result.get("applyUnitsUpdated").toString()).contains("python-functions");

        mockMvc.perform(get("/v1/units/python-functions/mastery").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.apply.state").value("solid"))
                .andExpect(jsonPath("$.evidence.apply.at").isNotEmpty());

        mockMvc.perform(get("/v1/portfolio").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].visibility").value("private"))
                .andExpect(jsonPath("$.items[0].projectSlug").value(PROJECT));

        String portfolioId = result.get("portfolioItemId").asText();
        mockMvc.perform(patch("/v1/portfolio/" + portfolioId)
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visibility":"public","reflection":"Fitted a line without a library."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("public"))
                .andExpect(jsonPath("$.reflection", containsString("Fitted")));
    }

    private void submit(MockHttpSession learner, String milestone, String source) throws Exception {
        mockMvc.perform(post("/v1/projects/" + PROJECT + "/milestones/" + milestone + "/submit")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("source", source))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));
    }
}
