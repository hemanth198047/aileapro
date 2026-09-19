package com.aileapro.api.intelligence;

import com.aileapro.persistence.AiUpdateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiUpdateRepository extends JpaRepository<AiUpdateEntity, UUID> {

    Optional<AiUpdateEntity> findBySlug(String slug);

    List<AiUpdateEntity> findByStatusOrderByOccurredAtDesc(String status);

    List<AiUpdateEntity> findAllByOrderByOccurredAtDesc();
}
