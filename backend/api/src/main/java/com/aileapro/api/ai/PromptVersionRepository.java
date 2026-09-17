package com.aileapro.api.ai;

import com.aileapro.persistence.PromptVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptVersionRepository extends JpaRepository<PromptVersionEntity, UUID> {

    List<PromptVersionEntity> findByPromptIdOrderByVersionDesc(UUID promptId);

    Optional<PromptVersionEntity> findByPromptIdAndVersion(UUID promptId, int version);
}
