package com.aileapro.api.curriculum;

import com.aileapro.persistence.UnitClosureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnitClosureRepository extends JpaRepository<UnitClosureEntity, UnitClosureEntity.Key> {

    /** Ancestors of a unit, furthest first, so the breadcrumb reads top-down. */
    List<UnitClosureEntity> findByIdDescendantIdOrderByDepthDesc(UUID descendantId);

    void deleteByIdDescendantId(UUID descendantId);
}
