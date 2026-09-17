package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A named misconception, derived from the {@code misconception} block that teaches against
 * it. The catalog exists so a flag can always be turned back into a page: a code with no
 * reteach block behind it is a label, not a diagnosis.
 */
@Entity
@Table(name = "misconception_catalog")
public class MisconceptionCatalogEntity {

    @Id
    private String code;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(columnDefinition = "text", nullable = false)
    private String claim;

    @Column(name = "reteach_block_id")
    private UUID reteachBlockId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getClaim() { return claim; }
    public void setClaim(String claim) { this.claim = claim; }
    public UUID getReteachBlockId() { return reteachBlockId; }
    public void setReteachBlockId(UUID reteachBlockId) { this.reteachBlockId = reteachBlockId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
