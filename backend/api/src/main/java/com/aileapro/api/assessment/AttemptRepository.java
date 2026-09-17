package com.aileapro.api.assessment;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.persistence.AttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Attempts, always keyed by {@code userId} first. No read of somebody's sitting is
 * satisfiable without knowing whose sitting it is.
 */
public interface AttemptRepository extends JpaRepository<AttemptEntity, UUID> {

    Optional<AttemptEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<AttemptEntity> findFirstByUserIdAndAssessmentIdAndStatusOrderByStartedAtDesc(
            UUID userId, UUID assessmentId, String status);

    List<AttemptEntity> findByUserIdOrderByStartedAtDesc(UUID userId);

    List<AttemptEntity> findByUserIdAndKindAndStatusOrderByStartedAtDesc(
            UUID userId, AssessmentKind kind, String status);

    List<AttemptEntity> findByUserIdAndUnitIdAndStatus(UUID userId, UUID unitId, String status);
}
