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
 * A generated plan, versioned rather than overwritten.
 *
 * <p>{@code reason} is not decoration. 07 requires every plan mutation to be explainable, so
 * a plan that cannot say why it exists is not a plan this system is allowed to produce.
 */
@Entity
@Table(name = "study_plans")
public class StudyPlanEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String horizon = "weekly";

    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    /** The frozen snapshot: what was known when this version was generated. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String body = "{}";

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(nullable = false)
    private String source = "diagnostic";

    @Column(name = "attempt_id")
    private UUID attemptId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getHorizon() { return horizon; }
    public void setHorizon(String horizon) { this.horizon = horizon; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
}
