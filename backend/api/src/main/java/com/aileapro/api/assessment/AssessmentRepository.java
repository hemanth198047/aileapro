package com.aileapro.api.assessment;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    Optional<AssessmentEntity> findBySlug(String slug);

    Optional<AssessmentEntity> findBySlugAndStatus(String slug, ContentStatus status);

    List<AssessmentEntity> findByUnitIdInAndStatus(Collection<UUID> unitIds, ContentStatus status);

    List<AssessmentEntity> findByKindAndStatus(AssessmentKind kind, ContentStatus status);

    Optional<AssessmentEntity> findByUnitIdAndKindAndStatus(
            UUID unitId, AssessmentKind kind, ContentStatus status);
}
