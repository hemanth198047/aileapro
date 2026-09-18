package com.aileapro.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Runs learner Python inside a network-restricted Docker container.
 *
 * <p>This is the only place in the platform that may spawn a process for learner source.
 * Source is piped on stdin to {@code python -}; nothing is written to a shared host path the
 * container can keep. Caps, read-only root, and {@code --network none} are mandatory.
 */
@Service
public class DockerPythonRunner {

    private static final Logger log = LoggerFactory.getLogger(DockerPythonRunner.class);

    private final ExecutorProperties props;
    private final Map<String, Process> live = new ConcurrentHashMap<>();

    public DockerPythonRunner(ExecutorProperties props) {
        this.props = props;
    }

    public record Result(
            String executorId,
            String status,
            String stdout,
            String stderr,
            Integer exitCode,
            int durationMs,
            boolean timedOut,
            String errorCode,
            String errorMessage) {
    }

    public Result run(String language, String source, String stdin, Integer timeoutMs) {
        String id = "docker-" + UUID.randomUUID();
        long start = System.nanoTime();

        if (!"python".equalsIgnoreCase(language == null ? "" : language)) {
            return fail(id, start, "unsupported_language", "Only python is supported.");
        }
        if (source == null || source.isBlank()) {
            return fail(id, start, "empty_source", "Source is empty.");
        }
        if (source.length() > props.getMaxSourceChars()) {
            return fail(id, start, "source_too_large", "Source exceeds the size limit.");
        }
        if (!props.isDockerEnabled()) {
            return fail(id, start, "docker_disabled", "Docker execution is disabled on this executor.");
        }
        if (!dockerAvailable()) {
            return fail(id, start, "docker_unavailable",
                    "Docker is not available to the executor. Start Docker Desktop or use the fake executor.");
        }

        int timeout = timeoutMs == null ? props.getDefaultTimeoutMs() : timeoutMs;
        timeout = Math.min(Math.max(timeout, 200), props.getMaxTimeoutMs());

        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("--rm");
        cmd.add("-i");
        cmd.add("--name");
        cmd.add(id);
        cmd.add("--network");
        cmd.add("none");
        cmd.add("--memory");
        cmd.add(props.getMemoryMb() + "m");
        cmd.add("--cpus");
        cmd.add(String.valueOf(props.getCpus()));
        cmd.add("--pids-limit");
        cmd.add(String.valueOf(props.getPidsLimit()));
        cmd.add("--read-only");
        cmd.add("--tmpfs");
        cmd.add("/tmp:rw,noexec,nosuid,size=16m");
        cmd.add("--security-opt");
        cmd.add("no-new-privileges");
        cmd.add("--user");
        cmd.add("65534:65534");
        cmd.add(props.getPythonImage());
        cmd.add("python");
        cmd.add("-");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        try {
            Process process = pb.start();
            live.put(id, process);
            try (OutputStream in = process.getOutputStream()) {
                in.write(source.getBytes(StandardCharsets.UTF_8));
                if (stdin != null && !stdin.isBlank()) {
                    in.write('\n');
                    in.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            }

            StreamBox out = drain(process.getInputStream());
            StreamBox err = drain(process.getErrorStream());

            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                destroy(id, process);
                out.join();
                err.join();
                return new Result(
                        id,
                        "timed_out",
                        trim(out.text()),
                        trim(err.text().isBlank() ? "killed: timeout" : err.text()),
                        null,
                        elapsed(start),
                        true,
                        "sandbox_timeout",
                        "The run exceeded its time limit and was killed.");
            }
            out.join();
            err.join();
            int exit = process.exitValue();
            String status = exit == 0 ? "succeeded" : "failed";
            return new Result(
                    id,
                    status,
                    trim(out.text()),
                    trim(err.text()),
                    exit,
                    elapsed(start),
                    false,
                    exit == 0 ? null : "nonzero_exit",
                    exit == 0 ? null : "Process exited with code " + exit);
        } catch (Exception e) {
            log.warn("Executor run failed: {}", e.toString());
            destroy(id, null);
            return fail(id, start, "executor_error", e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            live.remove(id);
        }
    }

    public boolean cancel(String executorId) {
        Process process = live.get(executorId);
        if (process == null) {
            // Container may still be alive under the docker name.
            try {
                new ProcessBuilder("docker", "kill", executorId).start().waitFor(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // best effort
            }
            return false;
        }
        destroy(executorId, process);
        return true;
    }

    private void destroy(String id, Process process) {
        if (process != null) {
            process.destroyForcibly();
        }
        try {
            new ProcessBuilder("docker", "kill", id).start().waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // best effort
        }
    }

    private boolean dockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            boolean ok = p.waitFor(8, TimeUnit.SECONDS);
            return ok && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Result fail(String id, long start, String code, String message) {
        return new Result(id, "failed", "", "", null, elapsed(start), false, code, message);
    }

    private static int elapsed(long startNanos) {
        return (int) Math.max(0, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String trim(String text) {
        if (text == null) {
            return "";
        }
        int max = props.getMaxOutputChars();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n… truncated …\n";
    }

    private static StreamBox drain(InputStream stream) {
        StreamBox box = new StreamBox();
        Thread t = new Thread(() -> {
            try {
                byte[] buf = new byte[4096];
                int n;
                while ((n = stream.read(buf)) >= 0) {
                    box.bytes.write(buf, 0, n);
                }
            } catch (IOException ignored) {
                // process ended
            }
        }, "executor-drain");
        t.setDaemon(true);
        t.start();
        box.thread = t;
        return box;
    }

    private static final class StreamBox {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Thread thread;

        void join() throws InterruptedException {
            if (thread != null) {
                thread.join(5_000);
            }
        }

        String text() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
