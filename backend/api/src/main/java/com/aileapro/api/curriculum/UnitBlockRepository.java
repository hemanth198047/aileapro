package com.aileapro.api.curriculum;

import com.aileapro.persistence.UnitBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UnitBlockRepository extends JpaRepository<UnitBlockEntity, UUID> {

    List<UnitBlockEntity> findByUnitIdAndLocaleOrderBySortAsc(UUID unitId, String locale);

    /** Block counts for a batch of units, so listing a path does not fan out into N queries. */
    @Query("select b.unitId, count(b) from UnitBlockEntity b where b.unitId in :ids group by b.unitId")
    List<Object[]> countByUnitIds(@Param("ids") Collection<UUID> ids);

    void deleteByUnitId(UUID unitId);
}
