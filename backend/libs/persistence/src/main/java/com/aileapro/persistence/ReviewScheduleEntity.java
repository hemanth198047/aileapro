package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The SM-2 state for one learner and one card. A projection of {@code review_answers}. */
@Entity
@Table(name = "review_schedules")
public class ReviewScheduleEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    /** A date, not a timestamp: a review is due on a day, not at a minute. */
    @Column(nullable = false)
    private LocalDate due;

    @Column(nullable = false)
    private BigDecimal ease = new BigDecimal("2.50");

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private int lapses;

    /** Flagged for reteaching rather than shown again forever. */
    @Column(nullable = false)
    private boolean leech;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "last_grade")
    private Short lastGrade;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public LocalDate getDue() { return due; }
    public void setDue(LocalDate due) { this.due = due; }
    public BigDecimal getEase() { return ease; }
    public void setEase(BigDecimal ease) { this.ease = ease; }
    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public int getLapses() { return lapses; }
    public void setLapses(int lapses) { this.lapses = lapses; }
    public boolean isLeech() { return leech; }
    public void setLeech(boolean leech) { this.leech = leech; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public Short getLastGrade() { return lastGrade; }
    public void setLastGrade(Short lastGrade) { this.lastGrade = lastGrade; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
