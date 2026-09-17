package com.aileapro.persistence;

import com.aileapro.domain.AssessmentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One sitting of one assessment.
 *
 * <p>An attempt is history, so the kind and version are copied from the assessment when it
 * starts. If the assessment is later rewritten or retired, what this attempt was must not
 * change underneath the report that cites it.
 */
@Entity
@Table(name = "attempts")
public class AttemptEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(nullable = false)
    private AssessmentKind kind;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "assessment_version", nullable = false)
    private String assessmentVersion = "1.0.0";

    @Column(nullable = false)
    private String status = "in_progress";

    @Column(nullable = false)
    private boolean timed;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    private Boolean passed;

    @Column(name = "stop_reason")
    private String stopReason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getAssessmentId() { return assessmentId; }
    public void setAssessmentId(UUID assessmentId) { this.assessmentId = assessmentId; }
    public AssessmentKind getKind() { return kind; }
    public void setKind(AssessmentKind kind) { this.kind = kind; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getAssessmentVersion() { return assessmentVersion; }
    public void setAssessmentVersion(String assessmentVersion) { this.assessmentVersion = assessmentVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isTimed() { return timed; }
    public void setTimed(boolean timed) { this.timed = timed; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }
}
