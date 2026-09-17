package com.aileapro.api;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Registers and signs in a real learner, returning the session.
 *
 * <p>Learner routes read the user from the session principal, so a stubbed
 * {@code user("ada")} would not exercise them — the tests go through registration and login
 * like a browser does. Each call sends its own forwarded address so the login rate limiter,
 * which is doing its job, does not throttle a test that logs in more than ten times.
 */
final class TestLearner {

    private static final AtomicInteger CALLS = new AtomicInteger();

    private TestLearner() {
    }

    static MockHttpSession signIn(MockMvc mockMvc, String email) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"password1\"}";
        String address = "10.0.0." + (CALLS.incrementAndGet() % 250 + 1);

        // A repeat registration is a 409, which is fine: the point is to have the account.
        mockMvc.perform(post("/v1/auth/register")
                .with(csrf())
                .header("X-Forwarded-For", address)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .with(csrf())
                        .header("X-Forwarded-For", address)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        if (login.getResponse().getStatus() != 200) {
            throw new IllegalStateException(
                    "Could not sign in " + email + ": " + login.getResponse().getContentAsString());
        }
        return (MockHttpSession) login.getRequest().getSession();
    }
}
