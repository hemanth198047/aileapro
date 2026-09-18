package com.aileapro.api.project;

import com.aileapro.persistence.ProjectApplyUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectApplyUnitRepository extends JpaRepository<ProjectApplyUnitEntity, ProjectApplyUnitEntity.Pk> {

    List<ProjectApplyUnitEntity> findByProjectId(UUID projectId);
}
