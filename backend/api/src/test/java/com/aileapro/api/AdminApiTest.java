package com.aileapro.api;

import com.aileapro.api.admin.AdminStudioService;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.UserRoleEntity;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 13: drafts stay invisible to learners; publish is audited; rollback restores draft.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    UserRepository users;

    @Autowired
    UserRoleRepository roles;

    @Autowired
    AuditLogRepository audits;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            seeded = true;
        }
    }

    @Test
    void learnerCannotListAdminUnits() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "admin-forbid@aileapro.dev");
        mockMvc.perform(get("/v1/admin/units").session(learner))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishIsAuditedAndDraftStaysHiddenUntilThen() throws Exception {
        String slug = "studio-" + UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession admin = signInAsAdmin("admin-pub-" + slug + "@aileapro.dev");

        mockMvc.perform(get("/v1/admin/units").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'authoring-queue-draft')].status")
                        .value(org.hamcrest.Matchers.hasItem("draft")));

        mockMvc.perform(post("/v1/admin/units")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"%s",
                                  "title":"Studio Draft",
                                  "description":"Disposable",
                                  "difficulty":2,
                                  "objectives":["State one fact"],
                                  "blocks":[
                                    {"type":"why","markdown":"Why it matters."},
                                    {"type":"beginner","markdown":"A first explanation."}
                                  ]
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.critic.publishable").value(true));

        MockHttpSession learner = TestLearner.signIn(mockMvc, "admin-learn-" + slug + "@aileapro.dev");
        mockMvc.perform(get("/v1/units/" + slug).session(learner))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/v1/admin/units/" + slug + "/publish")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Phase 13 gate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.toVersion").value("1.0.0"))
                .andExpect(jsonPath("$.auditId").exists())
                .andExpect(jsonPath("$.changeId").exists());

        String published = mockMvc.perform(get("/v1/admin/units/" + slug).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String unitId = json.readTree(published).path("id").asText();
        assertThat(audits.findByActionAndResourceIdOrderByAtDesc(AdminStudioService.AUDIT_PUBLISH, unitId))
                .isNotEmpty();

        mockMvc.perform(get("/v1/units/" + slug).session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Studio Draft"));

        mockMvc.perform(get("/v1/admin/units/" + slug + "/changelog").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("publish"));

        mockMvc.perform(post("/v1/admin/units/" + slug + "/rollback")
                        .session(admin)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.note", containsString("404")));

        mockMvc.perform(get("/v1/units/" + slug).session(learner))
                .andExpect(status().isNotFound());
    }

    @Test
    void criticBlocksIncompletePublish() throws Exception {
        String slug = "thin-" + UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession admin = signInAsAdmin("admin-thin-" + slug + "@aileapro.dev");

        mockMvc.perform(post("/v1/admin/units")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"%s","title":"Thin","objectives":[],"blocks":[]}
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.critic.publishable").value(false));

        mockMvc.perform(post("/v1/admin/units/" + slug + "/publish")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"))
                .andExpect(jsonPath("$.error.message", containsString("Critic blocked")));
    }

    @Test
    void patchUpdatesDraftOnly() throws Exception {
        String slug = "patch-" + UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession admin = signInAsAdmin("admin-patch-" + slug + "@aileapro.dev");

        mockMvc.perform(post("/v1/admin/units")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"%s",
                                  "title":"Before",
                                  "difficulty":1,
                                  "objectives":["A"],
                                  "blocks":[
                                    {"type":"why","markdown":"Why"},
                                    {"type":"beginner","markdown":"How"}
                                  ]
                                }
                                """.formatted(slug)))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/v1/admin/units/" + slug)
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"After\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("After"));

        mockMvc.perform(post("/v1/admin/units/" + slug + "/publish")
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/admin/units/" + slug)
                        .session(admin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("Published units cannot be patched")));
    }

    private MockHttpSession signInAsAdmin(String email) throws Exception {
        TestLearner.signIn(mockMvc, email);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        UserRoleEntity role = new UserRoleEntity();
        role.setUserId(user.getId());
        role.setRoleCode(RoleCodes.ADMIN);
        roles.save(role);
        // Authorities are captured at login — re-login so ROLE_ADMIN is on the session.
        return TestLearner.signIn(mockMvc, email);
    }
}
