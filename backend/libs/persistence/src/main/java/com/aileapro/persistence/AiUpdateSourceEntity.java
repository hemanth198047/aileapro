package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ai_update_sources")
@IdClass(AiUpdateSourceEntity.PK.class)
public class AiUpdateSourceEntity {

    @Id
    @Column(name = "update_id")
    private UUID updateId;

    @Id
    @Column(name = "source_id")
    private UUID sourceId;

    public UUID getUpdateId() { return updateId; }
    public void setUpdateId(UUID updateId) { this.updateId = updateId; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

    public static class PK implements Serializable {
        private UUID updateId;
        private UUID sourceId;

        public UUID getUpdateId() { return updateId; }
        public void setUpdateId(UUID updateId) { this.updateId = updateId; }
        public UUID getSourceId() { return sourceId; }
        public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(updateId, pk.updateId) && Objects.equals(sourceId, pk.sourceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(updateId, sourceId);
        }
    }
}
