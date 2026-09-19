package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_link_checks")
public class SourceLinkCheckEntity {

    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt = Instant.now();

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(nullable = false)
    private boolean ok;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
}
