package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Containment closure. Phase 4 adds the typed edges; this is PART_OF only. */
@Entity
@Table(name = "unit_closure")
public class UnitClosureEntity {

    @Embeddable
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "ancestor_id", nullable = false)
        private UUID ancestorId;

        @Column(name = "descendant_id", nullable = false)
        private UUID descendantId;

        public Key() {
        }

        public Key(UUID ancestorId, UUID descendantId) {
            this.ancestorId = ancestorId;
            this.descendantId = descendantId;
        }

        public UUID getAncestorId() { return ancestorId; }
        public void setAncestorId(UUID ancestorId) { this.ancestorId = ancestorId; }
        public UUID getDescendantId() { return descendantId; }
        public void setDescendantId(UUID descendantId) { this.descendantId = descendantId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(ancestorId, key.ancestorId)
                    && Objects.equals(descendantId, key.descendantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ancestorId, descendantId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int depth;

    public UnitClosureEntity() {
    }

    public UnitClosureEntity(UUID ancestorId, UUID descendantId, int depth) {
        this.id = new Key(ancestorId, descendantId);
        this.depth = depth;
    }

    public Key getId() { return id; }
    public void setId(Key id) { this.id = id; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
}
