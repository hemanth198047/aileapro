package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Snapshot of a unit at publish/rollback time. Enough to restore title, status,
 * version, and blocks without re-seeding from YAML.
 */
@Entity
@Table(name = "curriculum_changes")
public class CurriculumChangeEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "from_version", length = 32)
    private String fromVersion;

    @Column(name = "to_version", nullable = false, length = 32)
    private String toVersion;

    @Column(name = "from_status", length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 24)
    private String toStatus;

    @Column(columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getFromVersion() { return fromVersion; }
    public void setFromVersion(String fromVersion) { this.fromVersion = fromVersion; }
    public String getToVersion() { return toVersion; }
    public void setToVersion(String toVersion) { this.toVersion = toVersion; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
