package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Which models can serve a profile, in the order they are tried.
 *
 * <p>The order is the fallback chain. A provider outage should cost the learner a slightly
 * worse answer, not the feature.
 */
@Entity
@Table(name = "model_profile_models")
public class ModelProfileModelEntity {

    @Embeddable
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        @Column(name = "profile_id", nullable = false)
        private UUID profileId;

        @Column(name = "model_id", nullable = false)
        private UUID modelId;

        public Key() {
        }

        public Key(UUID profileId, UUID modelId) {
            this.profileId = profileId;
            this.modelId = modelId;
        }

        public UUID getProfileId() { return profileId; }
        public void setProfileId(UUID profileId) { this.profileId = profileId; }
        public UUID getModelId() { return modelId; }
        public void setModelId(UUID modelId) { this.modelId = modelId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(profileId, key.profileId) && Objects.equals(modelId, key.modelId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileId, modelId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false)
    private int sort;

    public ModelProfileModelEntity() {
    }

    public ModelProfileModelEntity(UUID profileId, UUID modelId, int sort) {
        this.id = new Key(profileId, modelId);
        this.sort = sort;
    }

    public Key getId() { return id; }
    public void setId(Key id) { this.id = id; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
}
