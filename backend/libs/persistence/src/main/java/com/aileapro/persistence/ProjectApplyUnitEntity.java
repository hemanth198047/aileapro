package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Units whose Apply chip becomes solid when this project is completed.
 */
@Entity
@Table(name = "project_apply_units")
@IdClass(ProjectApplyUnitEntity.Pk.class)
public class ProjectApplyUnitEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }

    public static final class Pk implements Serializable {
        private UUID projectId;
        private UUID unitId;

        public Pk() {
        }

        public Pk(UUID projectId, UUID unitId) {
            this.projectId = projectId;
            this.unitId = unitId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk pk)) {
                return false;
            }
            return Objects.equals(projectId, pk.projectId) && Objects.equals(unitId, pk.unitId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectId, unitId);
        }
    }
}
