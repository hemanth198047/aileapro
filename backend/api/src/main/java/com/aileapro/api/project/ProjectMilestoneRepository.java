package com.aileapro.api.project;

import com.aileapro.persistence.ProjectMilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestoneEntity, UUID> {

    List<ProjectMilestoneEntity> findByProjectIdOrderBySortOrderAsc(UUID projectId);

    Optional<ProjectMilestoneEntity> findByProjectIdAndSlug(UUID projectId, String slug);

    long countByProjectId(UUID projectId);
}
