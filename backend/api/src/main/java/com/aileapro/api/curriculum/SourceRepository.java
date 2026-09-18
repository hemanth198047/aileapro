package com.aileapro.api.curriculum;

import com.aileapro.domain.SourceType;
import com.aileapro.persistence.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository extends JpaRepository<SourceEntity, UUID> {

    Optional<SourceEntity> findBySlug(String slug);

    List<SourceEntity> findBySourceTypeOrderByPublishedOnDescTitleAsc(SourceType sourceType);
}
