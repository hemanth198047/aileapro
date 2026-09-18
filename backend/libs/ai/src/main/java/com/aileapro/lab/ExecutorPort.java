package com.aileapro.lab;

/**
 * Vendor-neutral sandbox port.
 *
 * <p>The API calls this. The API never spawns a process for learner source. Implementations
 * either talk to the executor HTTP service or return deterministic fixtures for CI.
 */
public interface ExecutorPort {

    /** {@code fake} or {@code docker} / {@code http} — persisted on every lab run. */
    String kind();

    /** True when stdout is a fixture rather than a real sandbox. */
    boolean development();

    RunResult run(RunRequest request);

    /** Best-effort cancel. No-op when the run already finished. */
    void cancel(String executorId);

    record RunRequest(
            String language,
            String source,
            String stdin,
            int timeoutMs,
            int memoryMb) {
    }

    record RunResult(
            String executorId,
            String status,
            String stdout,
            String stderr,
            Integer exitCode,
            int durationMs,
            boolean timedOut,
            String errorCode,
            String errorMessage) {

        public static RunResult ok(String id, String stdout, String stderr, int exit, int durationMs) {
            return new RunResult(id, "succeeded", stdout, stderr, exit, durationMs, false, null, null);
        }

        public static RunResult timedOut(String id, String stdout, String stderr, int durationMs) {
            return new RunResult(id, "timed_out", stdout, stderr, null, durationMs, true, "sandbox_timeout",
                    "The run exceeded its time limit and was killed.");
        }

        public static RunResult failed(String id, String code, String message, int durationMs) {
            return new RunResult(id, "failed", "", "", null, durationMs, false, code, message);
        }
    }
}
