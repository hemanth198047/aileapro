package com.aileapro.api.lab;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LabViews {

    private LabViews() {
    }

    public record Run(
            UUID id,
            String language,
            String status,
            String source,
            String sourceHash,
            String stdin,
            String stdout,
            String stderr,
            Integer exitCode,
            Integer durationMs,
            boolean timedOut,
            boolean cancelled,
            boolean development,
            String executorKind,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt) {
    }

    public record History(List<Run> runs, String note, int remainingToday, int dailyCap) {
    }

    public record Status(
            boolean available,
            boolean development,
            String executorKind,
            int remainingToday,
            int dailyCap,
            String demoSource) {
    }
}
