package com.aileapro.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void registerLoginMeAndForbidAdmin() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@aileapro.dev\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada@aileapro.dev"))
                .andExpect(jsonPath("$.roles[0]").value("learner"));

        mockMvc.perform(post("/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@aileapro.dev\",\"password\":\"password1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("conflict"));

        mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@aileapro.dev\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("unauthorized"));

        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@aileapro.dev\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        mockMvc.perform(get("/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@aileapro.dev"));

        mockMvc.perform(get("/v1/admin/ping").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestIdIsEchoed() throws Exception {
        mockMvc.perform(get("/v1/auth/csrf").header("X-Request-Id", "req-test-1"))
                .andExpect(status().isOk());
    }
}
