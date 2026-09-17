package com.aileapro.persistence;

import com.aileapro.domain.LearningEventKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One observed thing, append-only. Rows are never updated or reinterpreted in place: if the
 * rules change, the projection is rebuilt from these.
 */
@Entity
@Table(name = "mastery_events")
public class MasteryEventEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private LearningEventKind kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload = "{}";

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public LearningEventKind getKind() { return kind; }
    public void setKind(LearningEventKind kind) { this.kind = kind; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
