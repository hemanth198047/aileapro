package com.aileapro.api;

import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Phase 5 gate, as HTTP.
 *
 * <p>The tests that matter most are the ones that try to cheat: open a page and see whether
 * the platform calls it done, reveal a self-check answer and see whether Assess lights up,
 * hammer a dwell heartbeat and see whether a minute of attention buys an hour of credit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearningApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder seeder;

    static MockHttpSession session;

    @BeforeEach
    void seedAndSignIn() throws Exception {
        seeder.seed();
        if (session == null) {
            session = TestLearner.signIn(mockMvc, "learner@aileapro.dev");
        }
    }

    @Test
    void openingAUnitDoesNotCompleteIt() throws Exception {
        event("vectors", "{\"kind\":\"unit_opened\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("UNKNOWN"))
                .andExpect(jsonPath("$.evidence.read.state").value("partial"))
                .andExpect(jsonPath("$.evidence.practice.state").value("empty"))
                .andExpect(jsonPath("$.evidence.assess.state").value("empty"))
                .andExpect(jsonPath("$.evidence.apply.state").value("empty"))
                .andExpect(jsonPath("$.openedCount").value(1));

        // And the read is still only where the learner left it after coming back.
        mockMvc.perform(get("/v1/units/vectors/mastery").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("UNKNOWN"))
                .andExpect(jsonPath("$.evidence.read.state").value("partial"));
    }

    @Test
    void revealingAnswersAndLookingAtExercisesEarnNothing() throws Exception {
        event("matrices", "{\"kind\":\"unit_opened\"}").andExpect(status().isOk());
        event("matrices", "{\"kind\":\"self_check_revealed\"}").andExpect(status().isOk());
        event("matrices", "{\"kind\":\"exercise_viewed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.assess.state").value("empty"))
                .andExpect(jsonPath("$.evidence.practice.state").value("empty"))
                .andExpect(jsonPath("$.evidence.apply.state").value("empty"))
                .andExpect(jsonPath("$.state").value("UNKNOWN"));
    }

    @Test
    void aDwellHeartbeatCannotClaimMoreTimeThanItRepresents() throws Exception {
        event("matrix-multiplication", "{\"kind\":\"unit_opened\"}").andExpect(status().isOk());
        event("matrix-multiplication", "{\"kind\":\"dwell\",\"seconds\":99999}")
                .andExpect(status().isOk())
                // Clamped to one heartbeat's worth, whatever the client asked for.
                .andExpect(jsonPath("$.dwellSeconds").value(120))
                .andExpect(jsonPath("$.evidence.read.state").value("partial"));
    }

    @Test
    void readingProperlyReachesIntroducedAndStops() throws Exception {
        String slug = "instructions-and-state";
        event(slug, "{\"kind\":\"unit_opened\"}").andExpect(status().isOk());
        event(slug, "{\"kind\":\"explanation_level\",\"level\":\"beginner\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levelsSeen[0]").value("beginner"));

        for (int beat = 0; beat < 12; beat++) {
            event(slug, "{\"kind\":\"dwell\",\"seconds\":120}").andExpect(status().isOk());
        }

        mockMvc.perform(get("/v1/units/" + slug + "/mastery").session(session))
                .andExpect(jsonPath("$.evidence.read.state").value("solid"))
                .andExpect(jsonPath("$.evidence.read.at").isNotEmpty())
                .andExpect(jsonPath("$.state").value("INTRODUCED"))
                // The ceiling: everything above INTRODUCED needs work the learner produced.
                .andExpect(jsonPath("$.evidence.practice.state").value("empty"))
                .andExpect(jsonPath("$.score").value(org.hamcrest.Matchers.lessThanOrEqualTo(0.1)));
    }

    @Test
    void anUnknownEventKindIsARequestErrorNotAServerError() throws Exception {
        event("vectors", "{\"kind\":\"mastered_it\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"))
                .andExpect(jsonPath("$.error.message").value(containsString("unit_opened")));
    }

    @Test
    void aDraftUnitCannotCollectEvidence() throws Exception {
        event("mixture-of-experts-draft", "{\"kind\":\"unit_opened\"}")
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/units/mixture-of-experts-draft/mastery").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void learnerRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v1/me/today")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/me/next")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/me/notes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/units/vectors/mastery")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/units/vectors/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"unit_opened\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void todayAsksForOnboardingBeforeItRecommendsAnything() throws Exception {
        MockHttpSession fresh = TestLearner.signIn(mockMvc, "fresh@aileapro.dev");

        mockMvc.perform(get("/v1/me/profile").session(fresh))
                .andExpect(status().isOk())
                .andExpect(content -> {
                    String body = content.getResponse().getContentAsString();
                    if (!body.isBlank()) {
                        throw new AssertionError("A learner with no profile should get an empty body, got: " + body);
                    }
                });

        mockMvc.perform(get("/v1/me/today").session(fresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(false))
                .andExpect(jsonPath("$.next.rule").value("onboarding"))
                .andExpect(jsonPath("$.next.href").value("/app/onboarding"))
                // The queue is empty because cards are enrolled by reading, and this learner has
                // read nothing. The note says that rather than leaving a blank panel.
                .andExpect(jsonPath("$.reviews").isEmpty())
                .andExpect(jsonPath("$.reviewsNote").value(containsString("finish reading a unit")));
    }

    @Test
    void onceOnboardedTodayNamesAVerbAnObjectAndAnEstimate() throws Exception {
        MockHttpSession onboarded = TestLearner.signIn(mockMvc, "onboarded@aileapro.dev");

        mockMvc.perform(put("/v1/me/profile")
                        .session(onboarded)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPath\":\"ai-engineer-abridged\",\"hoursPerWeek\":6,"
                                + "\"modalities\":[\"visual\",\"code\"],\"experience\":\"some_programming\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetPathSlug").value("ai-engineer-abridged"))
                .andExpect(jsonPath("$.hoursPerWeek").value(6))
                .andExpect(jsonPath("$.onboardedAt").isNotEmpty());

        mockMvc.perform(get("/v1/me/today").session(onboarded))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(true))
                .andExpect(jsonPath("$.pathSlug").value("ai-engineer-abridged"))
                .andExpect(jsonPath("$.waypointCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.next.kind").value("read"))
                .andExpect(jsonPath("$.next.verb").value("Read"))
                .andExpect(jsonPath("$.next.object").isNotEmpty())
                .andExpect(jsonPath("$.next.minutes").value(greaterThan(0)))
                // The recommendation explains itself in a sentence, not just "Continue".
                .andExpect(jsonPath("$.next.reason").value(containsString("waypoint")))
                .andExpect(jsonPath("$.next.reason").value(not(containsString("Continue"))))
                .andExpect(jsonPath("$.next.href").value(containsString("/app/learn/")));
    }

    @Test
    void anInvalidProfileIsRejectedFieldByField() throws Exception {
        mockMvc.perform(put("/v1/me/profile")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hoursPerWeek\":900}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(containsString("hoursPerWeek")));

        mockMvc.perform(put("/v1/me/profile")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modalities\":[\"telepathy\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(containsString("telepathy")));

        mockMvc.perform(put("/v1/me/profile")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPath\":\"path-to-enlightenment\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void theRoadmapCarriesTheRequirementAndWhetherItIsMet() throws Exception {
        mockMvc.perform(get("/v1/me/roadmap?path=ai-engineer-abridged").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pathSlug").value("ai-engineer-abridged"))
                .andExpect(jsonPath("$.stops[0].requiredMasteryState").value("PRACTICED"))
                // Reading cannot satisfy a waypoint that asks for PRACTICED, and the roadmap
                // must say so rather than ticking the box.
                .andExpect(jsonPath("$.stops[0].requirementMet").value(false))
                .andExpect(jsonPath("$.stops[0].mastery.state").isNotEmpty())
                .andExpect(jsonPath("$.stops[0].mastery.readFloorSeconds").value(greaterThan(0)));
    }

    @Test
    void notesAreWrittenListedAndDeletedOnTheUnitTheyBelongTo() throws Exception {
        String created = mockMvc.perform(post("/v1/units/vectors/notes")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"A vector is a direction with a size.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitSlug").value("vectors"))
                .andExpect(jsonPath("$.body").value("A vector is a direction with a size."))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String noteId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/v1/units/vectors/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("A vector is a direction with a size."));

        mockMvc.perform(get("/v1/me/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unitTitle").isNotEmpty());

        mockMvc.perform(post("/v1/units/vectors/notes")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/v1/units/vectors/notes/" + noteId).session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/units/vectors/notes").session(session))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void oneLearnerCannotReachAnotherLearnersNote() throws Exception {
        MockHttpSession owner = TestLearner.signIn(mockMvc, "owner@aileapro.dev");
        MockHttpSession stranger = TestLearner.signIn(mockMvc, "stranger@aileapro.dev");

        String created = mockMvc.perform(post("/v1/units/vectors/notes")
                        .session(owner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Mine.\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String noteId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/v1/units/vectors/notes").session(stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(delete("/v1/units/vectors/notes/" + noteId).session(stranger).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void evidenceIsPerLearnerNotPerUnit() throws Exception {
        MockHttpSession reader = TestLearner.signIn(mockMvc, "reader@aileapro.dev");
        MockHttpSession bystander = TestLearner.signIn(mockMvc, "bystander@aileapro.dev");

        mockMvc.perform(post("/v1/units/vectors/events")
                        .session(reader)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"unit_opened\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/units/vectors/mastery").session(bystander))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.read.state").value("empty"))
                .andExpect(jsonPath("$.openedCount").value(0));
    }

    private ResultActions event(String slug, String body) throws Exception {
        return mockMvc.perform(post("/v1/units/" + slug + "/events")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
