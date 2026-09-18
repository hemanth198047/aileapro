package com.aileapro.api.project;

import com.aileapro.persistence.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    List<SubmissionEntity> findByUserIdAndProjectIdOrderByCreatedAtDesc(UUID userId, UUID projectId);

    Optional<SubmissionEntity> findFirstByUserIdAndMilestoneIdAndPassedTrueOrderByCreatedAtDesc(
            UUID userId, UUID milestoneId);

    @Query("""
            select count(distinct s.milestoneId) from SubmissionEntity s
            where s.userId = :userId and s.projectId = :projectId and s.passed = true
            """)
    long countPassedMilestones(@Param("userId") UUID userId, @Param("projectId") UUID projectId);
}
