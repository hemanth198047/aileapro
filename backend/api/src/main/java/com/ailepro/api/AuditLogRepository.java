package com.ailepro.api;

import com.ailepro.persistence.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {}
