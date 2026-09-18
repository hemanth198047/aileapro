package com.aileapro.api;

import com.aileapro.lab.FakeExecutorClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 9 lab gate over the fake executor.
 *
 * <p>The demo snippet must produce stdout. A forced-timeout source must land as timed_out.
 * Fixture replies are labelled development so a canned print cannot look like a real sandbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LabApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper json;

    @Test
    void theDemoSnippetRunsAndShowsOutput() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "lab-demo@aileapro.dev");

        String body = mockMvc.perform(post("/v1/lab/runs")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"python","source":"a = [1, 2, 3]\\nb = a\\nprint(a)\\n"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.development").value(true))
                .andExpect(jsonPath("$.stdout", containsString("[1, 2, 3]")))
                .andExpect(jsonPath("$.stdout", containsString("Development implementation")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode run = json.readTree(body);
        mockMvc.perform(get("/v1/lab/runs/" + run.get("id").asText()).session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceHash").isNotEmpty());
    }

    @Test
    void aTimeoutMarkerIsKilledAndReported() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "lab-timeout@aileapro.dev");

        mockMvc.perform(post("/v1/lab/runs")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"python","source":"print(1)\\n# __AILEAPRO_FORCE_TIMEOUT__\\n","timeoutMs":200}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("timed_out"))
                .andExpect(jsonPath("$.timedOut").value(true))
                .andExpect(jsonPath("$.errorCode").value("sandbox_timeout"));
    }

    @Test
    void helloFixtureIsRecognised() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "lab-hello@aileapro.dev");

        mockMvc.perform(post("/v1/lab/runs")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"python","source":"print(\\"Hello, AILEAPRO\\")\\n"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stdout", containsString("Hello, AILEAPRO")));
    }

    @Test
    void historyAndStatusExposeTheQuota() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "lab-status@aileapro.dev");

        mockMvc.perform(get("/v1/lab/status").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.development").value(true))
                .andExpect(jsonPath("$.executorKind").value(FakeExecutorClient.KIND))
                .andExpect(jsonPath("$.demoSource", containsString("print(a)")));

        mockMvc.perform(get("/v1/lab/runs").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyCap").value(40))
                .andExpect(jsonPath("$.note").isNotEmpty());
    }

    @Test
    void anotherLearnersRunIsNotVisible() throws Exception {
        MockHttpSession ada = TestLearner.signIn(mockMvc, "lab-ada@aileapro.dev");
        MockHttpSession grace = TestLearner.signIn(mockMvc, "lab-grace@aileapro.dev");

        String body = mockMvc.perform(post("/v1/lab/runs")
                        .session(ada)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"python","source":"print(\\"secret\\")\\n"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = json.readTree(body).get("id").asText();

        mockMvc.perform(get("/v1/lab/runs/" + id).session(grace))
                .andExpect(status().isNotFound());
    }
}
