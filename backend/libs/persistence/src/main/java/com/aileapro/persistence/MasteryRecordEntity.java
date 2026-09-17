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
}
