package com.aileapro.api.assessment;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.GradedEvidencePolicy.Gate;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.AssessmentEntity;
import com.aileapro.persistence.AssessmentItemEntity;
import com.aileapro.persistence.QuestionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the authored content on a unit can actually support.
 *
 * <p>The mastery rules need this to tell a learner falling short from a unit that never
 * asked. A page with five definition items cannot show understanding however well the
 * learner does, and the ceiling has to come from the content rather than from a guess.
 */
@Service
public class UnitFactsService {

    private final AssessmentRepository assessments;
    private final AssessmentItemRepository assessmentItems;
    private final QuestionRepository questions;

    public UnitFactsService(
            AssessmentRepository assessments,
            AssessmentItemRepository assessmentItems,
            QuestionRepository questions) {
        this.assessments = assessments;
        this.assessmentItems = assessmentItems;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public UnitFacts factsFor(UUID unitId) {
        return factsFor(List.of(unitId)).getOrDefault(unitId, UnitFacts.none());
    }

    /**
     * Facts for a batch of units.
     *
     * <p>An item's purpose comes from the assessment that contains it, never from the item,
     * so the same question can be practice on one unit's dock and a quiz item on another's.
     * That is why this counts through the join table instead of grouping the question bank.
     */
    @Transactional(readOnly = true)
    public Map<UUID, UnitFacts> factsFor(List<UUID> unitIds) {
        if (unitIds.isEmpty()) {
            return Map.of();
        }

        List<AssessmentEntity> published =
                assessments.findByUnitIdInAndStatus(unitIds, ContentStatus.PUBLISHED);
        if (published.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AssessmentEntity> byId = new HashMap<>();
        published.forEach(assessment -> byId.put(assessment.getId(), assessment));

        Map<UUID, Integer> practiceCounts = new HashMap<>();
        Map<UUID, Integer> assessCounts = new HashMap<>();
        Map<UUID, Set<UUID>> questionIdsByUnit = new HashMap<>();

        for (AssessmentItemEntity row : assessmentItems.findByIdAssessmentIdIn(byId.keySet())) {
            AssessmentEntity assessment = byId.get(row.getId().getAssessmentId());
            if (assessment == null || assessment.getUnitId() == null) {
                continue;
            }
            UUID unitId = assessment.getUnitId();
            questionIdsByUnit.computeIfAbsent(unitId, key -> new HashSet<>()).add(row.getId().getQuestionId());
            if (assessment.getKind() == AssessmentKind.PRACTICE) {
                practiceCounts.merge(unitId, 1, Integer::sum);
            } else if (assessment.getKind().chip() == com.aileapro.domain.EvidenceChip.ASSESS) {
                assessCounts.merge(unitId, 1, Integer::sum);
            }
        }

        List<UUID> allQuestionIds = new ArrayList<>();
        questionIdsByUnit.values().forEach(allQuestionIds::addAll);
        Set<UUID> aboveRecall = new HashSet<>();
        for (QuestionEntity question : questions.findByIdIn(allQuestionIds)) {
            if (!question.getBloom().isRecallOnly()) {
                aboveRecall.add(question.getId());
            }
        }

        Map<UUID, UnitFacts> out = new HashMap<>();
        for (UUID unitId : unitIds) {
            Set<UUID> forUnit = questionIdsByUnit.getOrDefault(unitId, Set.of());
            boolean hasUnderstand = forUnit.stream().anyMatch(aboveRecall::contains);
            out.put(unitId, new UnitFacts(
                    practiceCounts.getOrDefault(unitId, 0),
                    assessCounts.getOrDefault(unitId, 0),
                    hasUnderstand));
        }
        return out;
    }

    /**
     * One sentence saying what the learner's current state is waiting on.
     *
     * <p>A state with no explanation is a label, and a label the learner cannot act on is
     * worse than no label: it invites them to read it as a verdict. Where the content is what
     * stops them, this says so rather than implying they fell short.
     */
    public static String stateReason(Gate gate, UnitFacts facts, MasteryState ceiling) {
        return switch (gate) {
            case READ_THE_UNIT -> "Reading the unit through comes first — nothing graded counts until the page"
                    + " it is about has actually been read.";
            case DEMONSTRATE_RECALL -> "Two correct answers at a good enough rate would move this on. Recall is the"
                    + " first rung, and it is the only one the practice set can grant on its own.";
            case CLEAR_A_MISCONCEPTION -> "A named false belief is open on this unit, which holds the state at recall."
                    + " Answering something that depends on the opposite clears it.";
            case ANSWER_ABOVE_RECALL -> "Reciting the rule is not understanding it. Two correct answers on items that"
                    + " ask for more than recall would move this to UNDERSTOOD.";
            case COMPLETE_THE_PRACTICE_SET -> "Working most of the authored practice set correctly would move this to"
                    + " PRACTICED.";
            case PASS_THE_QUIZ -> "Passing the unit quiz at its own pass mark would move this to PROFICIENT.";
            case RETURN_ON_ANOTHER_DAY -> "Everything is passed, on one day. Consistency needs graded work on a"
                    + " second day, because one good afternoon is not evidence of durability.";
            case BUILD_SOMETHING -> "MASTERED needs applied work, which arrives with projects. Until then this is the"
                    + " top of what can be shown.";
            case PASS_A_SPACED_REVIEW -> "A retention check after a real interval is the last thing outstanding.";
            case CONTENT_CEILING -> ceilingReason(facts, ceiling);
            case NONE -> "Nothing is outstanding on this unit.";
        };
    }

    /** One sentence saying why this unit cannot currently go higher than it can. */
    public static String ceilingReason(UnitFacts facts, MasteryState ceiling) {
        return switch (ceiling) {
            case INTRODUCED -> "No items are authored for this unit yet, so reading is all the evidence there is.";
            case FAMILIAR -> "Every authored item on this unit asks for recall, so understanding cannot be shown here"
                    + " until items above that level are written.";
            case UNDERSTOOD -> "No practice set is authored for this unit, so PRACTICED is not reachable.";
            case PRACTICED -> "No quiz is authored for this unit, so PROFICIENT is not reachable.";
            case PROFICIENT -> "PROFICIENT is the top of what the product can currently evidence: MASTERED needs"
                    + " applied work and a retention check, which arrive with the lab.";
            case MASTERED, UNKNOWN -> "";
        };
    }
}
