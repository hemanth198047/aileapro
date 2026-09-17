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
 * The report for one attempt, stored rather than recomputed.
 *
 * <p>Stored because a report is what the learner was told. Recomputing it later would show
 * them what today's rules say about a sitting from last month, which is a different claim.
 *
 * <p>Every narrative column is NOT NULL in the schema. That is the enforcement of ASM-06:
 * a report cannot be written that is only a score.
 */
@Entity
@Table(name = "exam_reports")
public class ExamReportEntity {

    @Id
    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "template_version", nullable = false)
    private String templateVersion = "1.0.0";

    @Column(columnDefinition = "text", nullable = false)
    private String summary;

    @Column(nullable = false)
    private String readiness;

    @Column(name = "readiness_reason", columnDefinition = "text", nullable = false)
    private String readinessReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String strengths = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String weaknesses = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String misconceptions = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_prereqs", nullable = false)
    private String missingPrereqs = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String revision = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_steps", nullable = false)
    private String nextSteps = "[]";

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getReadiness() { return readiness; }
    public void setReadiness(String readiness) { this.readiness = readiness; }
    public String getReadinessReason() { return readinessReason; }
    public void setReadinessReason(String readinessReason) { this.readinessReason = readinessReason; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public String getMisconceptions() { return misconceptions; }
    public void setMisconceptions(String misconceptions) { this.misconceptions = misconceptions; }
    public String getMissingPrereqs() { return missingPrereqs; }
    public void setMissingPrereqs(String missingPrereqs) { this.missingPrereqs = missingPrereqs; }
    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }
    public String getNextSteps() { return nextSteps; }
    public void setNextSteps(String nextSteps) { this.nextSteps = nextSteps; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
