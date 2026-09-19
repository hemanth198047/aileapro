package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "feature_flags")
public class FeatureFlagEntity {

    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
