package com.aileapro.api.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pulls the learner-visible text out of a block payload.
 *
 * <p>Payloads are typed JSON. The indexer only needs the prose a learner would read — not
 * exercise keys, not quiz answers — so those fields are deliberately ignored.
 */
public final class BlockText {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BlockText() {
    }

    public static String fromPayloadJson(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return "";
        }
        try {
            return fromPayload(MAPPER.readTree(payloadJson));
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String fromPayload(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        String claim = text(payload, "claim");
        if (!claim.isBlank()) {
            out.append(claim).append('\n');
        }
        String markdown = text(payload, "markdown");
        if (!markdown.isBlank()) {
            out.append(markdown);
        }
        // Self-check stems are fair game; their answers are not.
        JsonNode items = payload.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                String question = text(item, "question");
                if (!question.isBlank()) {
                    if (out.length() > 0) {
                        out.append('\n');
                    }
                    out.append(question);
                }
            }
        }
        return out.toString().trim();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
