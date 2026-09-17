package com.aileapro.api.assessment;

import com.aileapro.persistence.ReviewScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Leeches are excluded from the due queue by the {@code leech} argument rather than filtered
 * out afterwards: a card that has failed four times needs the page re-read, and showing it
 * again is the thing that has already not worked.
 */
public interface ReviewScheduleRepository extends JpaRepository<ReviewScheduleEntity, UUID> {

    Optional<ReviewScheduleEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ReviewScheduleEntity> findByUserIdAndCardId(UUID userId, UUID cardId);

    List<ReviewScheduleEntity> findByUserIdAndLeechAndDueLessThanEqualOrderByDueAsc(
            UUID userId, boolean leech, LocalDate due);

    List<ReviewScheduleEntity> findByUserId(UUID userId);

    List<ReviewScheduleEntity> findByUserIdAndUnitIdIn(UUID userId, Collection<UUID> unitIds);

    int countByUserIdAndLeechAndDueLessThanEqual(UUID userId, boolean leech, LocalDate due);
}
