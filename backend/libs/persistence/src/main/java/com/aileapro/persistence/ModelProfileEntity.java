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
 * A task class a feature can ask for: {@code teach}, {@code cheap_chat}, {@code code}.
 *
 * <p>This is the indirection that keeps the product vendor-neutral. A feature says what kind
 * of thinking it needs; the profile decides which model does it. Nothing above this layer is
 * allowed to know a model name.
 */
@Entity
@Table(name = "model_profiles")
public class ModelProfileEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String purpose;

    /** Temperature and friends. JSONB because every provider spells them differently. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String params = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
