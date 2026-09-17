package com.aileapro.api.assessment;

import com.aileapro.persistence.ReviewAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Append-only. There is no update or delete on this table by design. */
public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswerEntity, UUID> {

    List<ReviewAnswerEntity> findByUserIdAndUnitIdOrderByAtDesc(UUID userId, UUID unitId);
}
