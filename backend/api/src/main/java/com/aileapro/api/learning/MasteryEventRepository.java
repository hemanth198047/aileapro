package com.aileapro.api.learning;

import com.aileapro.persistence.MasteryEventEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Append-only history. Nothing in the codebase updates or deletes rows through here. */
public interface MasteryEventRepository extends JpaRepository<MasteryEventEntity, UUID> {

    List<MasteryEventEntity> findByUserIdAndUnitIdOrderByAtDesc(UUID userId, UUID unitId, Limit limit);

    long countByUserIdAndUnitId(UUID userId, UUID unitId);
}
