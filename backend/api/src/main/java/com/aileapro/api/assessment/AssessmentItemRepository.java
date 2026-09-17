package com.aileapro.api.assessment;

import com.aileapro.persistence.AssessmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssessmentItemRepository
        extends JpaRepository<AssessmentItemEntity, AssessmentItemEntity.Key> {

    List<AssessmentItemEntity> findByIdAssessmentIdOrderBySort(UUID assessmentId);

    List<AssessmentItemEntity> findByIdAssessmentIdIn(Collection<UUID> assessmentIds);

    void deleteByIdAssessmentId(UUID assessmentId);
}
