package com.aileapro.api.curriculum;

import com.aileapro.persistence.UnitResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnitResourceRepository extends JpaRepository<UnitResourceEntity, UUID> {

    List<UnitResourceEntity> findByUnitIdOrderBySortAsc(UUID unitId);

    List<UnitResourceEntity> findByResourceId(UUID resourceId);

    void deleteByUnitId(UUID unitId);
}
