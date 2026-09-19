package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_updates")
public class AiUpdateEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String slug;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 32)
    private String lifecycle;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private BigDecimal confidence = new BigDecimal("0.500");

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_source_count", nullable = false)
    private int verifiedSourceCount;

    @Column(name = "currentness_note", nullable = false, columnDefinition = "text")
    private String currentnessNote;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getLifecycle() { return lifecycle; }
    public void setLifecycle(String lifecycle) { this.lifecycle = lifecycle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public int getVerifiedSourceCount() { return verifiedSourceCount; }
    public void setVerifiedSourceCount(int verifiedSourceCount) { this.verifiedSourceCount = verifiedSourceCount; }
    public String getCurrentnessNote() { return currentnessNote; }
    public void setCurrentnessNote(String currentnessNote) { this.currentnessNote = currentnessNote; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
