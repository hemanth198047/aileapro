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
 * One turn in a conversation, append-only.
 *
 * <p>Three fields exist to stop the transcript from flattering itself. {@code development} is
 * true when a fixture answered, so a development reply can never be displayed as teaching.
 * {@code retrievalIds} is empty until retrieval exists, so an ungrounded answer cannot appear
 * to have sources. {@code refusalCode} records a decline as a decision rather than a failure.
 */
@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private String role;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    /** The button the learner pressed, if they pressed one instead of typing. */
    private String action;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "retrieval_ids", nullable = false)
    private UUID[] retrievalIds = new UUID[0];

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(nullable = false)
    private boolean development = false;

    @Column(name = "refusal_code")
    private String refusalCode;

    /** Which rung of the reveal ladder this answer was allowed: HINT, GUIDED, FULL. */
    private String reveal;

    @Column(nullable = false)
    private int sort;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public UUID[] getRetrievalIds() { return retrievalIds; }
    public void setRetrievalIds(UUID[] retrievalIds) { this.retrievalIds = retrievalIds; }
    public UUID getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(UUID promptVersionId) { this.promptVersionId = promptVersionId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public boolean isDevelopment() { return development; }
    public void setDevelopment(boolean development) { this.development = development; }
    public String getRefusalCode() { return refusalCode; }
    public void setRefusalCode(String refusalCode) { this.refusalCode = refusalCode; }
    public String getReveal() { return reveal; }
    public void setReveal(String reveal) { this.reveal = reveal; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
