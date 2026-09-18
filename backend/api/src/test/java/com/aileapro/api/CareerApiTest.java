package com.aileapro.api;

import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.MasteryRecordEntity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 12 gate: gaps from demonstrated mastery only; self-report never appears mastered.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareerApiTest {

    private static final String PATH = "ai-engineer-abridged";
    private static final String UNIT = "python-values-and-types";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    LearningUnitRepository units;

    @Autowired
    MasteryRecordRepository records;

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
    void interviewKindDoesNotGradeEvidence() {
        assertThat(AssessmentKind.INTERVIEW.gradesEvidence()).isFalse();
        assertThat(AssessmentKind.INTERVIEW.chip()).isNull();
    }

    @Test
    void selfReportedExperienceDoesNotShrinkGaps() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "career-self@aileapro.dev");

        mockMvc.perform(put("/v1/me/profile")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetPath":"%s","experience":"working_in_ml","hoursPerWeek":10}
                                """.formatted(PATH)))
                .andExpect(status().isOk());

        String withClaim = mockMvc.perform(get("/v1/me/career/gaps").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pathSlug").value(PATH))
                .andExpect(jsonPath("$.readyForRole").value(false))
                .andExpect(jsonPath("$.honesty", org.hamcrest.Matchers.containsString("Not job-ready")))
                .andExpect(jsonPath("$.interviewPersona").value("interview_coach"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int gapsWithClaim = json.readTree(withClaim).path("gapCount").asInt();
        assertThat(gapsWithClaim).isGreaterThan(0);

        mockMvc.perform(put("/v1/me/profile")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"experience":"none"}
                                """))
                .andExpect(status().isOk());

        String withoutClaim = mockMvc.perform(get("/v1/me/career/gaps").session(learner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(json.readTree(withoutClaim).path("gapCount").asInt())
                .as("changing self-reported experience must not change gaps")
                .isEqualTo(gapsWithClaim);
    }

    @Test
    void gapsShrinkOnlyWhenMasteryRecordReachesProficient() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "career-mastery@aileapro.dev");

        mockMvc.perform(put("/v1/me/profile")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetPath":"%s","experience":"working_in_ml"}
                                """.formatted(PATH)))
                .andExpect(status().isOk());

        String beforeBody = mockMvc.perform(get("/v1/me/career/gaps").session(learner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode before = json.readTree(beforeBody);
        int beforeGaps = before.path("gapCount").asInt();
        String userId = meId(learner);

        UUID unitId = units.findBySlugAndLocale(UNIT, "en").orElseThrow().getId();
        MasteryRecordEntity record = new MasteryRecordEntity();
        record.setId(UUID.randomUUID());
        record.setUserId(UUID.fromString(userId));
        record.setUnitId(unitId);
        record.setState(MasteryState.PROFICIENT);
        record.setScore(BigDecimal.ONE);
        record.setReadState(EvidenceState.SOLID);
        record.setPracticeState(EvidenceState.SOLID);
        record.setAssessState(EvidenceState.SOLID);
        record.setApplyState(EvidenceState.EMPTY);
        record.setUpdatedAt(Instant.now());
        records.save(record);

        String afterBody = mockMvc.perform(get("/v1/me/career/gaps").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyUnitCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode after = json.readTree(afterBody);

        assertThat(after.path("gapCount").asInt()).isEqualTo(beforeGaps - 1);
        for (JsonNode gap : after.path("gaps")) {
            assertThat(gap.path("unitSlug").asText()).isNotEqualTo(UNIT);
        }
    }

    @Test
    void interviewCoachPersonaCanStartAConversation() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "career-interview@aileapro.dev");

        mockMvc.perform(post("/v1/tutor/conversations")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitSlug":"%s","persona":"interview_coach"}
                                """.formatted(UNIT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona").value("interview_coach"));
    }

    private String meId(MockHttpSession learner) throws Exception {
        String body = mockMvc.perform(get("/v1/me").session(learner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).path("id").asText();
    }
}
