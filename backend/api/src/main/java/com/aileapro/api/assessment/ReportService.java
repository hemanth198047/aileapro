package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.assessment.AssessmentViews.Report;
import com.aileapro.api.assessment.AssessmentViews.ReportLine;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.graph.GraphQueryService;
import com.aileapro.api.graph.GraphViews;
import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.MasteryService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.BloomLevel;
import com.aileapro.domain.GradedEvidencePolicy;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.AssessmentEntity;
import com.aileapro.persistence.AttemptAnswerEntity;
import com.aileapro.persistence.AttemptEntity;
import com.aileapro.persistence.ExamReportEntity;
import com.aileapro.persistence.QuestionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the report for a submitted attempt, and stores it.
 *
 * <p>Stored rather than recomputed, because a report is <em>what the learner was told</em>.
 * Regenerating it next month would show them what today's rules say about a sitting from
 * last month, which is a different claim wearing the same date.
 *
 * <p>ASM-06 — never score-only — is enforced in the schema: every narrative column is NOT
 * NULL. This class cannot produce a bare number even if a future edit tried to, because the
 * insert would fail. The sections are the report; the fraction is a detail inside it.
 */
@Service
@Transactional
public class ReportService {

    private static final String TEMPLATE_VERSION = "1.0.0";

    /** Below this on a topic, the report treats it as a weakness rather than a wobble. */
    private static final double WEAK_FRACTION = 0.60;

    /** At or above this, and with more than one item, it is worth calling a strength. */
    private static final double STRONG_FRACTION = 0.80;

    private final ExamReportRepository reports;
    private final AttemptService attemptService;
    private final CurriculumQueryService curriculum;
    private final GraphQueryService graph;
    private final MisconceptionService misconceptions;
    private final UnitFactsService unitFacts;
    private final MasteryService mastery;
    private final GradedMasteryService graded;
    private final ObjectMapper json;

    public ReportService(
            ExamReportRepository reports,
            AttemptService attemptService,
            CurriculumQueryService curriculum,
            GraphQueryService graph,
            MisconceptionService misconceptions,
            UnitFactsService unitFacts,
            MasteryService mastery,
            GradedMasteryService graded,
            ObjectMapper json) {
        this.reports = reports;
        this.attemptService = attemptService;
        this.curriculum = curriculum;
        this.graph = graph;
        this.misconceptions = misconceptions;
        this.unitFacts = unitFacts;
        this.mastery = mastery;
        this.graded = graded;
        this.json = json;
    }

    /** Returns the stored report, generating it on first read after submission. */
    public Report reportFor(UUID userId, UUID attemptId) {
        AttemptEntity attempt = attemptService.require(userId, attemptId);
        if (!AttemptService.SUBMITTED.equals(attempt.getStatus())) {
            throw ApiException.validation("This attempt has not been submitted yet");
        }
        ExamReportEntity stored = reports.findById(attemptId).orElseGet(() -> generate(userId, attempt));
        return view(userId, attempt, stored);
    }

    private ExamReportEntity generate(UUID userId, AttemptEntity attempt) {
        AssessmentEntity assessment = attemptService.assessmentFor(attempt);
        List<AttemptAnswerEntity> rows = attemptService.answersFor(attempt.getId());
        Map<UUID, QuestionEntity> items = attemptService.itemsFor(rows);

        Tally tally = tally(rows, items);
        Map<UUID, UnitRef> units = curriculum.publishedRefs(tally.byUnit().keySet());

        List<ReportLine> strengths = strengths(tally, units);
        List<ReportLine> weaknesses = weaknesses(tally, units);
        List<Misconception> raised = misconceptions.describe(tally.codes());
        List<ReportLine> missingPrereqs = missingPrereqs(userId, tally, units);
        List<ReportLine> revision = revision(tally, units, raised);
        Readiness readiness = readiness(attempt, assessment, tally);
        List<ReportLine> nextSteps = nextSteps(attempt, assessment, tally, units, raised, missingPrereqs);

        ExamReportEntity report = new ExamReportEntity();
        report.setAttemptId(attempt.getId());
        report.setTemplateVersion(TEMPLATE_VERSION);
        report.setSummary(summary(attempt, assessment, tally));
        report.setReadiness(readiness.verdict());
        report.setReadinessReason(readiness.reason());
        report.setStrengths(write(strengths));
        report.setWeaknesses(write(weaknesses));
        report.setMisconceptions(write(raised));
        report.setMissingPrereqs(write(missingPrereqs));
        report.setRevision(write(revision));
        report.setNextSteps(write(nextSteps));
        return reports.save(report);
    }

    // ----------------------------------------------------------------------- the tally

    /**
     * What was answered, grouped the two ways the narrative needs it.
     *
     * @param byUnit   per unit, because that is what the learner can act on
     * @param byBloom  per cognitive level, because "knows the definitions, cannot apply them"
     *                 is the single most useful thing a report can say
     */
    private record Tally(
            Map<UUID, Bucket> byUnit,
            Map<BloomLevel, Bucket> byBloom,
            Set<String> codes,
            int answered,
            int correct,
            int unmarked,
            double fraction) {
    }

    private record Bucket(int answered, int correct, double earned, double possible) {

        Bucket plus(boolean right, double earnedNow, double possibleNow) {
            return new Bucket(answered + 1, correct + (right ? 1 : 0), earned + earnedNow, possible + possibleNow);
        }

        double fraction() {
            return possible <= 0 ? 0 : earned / possible;
        }
    }

    private Tally tally(List<AttemptAnswerEntity> rows, Map<UUID, QuestionEntity> items) {
        Map<UUID, Bucket> byUnit = new LinkedHashMap<>();
        Map<BloomLevel, Bucket> byBloom = new LinkedHashMap<>();
        Set<String> codes = new LinkedHashSet<>();
        int answered = 0;
        int correct = 0;
        int unmarked = 0;
        double earned = 0;
        double possible = 0;

        for (AttemptAnswerEntity row : rows) {
            QuestionEntity item = items.get(row.getQuestionId());
            if (item == null || !row.isAnswered()) {
                continue;
            }
            codes.addAll(List.of(row.getMisconceptionCodes()));
            if (row.isUnverified()) {
                unmarked++;
                continue;
            }
            answered++;
            boolean right = Boolean.TRUE.equals(row.getCorrect());
            if (right) {
                correct++;
            }
            double points = row.getPoints().doubleValue();
            double score = row.getScore() == null ? 0 : row.getScore().doubleValue();
            earned += score;
            possible += points;
            byUnit.merge(item.getUnitId(), new Bucket(1, right ? 1 : 0, score, points),
                    (existing, ignored) -> existing.plus(right, score, points));
            byBloom.merge(item.getBloom(), new Bucket(1, right ? 1 : 0, score, points),
                    (existing, ignored) -> existing.plus(right, score, points));
        }

        return new Tally(byUnit, byBloom, codes, answered, correct, unmarked,
                possible <= 0 ? 0 : earned / possible);
    }

    // -------------------------------------------------------------------- the narrative

    private String summary(AttemptEntity attempt, AssessmentEntity assessment, Tally tally) {
        if (attempt.getKind() == AssessmentKind.DIAGNOSTIC) {
            return "You answered %d questions across %d topics. This was a placement exercise, so nothing here "
                    .formatted(tally.answered(), tally.byUnit().size())
                    + "counts as evidence of mastery — it decides where you start.";
        }
        String core = "You answered %d of them correctly out of %d marked, which is %.0f%%."
                .formatted(tally.correct(), tally.answered(), tally.fraction() * 100);
        String pass = Boolean.TRUE.equals(attempt.getPassed())
                ? " That is above the pass mark of %.0f%%.".formatted(assessment.getPassMark().doubleValue() * 100)
                : " The pass mark is %.0f%%.".formatted(assessment.getPassMark().doubleValue() * 100);
        String unmarked = tally.unmarked() == 0
                ? ""
                : " %d answer%s could not be marked automatically and %s excluded from the score."
                        .formatted(tally.unmarked(), tally.unmarked() == 1 ? "" : "s",
                                tally.unmarked() == 1 ? "was" : "were");
        return core + pass + unmarked;
    }

    /**
     * Strengths are per cognitive level as well as per topic. "You can recall the rules" and
     * "you can apply them to a new case" are different claims, and a learner who is strong at
     * the first and weak at the second is the most common case there is.
     */
    private List<ReportLine> strengths(Tally tally, Map<UUID, UnitRef> units) {
        List<ReportLine> lines = new ArrayList<>();
        tally.byUnit().entrySet().stream()
                .filter(entry -> entry.getValue().answered() > 1 && entry.getValue().fraction() >= STRONG_FRACTION)
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Bucket> entry) -> entry.getValue().fraction())
                        .reversed())
                .forEach(entry -> {
                    UnitRef unit = units.get(entry.getKey());
                    if (unit == null) {
                        return;
                    }
                    lines.add(new ReportLine(
                            unit.title(),
                            "%d of %d correct.".formatted(entry.getValue().correct(), entry.getValue().answered()),
                            "/app/learn/" + unit.slug()));
                });
        tally.byBloom().entrySet().stream()
                .filter(entry -> entry.getValue().answered() > 1 && entry.getValue().fraction() >= STRONG_FRACTION)
                .forEach(entry -> lines.add(new ReportLine(
                        capitalise(entry.getKey().humanName()),
                        "%d of %d correct at this level."
                                .formatted(entry.getValue().correct(), entry.getValue().answered()),
                        null)));
        if (lines.isEmpty()) {
            lines.add(new ReportLine(
                    "Nothing stood out yet",
                    "No topic in this attempt had enough correct answers to call it a strength. That is "
                            + "information, not a verdict — it says where to start, and the sections below say how.",
                    null));
        }
        return lines;
    }

    private List<ReportLine> weaknesses(Tally tally, Map<UUID, UnitRef> units) {
        List<ReportLine> lines = new ArrayList<>();
        tally.byUnit().entrySet().stream()
                .filter(entry -> entry.getValue().fraction() < WEAK_FRACTION)
                .sorted(Comparator.comparingDouble(entry -> entry.getValue().fraction()))
                .forEach(entry -> {
                    UnitRef unit = units.get(entry.getKey());
                    if (unit == null) {
                        return;
                    }
                    lines.add(new ReportLine(
                            unit.title(),
                            "%d of %d correct.".formatted(entry.getValue().correct(), entry.getValue().answered()),
                            "/app/learn/" + unit.slug()));
                });
        tally.byBloom().entrySet().stream()
                .filter(entry -> entry.getValue().answered() > 1 && entry.getValue().fraction() < WEAK_FRACTION)
                .forEach(entry -> lines.add(new ReportLine(
                        capitalise(entry.getKey().humanName()),
                        "%d of %d correct at this level, which is where the gap is rather than in the topic itself."
                                .formatted(entry.getValue().correct(), entry.getValue().answered()),
                        null)));
        if (lines.isEmpty()) {
            lines.add(new ReportLine(
                    "No weak topic in this attempt",
                    "Every topic here came out above 60%. That is a statement about these questions, not about "
                            + "the whole subject.",
                    null));
        }
        return lines;
    }

    /**
     * Prerequisites of the weak topics that the learner has not established.
     *
     * <p>This is the section that changes what people do. Failing matrix multiplication
     * because you do not have vectors is not a matrix multiplication problem, and telling
     * someone to re-read the page they just failed is the least useful advice available.
     */
    private List<ReportLine> missingPrereqs(UUID userId, Tally tally, Map<UUID, UnitRef> units) {
        List<UUID> weak = tally.byUnit().entrySet().stream()
                .filter(entry -> entry.getValue().fraction() < WEAK_FRACTION)
                .map(Map.Entry::getKey)
                .toList();

        List<ReportLine> lines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (UUID unitId : weak) {
            UnitRef unit = units.get(unitId);
            if (unit == null) {
                continue;
            }
            GraphViews.Prerequisites prerequisites = graph.prerequisites(unit.slug());
            Map<UUID, Mastery> states = mastery.masteryFor(
                    userId, prerequisites.required().stream().map(GraphViews.PrerequisiteStep::unit).toList());
            for (GraphViews.PrerequisiteStep step : prerequisites.required()) {
                Mastery standing = states.get(step.unit().id());
                boolean established = standing != null
                        && MasteryState.valueOf(standing.state()).ordinal()
                                >= MasteryState.UNDERSTOOD.ordinal();
                if (established || !seen.add(step.unit().slug())) {
                    continue;
                }
                lines.add(new ReportLine(
                        step.unit().title(),
                        "Required before %s, and you are at %s on it."
                                .formatted(unit.title(), standing == null ? "UNKNOWN" : standing.state()),
                        "/app/learn/" + step.unit().slug()));
            }
        }
        if (lines.isEmpty()) {
            lines.add(new ReportLine(
                    "Nothing upstream is missing",
                    "The topics you struggled with do not have unmet prerequisites, so the gap is in the topic "
                            + "itself rather than in its foundations.",
                    null));
        }
        return lines;
    }

    /** What to re-read, in the order it should be read. */
    private List<ReportLine> revision(Tally tally, Map<UUID, UnitRef> units, List<Misconception> raised) {
        List<ReportLine> lines = new ArrayList<>();
        for (Misconception misconception : raised) {
            if (misconception.unit() != null) {
                lines.add(new ReportLine(
                        misconception.unit().title() + " — the misconception block",
                        "Addresses the specific belief this attempt surfaced: \"" + misconception.claim() + "\"",
                        misconception.href()));
            }
        }
        tally.byUnit().entrySet().stream()
                .filter(entry -> entry.getValue().fraction() < STRONG_FRACTION)
                .sorted(Comparator.comparingDouble(entry -> entry.getValue().fraction()))
                .forEach(entry -> {
                    UnitRef unit = units.get(entry.getKey());
                    if (unit != null) {
                        lines.add(new ReportLine(
                                unit.title(),
                                "Re-read at a level above the one you used last time, then take the practice set.",
                                "/app/learn/" + unit.slug()));
                    }
                });
        if (lines.isEmpty()) {
            lines.add(new ReportLine(
                    "Nothing to re-read",
                    "Every topic here was solid. Spaced review will bring these back before they fade.",
                    "/app/today"));
        }
        return lines;
    }

    private record Readiness(String verdict, String reason) {
    }

    /**
     * Readiness is about the next step, not about the score. A pass with an open misconception
     * is not readiness, and the reason says which of the two decided it.
     */
    private Readiness readiness(AttemptEntity attempt, AssessmentEntity assessment, Tally tally) {
        if (attempt.getKind() == AssessmentKind.DIAGNOSTIC) {
            return new Readiness(
                    // 'unscored' rather than a pass or a fail: the diagnostic has no bar to clear.
                    "unscored",
                    "This was a placement exercise. It put you at a starting point and wrote a plan; it did not "
                            + "assess anything, and you will be assessed on these topics properly as you reach them.");
        }
        if (!tally.codes().isEmpty()) {
            return new Readiness(
                    "not_ready",
                    "%d named misconception%s came up in this attempt. Clearing those matters more than the score, "
                            .formatted(tally.codes().size(), tally.codes().size() == 1 ? "" : "s")
                            + "because a belief that survives a pass will keep producing wrong answers later.");
        }
        if (Boolean.TRUE.equals(attempt.getPassed())) {
            return new Readiness(
                    "ready",
                    "You cleared the pass mark with no misconception flagged. The next thing that moves your "
                            + "standing is consistency: the same result on another day.");
        }
        return new Readiness(
                "not_ready",
                "You came out at %.0f%% against a pass mark of %.0f%%. The weak topics above are the shortest "
                                .formatted(tally.fraction() * 100, assessment.getPassMark().doubleValue() * 100)
                        + "route to closing that.");
    }

    /** Concrete actions, each with the reason it is on the list. */
    private List<ReportLine> nextSteps(
            AttemptEntity attempt,
            AssessmentEntity assessment,
            Tally tally,
            Map<UUID, UnitRef> units,
            List<Misconception> raised,
            List<ReportLine> missingPrereqs) {
        List<ReportLine> lines = new ArrayList<>();

        for (Misconception misconception : raised) {
            lines.add(new ReportLine(
                    "Clear: " + misconception.claim(),
                    "Read the block that addresses it, then answer the practice item that raised it. The flag "
                            + "clears when you get that item right, not when you have read the page.",
                    misconception.href()));
        }
        missingPrereqs.stream()
                .filter(line -> line.href() != null)
                .limit(2)
                .forEach(line -> lines.add(new ReportLine(
                        "Study first: " + line.label(),
                        line.detail(),
                        line.href())));

        tally.byUnit().entrySet().stream()
                .filter(entry -> entry.getValue().fraction() < WEAK_FRACTION)
                .sorted(Comparator.comparingDouble(entry -> entry.getValue().fraction()))
                .limit(2)
                .forEach(entry -> {
                    UnitRef unit = units.get(entry.getKey());
                    if (unit != null) {
                        lines.add(new ReportLine(
                                "Practise: " + unit.title(),
                                "The practice set on this unit is graded, so working through it moves the Practice "
                                        + "chip rather than just feeling productive.",
                                "/app/learn/" + unit.slug()));
                    }
                });

        if (attempt.getKind() == AssessmentKind.QUIZ && Boolean.TRUE.equals(attempt.getPassed())) {
            lines.add(new ReportLine(
                    "Come back on another day",
                    "PROFICIENT needs graded evidence on more than one day. One good afternoon is not consistency, "
                            + "so the state will not move until you return.",
                    "/app/today"));
        }
        if (lines.isEmpty()) {
            lines.add(new ReportLine(
                    "Take the next unit on your path",
                    "Nothing in this attempt needs fixing first.",
                    "/app/today"));
        }
        return lines;
    }

    // -------------------------------------------------------------------------- reading

    private Report view(UUID userId, AttemptEntity attempt, ExamReportEntity stored) {
        AssessmentEntity assessment = attemptService.assessmentFor(attempt);
        List<AttemptAnswerEntity> rows = attemptService.answersFor(attempt.getId());
        Map<UUID, QuestionEntity> items = attemptService.itemsFor(rows);
        Tally tally = tally(rows, items);

        UnitRef unit = attempt.getUnitId() == null
                ? null
                : curriculum.publishedRefs(List.of(attempt.getUnitId())).get(attempt.getUnitId());

        String masteryState = null;
        String masteryNote = null;
        if (unit != null) {
            Mastery standing = mastery.mastery(userId, unit.slug());
            masteryState = standing.state();
            UnitFacts facts = unitFacts.factsFor(unit.id());
            // Always populated, and always the same sentence the unit page shows: a state with
            // no explanation reads as a verdict, which is the opposite of what it is for.
            masteryNote = UnitFactsService.stateReason(
                    graded.gateFor(userId, unit.id(), facts),
                    facts,
                    GradedEvidencePolicy.reachableCeiling(facts));
        }

        return new Report(
                attempt.getId(),
                attempt.getKind().code(),
                assessment.getTitle(),
                unit,
                attempt.getSubmittedAt(),
                tally.fraction(),
                assessment.getPassMark().doubleValue(),
                attempt.getPassed(),
                tally.answered(),
                tally.correct(),
                tally.unmarked(),
                stored.getSummary(),
                stored.getReadiness(),
                stored.getReadinessReason(),
                readLines(stored.getStrengths()),
                readLines(stored.getWeaknesses()),
                readMisconceptions(stored.getMisconceptions()),
                readLines(stored.getMissingPrereqs()),
                readLines(stored.getRevision()),
                readLines(stored.getNextSteps()),
                masteryState,
                masteryNote);
    }

    private String write(List<?> lines) {
        try {
            return json.writeValueAsString(lines);
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise a report section", impossible);
        }
    }

    private List<ReportLine> readLines(String stored) {
        try {
            return json.readValue(stored, new TypeReference<List<ReportLine>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException broken) {
            throw new IllegalStateException("Unreadable report section", broken);
        }
    }

    private List<Misconception> readMisconceptions(String stored) {
        try {
            return json.readValue(stored, new TypeReference<List<Misconception>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException broken) {
            throw new IllegalStateException("Unreadable report misconceptions", broken);
        }
    }

    private static String capitalise(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
