package com.aileapro.lab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic development sandbox.
 *
 * <p>Recognises a handful of fixtures so CI and local walks never need Docker. Anything it
 * does not recognise still returns a clear "development implementation" stdout rather than
 * pretending to interpret Python — a fake that silently invents output would teach the wrong
 * lesson about what ran.
 */
public final class FakeExecutorClient implements ExecutorPort {

    public static final String KIND = "fake";

    /** Source marker that forces a timeout fixture, used by the Phase 9 gate. */
    public static final String TIMEOUT_MARKER = "__AILEAPRO_FORCE_TIMEOUT__";

    private final Map<String, Boolean> cancelled = new ConcurrentHashMap<>();
    private final int fakeTimeoutSleepMs;

    public FakeExecutorClient() {
        this(30);
    }

    public FakeExecutorClient(int fakeTimeoutSleepMs) {
        this.fakeTimeoutSleepMs = Math.max(0, fakeTimeoutSleepMs);
    }

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public boolean development() {
        return true;
    }

    @Override
    public RunResult run(RunRequest request) {
        String id = "fake-" + UUID.randomUUID();
        long start = System.nanoTime();
        if (request.source() != null && request.source().contains(TIMEOUT_MARKER)) {
            sleep(Math.min(request.timeoutMs(), fakeTimeoutSleepMs));
            if (cancelled.containsKey(id)) {
                return cancelled(id, elapsed(start));
            }
            return RunResult.timedOut(id, "", "killed: timeout", elapsed(start));
        }

        if (!"python".equalsIgnoreCase(nullToEmpty(request.language()))) {
            return RunResult.failed(id, "unsupported_language", "Only python is supported.", elapsed(start));
        }

        String source = nullToEmpty(request.source()).trim();
        if (source.isBlank()) {
            return RunResult.failed(id, "empty_source", "Source is empty.", elapsed(start));
        }

        String stdout = fixtureStdout(source);
        String banner = "_Development implementation: no sandbox is configured, so this output is a fixture._\n";
        return RunResult.ok(id, banner + stdout, "", 0, elapsed(start));
    }

    @Override
    public void cancel(String executorId) {
        if (executorId != null) {
            cancelled.put(executorId, true);
        }
    }

    private static RunResult cancelled(String id, int durationMs) {
        return new RunResult(id, "cancelled", "", "cancelled", null, durationMs, false, "cancelled",
                "The run was cancelled.");
    }

    /**
     * Tiny pattern matchers for the gate demos. Not an interpreter: unknown source gets an
     * explicit fixture note rather than invented results.
     */
    static String fixtureStdout(String source) {
        String lower = source.toLowerCase(Locale.ROOT);
        if (lower.contains("print(\"hello, aileapro\")") || lower.contains("print('hello, aileapro')")) {
            return "Hello, AILEAPRO\n";
        }
        if (lower.contains("print(a)") && lower.contains("b = a") && lower.contains("a = [1, 2, 3]")) {
            return "[1, 2, 3]\n";
        }
        if (lower.contains("def mean") && lower.contains("print(mean(")) {
            return "2.0\n";
        }
        // Phase 10 project milestone checks. Fixtures recognise the harness marker plus the
        // required definition — they do not interpret Python, and missing defs fail loudly.
        if (lower.contains("aileapro_check:mse")) {
            return lower.contains("def mse") ? PASS : FAIL;
        }
        if (lower.contains("aileapro_check:fit_line")) {
            return lower.contains("def fit") ? PASS : FAIL;
        }
        if (lower.contains("aileapro_check:predict_report")) {
            boolean hasFit = lower.contains("def fit") || lower.contains("slope") || lower.contains("intercept");
            boolean hasPredict = lower.contains("def predict") || lower.contains("y_hat") || lower.contains("yhat");
            return hasFit && hasPredict ? PASS : FAIL;
        }
        // echo a simple print("...") literal when present
        int p = indexOfPrint(source);
        if (p >= 0) {
            String lit = extractPrintLiteral(source, p);
            if (lit != null) {
                return lit + "\n";
            }
        }
        return "Fixture ran. Configure the Docker executor to execute this source for real.\n";
    }

    public static final String PASS = "AILEAPRO_PASS\n";
    public static final String FAIL = "AILEAPRO_FAIL\n";

    private static int indexOfPrint(String source) {
        int d = source.indexOf("print(\"");
        int s = source.indexOf("print('");
        if (d < 0) {
            return s;
        }
        if (s < 0) {
            return d;
        }
        return Math.min(d, s);
    }

    private static String extractPrintLiteral(String source, int at) {
        char quote = source.charAt(at + 6);
        int end = source.indexOf(quote, at + 7);
        if (end < 0) {
            return null;
        }
        return source.substring(at + 7, end);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static int elapsed(long startNanos) {
        return (int) Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
