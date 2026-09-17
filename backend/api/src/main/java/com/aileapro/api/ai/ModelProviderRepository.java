package com.aileapro.api.ai;

import com.aileapro.persistence.ModelProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModelProviderRepository extends JpaRepository<ModelProviderEntity, UUID> {

    Optional<ModelProviderEntity> findBySlug(String slug);
}
