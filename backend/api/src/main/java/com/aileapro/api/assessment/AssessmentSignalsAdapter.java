package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.DueCard;
import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.assessment.AssessmentViews.ReviewQueue;
import com.aileapro.api.learning.AssessmentSignals;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.GradedEvidencePolicy;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.AssessmentEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Supplies the recommendation rules with the graded facts, so the learner package can rank
 * reviews and practice without depending on this one.
 */
@Component
class AssessmentSignalsAdapter implements AssessmentSignals {

    private final ReviewService reviews;
    private final MisconceptionService misconceptions;
    private final UnitFactsService unitFacts;
    private final AssessmentRepository assessments;
    private final AssessmentItemRepository assessmentItems;

    AssessmentSignalsAdapter(
            ReviewService reviews,
            MisconceptionService misconceptions,
            UnitFactsService unitFacts,
            AssessmentRepository assessments,
            AssessmentItemRepository assessmentItems) {
        this.reviews = reviews;
        this.misconceptions = misconceptions;
        this.unitFacts = unitFacts;
        this.assessments = assessments;
        this.assessmentItems = assessmentItems;
    }

    @Override
    @Transactional(readOnly = true)
    public DueReviews dueReviews(UUID userId) {
        ReviewQueue queue = reviews.due(userId);
        // The worst card, not the average: the queue is sorted most overdue first, so this is
        // the one that justifies the recommendation.
        Optional<DueCard> worst = queue.cards().stream().findFirst();
        return new DueReviews(
                queue.dueCount(),
                queue.overdueCount(),
                worst.map(DueCard::daysOverdue).orElse(0),
                worst.map(card -> card.unit() == null ? null : card.unit().title()).orElse(null),
                worst.map(card -> card.unit() == null ? null : card.unit().slug()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenMisconception> openMisconceptions(UUID userId) {
        return misconceptions.openFor(userId).stream()
                .filter(misconception -> misconception.unit() != null)
                .map(this::toSignal)
                .toList();
    }

    private OpenMisconception toSignal(Misconception misconception) {
        return new OpenMisconception(
                misconception.code(),
                misconception.claim(),
                misconception.unit().title(),
                misconception.unit().slug(),
                misconception.href());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GradedOffer> offerFor(UUID userId, UUID unitId) {
        Optional<AssessmentEntity> practice =
                assessments.findByUnitIdAndKindAndStatus(unitId, AssessmentKind.PRACTICE, ContentStatus.PUBLISHED);
        Optional<AssessmentEntity> quiz =
                assessments.findByUnitIdAndKindAndStatus(unitId, AssessmentKind.QUIZ, ContentStatus.PUBLISHED);
        if (practice.isEmpty() && quiz.isEmpty()) {
            return Optional.empty();
        }

        UnitFacts facts = unitFacts.factsFor(unitId);
        MasteryState ceiling = GradedEvidencePolicy.reachableCeiling(facts);
        return Optional.of(new GradedOffer(
                practice.map(AssessmentEntity::getSlug).orElse(null),
                practice.map(this::itemCount).orElse(0),
                quiz.map(AssessmentEntity::getSlug).orElse(null),
                quiz.map(this::itemCount).orElse(0),
                ceiling.name(),
                UnitFactsService.ceilingReason(facts, ceiling)));
    }

    private int itemCount(AssessmentEntity assessment) {
        return assessmentItems.findByIdAssessmentIdOrderBySort(assessment.getId()).size();
    }
}
