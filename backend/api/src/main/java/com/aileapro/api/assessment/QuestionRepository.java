package com.aileapro.api.assessment;

import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The authored item bank. Items are content, so every finder is keyed by status. */
public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID> {

    Optional<QuestionEntity> findBySlug(String slug);

    List<QuestionEntity> findByUnitIdAndStatus(UUID unitId, ContentStatus status);

    List<QuestionEntity> findByUnitIdInAndStatus(Collection<UUID> unitIds, ContentStatus status);

    List<QuestionEntity> findByIdIn(Collection<UUID> ids);
}
