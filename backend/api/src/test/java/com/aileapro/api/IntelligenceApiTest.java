package com.aileapro.api;

import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.intelligence.seed.IntelligenceSeeder;
import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.UserRoleEntity;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 15: Evolve feed is timestamped; admin can reject a proposal; currentness copy is honest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntelligenceApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    IntelligenceSeeder intelligence;

    @Autowired
    UserRepository users;

    @Autowired
    UserRoleRepository roles;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            intelligence.seed();
            seeded = true;
        }
    }

    @Test
    void evolveFeedShowsTimestampedPublishedItems() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "evolve-feed@aileapro.dev");
        String body = mockMvc.perform(get("/v1/evolve/feed").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[0].occurredAt").exists())
                .andExpect(jsonPath("$.items[0].currentnessNote", containsString("Verified against")))
                .andExpect(jsonPath("$.honesty", containsString("fully up to date")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode feed = json.readTree(body);
        assertThat(feed.path("items").get(0).path("currentnessNote").asText())
                .contains("Verified against")
                .containsIgnoringCase("not a claim");
    }

    @Test
    void adminCanRejectPendingProposal() throws Exception {
        MockHttpSession admin = signInAsAdmin("evolve-admin@aileapro.dev");
        String list = mockMvc.perform(get("/v1/admin/proposals?status=pending").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = json.readTree(list).get(0).path("id").asText();

        mockMvc.perform(post("/v1/admin/proposals/" + id + "/reject")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not curriculum-ready yet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectReason").value("Not curriculum-ready yet"));
    }

    @Test
    void learnerForbiddenOnProposalReject() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "evolve-forbid@aileapro.dev");
        mockMvc.perform(post("/v1/admin/proposals/00000000-0000-0000-0000-000000000001/reject")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession signInAsAdmin(String email) throws Exception {
        TestLearner.signIn(mockMvc, email);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        UserRoleEntity role = new UserRoleEntity();
        role.setUserId(user.getId());
        role.setRoleCode(RoleCodes.ADMIN);
        roles.save(role);
        return TestLearner.signIn(mockMvc, email);
    }
}
