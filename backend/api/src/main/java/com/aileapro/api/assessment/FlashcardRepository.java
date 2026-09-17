package com.aileapro.api.assessment;

import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.FlashcardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<FlashcardEntity, UUID> {

    Optional<FlashcardEntity> findBySlug(String slug);

    List<FlashcardEntity> findByUnitIdAndStatus(UUID unitId, ContentStatus status);

    List<FlashcardEntity> findByUnitIdInAndStatus(Collection<UUID> unitIds, ContentStatus status);

    List<FlashcardEntity> findByIdIn(Collection<UUID> ids);
}
