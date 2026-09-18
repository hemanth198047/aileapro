package com.aileapro.api.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminViews {

    private AdminViews() {
    }

    public record UnitSummary(
            UUID id,
            String slug,
            String title,
            String status,
            String version,
            String kind,
            Short difficulty,
            int blockCount,
            Instant updatedAt) {
    }

    public record Block(UUID id, String type, int sort, String payload) {
    }

    public record UnitDetail(
            UUID id,
            String slug,
            String title,
            String description,
            String status,
            String version,
            String kind,
            Short difficulty,
            Integer estimateMinutes,
            List<String> objectives,
            List<String> tags,
            List<Block> blocks,
            ContentCritic.Report critic,
            Instant effectiveAt,
            Instant updatedAt) {
    }

    public record Change(
            UUID id,
            String action,
            String fromVersion,
            String toVersion,
            String fromStatus,
            String toStatus,
            String reason,
            Instant createdAt) {
    }

    public record PublishResult(
            String slug,
            String fromVersion,
            String toVersion,
            String status,
            UUID changeId,
            UUID auditId,
            ContentCritic.Report critic) {
    }

    public record RollbackResult(
            String slug,
            String restoredVersion,
            String status,
            UUID changeId,
            String note) {
    }
}
