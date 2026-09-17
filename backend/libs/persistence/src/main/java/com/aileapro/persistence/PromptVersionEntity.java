package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One version of a prompt, with the hash of its text.
 *
 * <p>Every usage row points at one of these. That is the only thing that makes a regression
 * investigable later: without it, "the tutor got worse last week" cannot be tied to the edit
 * that caused it, because the text will have moved on.
 */
@Entity
@Table(name = "prompt_versions")
public class PromptVersionEntity {

    @Id
    private UUID id;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "text", nullable = false)
    private String template;

    /** SHA-256 of the template. A different text is a different version, never an edit. */
    @Column(nullable = false)
    private String hash;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPromptId() { return promptId; }
    public void setPromptId(UUID promptId) { this.promptId = promptId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
