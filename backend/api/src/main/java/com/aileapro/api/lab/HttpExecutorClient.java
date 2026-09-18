package com.aileapro.api.lab;

import com.aileapro.lab.ExecutorPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Talks to the executor HTTP service. The API never runs learner source itself.
 */
@Component
@ConditionalOnProperty(name = "aileapro.lab.executor", havingValue = "http")
public class HttpExecutorClient implements ExecutorPort {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json;
    private final String baseUrl;

    public HttpExecutorClient(
            ObjectMapper json,
            @Value("${aileapro.lab.executor-base-url:http://localhost:8082}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String kind() {
        return "http";
    }

    @Override
    public boolean development() {
        return false;
    }

    @Override
    public RunResult run(RunRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("language", request.language());
            body.put("source", request.source());
            body.put("stdin", request.stdin());
            body.put("timeoutMs", request.timeoutMs());
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/runs"))
                    .timeout(Duration.ofMillis(request.timeoutMs() + 10_000L))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return RunResult.failed(
                        "http-error",
                        "executor_http_error",
                        "Executor returned HTTP " + response.statusCode(),
                        0);
            }
            JsonNode node = json.readTree(response.body());
            return new RunResult(
                    text(node, "executorId"),
                    text(node, "status"),
                    text(node, "stdout"),
                    text(node, "stderr"),
                    node.path("exitCode").isNull() ? null : node.path("exitCode").asInt(),
                    node.path("durationMs").asInt(0),
                    node.path("timedOut").asBoolean(false),
                    blankToNull(text(node, "errorCode")),
                    blankToNull(text(node, "errorMessage")));
        } catch (Exception e) {
            return RunResult.failed(
                    "http-error",
                    "executor_unreachable",
                    e.getMessage() == null ? e.toString() : e.getMessage(),
                    0);
        }
    }

    @Override
    public void cancel(String executorId) {
        if (executorId == null || executorId.isBlank()) {
            return;
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/runs/" + executorId + "/cancel"))
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            http.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
