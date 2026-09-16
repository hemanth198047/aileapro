package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "path_waypoints")
public class PathWaypointEntity {
    @Id
    private UUID id;

    @Column(name = "path_id", nullable = false)
    private UUID pathId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(nullable = false)
    private int sort;

    /** Mastery state the learner must reach before the path considers this done. */
    @Column(name = "required_mastery_state")
    private String requiredMasteryState;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPathId() { return pathId; }
    public void setPathId(UUID pathId) { this.pathId = pathId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public String getRequiredMasteryState() { return requiredMasteryState; }
    public void setRequiredMasteryState(String requiredMasteryState) { this.requiredMasteryState = requiredMasteryState; }
}
