package com.aileapro.api.tutor;

import com.aileapro.ai.TutorPolicy;
import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.assessment.AttemptAnswerRepository;
import com.aileapro.api.assessment.MisconceptionService;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.MasteryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Assembles what the tutor is allowed to know about the learner.
 *
 * <p>Four things, each of which changes the answer: which unit they are on, what their record
 * says about it, which named false beliefs are currently open, and what they have recently got
 * wrong. A tutor without these gives a correct answer to a question the learner did not have.
 *
 * <p>Nothing here crosses a user boundary: every query takes the learner's id, and the open
 * misconceptions come from their own flags. There is no "learners like you" in this context and
 * there should not be — it is one person's record, used to teach one person.
 */
@Service
@Transactional(readOnly = true)
public class TutorContextService {

    /** Enough to shape an answer. More would spend context on noise. */
    private static final int RECENT_MISTAKES = 3;

    private final CurriculumQueryService curriculum;
    private final MasteryService mastery;
    private final MisconceptionService misconceptions;
    private final AttemptAnswerRepository answers;

    public TutorContextService(
            CurriculumQueryService curriculum,
            MasteryService mastery,
            MisconceptionService misconceptions,
            AttemptAnswerRepository answers) {
        this.curriculum = curriculum;
        this.mastery = mastery;
        this.misconceptions = misconceptions;
        this.answers = answers;
    }

    /** The context a unit-scoped conversation runs with, or {@code none} without a unit. */
    public TutorPolicy.UnitContext contextFor(UUID userId, UUID unitId) {
        if (unitId == null) {
            return TutorPolicy.UnitContext.none();
        }
        UnitRef unit = curriculum.publishedRefs(List.of(unitId)).get(unitId);
        if (unit == null) {
            return TutorPolicy.UnitContext.none();
        }

        Mastery record = mastery.mastery(userId, unit.slug());
        List<String> openCodes = misconceptions.openFor(userId, List.of(unitId)).stream()
                .map(Misconception::claim)
                .toList();
        List<String> recent = answers.recentWrongStems(
                userId, unitId, PageRequest.of(0, RECENT_MISTAKES));

        return new TutorPolicy.UnitContext(
                unit.slug(),
                unit.title(),
                record.state(),
                "solid".equals(record.evidence().read().state()),
                openCodes,
                recent);
    }

    /**
     * The same context by unit slug, for the dock.
     *
     * <p>A slug the learner cannot see published resolves to no context rather than to an
     * error: the panel is ambient, and it should go quiet rather than throw at somebody who
     * navigated somewhere unexpected.
     */
    public TutorViews.Context viewForSlug(UUID userId, String unitSlug) {
        if (unitSlug == null || unitSlug.isBlank()) {
            return view(userId, null);
        }
        UUID unitId;
        try {
            unitId = curriculum.publishedRef(unitSlug).id();
        } catch (RuntimeException unknown) {
            return view(userId, null);
        }
        return view(userId, unitId);
    }

    /** The same context, shaped for the dock so the learner can see what the tutor sees. */
    public TutorViews.Context view(UUID userId, UUID unitId) {
        TutorPolicy.UnitContext context = contextFor(userId, unitId);
        return new TutorViews.Context(
                context.slug(),
                context.title(),
                context.masteryState(),
                context.readSolid(),
                context.openMisconceptions(),
                context.recentMistakes());
    }
}
