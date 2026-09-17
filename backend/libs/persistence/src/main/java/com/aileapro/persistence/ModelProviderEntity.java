package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Somewhere a model can be called from.
 *
 * <p>The API key is not here and never will be: the row names the environment variable that
 * holds it. A provider whose key is absent is simply not available, which is a configuration
 * fact rather than an error.
 */
@Entity
@Table(name = "model_providers")
public class ModelProviderEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "api_kind", nullable = false)
    private String apiKind = "openai_compatible";

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "api_key_env")
    private String apiKeyEnv;

    @Column(nullable = false)
    private String status = "enabled";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getApiKind() { return apiKind; }
    public void setApiKind(String apiKind) { this.apiKind = apiKind; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKeyEnv() { return apiKeyEnv; }
    public void setApiKeyEnv(String apiKeyEnv) { this.apiKeyEnv = apiKeyEnv; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
