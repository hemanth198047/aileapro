package com.aileapro.api.assessment;

import com.aileapro.persistence.MisconceptionFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MisconceptionFlagRepository extends JpaRepository<MisconceptionFlagEntity, UUID> {

    Optional<MisconceptionFlagEntity> findByUserIdAndCode(UUID userId, String code);

    List<MisconceptionFlagEntity> findByUserIdAndOpen(UUID userId, boolean open);

    List<MisconceptionFlagEntity> findByUserIdAndUnitIdInAndOpen(
            UUID userId, Collection<UUID> unitIds, boolean open);

    int countByUserIdAndUnitIdAndOpen(UUID userId, UUID unitId, boolean open);
}
