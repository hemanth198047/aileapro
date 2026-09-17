package com.aileapro.api;

import com.aileapro.api.assessment.AssessmentItemRepository;
import com.aileapro.api.assessment.AssessmentRepository;
import com.aileapro.api.assessment.QuestionRepository;
import com.aileapro.api.assessment.seed.AssessmentSeeder;
import com.aileapro.api.curriculum.seed.CurriculumSeeder;
import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.QuestionEntity;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Phase 6 gate, as HTTP.
 *
 * <p>Most of these tests are attempts to get something for nothing: read the answer key out
 * of a served item, change an answer after seeing the mark, reach MASTERED off one quiz, get
 * credit for a card answered before it was due. The rest check that what the learner is told
 * afterwards is usable — a report with a percentage and nothing else is the failure mode this
 * phase exists to rule out.
 *
 * <p>The answer keys are read from the repository rather than hardcoded, because the seed is
 * content and content gets rewritten. A test that knows "the answer is b" fails the day an
 * author reorders the choices, which teaches everyone to distrust the suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssessmentApiTest {

    private static final String UNIT = "python-values-and-types";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CurriculumSeeder curriculum;

    @Autowired
    AssessmentSeeder assessmentSeeder;

    @Autowired
    QuestionRepository questions;

    @Autowired
    AssessmentRepository assessments;

    @Autowired
    AssessmentItemRepository assessmentItems;

    @Autowired
    ObjectMapper json;

    static boolean seeded;

    @BeforeEach
    void seed() {
        if (!seeded) {
            curriculum.seed();
            assessmentSeeder.seed();
            seeded = true;
        }
    }

    @Test
    void aServedItemDoesNotCarryItsAnswerKey() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "served@aileapro.dev");
        String body = start(learner, UNIT + "-practice");

        // The choices arrive as id and text. No field on the wire says which one is right,
        // so there is no client, honest or otherwise, that can mark this locally.
        JsonNode item = json.readTree(body).get("nextItem");
        for (JsonNode choice : item.get("choices")) {
            if (choice.has("correct") || choice.has("misconception")) {
                throw new AssertionError("A served choice leaked the key: " + choice);
            }
        }
        if (body.contains("\"explanation\"")) {
            throw new AssertionError("The explanation was served before the question was answered");
        }
    }

    @Test
    void answeringReturnsTheMarkAndTheTeachingAndCannotBeRedone() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "answers@aileapro.dev");
        String attemptId = idOf(start(learner, UNIT + "-practice"));
        JsonNode item = nextItem(learner, attemptId);

        answer(learner, attemptId, item.get("id").asText(), correctChoice(item.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.fraction").value(1.0))
                // The explanation is shown for a right answer too: being right for the wrong
                // reason is a thing, and this is the only moment the learner will read it.
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.answeredCount").value(1));

        // Answering again would turn every item into an unlimited retry, and the first mark
        // has already been counted as evidence.
        answer(learner, attemptId, item.get("id").asText(), correctChoice(item.get("id").asText()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(containsString("already answered")));
    }

    @Test
    void aWrongAnswerRaisesTheMisconceptionItWasBuiltFromAndARightOneClearsIt() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "beliefs@aileapro.dev");
        readUntilSolid(learner, UNIT);
        String attemptId = idOf(start(learner, UNIT + "-practice"));

        // The items are answered by id rather than in the order they are served, because this
        // test needs the belief held open while other items are answered correctly — and the
        // authored set puts a second detector of the same belief immediately after the first.
        List<String> itemIds = itemIdsFor(UNIT + "-practice");
        String offender = itemIds.stream()
                .filter(id -> codedChoice(id).isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No item on " + UNIT + " has a distractor built from a misconception"));

        String raised = answer(learner, attemptId, offender, List.of(codedChoice(offender).orElseThrow()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                // The flag is the point: "you got it wrong" and "you believe assignment copies"
                // call for completely different reading.
                .andExpect(jsonPath("$.raised[0].code").isNotEmpty())
                .andExpect(jsonPath("$.raised[0].claim").isNotEmpty())
                .andExpect(jsonPath("$.raised[0].href").value(containsString("/app/learn/")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String code = json.readTree(raised).get("raised").get(0).get("code").asText();

        // Held open, named, and pointed at the page that argues against it.
        mockMvc.perform(get("/v1/misconceptions").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(code))
                .andExpect(jsonPath("$[0].claim").isNotEmpty());

        // Everything the belief does not touch, answered correctly. Recall is now demonstrated,
        // so nothing but the open flag stands between this learner and the next rung.
        String detector = null;
        for (String itemId : itemIds) {
            if (itemId.equals(offender)) {
                continue;
            }
            if (detector == null && detects(itemId, code)) {
                detector = itemId;
                continue;
            }
            send(learner, attemptId, itemId, rightAnswer(itemId)).andExpect(status().isOk());
        }
        if (detector == null) {
            throw new AssertionError("Nothing in " + UNIT + "-practice could ever clear " + code);
        }

        // The state is pinned at recall while the flag is open, and the dock says which of the
        // two it is: someone who can recite the rule and still holds a named false belief about
        // it does not understand it, and being told "keep practising" would be useless advice.
        mockMvc.perform(get("/v1/units/" + UNIT + "/assessments").session(learner))
                .andExpect(jsonPath("$.mastery.state").value("FAMILIAR"))
                .andExpect(jsonPath("$.stateNote").value(containsString("false belief")));

        // Answering an item that can detect the belief, correctly, is what closes it. Anything
        // stricter would leave a flag on the record that nothing could ever remove.
        send(learner, attemptId, detector, rightAnswer(detector)).andExpect(status().isOk());
        mockMvc.perform(get("/v1/misconceptions").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/v1/units/" + UNIT + "/assessments").session(learner))
                .andExpect(jsonPath("$.stateNote").value(not(containsString("false belief"))));
    }

    @Test
    void aSubmittedQuizProducesAReportThatIsMoreThanAScore() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "report@aileapro.dev");
        String attemptId = idOf(start(learner, UNIT + "-quiz"));
        answerEverything(learner, attemptId, true);

        mockMvc.perform(post("/v1/attempts/" + attemptId + "/submit").session(learner).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fraction").value(1.0))
                .andExpect(jsonPath("$.passed").value(true))
                // Every narrative field is populated. There is no shape of this response that
                // is only a number, which is the whole of ASM-06 in one assertion.
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.readiness").isNotEmpty())
                .andExpect(jsonPath("$.readinessReason").isNotEmpty())
                .andExpect(jsonPath("$.strengths").isNotEmpty())
                .andExpect(jsonPath("$.revision").isNotEmpty())
                .andExpect(jsonPath("$.nextSteps").isNotEmpty())
                .andExpect(jsonPath("$.masteryState").isNotEmpty())
                // The state always explains itself, or it reads as a verdict.
                .andExpect(jsonPath("$.masteryNote").isNotEmpty())
                // A quiz cannot make anybody MASTERED, however well it went.
                .andExpect(jsonPath("$.masteryState").value(not("MASTERED")));

        // The report is addressable afterwards and comes back the same, because it was written
        // from the attempt once rather than recomputed against rules that may have moved.
        mockMvc.perform(get("/v1/attempts/" + attemptId + "/report").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.nextSteps").isNotEmpty());
    }

    @Test
    void passingAQuizMovesAssessButNotTheWholeLadder() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "chips@aileapro.dev");
        String attemptId = idOf(start(learner, UNIT + "-quiz"));
        answerEverything(learner, attemptId, true);
        mockMvc.perform(post("/v1/attempts/" + attemptId + "/submit").session(learner).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/units/" + UNIT + "/mastery").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.assess.state").value("solid"))
                .andExpect(jsonPath("$.evidence.assess.at").isNotEmpty())
                // Nothing here read the unit, and the ladder does not let graded work stand in
                // for that. Apply stays empty because nothing in the product can fill it.
                .andExpect(jsonPath("$.evidence.read.state").value("empty"))
                .andExpect(jsonPath("$.evidence.apply.state").value("empty"))
                .andExpect(jsonPath("$.state").value("UNKNOWN"));
    }

    @Test
    void theUnitDockNamesItsCeilingAndWhatWouldMoveTheLearnerOn() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "dock@aileapro.dev");

        mockMvc.perform(get("/v1/units/" + UNIT + "/assessments").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practice.slug").value(UNIT + "-practice"))
                .andExpect(jsonPath("$.practice.itemCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.quiz.slug").value(UNIT + "-quiz"))
                .andExpect(jsonPath("$.ceiling").isNotEmpty())
                .andExpect(jsonPath("$.ceilingReason").isNotEmpty())
                .andExpect(jsonPath("$.stateNote").isNotEmpty())
                .andExpect(jsonPath("$.mastery.state").value("UNKNOWN"));

        // A unit with nothing authored says so, rather than offering an empty quiz.
        mockMvc.perform(get("/v1/units/transformers/assessments").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practice").doesNotExist())
                .andExpect(jsonPath("$.quiz").doesNotExist())
                .andExpect(jsonPath("$.ceiling").value("INTRODUCED"))
                .andExpect(jsonPath("$.ceilingReason").value(containsString("No items are authored")));
    }

    @Test
    void readingAUnitEnrolsItsCardsAndTheQueueWithholdsTheAnswer() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "cards@aileapro.dev");
        readUntilSolid(learner, "python-functions");

        String queue = mockMvc.perform(get("/v1/reviews/due").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.cards[0].front").isNotEmpty())
                .andExpect(jsonPath("$.cards[0].due").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The back is not in the queue. Showing both sides at once measures recognition, which
        // feels like recall and is not, so the answer is released only against a grade.
        if (queue.contains("\"back\"")) {
            throw new AssertionError("The due queue served the back of a card: " + queue);
        }

        String scheduleId = json.readTree(queue).get("cards").get(0).get("scheduleId").asText();
        mockMvc.perform(post("/v1/reviews/" + scheduleId)
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.back").isNotEmpty())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.wasDue").value(true))
                .andExpect(jsonPath("$.intervalAfter").value(greaterThan(0)))
                .andExpect(jsonPath("$.nextDue").isNotEmpty())
                // The interval change is explained, so the schedule is never a black box.
                .andExpect(jsonPath("$.note").isNotEmpty());

        // Graded once, it leaves the queue: a card answered twice in a day is not two recalls.
        mockMvc.perform(post("/v1/reviews/" + scheduleId)
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wasDue").value(false))
                .andExpect(jsonPath("$.note").value(containsString("not due")));
    }

    @Test
    void anUngradedCardCannotBeGradedBySomebodyElse() throws Exception {
        MockHttpSession owner = TestLearner.signIn(mockMvc, "cardowner@aileapro.dev");
        MockHttpSession stranger = TestLearner.signIn(mockMvc, "cardstranger@aileapro.dev");
        readUntilSolid(owner, "python-collections");

        String queue = mockMvc.perform(get("/v1/reviews/due").session(owner))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String scheduleId = json.readTree(queue).get("cards").get(0).get("scheduleId").asText();

        mockMvc.perform(post("/v1/reviews/" + scheduleId)
                        .session(stranger)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anInvalidGradeIsRejectedRatherThanClamped() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "badgrade@aileapro.dev");
        readUntilSolid(learner, "vectors");
        String queue = mockMvc.perform(get("/v1/reviews/due").session(learner))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String scheduleId = json.readTree(queue).get("cards").get(0).get("scheduleId").asText();

        // Silently clamping a 9 to a 5 would record a confidence the learner never expressed.
        mockMvc.perform(post("/v1/reviews/" + scheduleId)
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theDiagnosticPlacesTheLearnerWithoutClaimingTheyKnowAnything() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "diagnostic@aileapro.dev");
        String attemptId = idOf(mockMvc.perform(post("/v1/diagnostic").session(learner).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("diagnostic"))
                // Adaptive, so there is no total to count down to.
                .andExpect(jsonPath("$.remaining").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString());

        int served = answerEverything(learner, attemptId, true);
        // The stop rules are what make this a placement rather than an exam: three right in a
        // row ends a domain, so a learner who knows everything answers far fewer than the cap.
        if (served >= 16) {
            throw new AssertionError("A learner answering everything correctly was asked " + served
                    + " items, so the stop rules did nothing");
        }

        mockMvc.perform(post("/v1/attempts/" + attemptId + "/submit").session(learner).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("diagnostic"))
                // Not a grade, and it says so in the field the UI reads.
                .andExpect(jsonPath("$.readiness").value("unscored"))
                .andExpect(jsonPath("$.nextSteps").isNotEmpty());

        // And none of it counted as evidence on any unit it asked about.
        mockMvc.perform(get("/v1/units/" + UNIT + "/mastery").session(learner))
                .andExpect(jsonPath("$.evidence.practice.state").value("empty"))
                .andExpect(jsonPath("$.evidence.assess.state").value("empty"))
                .andExpect(jsonPath("$.state").value("UNKNOWN"));

        // A plan needs a destination. Refusing with its own code lets the client send the
        // learner to onboarding instead of showing them a validation error they cannot fix.
        mockMvc.perform(post("/v1/attempts/" + attemptId + "/plan").session(learner).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("goal_required"));

        mockMvc.perform(put("/v1/me/profile")
                        .session(learner)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPath\":\"ai-engineer-abridged\",\"hoursPerWeek\":6,"
                                + "\"modalities\":[\"visual\"],\"experience\":\"some_programming\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/attempts/" + attemptId + "/plan").session(learner).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.source").value("diagnostic"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.priors").isNotEmpty())
                // A guess from two items is labelled as a guess from two items.
                .andExpect(jsonPath("$.priors[0].confidence").isNotEmpty())
                .andExpect(jsonPath("$.priors[0].reason").isNotEmpty())
                .andExpect(jsonPath("$.priors[0].estimate").value(lessThanOrEqualTo(1.0)))
                .andExpect(jsonPath("$.items").isNotEmpty())
                // Every line of a plan says why it is on the list.
                .andExpect(jsonPath("$.items[0].reason").isNotEmpty());

        mockMvc.perform(get("/v1/plan").session(learner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void anAttemptBelongsToTheLearnerWhoStartedIt() throws Exception {
        MockHttpSession owner = TestLearner.signIn(mockMvc, "mine@aileapro.dev");
        MockHttpSession stranger = TestLearner.signIn(mockMvc, "notmine@aileapro.dev");
        String attemptId = idOf(start(owner, UNIT + "-practice"));

        mockMvc.perform(get("/v1/attempts/" + attemptId).session(stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/attempts/" + attemptId + "/submit").session(stranger).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/attempts/" + attemptId + "/report").session(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void startingTwiceResumesRatherThanHandingOutAFreshSet() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "resume@aileapro.dev");
        String first = idOf(start(learner, UNIT + "-quiz"));
        JsonNode item = nextItem(learner, first);
        answer(learner, first, item.get("id").asText(), correctChoice(item.get("id").asText()))
                .andExpect(status().isOk());

        // Otherwise closing the tab on a hard question is a reroll, and the quiz measures
        // persistence with the refresh key rather than knowledge.
        String second = idOf(start(learner, UNIT + "-quiz"));
        if (!first.equals(second)) {
            throw new AssertionError("Starting again opened a second attempt: " + second);
        }
        mockMvc.perform(get("/v1/attempts/" + second).session(learner))
                .andExpect(jsonPath("$.answeredCount").value(1));
    }

    @Test
    void anUnpublishedOrUnknownAssessmentCannotBeAttempted() throws Exception {
        MockHttpSession learner = TestLearner.signIn(mockMvc, "unknown@aileapro.dev");
        mockMvc.perform(post("/v1/assessments/the-final-exam/attempts").session(learner).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void assessmentRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v1/reviews/due")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/misconceptions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/units/" + UNIT + "/assessments")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/diagnostic").with(csrf())).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------------------ helpers

    private String start(MockHttpSession session, String slug) throws Exception {
        return mockMvc.perform(post("/v1/assessments/" + slug + "/attempts").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private JsonNode nextItem(MockHttpSession session, String attemptId) throws Exception {
        String body = mockMvc.perform(get("/v1/attempts/" + attemptId).session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode next = json.readTree(body).get("nextItem");
        return next == null || next.isNull() ? null : next;
    }

    private ResultActions answer(
            MockHttpSession session, String attemptId, String itemId, List<String> selected) throws Exception {
        String ids = String.join("\",\"", selected);
        return send(session, attemptId, itemId, "{\"selectedIds\":[\"" + ids + "\"],\"elapsedSeconds\":10}");
    }

    private ResultActions send(MockHttpSession session, String attemptId, String itemId, String body)
            throws Exception {
        return mockMvc.perform(post("/v1/attempts/" + attemptId + "/items/" + itemId)
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** Answers every served item correctly and returns how many there were. */
    private int answerEverything(MockHttpSession session, String attemptId, boolean correctly) throws Exception {
        int count = 0;
        JsonNode item = nextItem(session, attemptId);
        while (item != null) {
            String itemId = item.get("id").asText();
            send(session, attemptId, itemId, correctly ? rightAnswer(itemId) : wrongAnswer(item))
                    .andExpect(status().isOk());
            count++;
            item = nextItem(session, attemptId);
        }
        return count;
    }

    /**
     * The keyed answer, read from the bank and shaped for the item's type.
     *
     * <p>Not every item has choices, which is the point of having types at all: a short-text
     * item is answered with a string, and a helper that only understood choices would quietly
     * skip exactly the items where marking is hardest.
     */
    private String rightAnswer(String itemId) {
        JsonNode data = dataFor(itemId);
        List<String> correct = new ArrayList<>();
        for (JsonNode choice : data.path("choices")) {
            if (choice.path("correct").asBoolean()) {
                correct.add(choice.get("id").asText());
            }
        }
        if (!correct.isEmpty()) {
            return "{\"selectedIds\":[\"" + String.join("\",\"", correct) + "\"],\"elapsedSeconds\":10}";
        }
        if (data.hasNonNull("accepted") && !data.get("accepted").isEmpty()) {
            return "{\"text\":\"" + escape(data.get("accepted").get(0).asText()) + "\",\"elapsedSeconds\":10}";
        }
        if (data.hasNonNull("answer")) {
            return "{\"text\":\"" + data.get("answer").asText() + "\",\"elapsedSeconds\":10}";
        }
        throw new IllegalStateException("Item " + itemId + " has no answer key of any kind");
    }

    private String wrongAnswer(JsonNode item) {
        JsonNode choices = item.path("choices");
        if (choices.isEmpty()) {
            return "{\"text\":\"certainly not this\",\"elapsedSeconds\":10}";
        }
        return "{\"selectedIds\":[\"" + choices.get(0).get("id").asText() + "\"],\"elapsedSeconds\":10}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** The keyed choices for a choice-based item. */
    private List<String> correctChoice(String itemId) {
        List<String> correct = new ArrayList<>();
        for (JsonNode choice : dataFor(itemId).path("choices")) {
            if (choice.path("correct").asBoolean()) {
                correct.add(choice.get("id").asText());
            }
        }
        if (correct.isEmpty()) {
            throw new IllegalStateException("Item " + itemId + " has no keyed choice");
        }
        return correct;
    }

    /** A distractor built from a named misconception, if this item has one. */
    private Optional<String> codedChoice(String itemId) {
        for (JsonNode choice : dataFor(itemId).path("choices")) {
            if (choice.hasNonNull("misconception")) {
                return Optional.of(choice.get("id").asText());
            }
        }
        return Optional.empty();
    }

    /** Whether answering this item correctly says anything about the named belief. */
    private boolean detects(String itemId, String code) {
        for (JsonNode choice : dataFor(itemId).path("choices")) {
            if (code.equals(choice.path("misconception").asText(null))) {
                return true;
            }
        }
        return false;
    }

    /** The authored items of a set, in order, read from the bank rather than the wire. */
    private List<String> itemIdsFor(String slug) {
        UUID assessmentId = assessments.findBySlug(slug)
                .orElseThrow(() -> new IllegalStateException("No assessment " + slug))
                .getId();
        return assessmentItems.findByIdAssessmentIdOrderBySort(assessmentId).stream()
                .map(row -> row.getId().getQuestionId().toString())
                .toList();
    }

    private JsonNode dataFor(String itemId) {
        QuestionEntity item = questions.findById(java.util.UUID.fromString(itemId))
                .orElseThrow(() -> new IllegalStateException("No item " + itemId));
        if (item.getStatus() != ContentStatus.PUBLISHED) {
            throw new IllegalStateException("Item " + item.getSlug() + " was served while unpublished");
        }
        try {
            return json.readTree(item.getData());
        } catch (Exception broken) {
            throw new IllegalStateException("Unreadable key on " + item.getSlug(), broken);
        }
    }

    /** Reads a unit the long way, because enrolment is meant to be earned by reading. */
    private void readUntilSolid(MockHttpSession session, String slug) throws Exception {
        event(session, slug, "{\"kind\":\"unit_opened\"}");
        event(session, slug, "{\"kind\":\"explanation_level\",\"level\":\"beginner\"}");
        for (int beat = 0; beat < 12; beat++) {
            event(session, slug, "{\"kind\":\"dwell\",\"seconds\":120}");
        }
        mockMvc.perform(get("/v1/units/" + slug + "/mastery").session(session))
                .andExpect(jsonPath("$.evidence.read.state").value("solid"));
    }

    private void event(MockHttpSession session, String slug, String body) throws Exception {
        mockMvc.perform(post("/v1/units/" + slug + "/events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String idOf(String attemptJson) throws Exception {
        return json.readTree(attemptJson).get("id").asText();
    }
}
