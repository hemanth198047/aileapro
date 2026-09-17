package com.aileapro.api.assessment;

import com.aileapro.persistence.PlanItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanItemRepository extends JpaRepository<PlanItemEntity, UUID> {

    List<PlanItemEntity> findByPlanIdOrderBySort(UUID planId);

    void deleteByPlanId(UUID planId);
}
