package com.aileapro.api.learning;

import com.aileapro.domain.CompositeScore;
import com.aileapro.domain.Evidence;
import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.GradedEvidencePolicy;
import com.aileapro.domain.GradedEvidencePolicy.Graded;
import com.aileapro.domain.GradedEvidencePolicy.Marked;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.LearningEventKind;
import com.aileapro.domain.MasteryState;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.MasteryEventEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The graded counterpart of {@link MasteryService}.
 *
 * <p>{@link MasteryService} can only raise the Read chip. This class is the only thing in the
 * system that can move Practice and Assess, and therefore the only thing that can take a unit
 * past INTRODUCED. Splitting them is not tidiness: it means no amount of page-opening can
 * reach the states that are supposed to require work, because the code that would do it does
 * not exist on that path.
 *
 * <p>Like its counterpart, it decides nothing itself. Every judgement is delegated to
 * {@link GradedEvidencePolicy}, and this class only appends events and persists the
 * projection those rules produce.
 */
@Service
@Transactional
public class GradedMasteryService {

    /**
     * Prerequisite health as a signal for the composite score, supplied from outside so the
     * learner code never has to know how the graph is walked.
     */
    public interface PrerequisiteHealth {

        /** 0 to 1: how much of what this unit depends on is itself in good standing. */
        double healthFor(UUID userId, UUID unitId);
    }

    private final MasteryRecordRepository records;
    private final MasteryEventRepository events;
    private final PrerequisiteHealth prerequisiteHealth;
    private final ObjectMapper json;

    public GradedMasteryService(
            MasteryRecordRepository records,
            MasteryEventRepository events,
            PrerequisiteHealth prerequisiteHealth,
            ObjectMapper json) {
        this.records = records;
        this.events = events;
        this.prerequisiteHealth = prerequisiteHealth;
        this.json = json;
    }

    /** One marked item. Unmarked items are appended as history and change no count. */
    public void recordItem(
            UUID userId, UUID unitId, UnitFacts facts, Volatility volatility, Marked item, UUID attemptId) {
        MasteryRecordEntity record = record(userId, unitId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean newDay = !today.equals(record.getLastGradedOn());

        Graded after = GradedEvidencePolicy.apply(toGraded(record), item, newDay);
        if (item.marked()) {
            record.setLastGradedOn(today);
            record.setLastGradedAt(Instant.now());
        }

        append(userId, unitId, LearningEventKind.ITEM_ANSWERED, Map.of(
                "attemptId", attemptId.toString(),
                "kind", item.kind().code(),
                "bloom", item.bloom().code(),
                "difficulty", item.difficulty(),
                "fraction", item.fraction(),
                "marked", item.marked()));
        persist(record, after, facts, volatility);
    }

    /** One submitted assessment. This is what can fill the Assess chip, and only if it passed. */
    public void recordSubmission(
            UUID userId,
            UUID unitId,
            UnitFacts facts,
            Volatility volatility,
            com.aileapro.domain.AssessmentKind kind,
            double fraction,
            boolean passed,
            UUID attemptId) {
        MasteryRecordEntity record = record(userId, unitId);
        Graded after = GradedEvidencePolicy.applySubmission(toGraded(record), kind, fraction, passed);
        append(userId, unitId, LearningEventKind.ATTEMPT_SUBMITTED, Map.of(
                "attemptId", attemptId.toString(),
                "kind", kind.code(),
                "fraction", fraction,
                "passed", passed));
        persist(record, after, facts, volatility);
    }

    /** One graded card. Only a card answered on or after its due date counts for retention. */
    public void recordReview(
            UUID userId,
            UUID unitId,
            UnitFacts facts,
            Volatility volatility,
            boolean wasDue,
            boolean passed,
            UUID cardId,
            int grade) {
        MasteryRecordEntity record = record(userId, unitId);
        Graded after = GradedEvidencePolicy.applyReview(toGraded(record), wasDue, passed);
        append(userId, unitId, LearningEventKind.REVIEW_ANSWERED, Map.of(
                "cardId", cardId.toString(),
                "grade", grade,
                "wasDue", wasDue,
                "passed", passed));
        persist(record, after, facts, volatility);
    }

    /**
     * Sets how many named false beliefs are currently open on this unit, which is the one
     * input that can pull a state <em>down</em>.
     */
    public void setOpenMisconceptions(
            UUID userId,
            UUID unitId,
            UnitFacts facts,
            Volatility volatility,
            int open,
            LearningEventKind kind,
            String code) {
        MasteryRecordEntity record = record(userId, unitId);
        Graded after = GradedEvidencePolicy.withOpenMisconceptions(toGraded(record), open);
        if (code != null) {
            append(userId, unitId, kind, Map.of("code", code, "open", open));
        }
        persist(record, after, facts, volatility);
    }

    @Transactional(readOnly = true)
    public MasteryState ceilingFor(UnitFacts facts) {
        return GradedEvidencePolicy.reachableCeiling(facts);
    }

    /**
     * The one requirement between the learner and their next rung on this unit.
     *
     * <p>A learner with no record is not a special case: the gate for no evidence at all is
     * the same gate as for a page that was opened and closed, which is to go and read it.
     */
    @Transactional(readOnly = true)
    public GradedEvidencePolicy.Gate gateFor(UUID userId, UUID unitId, UnitFacts facts) {
        return records
                .findByUserIdAndUnitId(userId, unitId)
                .map(record -> GradedEvidencePolicy.nextGate(evidenceOf(record), toGraded(record), facts))
                .orElseGet(() -> GradedEvidencePolicy.nextGate(Evidence.none(), Graded.empty(), facts));
    }

    private MasteryRecordEntity record(UUID userId, UUID unitId) {
        return records.findByUserIdAndUnitId(userId, unitId).orElseGet(() -> {
            MasteryRecordEntity fresh = new MasteryRecordEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setUserId(userId);
            fresh.setUnitId(unitId);
            return fresh;
        });
    }

    /**
     * Writes the projection.
     *
     * <p>Chip timestamps only move when the chip itself moves, so a tooltip can never claim
     * evidence that is older than it looks.
     */
    private void persist(
            MasteryRecordEntity record, Graded after, UnitFacts facts, Volatility volatility) {
        Instant now = Instant.now();
        Evidence before = evidenceOf(record);
        Evidence evidence = GradedEvidencePolicy.evidenceFor(before, after, facts);
        MasteryState state = GradedEvidencePolicy.stateFor(evidence, after, facts);

        record.setPracticeItemsCorrect(after.practiceCorrect());
        record.setPracticeItemsAnswered(after.practiceAnswered());
        record.setAssessItemsCorrect(after.assessCorrect());
        record.setAssessItemsAnswered(after.assessAnswered());
        record.setAssessBestFraction(after.assessBestFraction() == null
                ? null
                : BigDecimal.valueOf(after.assessBestFraction()).setScale(4, RoundingMode.HALF_UP));
        record.setAssessmentsPassed(after.assessmentsPassed());
        record.setUnderstandItemsCorrect(after.understandCorrect());
        record.setGradedDays(after.gradedDays());
        record.setOpenMisconceptions(after.openMisconceptions());
        record.setReviewsDuePassed(after.reviewsDuePassed());
        record.setReviewsDueFailed(after.reviewsDueFailed());
        record.setAccuracyEma(after.accuracyEma() == null
                ? null
                : BigDecimal.valueOf(after.accuracyEma()).setScale(4, RoundingMode.HALF_UP));

        if (evidence.practice() != before.practice()) {
            record.setPracticeState(evidence.practice());
            record.setPracticeAt(now);
        }
        if (evidence.assess() != before.assess()) {
            record.setAssessState(evidence.assess());
            record.setAssessAt(now);
        }

        record.setState(state);
        record.setScore(BigDecimal.valueOf(score(record, after, volatility, now))
                .setScale(3, RoundingMode.HALF_UP));
        record.setLastEventAt(now);
        record.setUpdatedAt(now);
        records.save(record);
    }

    /**
     * The composite score from 07, used for ranking rather than shown as a percentage.
     *
     * <p>Application is zero throughout: nothing in the product can fill the Apply chip yet,
     * and inventing a value for it would quietly inflate every score. The ceiling is reported
     * separately so the number can be read against what is actually achievable.
     */
    private double score(MasteryRecordEntity record, Graded graded, Volatility volatility, Instant now) {
        Integer daysSince = record.getLastGradedAt() == null
                ? null
                : (int) java.time.Duration.between(record.getLastGradedAt(), now).toDays();
        return CompositeScore.of(new CompositeScore.Signals(
                graded.accuracyEma() == null ? 0 : graded.accuracyEma(),
                CompositeScore.consistencyFrom(graded.gradedDays()),
                CompositeScore.retentionFrom(graded.reviewsDuePassed(), graded.reviewsDueFailed()),
                0,
                prerequisiteHealth.healthFor(record.getUserId(), record.getUnitId()),
                CompositeScore.recencyFrom(daysSince == null ? 0 : daysSince, volatility),
                CompositeScore.misconceptionLoadFrom(graded.openMisconceptions())));
    }

    private void append(UUID userId, UUID unitId, LearningEventKind kind, Map<String, Object> payload) {
        MasteryEventEntity event = new MasteryEventEntity();
        event.setId(UUID.randomUUID());
        event.setUserId(userId);
        event.setUnitId(unitId);
        event.setKind(kind);
        event.setPayload(payloadJson(payload));
        event.setAt(Instant.now());
        events.save(event);
    }

    private String payloadJson(Map<String, Object> payload) {
        try {
            return json.writeValueAsString(new LinkedHashMap<>(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise graded event payload", impossible);
        }
    }

    private static Evidence evidenceOf(MasteryRecordEntity record) {
        return new Evidence(
                record.getReadState(),
                record.getPracticeState(),
                record.getAssessState(),
                record.getApplyState());
    }

    private static Graded toGraded(MasteryRecordEntity record) {
        return new Graded(
                record.getPracticeItemsCorrect(),
                record.getPracticeItemsAnswered(),
                record.getAssessItemsCorrect(),
                record.getAssessItemsAnswered(),
                record.getAssessBestFraction() == null ? null : record.getAssessBestFraction().doubleValue(),
                record.getAssessmentsPassed(),
                record.getUnderstandItemsCorrect(),
                record.getGradedDays(),
                record.getOpenMisconceptions(),
                record.getReviewsDuePassed(),
                record.getReviewsDueFailed(),
                record.getAccuracyEma() == null ? null : record.getAccuracyEma().doubleValue());
    }

    /** True once any graded work exists for this learner, for the dashboard. */
    @Transactional(readOnly = true)
    public boolean hasAnyGradedEvidence(UUID userId) {
        return records.findByUserId(userId).stream()
                .anyMatch(record -> record.getPracticeState() != EvidenceState.EMPTY
                        || record.getAssessState() != EvidenceState.EMPTY);
    }
}
