package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "curriculum_change_proposals")
public class CurriculumChangeProposalEntity {

    @Id
    private UUID id;

    @Column(name = "update_id", nullable = false)
    private UUID updateId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 512)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", nullable = false)
    private String diffJson = "{}";

    @Column(name = "impact_score", nullable = false)
    private BigDecimal impactScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impact_reasons", nullable = false)
    private String impactReasons = "[]";

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUpdateId() { return updateId; }
    public void setUpdateId(UUID updateId) { this.updateId = updateId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }
    public BigDecimal getImpactScore() { return impactScore; }
    public void setImpactScore(BigDecimal impactScore) { this.impactScore = impactScore; }
    public String getImpactReasons() { return impactReasons; }
    public void setImpactReasons(String impactReasons) { this.impactReasons = impactReasons; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
