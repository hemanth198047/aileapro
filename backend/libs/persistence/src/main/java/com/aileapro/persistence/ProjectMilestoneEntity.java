package com.aileapro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "project_milestones")
public class ProjectMilestoneEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(nullable = false, columnDefinition = "text")
    private String criteria = "";

    @Column(nullable = false, columnDefinition = "text")
    private String hint = "";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "check_key", length = 64)
    private String checkKey;

    @Column(name = "starter_source", columnDefinition = "text")
    private String starterSource;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getCheckKey() { return checkKey; }
    public void setCheckKey(String checkKey) { this.checkKey = checkKey; }
    public String getStarterSource() { return starterSource; }
    public void setStarterSource(String starterSource) { this.starterSource = starterSource; }
}
