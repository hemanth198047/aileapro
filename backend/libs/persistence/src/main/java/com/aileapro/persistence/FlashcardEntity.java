package com.aileapro.persistence;

import com.aileapro.domain.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A review card. Authored cards are shared and have no owner; generated ones belong to the
 * learner whose wrong answer produced them.
 *
 * <p>{@code locked} marks an author's golden card, which generation may not replace or
 * retire. The authors get the last word on what is worth remembering.
 */
@Entity
@Table(name = "flashcards")
public class FlashcardEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    /** Null for an authored card shared by everyone. */
    @Column(name = "user_id")
    private UUID userId;

    private String slug;

    @Column(columnDefinition = "text", nullable = false)
    private String front;

    @Column(columnDefinition = "text", nullable = false)
    private String back;

    @Column(nullable = false)
    private String origin = "author";

    @Column(nullable = false)
    private boolean locked;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getFront() { return front; }
    public void setFront(String front) { this.front = front; }
    public String getBack() { return back; }
    public void setBack(String back) { this.back = back; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
