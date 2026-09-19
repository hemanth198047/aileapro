package com.aileapro.api.lab;

import com.aileapro.api.platform.ApiException;
import com.aileapro.api.platform.FeatureFlagService;
import com.aileapro.lab.ExecutorPort;
import com.aileapro.lab.FakeExecutorClient;
import com.aileapro.persistence.LabRunEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LabService {

    public static final String DEMO_SOURCE = """
            # Values and types — names point at objects.
            a = [1, 2, 3]
            b = a
            print(a)
            """;

    private final LabRunRepository runs;
    private final ExecutorPort executor;
    private final FeatureFlagService flags;
    private final int dailyCap;
    private final int defaultTimeoutMs;
    private final int maxTimeoutMs;
    private final int maxSourceChars;

    public LabService(
            LabRunRepository runs,
            ExecutorPort executor,
            FeatureFlagService flags,
            @Value("${aileapro.lab.daily-runs-per-user:40}") int dailyCap,
            @Value("${aileapro.lab.default-timeout-ms:5000}") int defaultTimeoutMs,
            @Value("${aileapro.lab.max-timeout-ms:15000}") int maxTimeoutMs,
            @Value("${aileapro.lab.max-source-chars:50000}") int maxSourceChars) {
        this.runs = runs;
        this.executor = executor;
        this.flags = flags;
        this.dailyCap = dailyCap;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.maxTimeoutMs = maxTimeoutMs;
        this.maxSourceChars = maxSourceChars;
    }

    public LabViews.Status status(UUID userId) {
        boolean on = flags.enabled(FeatureFlagService.LAB);
        return new LabViews.Status(
                on,
                executor.development(),
                executor.kind(),
                remainingToday(userId),
                dailyCap,
                DEMO_SOURCE);
    }

    @Transactional
    public LabViews.Run start(UUID userId, String language, String source, String stdin, Integer timeoutMs) {
        flags.require(FeatureFlagService.LAB, "The lab is temporarily disabled");
        requireQuota(userId);
        if (source == null || source.isBlank()) {
            throw ApiException.validation("Source is required");
        }
        if (source.length() > maxSourceChars) {
            throw ApiException.validation("Source exceeds the size limit");
        }
        String lang = language == null || language.isBlank() ? "python" : language.trim().toLowerCase();
        if (!"python".equals(lang)) {
            throw ApiException.validation("Only python is supported in this phase");
        }
        int timeout = timeoutMs == null ? defaultTimeoutMs : timeoutMs;
        timeout = Math.min(Math.max(timeout, 200), maxTimeoutMs);

        LabRunEntity row = new LabRunEntity();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        row.setLanguage(lang);
        row.setStatus("running");
        row.setSource(source);
        row.setSourceHash(FakeExecutorClient.sha256(source));
        row.setStdin(stdin);
        row.setExecutorKind(executor.kind());
        row.setCreatedAt(Instant.now());
        row.setStartedAt(Instant.now());
        runs.save(row);

        ExecutorPort.RunResult result = executor.run(
                new ExecutorPort.RunRequest(lang, source, stdin, timeout, 128));

        applyResult(row, result);
        runs.save(row);
        return view(row);
    }

    @Transactional(readOnly = true)
    public LabViews.Run get(UUID userId, UUID id) {
        return view(owned(userId, id));
    }

    @Transactional
    public LabViews.Run cancel(UUID userId, UUID id) {
        LabRunEntity row = owned(userId, id);
        if (row.getFinishedAt() != null) {
            return view(row);
        }
        executor.cancel(row.getExecutorId());
        row.setCancelled(true);
        row.setStatus("cancelled");
        row.setFinishedAt(Instant.now());
        row.setErrorCode("cancelled");
        row.setErrorMessage("The run was cancelled.");
        runs.save(row);
        return view(row);
    }

    @Transactional(readOnly = true)
    public LabViews.History history(UUID userId) {
        List<LabViews.Run> items = runs.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::view)
                .toList();
        String note = items.isEmpty()
                ? "No runs yet. Start with the demo snippet — it prints what the Values and Types unit argues."
                : items.size() + " recent runs.";
        return new LabViews.History(items, note, remainingToday(userId), dailyCap);
    }

    /** Most recent finished run for the tutor to cite as actual stdout. */
    @Transactional(readOnly = true)
    public LabViews.Run latestFinished(UUID userId) {
        return runs.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(row -> row.getFinishedAt() != null)
                .map(this::view)
                .orElse(null);
    }

    private void requireQuota(UUID userId) {
        if (remainingToday(userId) <= 0) {
            throw new ApiException(
                    429,
                    "budget_exceeded",
                    "You have used today's lab limit of " + dailyCap + " runs. It resets at midnight UTC.");
        }
    }

    private int remainingToday(UUID userId) {
        Instant since = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long used = runs.countSince(userId, since);
        return (int) Math.max(0, dailyCap - used);
    }

    private LabRunEntity owned(UUID userId, UUID id) {
        return runs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Lab run not found"));
    }

    private void applyResult(LabRunEntity row, ExecutorPort.RunResult result) {
        row.setExecutorId(result.executorId());
        row.setStdout(result.stdout());
        row.setStderr(result.stderr());
        row.setExitCode(result.exitCode());
        row.setDurationMs(result.durationMs());
        row.setTimedOut(result.timedOut());
        row.setErrorCode(result.errorCode());
        row.setErrorMessage(result.errorMessage());
        row.setFinishedAt(Instant.now());
        row.setStatus(result.status() == null ? "failed" : result.status());
        if (result.timedOut()) {
            row.setStatus("timed_out");
        }
    }

    private LabViews.Run view(LabRunEntity row) {
        return new LabViews.Run(
                row.getId(),
                row.getLanguage(),
                row.getStatus(),
                row.getSource(),
                row.getSourceHash(),
                row.getStdin(),
                row.getStdout(),
                row.getStderr(),
                row.getExitCode(),
                row.getDurationMs(),
                row.isTimedOut(),
                row.isCancelled(),
                "fake".equals(row.getExecutorKind()),
                row.getExecutorKind(),
                row.getErrorCode(),
                row.getErrorMessage(),
                row.getCreatedAt(),
                row.getStartedAt(),
                row.getFinishedAt());
    }
}
