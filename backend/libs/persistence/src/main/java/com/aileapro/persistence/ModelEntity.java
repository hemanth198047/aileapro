package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One model, with the prices it is billed at.
 *
 * <p>Prices live here rather than in code so that changing one is an operator action, and so
 * that a usage row written last month keeps the cost it was actually charged instead of being
 * silently restated by today's price list.
 */
@Entity
@Table(name = "models")
public class ModelEntity {

    @Id
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private String slug;

    /** What the provider calls it on the wire, which is not always what we call it. */
    @Column(name = "wire_name", nullable = false)
    private String wireName;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private String[] capabilities = {"chat"};

    @Column(name = "context_tokens", nullable = false)
    private int contextTokens = 8192;

    @Column(name = "max_output_tokens", nullable = false)
    private int maxOutputTokens = 1024;

    @Column(name = "price_in_per_mtok", nullable = false)
    private BigDecimal priceInPerMtok = BigDecimal.ZERO;

    @Column(name = "price_out_per_mtok", nullable = false)
    private BigDecimal priceOutPerMtok = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "enabled";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getWireName() { return wireName; }
    public void setWireName(String wireName) { this.wireName = wireName; }
    public String[] getCapabilities() { return capabilities; }
    public void setCapabilities(String[] capabilities) { this.capabilities = capabilities; }
    public int getContextTokens() { return contextTokens; }
    public void setContextTokens(int contextTokens) { this.contextTokens = contextTokens; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public BigDecimal getPriceInPerMtok() { return priceInPerMtok; }
    public void setPriceInPerMtok(BigDecimal priceInPerMtok) { this.priceInPerMtok = priceInPerMtok; }
    public BigDecimal getPriceOutPerMtok() { return priceOutPerMtok; }
    public void setPriceOutPerMtok(BigDecimal priceOutPerMtok) { this.priceOutPerMtok = priceOutPerMtok; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
