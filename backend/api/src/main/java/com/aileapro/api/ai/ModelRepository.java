package com.aileapro.api.ai;

import com.aileapro.persistence.ModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModelRepository extends JpaRepository<ModelEntity, UUID> {

    Optional<ModelEntity> findBySlug(String slug);
}
