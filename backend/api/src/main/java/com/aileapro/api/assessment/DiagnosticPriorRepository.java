package com.aileapro.api.assessment;

import com.aileapro.persistence.DiagnosticPriorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiagnosticPriorRepository extends JpaRepository<DiagnosticPriorEntity, UUID> {

    List<DiagnosticPriorEntity> findByAttemptId(UUID attemptId);

    List<DiagnosticPriorEntity> findByUserIdOrderByAtDesc(UUID userId);
}
