package com.aileapro.persistence;

import com.aileapro.domain.BloomLevel;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One authored assessment item.
 *
 * <p>It looks like the rest of the curriculum on purpose: a slug, a status, a version, and
 * an owning unit. An item is content, and content that cannot be traced to a page cannot be
 * revised when the page changes.
 */
@Entity
@Table(name = "question_bank")
public class QuestionEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private String slug;

    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private short difficulty = 2;

    @Column(nullable = false)
    private BloomLevel bloom = BloomLevel.UNDERSTAND;

    @Column(columnDefinition = "text", nullable = false)
    private String stem;

    /** Choices and answer key, shaped by the item type. Validated by the seeder. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String data;

    /** Shown after answering, right or wrong. An item without teaching is a gate, not a lesson. */
    @Column(columnDefinition = "text", nullable = false)
    private String explanation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "misconception_codes", nullable = false)
    private String[] misconceptionCodes = new String[0];

    @Column(name = "estimate_seconds")
    private Short estimateSeconds;

    @Column(nullable = false)
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(nullable = false)
    private String version = "1.0.0";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public short getDifficulty() { return difficulty; }
    public void setDifficulty(short difficulty) { this.difficulty = difficulty; }
    public BloomLevel getBloom() { return bloom; }
    public void setBloom(BloomLevel bloom) { this.bloom = bloom; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String[] getMisconceptionCodes() { return misconceptionCodes; }
    public void setMisconceptionCodes(String[] misconceptionCodes) { this.misconceptionCodes = misconceptionCodes; }
    public Short getEstimateSeconds() { return estimateSeconds; }
    public void setEstimateSeconds(Short estimateSeconds) { this.estimateSeconds = estimateSeconds; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
