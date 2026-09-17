package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** One line of a study plan, with the reason it is on the list. */
@Entity
@Table(name = "plan_items")
public class PlanItemEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false)
    private String kind = "learn";

    private LocalDate due;

    @Column(nullable = false)
    private int sort;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public LocalDate getDue() { return due; }
    public void setDue(LocalDate due) { this.due = due; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getAssessmentId() { return assessmentId; }
    public void setAssessmentId(UUID assessmentId) { this.assessmentId = assessmentId; }
}
