package com.aileapro.api.ai;

import com.aileapro.persistence.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptRepository extends JpaRepository<PromptEntity, UUID> {

    Optional<PromptEntity> findByName(String name);
}
