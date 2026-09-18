package com.aileapro.api.project;

import com.aileapro.persistence.PortfolioItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItemEntity, UUID> {

    List<PortfolioItemEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PortfolioItemEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<PortfolioItemEntity> findByUserIdAndProjectId(UUID userId, UUID projectId);

    List<PortfolioItemEntity> findByUserIdAndVisibilityOrderByCreatedAtDesc(UUID userId, String visibility);
}
