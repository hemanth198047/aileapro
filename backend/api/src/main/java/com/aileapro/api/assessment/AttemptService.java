package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.AssessmentSummary;
import com.aileapro.api.assessment.AssessmentViews.AttemptState;
import com.aileapro.api.assessment.AssessmentViews.Marked;
import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.assessment.AssessmentViews.ServedItem;
import com.aileapro.api.assessment.AssessmentViews.UnitAssessments;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.api.learning.MasteryService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.GradedEvidencePolicy;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.ItemScoring;
import com.aileapro.domain.ItemType;
import com.aileapro.domain.MasteryState;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.AssessmentEntity;
import com.aileapro.persistence.AssessmentItemEntity;
import com.aileapro.persistence.AttemptAnswerEntity;
import com.aileapro.persistence.AttemptEntity;
import com.aileapro.persistence.QuestionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The attempt lifecycle: start, serve, answer, submit.
 *
 * <p>Two properties are structural rather than enforced by care. Marking happens on the
 * server, from the stored key, so the answer never travels to the client before the learner
 * commits. And an answer can be recorded once: a second POST for the same item is refused
 * rather than re-marked, because an assessment that can be retried per item is a quiz with
 * the answers in it.
 */
@Service
@Transactional
public class AttemptService {

    static final String IN_PROGRESS = "in_progress";
    static final String SUBMITTED = "submitted";
    static final String ABANDONED = "abandoned";

    private final AssessmentRepository assessments;
    private final AssessmentItemRepository assessmentItems;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final CurriculumQueryService curriculum;
    private final UnitFactsService unitFacts;
    private final MisconceptionService misconceptions;
    private final GradedMasteryService graded;
    private final MasteryService mastery;
    private final ObjectMapper json;

    public AttemptService(
            AssessmentRepository assessments,
            AssessmentItemRepository assessmentItems,
            QuestionRepository questions,
            AttemptRepository attempts,
            AttemptAnswerRepository answers,
            CurriculumQueryService curriculum,
            UnitFactsService unitFacts,
            MisconceptionService misconceptions,
            GradedMasteryService graded,
            MasteryService mastery,
            ObjectMapper json) {
        this.assessments = assessments;
        this.assessmentItems = assessmentItems;
        this.questions = questions;
        this.attempts = attempts;
        this.answers = answers;
        this.curriculum = curriculum;
        this.unitFacts = unitFacts;
        this.misconceptions = misconceptions;
        this.graded = graded;
        this.mastery = mastery;
        this.json = json;
    }

    /** A submitted answer as it arrives from the client. */
    public record AnswerRequest(List<String> selectedIds, String text, Integer elapsedSeconds) {
    }

    // ------------------------------------------------------------------ what is on offer

    @Transactional(readOnly = true)
    public UnitAssessments forUnit(UUID userId, String idOrSlug) {
        UnitRef unit = curriculum.publishedRef(idOrSlug);
        UnitFacts facts = unitFacts.factsFor(unit.id());
        MasteryState ceiling = GradedEvidencePolicy.reachableCeiling(facts);

        return new UnitAssessments(
                unit,
                summary(userId, unit, AssessmentKind.PRACTICE),
                summary(userId, unit, AssessmentKind.QUIZ),
                ceiling.name(),
                UnitFactsService.ceilingReason(facts, ceiling),
                UnitFactsService.stateReason(graded.gateFor(userId, unit.id(), facts), facts, ceiling),
                mastery.mastery(userId, idOrSlug));
    }

    private AssessmentSummary summary(UUID userId, UnitRef unit, AssessmentKind kind) {
        return assessments
                .findByUnitIdAndKindAndStatus(unit.id(), kind, ContentStatus.PUBLISHED)
                .map(assessment -> new AssessmentSummary(
                        assessment.getId(),
                        assessment.getSlug(),
                        assessment.getTitle(),
                        assessment.getKind().code(),
                        assessment.getDescription(),
                        assessment.getTimeLimitSeconds(),
                        assessment.getPassMark().doubleValue(),
                        assessmentItems.findByIdAssessmentIdOrderBySort(assessment.getId()).size(),
                        unit,
                        attempts.findFirstByUserIdAndAssessmentIdAndStatusOrderByStartedAtDesc(
                                        userId, assessment.getId(), IN_PROGRESS)
                                .map(AttemptEntity::getId)
                                .orElse(null)))
                .orElse(null);
    }

    // ---------------------------------------------------------------------- the attempt

    /**
     * Starts an attempt, or resumes the one already open.
     *
     * <p>Resuming rather than starting fresh is deliberate: a learner who closed the tab
     * halfway through a timed quiz has not earned a clean slate, and a system that hands out
     * one turns every hard question into a reroll.
     */
    public AttemptState start(UUID userId, String assessmentSlug) {
        AssessmentEntity assessment = assessments
                .findBySlugAndStatus(assessmentSlug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("No published assessment '" + assessmentSlug + "'"));

        Optional<AttemptEntity> open = attempts.findFirstByUserIdAndAssessmentIdAndStatusOrderByStartedAtDesc(
                userId, assessment.getId(), IN_PROGRESS);
        if (open.isPresent()) {
            return state(userId, open.get().getId());
        }

        AttemptEntity attempt = new AttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setUserId(userId);
        attempt.setAssessmentId(assessment.getId());
        // Copied, not referenced: if the assessment is rewritten later, what this attempt was
        // must not change underneath the report that cites it.
        attempt.setKind(assessment.getKind());
        attempt.setUnitId(assessment.getUnitId());
        attempt.setAssessmentVersion(assessment.getVersion());
        attempt.setStatus(IN_PROGRESS);
        attempt.setTimed(assessment.getTimeLimitSeconds() != null);
        attempt.setTimeLimitSeconds(assessment.getTimeLimitSeconds());
        attempt.setStartedAt(Instant.now());
        attempts.save(attempt);

        Blueprint blueprint = Blueprint.parse(assessment.getBlueprint(), json);
        if (blueprint.isAdaptive()) {
            serveNextAdaptive(attempt, assessment, blueprint);
        } else {
            serveFixedSet(attempt, assessment);
        }
        return state(userId, attempt.getId());
    }

    /** A fixed assessment serves everything up front, so the learner can see the shape of it. */
    private void serveFixedSet(AttemptEntity attempt, AssessmentEntity assessment) {
        List<AssessmentItemEntity> rows = assessmentItems.findByIdAssessmentIdOrderBySort(assessment.getId());
        if (rows.isEmpty()) {
            throw ApiException.validation("Assessment '" + assessment.getSlug() + "' has no items");
        }
        List<AttemptAnswerEntity> served = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            served.add(serveRow(attempt.getId(), rows.get(i).getId().getQuestionId(), i, rows.get(i).getPoints()));
        }
        answers.saveAll(served);
    }

    private AttemptAnswerEntity serveRow(UUID attemptId, UUID questionId, int sort, BigDecimal points) {
        AttemptAnswerEntity row = new AttemptAnswerEntity();
        row.setId(UUID.randomUUID());
        row.setAttemptId(attemptId);
        row.setQuestionId(questionId);
        row.setSort(sort);
        row.setPoints(points == null ? BigDecimal.ONE : points);
        row.setServedAt(Instant.now());
        return row;
    }

    @Transactional(readOnly = true)
    public AttemptState state(UUID userId, UUID attemptId) {
        AttemptEntity attempt = require(userId, attemptId);
        AssessmentEntity assessment = assessments.findById(attempt.getAssessmentId())
                .orElseThrow(() -> ApiException.notFound("The assessment behind this attempt is gone"));
        List<AttemptAnswerEntity> rows = answers.findByAttemptIdOrderBySort(attemptId);
        return view(attempt, assessment, rows);
    }

    // ------------------------------------------------------------------------ answering

    /**
     * Records and marks one answer.
     *
     * <p>Everything downstream of the mark happens here rather than at submission: the chip
     * moves, the flag is raised or cleared, and the learner sees the explanation immediately.
     * Waiting until the end would mean feedback arriving after the learner has stopped caring
     * about that particular question.
     */
    public Marked answer(UUID userId, UUID attemptId, UUID itemId, AnswerRequest request) {
        AttemptEntity attempt = require(userId, attemptId);
        if (!IN_PROGRESS.equals(attempt.getStatus())) {
            throw ApiException.validation("This attempt was already " + attempt.getStatus());
        }
        checkTimeLimit(attempt);

        AttemptAnswerEntity row = answers.findByAttemptIdAndQuestionId(attemptId, itemId)
                .orElseThrow(() -> ApiException.notFound("That item was not served in this attempt"));
        if (row.isAnswered()) {
            throw ApiException.validation("That item is already answered");
        }

        QuestionEntity item = questions.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("That item no longer exists"));
        ItemKeys.Parsed parsed = ItemKeys.parse(item, json);
        ItemScoring.Response response =
                ItemKeys.response(item.getItemType(), request.selectedIds(), request.text());
        ItemScoring.Result result = ItemScoring.mark(parsed.key(), response);

        boolean unmarkable = result.outcome() == ItemScoring.Outcome.UNMARKED;
        row.setResponse(responseJson(request));
        row.setCorrect(unmarkable ? null : result.outcome().isCorrect());
        row.setUnverified(unmarkable);
        row.setScore(row.getPoints().multiply(BigDecimal.valueOf(result.fraction()))
                .setScale(4, RoundingMode.HALF_UP));
        row.setFeedback(item.getExplanation());
        row.setAnsweredAt(Instant.now());
        row.setElapsedSeconds(request.elapsedSeconds());

        // A distractor's own code is the diagnostic signal; the item's codes are the fallback
        // for types with no choices to attach one to.
        Set<String> codes = new LinkedHashSet<>();
        if (!unmarkable && !result.outcome().isCorrect()) {
            for (String chosen : response.selectedIds()) {
                String code = parsed.byChoice().get(chosen);
                if (code != null) {
                    codes.add(code);
                }
            }
            if (codes.isEmpty()) {
                codes.addAll(List.of(item.getMisconceptionCodes()));
            }
        }
        row.setMisconceptionCodes(codes.toArray(String[]::new));
        answers.save(row);

        List<Misconception> raised = codes.isEmpty()
                ? List.of()
                : misconceptions.raise(userId, codes, attemptId);
        if (!unmarkable && result.outcome().isCorrect()) {
            // The same evidence in the other direction. It clears everything this item can
            // detect, distractor codes included: an item whose wrong answer means "you think
            // assignment copies" must mean the opposite when answered correctly, or a belief
            // raised by a choice could never be cleared by anything at all.
            misconceptions.clear(userId, detectableCodes(item, parsed));
        }

        if (attempt.getKind().gradesEvidence()) {
            UnitFacts facts = unitFacts.factsFor(item.getUnitId());
            graded.recordItem(
                    userId,
                    item.getUnitId(),
                    facts,
                    Volatility.MEDIUM,
                    new GradedEvidencePolicy.Marked(
                            attempt.getKind(),
                            item.getBloom(),
                            item.getDifficulty(),
                            result.fraction(),
                            !unmarkable && result.outcome().isCorrect(),
                            !unmarkable),
                    attemptId);
        }

        List<AttemptAnswerEntity> rows = answers.findByAttemptIdOrderBySort(attemptId);
        if (attempt.getKind() == AssessmentKind.DIAGNOSTIC) {
            AssessmentEntity assessment = assessments.findById(attempt.getAssessmentId()).orElseThrow();
            serveNextAdaptive(attempt, assessment, Blueprint.parse(assessment.getBlueprint(), json));
            rows = answers.findByAttemptIdOrderBySort(attemptId);
        }

        int answered = (int) rows.stream().filter(AttemptAnswerEntity::isAnswered).count();
        return new Marked(
                itemId,
                row.getCorrect(),
                unmarkable,
                result.fraction(),
                result.missedIds(),
                item.getExplanation(),
                raised,
                answered,
                rows.size(),
                answered == rows.size());
    }

    /** Every misconception this item speaks to: its own codes plus its distractors'. */
    private static List<String> detectableCodes(QuestionEntity item, ItemKeys.Parsed parsed) {
        Set<String> codes = new LinkedHashSet<>(List.of(item.getMisconceptionCodes()));
        codes.addAll(parsed.byChoice().values());
        return List.copyOf(codes);
    }

    // ----------------------------------------------------------------------- submitting

    /**
     * Closes the attempt and records what it was worth.
     *
     * <p>Only the whole assessment can fill the Assess chip, and only by reaching its own pass
     * mark. Individual right answers along the way moved counts; passing is a separate claim.
     */
    public AttemptEntity submit(UUID userId, UUID attemptId) {
        AttemptEntity attempt = require(userId, attemptId);
        if (SUBMITTED.equals(attempt.getStatus())) {
            return attempt;
        }
        AssessmentEntity assessment = assessments.findById(attempt.getAssessmentId())
                .orElseThrow(() -> ApiException.notFound("The assessment behind this attempt is gone"));
        List<AttemptAnswerEntity> rows = answers.findByAttemptIdOrderBySort(attemptId);

        // Unmarkable items are excluded from both halves of the fraction. Counting them as
        // wrong would penalise the learner for a limitation of the marker, and counting them
        // as right would hand out marks nobody checked.
        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal possible = BigDecimal.ZERO;
        for (AttemptAnswerEntity row : rows) {
            if (row.isUnverified()) {
                continue;
            }
            possible = possible.add(row.getPoints());
            if (row.getScore() != null) {
                earned = earned.add(row.getScore());
            }
        }

        double fraction = possible.signum() == 0 ? 0 : earned.doubleValue() / possible.doubleValue();
        boolean passed = possible.signum() != 0 && fraction >= assessment.getPassMark().doubleValue();

        attempt.setStatus(SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.setScore(earned.setScale(4, RoundingMode.HALF_UP));
        attempt.setMaxScore(possible.setScale(4, RoundingMode.HALF_UP));
        attempt.setPassed(attempt.getKind().gradesEvidence() ? passed : null);
        if (attempt.getStopReason() == null) {
            attempt.setStopReason("completed");
        }
        attempts.save(attempt);

        if (attempt.getKind().gradesEvidence() && attempt.getUnitId() != null) {
            graded.recordSubmission(
                    userId,
                    attempt.getUnitId(),
                    unitFacts.factsFor(attempt.getUnitId()),
                    Volatility.MEDIUM,
                    attempt.getKind(),
                    fraction,
                    passed,
                    attemptId);
        }
        return attempt;
    }

    // ------------------------------------------------------------------------- adaptive

    /**
     * Serves the next diagnostic item, or nothing if the stop rules are satisfied.
     *
     * <p>The rules are per domain, and they are about spending a short budget well rather than
     * measuring anyone precisely. Two wrong in a row ends a domain because the ceiling is below
     * it and further probing is demoralising; three right ends it because the remaining budget
     * belongs where the answer is still unknown.
     */
    private void serveNextAdaptive(AttemptEntity attempt, AssessmentEntity assessment, Blueprint blueprint) {
        List<AttemptAnswerEntity> rows = answers.findByAttemptIdOrderBySort(attempt.getId());
        boolean pendingItem = rows.stream().anyMatch(row -> !row.isAnswered());
        if (pendingItem) {
            return;
        }
        if (blueprint.maxItems() > 0 && rows.size() >= blueprint.maxItems()) {
            attempt.setStopReason("item_budget_reached");
            return;
        }

        Map<UUID, QuestionEntity> byId = new HashMap<>();
        questions.findByIdIn(rows.stream().map(AttemptAnswerEntity::getQuestionId).toList())
                .forEach(question -> byId.put(question.getId(), question));
        Map<String, UUID> idsBySlug = new LinkedHashMap<>();

        for (Blueprint.Domain domain : blueprint.domains()) {
            List<QuestionEntity> ladder = ladderFor(domain, idsBySlug);
            List<AttemptAnswerEntity> asked = rows.stream()
                    .filter(row -> ladder.stream().anyMatch(item -> item.getId().equals(row.getQuestionId())))
                    .sorted(Comparator.comparingInt(AttemptAnswerEntity::getSort))
                    .toList();

            if (domainSettled(asked, blueprint.stopRules())) {
                continue;
            }
            Optional<QuestionEntity> next = ladder.stream()
                    .filter(item -> asked.stream().noneMatch(row -> row.getQuestionId().equals(item.getId())))
                    .findFirst();
            if (next.isEmpty()) {
                continue;
            }
            answers.save(serveRow(attempt.getId(), next.get().getId(), rows.size(), BigDecimal.ONE));
            return;
        }
        attempt.setStopReason("all_domains_settled");
    }

    /** The authored ladder for one domain, easiest first, skipping anything unpublished. */
    private List<QuestionEntity> ladderFor(Blueprint.Domain domain, Map<String, UUID> cache) {
        List<QuestionEntity> ladder = new ArrayList<>(domain.items().size());
        for (String slug : domain.items()) {
            questions.findBySlug(slug)
                    .filter(item -> item.getStatus() == ContentStatus.PUBLISHED)
                    .ifPresent(item -> {
                        cache.put(slug, item.getId());
                        ladder.add(item);
                    });
        }
        return ladder;
    }

    private static boolean domainSettled(List<AttemptAnswerEntity> asked, Blueprint.StopRules rules) {
        long answered = asked.stream().filter(AttemptAnswerEntity::isAnswered).count();
        if (answered < rules.minPerDomain()) {
            return false;
        }
        if (answered >= rules.maxPerDomain()) {
            return true;
        }
        int consecutiveWrong = 0;
        int consecutiveRight = 0;
        for (AttemptAnswerEntity row : asked) {
            if (!row.isAnswered() || row.getCorrect() == null) {
                continue;
            }
            if (row.getCorrect()) {
                consecutiveRight++;
                consecutiveWrong = 0;
            } else {
                consecutiveWrong++;
                consecutiveRight = 0;
            }
        }
        return consecutiveWrong >= rules.stopAfterWrong() || consecutiveRight >= rules.stopAfterCorrect();
    }

    // ---------------------------------------------------------------------------- views

    private AttemptState view(
            AttemptEntity attempt, AssessmentEntity assessment, List<AttemptAnswerEntity> rows) {
        Optional<AttemptAnswerEntity> pending = rows.stream()
                .filter(row -> !row.isAnswered())
                .min(Comparator.comparingInt(AttemptAnswerEntity::getSort));

        int answered = (int) rows.stream().filter(AttemptAnswerEntity::isAnswered).count();
        Blueprint blueprint = Blueprint.parse(assessment.getBlueprint(), json);

        return new AttemptState(
                attempt.getId(),
                attempt.getKind().code(),
                attempt.getStatus(),
                assessment.getSlug(),
                assessment.getTitle(),
                attempt.getUnitId() == null ? null : curriculum.publishedRefs(List.of(attempt.getUnitId()))
                        .get(attempt.getUnitId()),
                attempt.getStartedAt(),
                attempt.getTimeLimitSeconds(),
                secondsRemaining(attempt),
                answered,
                rows.size(),
                // An adaptive attempt honestly cannot say how many are left, and a made-up
                // number would be the only dishonest thing on the screen.
                blueprint.isAdaptive() ? null : rows.size() - answered,
                pending.map(this::served).orElse(null));
    }

    private ServedItem served(AttemptAnswerEntity row) {
        QuestionEntity item = questions.findById(row.getQuestionId())
                .orElseThrow(() -> ApiException.notFound("A served item no longer exists"));
        ItemKeys.Parsed parsed = ItemKeys.parse(item, json);
        return new ServedItem(
                item.getId(),
                item.getItemType().code(),
                item.getStem(),
                parsed.choices(),
                item.getDifficulty(),
                item.getBloom().code(),
                item.getEstimateSeconds(),
                row.getSort(),
                row.isAnswered(),
                curriculum.publishedRefs(List.of(item.getUnitId())).get(item.getUnitId()));
    }

    private static Integer secondsRemaining(AttemptEntity attempt) {
        if (attempt.getTimeLimitSeconds() == null) {
            return null;
        }
        long spent = Duration.between(attempt.getStartedAt(), Instant.now()).toSeconds();
        return (int) Math.max(0, attempt.getTimeLimitSeconds() - spent);
    }

    /**
     * A timed attempt that has run out is submitted rather than refused, so the learner gets
     * their report instead of an error about a sitting they cannot finish.
     */
    private void checkTimeLimit(AttemptEntity attempt) {
        Integer remaining = secondsRemaining(attempt);
        if (remaining != null && remaining <= 0) {
            attempt.setStopReason("time_expired");
            attempts.save(attempt);
            submit(attempt.getUserId(), attempt.getId());
            throw ApiException.validation("Time is up. This attempt has been submitted.");
        }
    }

    AttemptEntity require(UUID userId, UUID attemptId) {
        return attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("No such attempt"));
    }

    private String responseJson(AnswerRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.selectedIds() != null && !request.selectedIds().isEmpty()) {
            body.put("selectedIds", request.selectedIds());
        }
        if (request.text() != null && !request.text().isBlank()) {
            body.put("text", request.text());
        }
        try {
            return json.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise the submitted answer", impossible);
        }
    }

    /** Exposed for the report, which needs the marked rows and the item metadata together. */
    @Transactional(readOnly = true)
    List<AttemptAnswerEntity> answersFor(UUID attemptId) {
        return answers.findByAttemptIdOrderBySort(attemptId);
    }

    @Transactional(readOnly = true)
    Map<UUID, QuestionEntity> itemsFor(List<AttemptAnswerEntity> rows) {
        Map<UUID, QuestionEntity> byId = new LinkedHashMap<>();
        questions.findByIdIn(rows.stream().map(AttemptAnswerEntity::getQuestionId).distinct().toList())
                .forEach(item -> byId.put(item.getId(), item));
        return byId;
    }

    @Transactional(readOnly = true)
    AssessmentEntity assessmentFor(AttemptEntity attempt) {
        return assessments.findById(attempt.getAssessmentId())
                .orElseThrow(() -> ApiException.notFound("The assessment behind this attempt is gone"));
    }

    /** Item type, exposed so the client can be told what widget to draw without guessing. */
    static boolean isTextual(ItemType type) {
        return type == ItemType.SHORT_TEXT || type == ItemType.CODING_STUB;
    }
}
