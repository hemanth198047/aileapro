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
 * What the learner says they want. Goals and self-reported experience shape recommendations
 * and nothing else — none of it is evidence of competence, and none of it touches mastery.
 */
@Entity
@Table(name = "learner_profiles")
public class LearnerProfileEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "target_path_id")
    private UUID targetPathId;

    @Column(name = "hours_per_week")
    private Short hoursPerWeek;

    /** Preferred lenses as a JSON array of codes: visual, code, math, analogy. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String modalities = "[]";

    private String experience;

    @Column(columnDefinition = "text")
    private String goals;

    @Column(name = "onboarded_at")
    private Instant onboardedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getTargetPathId() { return targetPathId; }
    public void setTargetPathId(UUID targetPathId) { this.targetPathId = targetPathId; }
    public Short getHoursPerWeek() { return hoursPerWeek; }
    public void setHoursPerWeek(Short hoursPerWeek) { this.hoursPerWeek = hoursPerWeek; }
    public String getModalities() { return modalities; }
    public void setModalities(String modalities) { this.modalities = modalities; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }
    public Instant getOnboardedAt() { return onboardedAt; }
    public void setOnboardedAt(Instant onboardedAt) { this.onboardedAt = onboardedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
