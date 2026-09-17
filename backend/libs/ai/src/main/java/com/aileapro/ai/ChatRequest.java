package com.aileapro.ai;

import java.util.List;

/**
 * What to ask, and how much of an answer to allow.
 *
 * <p>The model name is a wire name handed down by the router, not chosen by the caller. A
 * feature that could name a model would eventually name a specific one, and then the profile
 * table would be decoration.
 *
 * @param wireName        what the provider calls the model
 * @param messages        system, data, and turn history, in order
 * @param temperature     0 for deterministic work, higher for teaching prose
 * @param maxOutputTokens hard ceiling, so one runaway answer cannot eat a daily budget
 */
public record ChatRequest(
        String wireName,
        List<ChatMessage> messages,
        double temperature,
        int maxOutputTokens) {

    public ChatRequest {
        messages = List.copyOf(messages);
    }
}
