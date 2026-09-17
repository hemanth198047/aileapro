package com.aileapro.persistence;

import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.EdgeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "concept_edges")
public class ConceptEdgeEntity {
    @Id
    private UUID id;

    @Column(name = "from_id", nullable = false)
    private UUID fromId;

    @Column(name = "to_id", nullable = false)
    private UUID toId;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type", nullable = false)
    private EdgeType edgeType;

    /** How strongly the edge holds, 0 to 1. Used to rank a crowded related panel. */
    @Column(nullable = false)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    /** Why the author drew this edge, shown beside it in the map. */
    @Column(columnDefinition = "text")
    private String note;

    /** Sources backing the claim, so an edge can be argued with rather than trusted. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "evidence_source_ids", nullable = false)
    private UUID[] evidenceSourceIds = new UUID[0];

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getFromId() { return fromId; }
    public void setFromId(UUID fromId) { this.fromId = fromId; }
    public UUID getToId() { return toId; }
    public void setToId(UUID toId) { this.toId = toId; }
    public EdgeType getEdgeType() { return edgeType; }
    public void setEdgeType(EdgeType edgeType) { this.edgeType = edgeType; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public UUID[] getEvidenceSourceIds() { return evidenceSourceIds; }
    public void setEvidenceSourceIds(UUID[] evidenceSourceIds) { this.evidenceSourceIds = evidenceSourceIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
