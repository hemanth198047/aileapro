package com.aileapro.persistence;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A named selection of items: a practice set, a quiz, an exam, or the diagnostic. */
@Entity
@Table(name = "assessments")
public class AssessmentEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private AssessmentKind kind;

    /** Null only for the diagnostic, which spans the curriculum. */
    @Column(name = "unit_id")
    private UUID unitId;

    @Column(columnDefinition = "text")
    private String description;

    /** Selection and stop rules for assessments whose items are chosen per attempt. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String blueprint = "{}";

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "pass_mark", nullable = false)
    private BigDecimal passMark = new BigDecimal("0.70");

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
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AssessmentKind getKind() { return kind; }
    public void setKind(AssessmentKind kind) { this.kind = kind; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBlueprint() { return blueprint; }
    public void setBlueprint(String blueprint) { this.blueprint = blueprint; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public BigDecimal getPassMark() { return passMark; }
    public void setPassMark(BigDecimal passMark) { this.passMark = passMark; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
