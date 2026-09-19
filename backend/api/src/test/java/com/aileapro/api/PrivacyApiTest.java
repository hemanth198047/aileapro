package com.aileapro.api;

import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.UserRoleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivacyApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository users;

    @Autowired
    UserRoleRepository roles;

    @Test
    void exportAndAnonymize() throws Exception {
        String email = "privacy-export-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@aileapro.dev";
        MockHttpSession session = TestLearner.signIn(mockMvc, email);
        var before = users.findByEmailIgnoreCase(email).orElseThrow();

        mockMvc.perform(get("/v1/me/export").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value(email))
                .andExpect(jsonPath("$.exportedAt").exists());

        mockMvc.perform(delete("/v1/me").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("deleted"));

        var user = users.findById(before.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(user.getStatus()).isEqualTo("deleted");
        org.assertj.core.api.Assertions.assertThat(user.getEmail()).startsWith("deleted-");
        org.assertj.core.api.Assertions.assertThat(user.getPasswordHash()).isNull();

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .header("X-Forwarded-For", "10.9.9.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanToggleLabFlag() throws Exception {
        String email = "flags-admin-" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@aileapro.dev";
        MockHttpSession session = TestLearner.signIn(mockMvc, email);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        UserRoleEntity role = new UserRoleEntity();
        role.setUserId(user.getId());
        role.setRoleCode(RoleCodes.ADMIN);
        roles.save(role);

        // Re-login so authorities refresh
        session = TestLearner.signIn(mockMvc, email);

        try {
            mockMvc.perform(get("/v1/flags").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lab_enabled").value(true));

            mockMvc.perform(put("/v1/admin/flags/lab_enabled")
                            .with(csrf())
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));

            mockMvc.perform(get("/v1/lab/status").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));

            mockMvc.perform(post("/v1/lab/runs")
                            .with(csrf())
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"language\":\"python\",\"source\":\"print(1)\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.message", containsString("disabled")));
        } finally {
            mockMvc.perform(put("/v1/admin/flags/lab_enabled")
                            .with(csrf())
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"enabled\":true}"))
                    .andExpect(status().isOk());
        }
    }
}
