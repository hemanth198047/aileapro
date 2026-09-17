package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One graded review, append-only.
 *
 * <p>{@code wasDue} is the column that makes retention measurable: a card answered before
 * its due date says nothing about remembering, so the distinction has to be recorded at the
 * time rather than inferred later from a schedule that has since moved on.
 */
@Entity
@Table(name = "review_answers")
public class ReviewAnswerEntity {

    @Id
    private UUID id;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private short grade;

    @Column(name = "interval_before", nullable = false)
    private int intervalBefore;

    @Column(name = "interval_after", nullable = false)
    private int intervalAfter;

    @Column(name = "ease_after", nullable = false)
    private BigDecimal easeAfter;

    @Column(name = "was_due", nullable = false)
    private boolean wasDue;

    @Column(nullable = false)
    private Instant at = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getScheduleId() { return scheduleId; }
    public void setScheduleId(UUID scheduleId) { this.scheduleId = scheduleId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public short getGrade() { return grade; }
    public void setGrade(short grade) { this.grade = grade; }
    public int getIntervalBefore() { return intervalBefore; }
    public void setIntervalBefore(int intervalBefore) { this.intervalBefore = intervalBefore; }
    public int getIntervalAfter() { return intervalAfter; }
    public void setIntervalAfter(int intervalAfter) { this.intervalAfter = intervalAfter; }
    public BigDecimal getEaseAfter() { return easeAfter; }
    public void setEaseAfter(BigDecimal easeAfter) { this.easeAfter = easeAfter; }
    public boolean isWasDue() { return wasDue; }
    public void setWasDue(boolean wasDue) { this.wasDue = wasDue; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
