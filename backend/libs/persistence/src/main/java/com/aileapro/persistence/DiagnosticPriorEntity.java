package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * What the diagnostic guessed about one domain.
 *
 * <p>A prior, kept per domain rather than per unit and kept with its confidence, because two
 * items cannot say anything trustworthy about a single page. It decides where the learner
 * starts; it is never evidence of what they know.
 */
@Entity
@Table(name = "diagnostic_priors")
public class DiagnosticPriorEntity {

    @Id
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "domain_unit_id", nullable = false)
    private UUID domainUnitId;

    @Column(name = "items_served", nullable = false)
    private int itemsServed;

    @Column(name = "items_correct", nullable = false)
    private int itemsCorrect;

    @Column(nullable = false)
    private BigDecimal estimate = BigDecimal.ZERO;

    /** How wide the guess is, so thin evidence stays visibly thin. */
    @Column(nullable = false)
    private String confidence = "low";

    @Column(nullable = false)
    private Instant at = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAttemptId() { return attemptId; }
    public void setAttemptId(UUID attemptId) { this.attemptId = attemptId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getDomainUnitId() { return domainUnitId; }
    public void setDomainUnitId(UUID domainUnitId) { this.domainUnitId = domainUnitId; }
    public int getItemsServed() { return itemsServed; }
    public void setItemsServed(int itemsServed) { this.itemsServed = itemsServed; }
    public int getItemsCorrect() { return itemsCorrect; }
    public void setItemsCorrect(int itemsCorrect) { this.itemsCorrect = itemsCorrect; }
    public BigDecimal getEstimate() { return estimate; }
    public void setEstimate(BigDecimal estimate) { this.estimate = estimate; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
