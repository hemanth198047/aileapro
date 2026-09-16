package com.aileapro.api.curriculum;

import com.aileapro.persistence.LearningResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningResourceRepository extends JpaRepository<LearningResourceEntity, UUID> {

    Optional<LearningResourceEntity> findBySourceId(UUID sourceId);
}
