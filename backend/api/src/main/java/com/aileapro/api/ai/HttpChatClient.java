package com.aileapro.api.ai;

import com.aileapro.ai.ChatMessage;
import com.aileapro.ai.ChatPort;
import com.aileapro.ai.ChatRequest;
import com.aileapro.ai.ChatResult;
import com.aileapro.ai.ProviderException;
import com.aileapro.ai.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * One client for every provider that speaks OpenAI-compatible chat completions.
 *
 * <p>Which is most of them: OpenAI, Azure OpenAI, OpenRouter, Together, vLLM, Ollama, and
 * llama.cpp all accept this request shape. Speaking one wire format rather than importing one
 * SDK per vendor is what makes "switching provider is a configuration change" true, and it
 * keeps the dependency list short enough to audit.
 *
 * <p>A deliberate limitation: no tool calls and no structured output. Those exist in the
 * format but not in this feature, and an unused code path in the one class that talks to the
 * outside world is a liability rather than a head start.
 */
public class HttpChatClient implements ChatPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Generous, because a teaching answer is long and a learner will wait a little. The real
     * protection against a hung provider is the output ceiling, not this.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final String providerSlug;
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;
    private final ObjectMapper json;

    public HttpChatClient(String providerSlug, String baseUrl, String apiKey, ObjectMapper json) {
        this.providerSlug = providerSlug;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public String providerSlug() {
        return providerSlug;
    }

    @Override
    public boolean development() {
        return false;
    }

    @Override
    public ChatResult complete(ChatRequest request) throws ProviderException {
        HttpResponse<String> response = send(body(request, false), HttpResponse.BodyHandlers.ofString());
        JsonNode root = parse(response.body());
        JsonNode choice = root.path("choices").path(0);
        String text = choice.path("message").path("content").asText("");
        return new ChatResult(
                text,
                root.path("usage").path("prompt_tokens").asInt(Tokens.estimate(request.messages())),
                root.path("usage").path("completion_tokens").asInt(Tokens.estimate(text)),
                !root.path("usage").has("prompt_tokens"),
                choice.path("finish_reason").asText("stop"));
    }

    /**
     * Streams a completion, parsing the server-sent event format the API replies with.
     *
     * <p>Token counts are only sent in the final chunk, and only when asked for, so
     * {@code stream_options.include_usage} is set. Providers that ignore it fall back to the
     * estimate, flagged as an estimate.
     */
    @Override
    public ChatResult stream(ChatRequest request, Consumer<String> onChunk) throws ProviderException {
        HttpResponse<java.io.InputStream> response =
                send(body(request, true), HttpResponse.BodyHandlers.ofInputStream());

        StringBuilder text = new StringBuilder();
        String finishReason = "stop";
        int tokensIn = -1;
        int tokensOut = -1;

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }
                JsonNode event = json.readTree(payload);
                JsonNode choice = event.path("choices").path(0);
                String piece = choice.path("delta").path("content").asText("");
                if (!piece.isEmpty()) {
                    text.append(piece);
                    onChunk.accept(piece);
                }
                if (choice.hasNonNull("finish_reason") && !choice.get("finish_reason").isNull()) {
                    finishReason = choice.get("finish_reason").asText(finishReason);
                }
                if (event.path("usage").has("prompt_tokens")) {
                    tokensIn = event.path("usage").path("prompt_tokens").asInt();
                    tokensOut = event.path("usage").path("completion_tokens").asInt();
                }
            }
        } catch (IOException broken) {
            // Mid-stream failures are retryable, but only if nothing reached the learner: a
            // partial answer followed by a second, different answer is worse than a short one.
            throw new ProviderException(
                    providerSlug + " stopped mid-stream: " + broken.getMessage(),
                    text.isEmpty(),
                    broken);
        }

        boolean estimated = tokensIn < 0;
        return new ChatResult(
                text.toString(),
                estimated ? Tokens.estimate(request.messages()) : tokensIn,
                estimated ? Tokens.estimate(text.toString()) : tokensOut,
                estimated,
                finishReason);
    }

    private <T> HttpResponse<T> send(String body, HttpResponse.BodyHandler<T> handler) throws ProviderException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<T> response = http.send(httpRequest, handler);
            if (response.statusCode() >= 400) {
                // 429 and 5xx are worth another model; 400 and 401 mean our request or our key
                // is wrong, and retrying that just spends the learner's patience three times.
                boolean retryable = response.statusCode() == 429 || response.statusCode() >= 500;
                throw new ProviderException(
                        providerSlug + " returned " + response.statusCode(), retryable);
            }
            return response;
        } catch (IOException network) {
            throw new ProviderException(providerSlug + " is unreachable: " + network.getMessage(), true, network);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ProviderException(providerSlug + " call was interrupted", false, interrupted);
        }
    }

    private String body(ChatRequest request, boolean stream) {
        ObjectNode root = json.createObjectNode();
        root.put("model", request.wireName());
        root.put("temperature", request.temperature());
        root.put("max_tokens", request.maxOutputTokens());
        root.put("stream", stream);
        if (stream) {
            root.putObject("stream_options").put("include_usage", true);
        }

        ArrayNode messages = root.putArray("messages");
        for (ChatMessage message : request.messages()) {
            ObjectNode node = messages.addObject();
            node.put("role", switch (message.role()) {
                case SYSTEM -> "system";
                case USER -> "user";
                case ASSISTANT -> "assistant";
            });
            node.put("content", message.text());
        }

        try {
            return json.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise a chat request", impossible);
        }
    }

    private JsonNode parse(String body) throws ProviderException {
        try {
            return json.readTree(body);
        } catch (IOException broken) {
            throw new ProviderException(providerSlug + " sent a reply we could not read", false, broken);
        }
    }
}
