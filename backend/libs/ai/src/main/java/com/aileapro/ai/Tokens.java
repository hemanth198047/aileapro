package com.aileapro.ai;

import java.util.List;

/**
 * A token estimate, for when the provider does not send counts.
 *
 * <p>Four characters per token is the well-worn rule of thumb for English prose and it is
 * wrong by a noticeable margin on code and on non-Latin scripts. It is used only where the
 * provider reported nothing, and every row it produces is flagged as an estimate, so the cost
 * table never presents a guess as a measurement.
 */
public final class Tokens {

    private Tokens() {
    }

    private static final double CHARS_PER_TOKEN = 4.0;

    /** Per-message overhead providers charge for role framing. Small, but not zero. */
    private static final int PER_MESSAGE_OVERHEAD = 4;

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    public static int estimate(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimate(message.text()) + PER_MESSAGE_OVERHEAD;
        }
        return total;
    }
}
