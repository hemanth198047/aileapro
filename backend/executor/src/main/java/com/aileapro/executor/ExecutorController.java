package com.aileapro.executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal executor API. Not exposed through the Next.js rewrite; the platform API is the
 * only caller. No auth against a user database — the executor has no database — so it must
 * sit on a private network in production.
 */
@RestController
@RequestMapping("/v1/runs")
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorController {

    private final DockerPythonRunner runner;

    public ExecutorController(DockerPythonRunner runner) {
        this.runner = runner;
    }

    public record RunBody(String language, String source, String stdin, Integer timeoutMs) {
    }

    @PostMapping
    public Map<String, Object> run(@RequestBody RunBody body) {
        DockerPythonRunner.Result result = runner.run(
                body.language(), body.source(), body.stdin(), body.timeoutMs());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("executorId", result.executorId());
        out.put("status", result.status());
        out.put("stdout", nullToEmpty(result.stdout()));
        out.put("stderr", nullToEmpty(result.stderr()));
        out.put("exitCode", result.exitCode());
        out.put("durationMs", result.durationMs());
        out.put("timedOut", result.timedOut());
        out.put("errorCode", result.errorCode());
        out.put("errorMessage", result.errorMessage());
        return out;
    }

    @PostMapping("/{executorId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String executorId) {
        runner.cancel(executorId);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
