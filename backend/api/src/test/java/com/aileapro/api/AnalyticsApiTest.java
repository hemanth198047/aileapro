package com.aileapro.api;

import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.MasteryState;
import com.aileapro.domain.RoleCodes;
import com.aileapro.persistence.AiUsageEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import com.aileapro.persistence.UserRoleEntity;
import com.aileapro.api.ai.AiUsageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 14: dashboard matches mastery_records; admin cost burn from ai_usage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsApiTest {

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
    AiUsageRepository usage;

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
            seeded = true;
        }
    }

    @Test
    void dashboardTimeOnTaskMatchesMasteryRecordDwell() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "dash-dwell@aileapro.dev");
        String userId = meId(learner);
        UUID unitId = units.findBySlugAndLocale(UNIT, "en").orElseThrow().getId();

        MasteryRecordEntity record = new MasteryRecordEntity();
        record.setId(UUID.randomUUID());
        record.setUserId(UUID.fromString(userId));
        record.setUnitId(unitId);
        record.setState(MasteryState.INTRODUCED);
        record.setScore(BigDecimal.ZERO);
        record.setReadState(EvidenceState.SOLID);
        record.setPracticeState(EvidenceState.EMPTY);
        record.setAssessState(EvidenceState.EMPTY);
        record.setApplyState(EvidenceState.EMPTY);
        record.setDwellSeconds(240);
        record.setOpenedCount(2);
        record.setOpenMisconceptions(1);
        record.setUpdatedAt(Instant.now());
        records.save(record);

        String body = mockMvc.perform(get("/v1/me/dashboard").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeOnTaskSeconds").value(240))
                .andExpect(jsonPath("$.weak[0].unitSlug").value(UNIT))
                .andExpect(jsonPath("$.weak[0].openMisconceptions").value(1))
                .andExpect(jsonPath("$.note", org.hamcrest.Matchers.containsString("No XP")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode dash = json.readTree(body);
        assertThat(dash.path("timeOnTaskSeconds").asInt()).isEqualTo(240);
        assertThat(dash.path("practicedOrHigherInWindow").asInt()).isZero();
    }

    @Test
    void velocityCountsRecentPracticedFromRecords() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "dash-vel@aileapro.dev");
        String userId = meId(learner);
        UUID unitId = units.findBySlugAndLocale(UNIT, "en").orElseThrow().getId();

        MasteryRecordEntity record = new MasteryRecordEntity();
        record.setId(UUID.randomUUID());
        record.setUserId(UUID.fromString(userId));
        record.setUnitId(unitId);
        record.setState(MasteryState.PRACTICED);
        record.setScore(BigDecimal.ONE);
        record.setReadState(EvidenceState.SOLID);
        record.setPracticeState(EvidenceState.SOLID);
        record.setAssessState(EvidenceState.EMPTY);
        record.setApplyState(EvidenceState.EMPTY);
        record.setDwellSeconds(60);
        record.setLastGradedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        records.save(record);

        mockMvc.perform(get("/v1/me/dashboard").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practicedOrHigherInWindow").value(1))
                .andExpect(jsonPath("$.velocityWindowDays").value(7));
    }

    @Test
    void learnerForbiddenOnAdminAnalytics() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "analytics-forbid@aileapro.dev");
        mockMvc.perform(get("/v1/admin/analytics").session(learner))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCostBurnSumsAiUsage() throws Exception {
        MockHttpSession admin = signInAsAdmin("analytics-admin@aileapro.dev");
        UUID actor = users.findByEmailIgnoreCase("analytics-admin@aileapro.dev").orElseThrow().getId();

        AiUsageEntity row = new AiUsageEntity();
        row.setId(UUID.randomUUID());
        row.setRequestId("phase14-test");
        row.setUserId(actor);
        row.setFeature("tutor");
        row.setProfileSlug("tutor-default");
        row.setProviderSlug("fixture");
        row.setModelSlug("fixture");
        row.setTokensIn(100);
        row.setTokensOut(50);
        row.setLatencyMs(12);
        row.setCostUsd(new BigDecimal("0.42"));
        row.setOutcome("ok");
        row.setAt(Instant.now());
        usage.save(row);

        mockMvc.perform(get("/v1/admin/analytics/cost?days=30").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costUsd").value(0.42))
                .andExpect(jsonPath("$.tokensIn").value(100))
                .andExpect(jsonPath("$.tokensOut").value(50))
                .andExpect(jsonPath("$.calls").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.byFeature[0].feature").value("tutor"))
                .andExpect(jsonPath("$.note", org.hamcrest.Matchers.containsString("Estimated")));

        mockMvc.perform(get("/v1/admin/analytics/funnel").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.note", org.hamcrest.Matchers.containsString("PRACTICED")));
    }

    private String meId(MockHttpSession session) throws Exception {
        String body = mockMvc.perform(get("/v1/me").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).path("id").asText();
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
