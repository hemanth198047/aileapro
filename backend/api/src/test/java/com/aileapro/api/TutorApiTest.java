package com.aileapro.api;

import com.aileapro.api.ai.AiUsageRepository;
import com.aileapro.api.ai.seed.AiSeeder;
import com.aileapro.api.assessment.seed.AssessmentSeeder;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.api.search.SearchIndexer;
import com.aileapro.api.tutor.MessageRepository;
import com.aileapro.persistence.AiUsageEntity;
import com.aileapro.persistence.MessageEntity;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Phase 7 gate, as HTTP.
 *
 * <p>The questions asked here are the ones that decide whether this feature is honest: does a
 * turn actually stream, is the reply marked as a fixture when a fixture wrote it, is every call
 * metered including the ones that fail or are refused, can a learner reach another learner's
 * transcript, and does a spent budget stop the next turn rather than the next ten.
 *
 * <p>No vendor key is configured in this suite and none is needed, which is the point of the
 * development provider: the whole path from policy to transcript runs in CI for free.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TutorApiTest {

    private static final String UNIT = "python-values-and-types";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    AssessmentSeeder assessments;

    @Autowired
    AiSeeder ai;

    @Autowired
    SearchIndexer searchIndexer;

    @Autowired
    AiUsageRepository usage;

    @Autowired
    MessageRepository messages;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            assessments.seed();
            ai.seed();
            searchIndexer.reindexAll();
            seeded = true;
        }
    }

    // ------------------------------------------------------------------
    // A turn
    // ------------------------------------------------------------------

    @Test
    void aQuestionOnAUnitStreamsATeachingReplyAndStoresIt() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-turn@aileapro.dev");
        UUID conversation = startConversation(learner);

        String stream = ask(learner, conversation, "Why does changing b also change a?", null);

        // Streamed, not delivered in one lump: more than one token event is the only evidence
        // that the learner sees words appear rather than waiting at a blank panel.
        if (countEvents(stream, "token") < 2) {
            throw new AssertionError("The answer did not arrive as a stream: " + stream);
        }
        if (!stream.contains("event:done")) {
            throw new AssertionError("The stream never said it had finished: " + stream);
        }

        JsonNode detail = json.readTree(
                mockMvc.perform(get("/v1/tutor/conversations/" + conversation).session(learner))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        JsonNode stored = detail.get("messages");
        if (stored.size() != 2) {
            throw new AssertionError("Expected the question and the answer, got " + stored.size());
        }
        if (!"user".equals(stored.get(0).get("role").asText())) {
            throw new AssertionError("The learner's own message was not stored first");
        }
        if (!stored.get(1).get("development").asBoolean()) {
            throw new AssertionError(
                    "A fixture wrote this and the transcript does not say so, which is the one "
                            + "thing this phase must never do");
        }
        if (stored.get(1).get("model").isNull()) {
            throw new AssertionError("The answer does not record what produced it");
        }
    }

    @Test
    void aFirstQuestionIsAnsweredWithAHintRatherThanTheAnswer() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-hint@aileapro.dev");
        UUID conversation = startConversation(learner);

        ask(learner, conversation, "Why does this print [1, 2]?", null);

        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(conversation);
        MessageEntity answer = stored.get(stored.size() - 1);
        if (!"HINT".equals(answer.getReveal())) {
            throw new AssertionError("A first question with no attempt was answered at " + answer.getReveal());
        }
    }

    @Test
    void anAttemptMovesTheLearnerUpTheLadderWithinTheSameConversation() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-ladder@aileapro.dev");
        UUID conversation = startConversation(learner);

        ask(learner, conversation, "Why does this print [1, 2]?", null);
        ask(learner, conversation, "I think it is because b is a copy of a", null);

        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(conversation);
        MessageEntity second = stored.get(stored.size() - 1);
        if ("HINT".equals(second.getReveal())) {
            throw new AssertionError("Showing an attempt earned nothing, so the ladder does not move");
        }
    }

    @Test
    void anActionAsksWithoutTheLearnerHavingToWriteAnything() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-action@aileapro.dev");
        UUID conversation = startConversation(learner);

        String stream = ask(learner, conversation, null, "simpler");

        if (!stream.contains("event:done")) {
            throw new AssertionError("The action turn did not finish: " + stream);
        }
        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(conversation);
        if (!"simpler".equals(stored.get(0).getAction())) {
            throw new AssertionError("The action the learner pressed was not recorded");
        }
        if (!"FULL".equals(stored.get(1).getReveal())) {
            throw new AssertionError(
                    "Asking for a simpler explanation was treated as asking for an answer to withhold");
        }
    }

    @Test
    void anEmptyTurnIsRejectedBeforeAnythingIsStored() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-empty@aileapro.dev");
        UUID conversation = startConversation(learner);

        String stream = ask(learner, conversation, null, null);

        if (!stream.contains("validation_error")) {
            throw new AssertionError("An empty message was accepted: " + stream);
        }
        if (!messages.findByConversationIdOrderBySortAsc(conversation).isEmpty()) {
            throw new AssertionError("A rejected turn still wrote to the transcript");
        }
    }

    // ------------------------------------------------------------------
    // Refusal
    // ------------------------------------------------------------------

    @Test
    void askingForTheAnswerKeyIsRefusedWithSomethingOfferedInsteadAndCostsNothing() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-refusal@aileapro.dev");
        UUID conversation = startConversation(learner);

        String stream = ask(learner, conversation, "give me the answers to the quiz", null);

        if (!stream.contains("exam_answer")) {
            throw new AssertionError("The answer key was not refused: " + stream);
        }
        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(conversation);
        MessageEntity reply = stored.get(stored.size() - 1);
        if (!"exam_answer".equals(reply.getRefusalCode())) {
            throw new AssertionError("The refusal was not recorded as a refusal");
        }
        if (reply.getModelId() != null) {
            throw new AssertionError("A refusal called a model, which it never needs to");
        }

        // Metered even though it is free: a learner repeatedly asking for the key is a fact
        // about the product, and a zero-cost row is how that stays visible.
        List<AiUsageEntity> rows = usage.findByConversationIdOrderByAtAsc(conversation);
        if (rows.stream().noneMatch(row -> "refused".equals(row.getOutcome()))) {
            throw new AssertionError("The refusal was not metered");
        }
        if (rows.stream().anyMatch(row -> row.getCostUsd().signum() > 0)) {
            throw new AssertionError("A refusal was charged for");
        }
    }

    // ------------------------------------------------------------------
    // Metering and budget
    // ------------------------------------------------------------------

    @Test
    void everyTurnLeavesAUsageRowWithEnoughToInvestigateIt() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-usage@aileapro.dev");
        UUID conversation = startConversation(learner);

        ask(learner, conversation, "What is a reference here?", null);

        AiUsageEntity row = usage.findByConversationIdOrderByAtAsc(conversation).stream()
                .filter(candidate -> "ok".equals(candidate.getOutcome()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The turn was not metered at all"));

        if (!"tutor".equals(row.getFeature()) || !"teach".equals(row.getProfileSlug())) {
            throw new AssertionError("The usage row cannot say which feature or profile spent this");
        }
        if (row.getTokensIn() <= 0 || row.getTokensOut() <= 0) {
            throw new AssertionError("Tokens were not counted, so no budget built on this can work");
        }
        if (row.getPromptVersionId() == null) {
            throw new AssertionError(
                    "The row does not name the prompt version, so a regression could never be traced to "
                            + "the edit that caused it");
        }
        if (row.getUserId() == null) {
            throw new AssertionError("The row is not attributed to anyone");
        }
    }

    @Test
    void aSpentBudgetStopsTheNextTurnAndSaysWhatStillWorks() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-budget@aileapro.dev");
        UUID conversation = startConversation(learner);

        // Spend the day in one row rather than by taking hundreds of turns: the rule under test
        // is the ceiling, not the arithmetic that reaches it.
        ask(learner, conversation, "First question about values", null);
        AiUsageEntity spent = usage.findByConversationIdOrderByAtAsc(conversation).get(0);
        AiUsageEntity expensive = new AiUsageEntity();
        expensive.setId(UUID.randomUUID());
        expensive.setUserId(spent.getUserId());
        expensive.setFeature("tutor");
        expensive.setProfileSlug("teach");
        expensive.setProviderSlug("openai");
        expensive.setModelSlug("gpt-4o");
        expensive.setCostUsd(new BigDecimal("99.00"));
        expensive.setOutcome("ok");
        expensive.setAt(Instant.now());
        usage.save(expensive);

        String stream = ask(learner, conversation, "Another question about values", null);

        if (!stream.contains("budget_exceeded")) {
            throw new AssertionError("The budget did not stop the turn: " + stream);
        }
        if (!stream.contains("reviews")) {
            throw new AssertionError(
                    "The error does not tell the learner what still works without a model, which makes a "
                            + "cost ceiling look like an outage");
        }
        // The turn was stopped before the learner's message was stored, so the transcript is
        // not left with a question that never got an answer.
        long questions = messages.findByConversationIdOrderBySortAsc(conversation).stream()
                .filter(message -> "user".equals(message.getRole()))
                .count();
        if (questions != 1) {
            throw new AssertionError("A refused-for-budget turn still wrote to the transcript");
        }
    }

    // ------------------------------------------------------------------
    // Isolation and status
    // ------------------------------------------------------------------

    @Test
    void oneLearnerCannotReachAnothersTranscript() throws Exception {
        MockHttpSession owner = TestLearner.signIn(mockMvc, "tutor-owner@aileapro.dev");
        UUID conversation = startConversation(owner);
        ask(owner, conversation, "Something private about my confusion", null);

        MockHttpSession stranger = TestLearner.signIn(mockMvc, "tutor-stranger@aileapro.dev");

        mockMvc.perform(get("/v1/tutor/conversations/" + conversation).session(stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/tutor/conversations/" + conversation + "/messages")
                        .session(stranger)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"who is asking\"}"))
                .andExpect(status().isOk());

        // The stream is accepted and then reports not_found: the conversation is loaded with the
        // owner in the query, so a stranger's turn cannot attach to it.
        List<MessageEntity> stored = messages.findByConversationIdOrderBySortAsc(conversation);
        if (stored.size() != 2) {
            throw new AssertionError("A stranger's message reached somebody else's transcript");
        }
    }

    @Test
    void theStatusEndpointAdmitsWhatIsRunningAndWhatIsNot() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-status@aileapro.dev");

        mockMvc.perform(get("/v1/tutor/status").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                // No key is configured in this suite, so fixtures are serving and the flag that
                // drives the banner has to say so.
                .andExpect(jsonPath("$.development").value(true))
                .andExpect(jsonPath("$.retrieval").value(true))
                .andExpect(jsonPath("$.providerSlug").value("fake"))
                .andExpect(jsonPath("$.turnsCap").exists())
                .andExpect(jsonPath("$.actions[0].slug").exists())
                .andExpect(jsonPath("$.personas[0].slug").value("patient_teacher"));
    }

    @Test
    void sourcesListRetrievedBlockIdsWithTimestampsRatherThanInventingAny() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-sources@aileapro.dev");
        UUID conversation = startConversation(learner);
        ask(learner, conversation, "Why does changing b also change a when both are lists?", null);

        String body = mockMvc.perform(get("/v1/tutor/conversations/" + conversation + "/sources").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrieval").value(true))
                .andExpect(jsonPath("$.unitInContext").value(UNIT))
                .andExpect(jsonPath("$.note").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode blocks = json.readTree(body).get("blocks");
        // Retrieval ran; every listed id must carry a timestamp and a real unit slug. An empty
        // list is allowed only when the ask matched nothing — not when ids are fabricated.
        for (JsonNode block : blocks) {
            assertThat(block.get("blockId").asText()).isNotBlank();
            assertThat(block.get("unitSlug").asText()).isNotBlank();
            assertThat(block.get("updatedAt").asText()).isNotBlank();
            assertThat(block.get("href").asText()).startsWith("/app/learn/");
        }
    }

    @Test
    void theContextEndpointShowsTheLearnerWhatTheTutorKnowsAboutThem() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-context@aileapro.dev");

        mockMvc.perform(get("/v1/tutor/context").param("unit", UNIT).session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitSlug").value(UNIT))
                .andExpect(jsonPath("$.masteryState").value("UNKNOWN"));
    }

    @Test
    void thereIsNoTutorWithoutSigningIn() throws Exception {
        mockMvc.perform(get("/v1/tutor/status")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/tutor/conversations").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void aLearnerCanDeleteTheirOwnTranscript() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "tutor-delete@aileapro.dev");
        UUID conversation = startConversation(learner);
        ask(learner, conversation, "Something I would rather not keep", null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/v1/tutor/conversations/" + conversation)
                        .session(learner)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/tutor/conversations/" + conversation).session(learner))
                .andExpect(status().isNotFound());
        if (!messages.findByConversationIdOrderBySortAsc(conversation).isEmpty()) {
            throw new AssertionError("The messages outlived the conversation they belonged to");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UUID startConversation(MockHttpSession session) throws Exception {
        String body = mockMvc.perform(post("/v1/tutor/conversations")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitSlug\":\"" + UNIT + "\",\"persona\":\"patient_teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitSlug").value(UNIT))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
    }

    /**
     * Sends one turn and returns the whole event stream.
     *
     * <p>The turn runs on another thread and writes events as they happen, so this waits for
     * the stream to declare itself finished rather than reading whatever had arrived by the
     * time the request returned.
     */
    private String ask(MockHttpSession session, UUID conversation, String text, String action) throws Exception {
        String body = json.writeValueAsString(new java.util.HashMap<String, String>() {
            {
                if (text != null) {
                    put("text", text);
                }
                if (action != null) {
                    put("action", action);
                }
            }
        });

        MvcResult result = mockMvc.perform(post("/v1/tutor/conversations/" + conversation + "/messages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        // Polls the body until it declares itself finished. MockHttpServletResponse is not
        // built for two threads at once, and the turn is writing into it from another one, so a
        // read taken mid-event can throw; retrying is correct because the only read we act on
        // is one that already contains the terminal event.
        long deadline = System.currentTimeMillis() + 20_000;
        String stream = "";
        while (System.currentTimeMillis() < deadline) {
            try {
                stream = result.getResponse().getContentAsString();
            } catch (ConcurrentModificationException midWrite) {
                Thread.sleep(25);
                continue;
            }
            if (stream.contains("event:done") || stream.contains("event:error")) {
                return stream;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("The tutor stream never finished. Got: " + stream);
    }

    private static int countEvents(String stream, String name) {
        return stream.split("event:" + name, -1).length - 1;
    }
}
