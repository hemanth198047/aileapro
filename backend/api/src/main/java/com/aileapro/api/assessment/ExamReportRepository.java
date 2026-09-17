package com.aileapro.api.assessment;

import com.aileapro.persistence.ExamReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Keyed by attempt: one sitting, one report, and the report is never regenerated. */
public interface ExamReportRepository extends JpaRepository<ExamReportEntity, UUID> {
}
