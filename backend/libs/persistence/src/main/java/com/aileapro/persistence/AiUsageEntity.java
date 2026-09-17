package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One metered call.
 *
 * <p>Written for failures as well as successes. An outage that costs nothing still has to be
 * visible, or the only thing the cost table proves is that we stopped asking.
 *
 * <p>Provider, model, and profile are denormalised as slugs on purpose: a usage row is a
 * historical record, and it must still read correctly after a model is deleted from the
 * catalogue.
 */
@Entity
@Table(name = "ai_usage")
public class AiUsageEntity {

    @Id
    private UUID id;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String feature;

    @Column(name = "profile_slug", nullable = false)
    private String profileSlug;

    @Column(name = "provider_slug", nullable = false)
    private String providerSlug;

    @Column(name = "model_slug", nullable = false)
    private String modelSlug;

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "tokens_in", nullable = false)
    private int tokensIn;

    @Column(name = "tokens_out", nullable = false)
    private int tokensOut;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    /** Estimated, and named so. Invoice reconciliation is an operator job, not this row. */
    @Column(name = "cost_usd", nullable = false)
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(nullable = false)
    private String outcome = "ok";

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }
    public String getProfileSlug() { return profileSlug; }
    public void setProfileSlug(String profileSlug) { this.profileSlug = profileSlug; }
    public String getProviderSlug() { return providerSlug; }
    public void setProviderSlug(String providerSlug) { this.providerSlug = providerSlug; }
    public String getModelSlug() { return modelSlug; }
    public void setModelSlug(String modelSlug) { this.modelSlug = modelSlug; }
    public UUID getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(UUID promptVersionId) { this.promptVersionId = promptVersionId; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public int getTokensIn() { return tokensIn; }
    public void setTokensIn(int tokensIn) { this.tokensIn = tokensIn; }
    public int getTokensOut() { return tokensOut; }
    public void setTokensOut(int tokensOut) { this.tokensOut = tokensOut; }
    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
