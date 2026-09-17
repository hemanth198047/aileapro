package com.aileapro.api.assessment;

import com.aileapro.persistence.StudyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Plans are versioned rather than overwritten, so "the plan" is the highest version. */
public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, UUID> {

    Optional<StudyPlanEntity> findFirstByUserIdOrderByVersionDesc(UUID userId);

    Optional<StudyPlanEntity> findByIdAndUserId(UUID id, UUID userId);
}
