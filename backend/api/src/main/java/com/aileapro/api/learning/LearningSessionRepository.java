package com.aileapro.api.learning;

import com.aileapro.persistence.LearningSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningSessionRepository extends JpaRepository<LearningSessionEntity, UUID> {

    /** The session still running for this unit, if there is one. */
    Optional<LearningSessionEntity> findFirstByUserIdAndUnitIdAndEndedAtIsNullOrderByStartedAtDesc(
            UUID userId, UUID unitId);
}
