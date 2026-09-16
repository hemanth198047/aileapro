package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A source made usable for teaching: difficulty and default relevance on top of provenance. */
@Entity
@Table(name = "learning_resources")
public class LearningResourceEntity {
    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    private Short difficulty;

    @Column(name = "relevance_default", nullable = false)
    private BigDecimal relevanceDefault = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public Short getDifficulty() { return difficulty; }
    public void setDifficulty(Short difficulty) { this.difficulty = difficulty; }
    public BigDecimal getRelevanceDefault() { return relevanceDefault; }
    public void setRelevanceDefault(BigDecimal relevanceDefault) { this.relevanceDefault = relevanceDefault; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
