package com.aileapro.api.ai;

import com.aileapro.persistence.ModelProfileModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModelProfileModelRepository
        extends JpaRepository<ModelProfileModelEntity, ModelProfileModelEntity.Key> {

    /** The fallback chain, in order. */
    List<ModelProfileModelEntity> findByIdProfileIdOrderBySort(UUID profileId);

    void deleteByIdProfileId(UUID profileId);
}
