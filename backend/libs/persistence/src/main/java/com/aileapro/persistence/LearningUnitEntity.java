package com.aileapro.persistence;

import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.KnowledgeClass;
import com.aileapro.domain.UnitKind;
import com.aileapro.domain.Volatility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_units")
public class LearningUnitEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String locale = "en";

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "unit_kind", nullable = false)
    private UnitKind unitKind;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private int depth;

    // Postgres text[]: kept as a real array so tags stay indexable with GIN. The column type
    // is declared in the migration, not here, so tests on H2 can generate their own DDL.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private String[] objectives = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private String[] tags = new String[0];

    private Short difficulty;

    @Column(name = "estimate_minutes")
    private Integer estimateMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Volatility volatility = Volatility.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "knowledge_class", nullable = false)
    private KnowledgeClass knowledgeClass = KnowledgeClass.STABLE;

    @Column(nullable = false)
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(nullable = false)
    private String version = "0.1.0";

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private String freshness;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UnitKind getUnitKind() { return unitKind; }
    public void setUnitKind(UnitKind unitKind) { this.unitKind = unitKind; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    public String[] getObjectives() { return objectives; }
    public void setObjectives(String[] objectives) { this.objectives = objectives; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public Short getDifficulty() { return difficulty; }
    public void setDifficulty(Short difficulty) { this.difficulty = difficulty; }
    public Integer getEstimateMinutes() { return estimateMinutes; }
    public void setEstimateMinutes(Integer estimateMinutes) { this.estimateMinutes = estimateMinutes; }
    public Volatility getVolatility() { return volatility; }
    public void setVolatility(Volatility volatility) { this.volatility = volatility; }
    public KnowledgeClass getKnowledgeClass() { return knowledgeClass; }
    public void setKnowledgeClass(KnowledgeClass knowledgeClass) { this.knowledgeClass = knowledgeClass; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }
    public Instant getSupersededAt() { return supersededAt; }
    public void setSupersededAt(Instant supersededAt) { this.supersededAt = supersededAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public String getFreshness() { return freshness; }
    public void setFreshness(String freshness) { this.freshness = freshness; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
