package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.DueCard;
import com.aileapro.api.assessment.AssessmentViews.ReviewQueue;
import com.aileapro.api.assessment.AssessmentViews.ReviewResult;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.ReviewScheduler;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.FlashcardEntity;
import com.aileapro.persistence.ReviewAnswerEntity;
import com.aileapro.persistence.ReviewScheduleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spaced review: what is due, and what answering it does to the schedule.
 *
 * <p>Scheduling is delegated entirely to the pure {@link ReviewScheduler}. This class does
 * three things the scheduler cannot: it decides which cards a learner is entitled to see,
 * it writes the append-only answer log, and it reports retention to the mastery projection.
 *
 * <p>Only a card answered <em>on or after</em> its due date counts for retention. Answering
 * early is re-reading with extra steps, and a system that rewarded it would let someone reach
 * a retention threshold in an afternoon.
 */
@Service
@Transactional
public class ReviewService implements com.aileapro.api.learning.MasteryService.ReviewEnrolment {

    /** One sitting's worth. More than this and the queue stops being a queue and becomes a wall. */
    private static final int DAILY_CAP = 20;

    private final FlashcardRepository cards;
    private final ReviewScheduleRepository schedules;
    private final ReviewAnswerRepository answers;
    private final CurriculumQueryService curriculum;
    private final UnitFactsService unitFacts;
    private final GradedMasteryService graded;

    public ReviewService(
            FlashcardRepository cards,
            ReviewScheduleRepository schedules,
            ReviewAnswerRepository answers,
            CurriculumQueryService curriculum,
            UnitFactsService unitFacts,
            GradedMasteryService graded) {
        this.cards = cards;
        this.schedules = schedules;
        this.answers = answers;
        this.curriculum = curriculum;
        this.unitFacts = unitFacts;
        this.graded = graded;
    }

    /**
     * Enrols the authored cards for a unit the learner has started.
     *
     * <p>Called when a unit's Read chip first goes solid. Enrolling earlier would put cards
     * for material nobody has met into the queue, which is how review systems become a chore
     * people abandon.
     */
    @Override
    public int enrol(UUID userId, UUID unitId) {
        List<FlashcardEntity> authored = cards.findByUnitIdAndStatus(unitId, ContentStatus.PUBLISHED);
        if (authored.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Set<UUID> alreadyEnrolled = new HashSet<>();
        schedules.findByUserIdAndUnitIdIn(userId, List.of(unitId))
                .forEach(schedule -> alreadyEnrolled.add(schedule.getCardId()));

        List<ReviewScheduleEntity> fresh = new ArrayList<>();
        for (FlashcardEntity card : authored) {
            if (card.getUserId() != null && !card.getUserId().equals(userId)) {
                continue;
            }
            if (alreadyEnrolled.contains(card.getId())) {
                continue;
            }
            ReviewScheduleEntity schedule = new ReviewScheduleEntity();
            schedule.setId(UUID.randomUUID());
            schedule.setUserId(userId);
            schedule.setCardId(card.getId());
            schedule.setUnitId(unitId);
            // Due today: a card you have just met is worth seeing once before the intervals
            // start stretching.
            schedule.setDue(today);
            schedule.setEase(BigDecimal.valueOf(ReviewScheduler.DEFAULT_EASE));
            fresh.add(schedule);
        }
        schedules.saveAll(fresh);
        return fresh.size();
    }

    @Transactional(readOnly = true)
    public ReviewQueue due(UUID userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<ReviewScheduleEntity> dueNow =
                schedules.findByUserIdAndLeechAndDueLessThanEqualOrderByDueAsc(userId, false, today);
        int leeches = schedules.countByUserIdAndLeechAndDueLessThanEqual(userId, true, today);

        if (dueNow.isEmpty()) {
            return new ReviewQueue(0, 0, leeches, List.of(), note(0, leeches, userId));
        }

        Map<UUID, FlashcardEntity> byId = new LinkedHashMap<>();
        cards.findByIdIn(dueNow.stream().map(ReviewScheduleEntity::getCardId).toList())
                .forEach(card -> byId.put(card.getId(), card));
        Map<UUID, UnitRef> units = curriculum.publishedRefs(
                dueNow.stream().map(ReviewScheduleEntity::getUnitId).distinct().toList());

        // Most overdue first: a card three weeks late is the one actually decaying.
        List<DueCard> queue = dueNow.stream()
                .filter(schedule -> byId.containsKey(schedule.getCardId()))
                .sorted(Comparator.comparing(ReviewScheduleEntity::getDue))
                .limit(DAILY_CAP)
                .map(schedule -> new DueCard(
                        schedule.getId(),
                        schedule.getCardId(),
                        byId.get(schedule.getCardId()).getFront(),
                        units.get(schedule.getUnitId()),
                        schedule.getDue(),
                        (int) ChronoUnit.DAYS.between(schedule.getDue(), today),
                        schedule.getIntervalDays(),
                        schedule.getReps(),
                        schedule.getLapses()))
                .toList();

        int overdue = (int) queue.stream().filter(card -> card.daysOverdue() > 0).count();
        return new ReviewQueue(dueNow.size(), overdue, leeches, queue, note(dueNow.size(), leeches, userId));
    }

    /**
     * Grades one card and reschedules it.
     *
     * @param grade 0–5, the SM-2 self-report; 3 and above is a pass
     */
    public ReviewResult answer(UUID userId, UUID scheduleId, int grade) {
        if (grade < ReviewScheduler.MIN_GRADE || grade > ReviewScheduler.MAX_GRADE) {
            throw ApiException.validation("Grade must be between %d and %d"
                    .formatted(ReviewScheduler.MIN_GRADE, ReviewScheduler.MAX_GRADE));
        }
        ReviewScheduleEntity schedule = schedules.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> ApiException.notFound("No such review"));
        FlashcardEntity card = cards.findById(schedule.getCardId())
                .orElseThrow(() -> ApiException.notFound("That card no longer exists"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean wasDue = !schedule.getDue().isAfter(today);
        Volatility volatility = volatilityFor(schedule.getUnitId());

        ReviewScheduler.State before = new ReviewScheduler.State(
                schedule.getEase().doubleValue(),
                schedule.getIntervalDays(),
                schedule.getReps(),
                schedule.getLapses(),
                schedule.isLeech(),
                schedule.getDue());
        ReviewScheduler.State after = ReviewScheduler.apply(before, grade, today, volatility);
        boolean passed = ReviewScheduler.isPass(grade);

        schedule.setEase(BigDecimal.valueOf(after.ease()).setScale(2, RoundingMode.HALF_UP));
        schedule.setIntervalDays(after.intervalDays());
        schedule.setReps(after.reps());
        schedule.setLapses(after.lapses());
        schedule.setLeech(after.leech());
        schedule.setDue(after.due());
        schedule.setLastReviewedAt(Instant.now());
        schedule.setLastGrade((short) grade);
        schedule.setUpdatedAt(Instant.now());
        schedules.save(schedule);

        ReviewAnswerEntity answer = new ReviewAnswerEntity();
        answer.setId(UUID.randomUUID());
        answer.setScheduleId(schedule.getId());
        answer.setUserId(userId);
        answer.setCardId(card.getId());
        answer.setUnitId(schedule.getUnitId());
        answer.setGrade((short) grade);
        answer.setIntervalBefore(before.intervalDays());
        answer.setIntervalAfter(after.intervalDays());
        answer.setEaseAfter(schedule.getEase());
        answer.setWasDue(wasDue);
        answers.save(answer);

        graded.recordReview(
                userId,
                schedule.getUnitId(),
                unitFacts.factsFor(schedule.getUnitId()),
                volatility,
                wasDue,
                passed,
                card.getId(),
                grade);

        return new ReviewResult(
                schedule.getId(),
                card.getId(),
                card.getBack(),
                grade,
                passed,
                wasDue,
                before.intervalDays(),
                after.intervalDays(),
                after.due(),
                schedule.getEase().doubleValue(),
                after.leech(),
                resultNote(after, wasDue, passed, volatility),
                Math.max(0, due(userId).dueCount()));
    }

    /**
     * The interval, explained. A number that appears without a reason invites the learner to
     * game it; a reason invites them to trust it or argue with it.
     */
    private static String resultNote(
            ReviewScheduler.State after, boolean wasDue, boolean passed, Volatility volatility) {
        if (after.leech()) {
            return "This card has lapsed " + after.lapses() + " times, so it is out of the queue and flagged for "
                    + "reteaching. Seeing it again will not help until the underlying page has been re-read.";
        }
        if (!passed) {
            return "Back to tomorrow, and the interval multiplier came down. A failed recall resets the clock "
                    + "because the point is remembering it unaided, not seeing it again.";
        }
        String base = "Next due in " + after.intervalDays() + " day" + (after.intervalDays() == 1 ? "" : "s") + ".";
        if (!wasDue) {
            return base + " This one was not due yet, so it did not count towards retention — answering early is "
                    + "re-reading rather than recall.";
        }
        int cap = ReviewScheduler.cap(volatility);
        if (after.intervalDays() >= cap) {
            return base + " That is the ceiling for this topic: it changes fast enough that a longer gap would "
                    + "mean revising something that has since moved.";
        }
        return base;
    }

    private String note(int dueCount, int leeches, UUID userId) {
        if (dueCount == 0 && leeches == 0) {
            boolean enrolled = !schedules.findByUserId(userId).isEmpty();
            return enrolled
                    ? "Nothing is due. Cards come back on their own schedule, and answering early does not count."
                    : "No cards yet. They are enrolled when you finish reading a unit for the first time.";
        }
        if (dueCount > DAILY_CAP) {
            return "%d cards are due; the first %d are shown. Clearing a backlog in one sitting does not work, "
                    .formatted(dueCount, DAILY_CAP) + "so the rest will wait.";
        }
        if (leeches > 0) {
            return "%d card%s flagged for reteaching rather than repetition. Re-read the unit; repetition has "
                    .formatted(leeches, leeches == 1 ? " is" : "s are")
                    + "already failed on those.";
        }
        return "";
    }

    /**
     * Volatility caps how far an interval can stretch, so it is worth the lookup here in a way
     * it is not in the score: a fast-moving topic scheduled a year out means revising something
     * that has since changed.
     */
    private Volatility volatilityFor(UUID unitId) {
        UnitRef unit = curriculum.publishedRefs(List.of(unitId)).get(unitId);
        if (unit == null || unit.volatility() == null) {
            return Volatility.MEDIUM;
        }
        return Volatility.valueOf(unit.volatility());
    }
}
