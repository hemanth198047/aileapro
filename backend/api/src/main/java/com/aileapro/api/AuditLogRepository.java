package com.aileapro.api;

import com.aileapro.persistence.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByActionAndResourceIdOrderByAtDesc(String action, String resourceId);

    List<AuditLogEntity> findTop20ByActionOrderByAtDesc(String action);
}
