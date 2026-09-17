package com.aileapro.api.ai;

import com.aileapro.persistence.ModelProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModelProfileRepository extends JpaRepository<ModelProfileEntity, UUID> {

    Optional<ModelProfileEntity> findBySlug(String slug);
}
