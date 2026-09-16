package com.aileapro.api.curriculum;

import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.LearningPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningPathRepository extends JpaRepository<LearningPathEntity, UUID> {

    Optional<LearningPathEntity> findBySlug(String slug);

    List<LearningPathEntity> findByStatusOrderByTitleAsc(ContentStatus status);
}
