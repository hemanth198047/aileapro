package com.aileapro.api.lab;

import com.aileapro.persistence.LabRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabRunRepository extends JpaRepository<LabRunEntity, UUID> {

    Optional<LabRunEntity> findByIdAndUserId(UUID id, UUID userId);

    List<LabRunEntity> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<LabRunEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("select count(r) from LabRunEntity r where r.userId = :userId and r.createdAt >= :since")
    long countSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
