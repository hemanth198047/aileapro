package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.PlanLine;
import com.aileapro.api.assessment.AssessmentViews.Prior;
import com.aileapro.api.assessment.AssessmentViews.StudyPlan;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PathDetail;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.curriculum.CurriculumViews.Waypoint;
import com.aileapro.api.learning.LearnerProfileService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.persistence.AttemptAnswerEntity;
import com.aileapro.persistence.AttemptEntity;
import com.aileapro.persistence.DiagnosticPriorEntity;
import com.aileapro.persistence.PlanItemEntity;
import com.aileapro.persistence.QuestionEntity;
import com.aileapro.persistence.StudyPlanEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a submitted diagnostic into a starting point and a plan.
 *
 * <p>The distinction this class exists to preserve: the diagnostic produces <em>priors</em>,
 * not evidence. A prior is a guess about where to begin, held per domain and carried with its
 * own confidence, and it is allowed to be wrong. Two questions cannot establish that someone
 * knows a topic, so nothing here touches a mastery state or an evidence chip — those move
 * only through work done on the units themselves.
 *
 * <p>The plan is versioned rather than overwritten, and every line carries the reason it is
 * there. A plan that cannot say why it contains something is a list, and a learner cannot
 * argue with a list.
 */
@Service
@Transactional
public class DiagnosticService {

    /** Below this share correct, the domain is treated as unstarted rather than shaky. */
    private static final double START_FROM_SCRATCH = 0.34;

    /** At or above this, the domain is skipped in the plan and left to spaced review. */
    private static final double LIKELY_SOLID = 0.80;

    static final String SKIP = "skip";
    static final String LEARN = "learn";
    static final String PRACTICE = "practice";
    static final String ASSESS = "assess";

    private final AttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final QuestionRepository questions;
    private final DiagnosticPriorRepository priors;
    private final StudyPlanRepository plans;
    private final PlanItemRepository planItems;
    private final AttemptService attemptService;
    private final CurriculumQueryService curriculum;
    private final LearnerProfileService profiles;
    private final ObjectMapper json;

    public DiagnosticService(
            AttemptRepository attempts,
            AttemptAnswerRepository answers,
            QuestionRepository questions,
            DiagnosticPriorRepository priors,
            StudyPlanRepository plans,
            PlanItemRepository planItems,
            AttemptService attemptService,
            CurriculumQueryService curriculum,
            LearnerProfileService profiles,
            ObjectMapper json) {
        this.attempts = attempts;
        this.answers = answers;
        this.questions = questions;
        this.priors = priors;
        this.plans = plans;
        this.planItems = planItems;
        this.attemptService = attemptService;
        this.curriculum = curriculum;
        this.profiles = profiles;
        this.json = json;
    }

    /** Writes priors and a plan for a submitted diagnostic, or returns the plan already written. */
    public StudyPlan planFrom(UUID userId, UUID attemptId) {
        AttemptEntity attempt = attemptService.require(userId, attemptId);
        if (attempt.getKind() != AssessmentKind.DIAGNOSTIC) {
            throw ApiException.validation("That attempt was not a diagnostic");
        }
        if (!AttemptService.SUBMITTED.equals(attempt.getStatus())) {
            throw ApiException.validation("Submit the diagnostic before asking for a plan");
        }

        Optional<StudyPlanEntity> existing = plans.findFirstByUserIdOrderByVersionDesc(userId)
                .filter(plan -> attemptId.equals(plan.getAttemptId()));
        if (existing.isPresent()) {
            return view(existing.get());
        }

        List<Prior> estimated = writePriors(userId, attempt);
        StudyPlanEntity plan = writePlan(userId, attempt, estimated);
        return view(plan);
    }

    @Transactional(readOnly = true)
    public Optional<StudyPlan> latestPlan(UUID userId) {
        return plans.findFirstByUserIdOrderByVersionDesc(userId).map(this::view);
    }

    // ---------------------------------------------------------------------------- priors

    /**
     * One estimate per domain, from the items that domain actually served.
     *
     * <p>The estimate is the share correct, and the confidence is a function of how many items
     * it rests on. Reporting a bare number from two items would be the dishonest option: the
     * number would look like the ones derived from four, and the learner would have no way to
     * tell how much to trust the placement.
     */
    private List<Prior> writePriors(UUID userId, AttemptEntity attempt) {
        List<AttemptAnswerEntity> rows = answers.findByAttemptIdOrderBySort(attempt.getId());
        Map<UUID, QuestionEntity> items = attemptService.itemsFor(rows);

        // Items are tagged with the knowledge unit they test; the prior belongs to the domain
        // that contains it, which is the level a starting point is chosen at.
        Map<UUID, UUID> domainByUnit = new LinkedHashMap<>();
        Map<UUID, int[]> counts = new LinkedHashMap<>();
        for (AttemptAnswerEntity row : rows) {
            QuestionEntity item = items.get(row.getQuestionId());
            if (item == null || !row.isAnswered() || row.getCorrect() == null) {
                continue;
            }
            UUID domainId = domainByUnit.computeIfAbsent(item.getUnitId(), this::domainOf);
            if (domainId == null) {
                continue;
            }
            int[] tally = counts.computeIfAbsent(domainId, key -> new int[2]);
            tally[0]++;
            if (row.getCorrect()) {
                tally[1]++;
            }
        }

        Map<UUID, UnitRef> refs = curriculum.publishedRefs(counts.keySet());
        List<Prior> out = new ArrayList<>(counts.size());
        for (Map.Entry<UUID, int[]> entry : counts.entrySet()) {
            int served = entry.getValue()[0];
            int correct = entry.getValue()[1];
            double estimate = served == 0 ? 0 : correct / (double) served;
            String confidence = served >= 4 ? "medium" : "low";

            DiagnosticPriorEntity row = new DiagnosticPriorEntity();
            row.setId(UUID.randomUUID());
            row.setAttemptId(attempt.getId());
            row.setUserId(userId);
            row.setDomainUnitId(entry.getKey());
            row.setItemsServed(served);
            row.setItemsCorrect(correct);
            row.setEstimate(BigDecimal.valueOf(estimate).setScale(4, RoundingMode.HALF_UP));
            row.setConfidence(confidence);
            priors.save(row);

            UnitRef domain = refs.get(entry.getKey());
            out.add(new Prior(domain, served, correct, estimate, confidence, priorReason(served, correct, estimate)));
        }
        out.sort(Comparator.comparingDouble(Prior::estimate));
        return out;
    }

    private String priorReason(int served, int correct, double estimate) {
        String basis = "%d of %d correct".formatted(correct, served);
        if (served < 3) {
            return basis + ", which is too little to be sure of — it sets a starting point, not a verdict.";
        }
        if (estimate >= LIKELY_SOLID) {
            return basis + ", so this is likely already comfortable and the plan starts you past it.";
        }
        if (estimate <= START_FROM_SCRATCH) {
            return basis + ", so the plan starts this domain from the beginning.";
        }
        return basis + ", so there is something here already and the plan starts mid-domain rather than at the top.";
    }

    /** The domain a unit belongs to: the outermost ancestor, which is where placement happens. */
    private UUID domainOf(UUID unitId) {
        List<UnitRef> breadcrumb = curriculum.unit(unitId.toString()).breadcrumb();
        return breadcrumb.isEmpty() ? unitId : breadcrumb.getFirst().id();
    }

    // ------------------------------------------------------------------------- the plan

    /**
     * The plan: the learner's target path, with the domains the diagnostic found solid trimmed
     * from the front and the rest paced against the hours they said they had.
     */
    private StudyPlanEntity writePlan(UUID userId, AttemptEntity attempt, List<Prior> estimated) {
        // A distinct code, not a generic validation error: the client can route the learner to
        // onboarding, which is a fixable situation rather than a malformed request.
        var profile = profiles.profile(userId)
                .filter(saved -> saved.targetPathSlug() != null)
                .orElseThrow(() -> new ApiException(
                        400,
                        "goal_required",
                        "Set a goal before asking for a plan: a plan without a destination is a reading list."));
        PathDetail path = curriculum.path(profile.targetPathSlug());

        Map<UUID, Prior> byDomain = new LinkedHashMap<>();
        estimated.stream()
                .filter(prior -> prior.domain() != null)
                .forEach(prior -> byDomain.put(prior.domain().id(), prior));

        int version = plans.findFirstByUserIdOrderByVersionDesc(userId)
                .map(StudyPlanEntity::getVersion)
                .orElse(0) + 1;

        StudyPlanEntity plan = new StudyPlanEntity();
        plan.setId(UUID.randomUUID());
        plan.setUserId(userId);
        plan.setVersion(version);
        plan.setHorizon("weekly");
        plan.setSource("diagnostic");
        plan.setAttemptId(attempt.getId());
        plan.setReason(planReason(estimated, path.title()));
        plan.setBody(bodyJson(estimated));
        plan.setGeneratedAt(Instant.now());
        plans.save(plan);

        // Paced against the hours the learner said they had, not against an ideal week.
        int hoursPerWeek = profile.hoursPerWeek() == null ? 5 : Math.max(1, profile.hoursPerWeek());
        int minutesPerWeek = hoursPerWeek * 60;

        List<PlanItemEntity> rows = new ArrayList<>();
        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        int minutesThisWeek = 0;
        int sort = 0;

        for (Waypoint waypoint : path.waypoints()) {
            UnitRef unit = waypoint.unit();
            Prior prior = priorFor(unit, byDomain);
            if (prior != null && prior.estimate() >= LIKELY_SOLID) {
                // Skipped, and the plan says so rather than silently omitting it.
                rows.add(item(plan.getId(), unit.id(), SKIP, null, sort++,
                        "The diagnostic suggests you already have this (%d of %d correct in %s). Spaced review "
                                .formatted(prior.itemsCorrect(), prior.itemsServed(), prior.domain().title())
                                + "will catch it if that guess was wrong."));
                continue;
            }

            int estimate = unit.estimateMinutes() == null ? 30 : unit.estimateMinutes();
            if (minutesThisWeek + estimate > minutesPerWeek && minutesThisWeek > 0) {
                cursor = cursor.plusWeeks(1);
                minutesThisWeek = 0;
            }
            minutesThisWeek += estimate;

            rows.add(item(plan.getId(), unit.id(), LEARN, cursor, sort++, learnReason(unit, waypoint, prior)));
            rows.add(item(plan.getId(), unit.id(), PRACTICE, cursor, sort++,
                    "Reading moves the Read chip and nothing else. The graded practice set on this unit is what "
                            + "takes it past INTRODUCED."));
            if ("PROFICIENT".equals(waypoint.requiredMasteryState())
                    || "PRACTICED".equals(waypoint.requiredMasteryState())) {
                rows.add(item(plan.getId(), unit.id(), ASSESS, cursor, sort++,
                        "This waypoint asks for %s, which needs a quiz passed cold on a day after the practice."
                                .formatted(waypoint.requiredMasteryState())));
            }
        }
        planItems.saveAll(rows);
        return plan;
    }

    private Prior priorFor(UnitRef unit, Map<UUID, Prior> byDomain) {
        UUID domainId = domainOf(unit.id());
        return byDomain.get(domainId);
    }

    private String learnReason(UnitRef unit, Waypoint waypoint, Prior prior) {
        if (prior == null) {
            return "The diagnostic did not cover this area, so it is on the plan at its normal place on the path.";
        }
        if (prior.estimate() <= START_FROM_SCRATCH) {
            return "%s came out at %d of %d in the diagnostic, so this starts from the beginning."
                    .formatted(prior.domain().title(), prior.itemsCorrect(), prior.itemsServed());
        }
        return "%s was partly there (%d of %d), and this waypoint asks for %s."
                .formatted(prior.domain().title(), prior.itemsCorrect(), prior.itemsServed(),
                        waypoint.requiredMasteryState());
    }

    private String planReason(List<Prior> estimated, String pathTitle) {
        if (estimated.isEmpty()) {
            return "Built from " + pathTitle + " in its authored order: the diagnostic produced no usable "
                    + "estimates, so nothing was skipped.";
        }
        long solid = estimated.stream().filter(prior -> prior.estimate() >= LIKELY_SOLID).count();
        return "Built from %s, adjusted by a diagnostic across %d domain%s. %s"
                .formatted(pathTitle, estimated.size(), estimated.size() == 1 ? "" : "s",
                        solid == 0
                                ? "Nothing was skipped: no domain came out high enough to justify it."
                                : solid + " domain" + (solid == 1 ? " was" : "s were")
                                        + " marked as likely already held and moved to review rather than study.");
    }

    /**
     * Every plan item kind this service can write.
     *
     * <p>The kinds are a string column with a CHECK constraint behind it, and the test schema is
     * built from the entities rather than from the migrations — so nothing but a test comparing
     * this list against the migration stands between a new kind and a 500 in production.
     */
    static final List<String> PLAN_ITEM_KINDS = List.of(SKIP, LEARN, PRACTICE, ASSESS);

    private PlanItemEntity item(UUID planId, UUID unitId, String kind, LocalDate due, int sort, String reason) {
        PlanItemEntity row = new PlanItemEntity();
        row.setId(UUID.randomUUID());
        row.setPlanId(planId);
        row.setUnitId(unitId);
        row.setKind(kind);
        row.setDue(due);
        row.setSort(sort);
        row.setReason(reason);
        return row;
    }

    // ----------------------------------------------------------------------------- views

    private StudyPlan view(StudyPlanEntity plan) {
        List<PlanItemEntity> rows = planItems.findByPlanIdOrderBySort(plan.getId());
        Map<UUID, UnitRef> refs = curriculum.publishedRefs(
                rows.stream().map(PlanItemEntity::getUnitId).filter(java.util.Objects::nonNull).distinct().toList());

        List<PlanLine> lines = rows.stream()
                .map(row -> {
                    UnitRef unit = row.getUnitId() == null ? null : refs.get(row.getUnitId());
                    return new PlanLine(
                            row.getKind(),
                            unit,
                            row.getReason(),
                            row.getDue(),
                            unit == null ? null : "/app/learn/" + unit.slug());
                })
                .toList();

        return new StudyPlan(
                plan.getId(),
                plan.getVersion(),
                plan.getHorizon(),
                plan.getReason(),
                plan.getSource(),
                plan.getGeneratedAt(),
                readPriors(plan),
                lines);
    }

    private List<Prior> readPriors(StudyPlanEntity plan) {
        if (plan.getAttemptId() == null) {
            return List.of();
        }
        List<DiagnosticPriorEntity> rows = priors.findByAttemptId(plan.getAttemptId());
        Map<UUID, UnitRef> refs = curriculum.publishedRefs(
                rows.stream().map(DiagnosticPriorEntity::getDomainUnitId).distinct().toList());
        return rows.stream()
                .map(row -> new Prior(
                        refs.get(row.getDomainUnitId()),
                        row.getItemsServed(),
                        row.getItemsCorrect(),
                        row.getEstimate().doubleValue(),
                        row.getConfidence(),
                        priorReason(row.getItemsServed(), row.getItemsCorrect(), row.getEstimate().doubleValue())))
                .sorted(Comparator.comparingDouble(Prior::estimate))
                .toList();
    }

    private String bodyJson(List<Prior> estimated) {
        try {
            return json.writeValueAsString(Map.of("priors", estimated));
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise the plan body", impossible);
        }
    }
}
