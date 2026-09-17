package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A named false belief currently attributed to a learner.
 *
 * <p>Cleared flags are kept rather than deleted. That someone once held a belief is useful
 * for teaching them, and a record that disappears cannot be questioned.
 */
@Entity
@Table(name = "misconception_flags")
public class MisconceptionFlagEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String code;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "open", nullable = false)
    private boolean open = true;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt = Instant.now();

    @Column(name = "cleared_at")
    private Instant clearedAt;

    @Column(name = "times_raised", nullable = false)
    private int timesRaised = 1;

    @Column(nullable = false)
    private String source = "item";

    @Column(name = "last_attempt_id")
    private UUID lastAttemptId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public Instant getRaisedAt() { return raisedAt; }
    public void setRaisedAt(Instant raisedAt) { this.raisedAt = raisedAt; }
    public Instant getClearedAt() { return clearedAt; }
    public void setClearedAt(Instant clearedAt) { this.clearedAt = clearedAt; }
    public int getTimesRaised() { return timesRaised; }
    public void setTimesRaised(int timesRaised) { this.timesRaised = timesRaised; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public UUID getLastAttemptId() { return lastAttemptId; }
    public void setLastAttemptId(UUID lastAttemptId) { this.lastAttemptId = lastAttemptId; }
}
