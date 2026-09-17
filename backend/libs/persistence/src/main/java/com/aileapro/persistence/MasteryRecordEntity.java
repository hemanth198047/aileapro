package com.aileapro.persistence;

import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.MasteryState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The projection of one learner's evidence on one unit.
 *
 * <p>Derived from {@code mastery_events} and safe to rebuild from them. Nothing here is the
 * source of truth, which is what lets the scoring rules change without losing history.
 */
@Entity
@Table(name = "mastery_records")
public class MasteryRecordEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MasteryState state = MasteryState.UNKNOWN;

    /** Internal ranking score, never the headline number in the UI. */
    @Column(nullable = false)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "read_state", nullable = false)
    private EvidenceState readState = EvidenceState.EMPTY;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "practice_state", nullable = false)
    private EvidenceState practiceState = EvidenceState.EMPTY;

    @Column(name = "practice_at")
    private Instant practiceAt;

    @Column(name = "assess_state", nullable = false)
    private EvidenceState assessState = EvidenceState.EMPTY;

    @Column(name = "assess_at")
    private Instant assessAt;

    @Column(name = "apply_state", nullable = false)
    private EvidenceState applyState = EvidenceState.EMPTY;

    @Column(name = "apply_at")
    private Instant applyAt;

    @Column(name = "dwell_seconds", nullable = false)
    private int dwellSeconds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "levels_seen", nullable = false)
    private String[] levelsSeen = new String[0];

    @Column(name = "opened_count", nullable = false)
    private int openedCount;

    @Column(name = "first_opened_at")
    private Instant firstOpenedAt;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    // Graded inputs. Same reasoning as the reading columns above: carried on the projection
    // so a read path never replays the event log.

    @Column(name = "practice_items_correct", nullable = false)
    private int practiceItemsCorrect;

    @Column(name = "practice_items_answered", nullable = false)
    private int practiceItemsAnswered;

    @Column(name = "assess_items_correct", nullable = false)
    private int assessItemsCorrect;

    @Column(name = "assess_items_answered", nullable = false)
    private int assessItemsAnswered;

    @Column(name = "assess_best_fraction")
    private BigDecimal assessBestFraction;

    @Column(name = "assessments_passed", nullable = false)
    private int assessmentsPassed;

    /** Correct answers on items above recall level, which is the UNDERSTOOD gate. */
    @Column(name = "understand_items_correct", nullable = false)
    private int understandItemsCorrect;

    @Column(name = "accuracy_ema")
    private BigDecimal accuracyEma;

    @Column(name = "reviews_due_passed", nullable = false)
    private int reviewsDuePassed;

    @Column(name = "reviews_due_failed", nullable = false)
    private int reviewsDueFailed;

    /** Distinct days with graded evidence, which is how consistency is measured. */
    @Column(name = "graded_days", nullable = false)
    private int gradedDays;

    @Column(name = "last_graded_on")
    private LocalDate lastGradedOn;

    @Column(name = "open_misconceptions", nullable = false)
    private int openMisconceptions;

    @Column(name = "last_graded_at")
    private Instant lastGradedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public MasteryState getState() { return state; }
    public void setState(MasteryState state) { this.state = state; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public EvidenceState getReadState() { return readState; }
    public void setReadState(EvidenceState readState) { this.readState = readState; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public EvidenceState getPracticeState() { return practiceState; }
    public void setPracticeState(EvidenceState practiceState) { this.practiceState = practiceState; }
    public Instant getPracticeAt() { return practiceAt; }
    public void setPracticeAt(Instant practiceAt) { this.practiceAt = practiceAt; }
    public EvidenceState getAssessState() { return assessState; }
    public void setAssessState(EvidenceState assessState) { this.assessState = assessState; }
    public Instant getAssessAt() { return assessAt; }
    public void setAssessAt(Instant assessAt) { this.assessAt = assessAt; }
    public EvidenceState getApplyState() { return applyState; }
    public void setApplyState(EvidenceState applyState) { this.applyState = applyState; }
    public Instant getApplyAt() { return applyAt; }
    public void setApplyAt(Instant applyAt) { this.applyAt = applyAt; }
    public int getDwellSeconds() { return dwellSeconds; }
    public void setDwellSeconds(int dwellSeconds) { this.dwellSeconds = dwellSeconds; }
    public String[] getLevelsSeen() { return levelsSeen; }
    public void setLevelsSeen(String[] levelsSeen) { this.levelsSeen = levelsSeen; }
    public int getOpenedCount() { return openedCount; }
    public void setOpenedCount(int openedCount) { this.openedCount = openedCount; }
    public Instant getFirstOpenedAt() { return firstOpenedAt; }
    public void setFirstOpenedAt(Instant firstOpenedAt) { this.firstOpenedAt = firstOpenedAt; }
    public Instant getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getPracticeItemsCorrect() { return practiceItemsCorrect; }
    public void setPracticeItemsCorrect(int practiceItemsCorrect) { this.practiceItemsCorrect = practiceItemsCorrect; }
    public int getPracticeItemsAnswered() { return practiceItemsAnswered; }
    public void setPracticeItemsAnswered(int practiceItemsAnswered) { this.practiceItemsAnswered = practiceItemsAnswered; }
    public int getAssessItemsCorrect() { return assessItemsCorrect; }
    public void setAssessItemsCorrect(int assessItemsCorrect) { this.assessItemsCorrect = assessItemsCorrect; }
    public int getAssessItemsAnswered() { return assessItemsAnswered; }
    public void setAssessItemsAnswered(int assessItemsAnswered) { this.assessItemsAnswered = assessItemsAnswered; }
    public BigDecimal getAssessBestFraction() { return assessBestFraction; }
    public void setAssessBestFraction(BigDecimal assessBestFraction) { this.assessBestFraction = assessBestFraction; }
    public int getAssessmentsPassed() { return assessmentsPassed; }
    public void setAssessmentsPassed(int assessmentsPassed) { this.assessmentsPassed = assessmentsPassed; }
    public int getUnderstandItemsCorrect() { return understandItemsCorrect; }
    public void setUnderstandItemsCorrect(int understandItemsCorrect) { this.understandItemsCorrect = understandItemsCorrect; }
    public BigDecimal getAccuracyEma() { return accuracyEma; }
    public void setAccuracyEma(BigDecimal accuracyEma) { this.accuracyEma = accuracyEma; }
    public int getReviewsDuePassed() { return reviewsDuePassed; }
    public void setReviewsDuePassed(int reviewsDuePassed) { this.reviewsDuePassed = reviewsDuePassed; }
    public int getReviewsDueFailed() { return reviewsDueFailed; }
    public void setReviewsDueFailed(int reviewsDueFailed) { this.reviewsDueFailed = reviewsDueFailed; }
    public int getGradedDays() { return gradedDays; }
    public void setGradedDays(int gradedDays) { this.gradedDays = gradedDays; }
    public LocalDate getLastGradedOn() { return lastGradedOn; }
    public void setLastGradedOn(LocalDate lastGradedOn) { this.lastGradedOn = lastGradedOn; }
    public int getOpenMisconceptions() { return openMisconceptions; }
    public void setOpenMisconceptions(int openMisconceptions) { this.openMisconceptions = openMisconceptions; }
    public Instant getLastGradedAt() { return lastGradedAt; }
    public void setLastGradedAt(Instant lastGradedAt) { this.lastGradedAt = lastGradedAt; }
}
