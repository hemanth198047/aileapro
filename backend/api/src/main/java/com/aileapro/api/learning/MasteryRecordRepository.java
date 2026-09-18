package com.aileapro.api.learning;

import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.MasteryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every finder is keyed by {@code userId} first. No read of somebody's evidence is
 * satisfiable without knowing whose evidence it is.
 */
public interface MasteryRecordRepository extends JpaRepository<MasteryRecordEntity, UUID> {

    Optional<MasteryRecordEntity> findByUserIdAndUnitId(UUID userId, UUID unitId);

    List<MasteryRecordEntity> findByUserIdAndUnitIdIn(UUID userId, Collection<UUID> unitIds);

    List<MasteryRecordEntity> findByUserId(UUID userId);

    @Query("select count(distinct r.userId) from MasteryRecordEntity r where r.state in :states")
    long countDistinctUsersWithStateIn(@Param("states") Collection<MasteryState> states);
}
