package com.aileapro.api.admin;

import com.aileapro.persistence.CurriculumChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumChangeRepository extends JpaRepository<CurriculumChangeEntity, UUID> {

    List<CurriculumChangeEntity> findByUnitIdOrderByCreatedAtDesc(UUID unitId);

    Optional<CurriculumChangeEntity> findFirstByUnitIdAndActionOrderByCreatedAtDesc(UUID unitId, String action);
}
