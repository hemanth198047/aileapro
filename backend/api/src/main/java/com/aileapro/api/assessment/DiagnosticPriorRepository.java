package com.aileapro.api.assessment;

import com.aileapro.persistence.DiagnosticPriorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DiagnosticPriorRepository extends JpaRepository<DiagnosticPriorEntity, UUID> {

    List<DiagnosticPriorEntity> findByAttemptId(UUID attemptId);

    List<DiagnosticPriorEntity> findByUserIdOrderByAtDesc(UUID userId);

    @Query("select count(distinct p.userId) from DiagnosticPriorEntity p")
    long countDistinctUsers();
}
