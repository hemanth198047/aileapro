package com.aileapro.ai;

import java.util.List;
import java.util.function.Consumer;

/**
 * The development implementation of {@link ChatPort}.
 *
 * <p>It is deterministic, it is labelled in its own output, and it answers in the shape the
 * policy asked for — a hint where a hint was ordered, a full explanation where one was earned.
 * That last property is the point: the fixtures are how the reveal ladder, the streaming, the
 * transcript, and the usage meter are exercised end to end without a key, so CI can hold the
 * whole feature honest for free.
 *
 * <p>What it deliberately is not: random text, and not a silent fallback. Every reply says it
 * is a fixture, every message it produces is stored with {@code development = true}, and the
 * dock shows a banner. A fixture that could be mistaken for teaching would be the single worst
 * thing this phase could ship.
 */
public final class FakeChatClient implements ChatPort {

    /** Sentinels from {@link TutorPolicy}'s reveal instructions. Same module, so they hold. */
    private static final String HINT_MARKER = "Do not give the answer";
    private static final String GUIDED_MARKER = "leaving the final step";

    private static final String LABEL =
            "_Development implementation: no model is configured, so this reply is a fixture "
                    + "rather than teaching._";

    private final long chunkDelayMillis;

    public FakeChatClient() {
        this(0);
    }

    /**
     * @param chunkDelayMillis pause between streamed chunks, so a developer can see that
     *                         streaming works. Zero in tests, where it would only add minutes.
     */
    public FakeChatClient(long chunkDelayMillis) {
        this.chunkDelayMillis = Math.max(0, chunkDelayMillis);
    }

    @Override
    public String providerSlug() {
        return "fake";
    }

    @Override
    public boolean development() {
        return true;
    }

    @Override
    public ChatResult complete(ChatRequest request) {
        String text = reply(request);
        return new ChatResult(
                text, Tokens.estimate(request.messages()), Tokens.estimate(text), true, "stop");
    }

    @Override
    public ChatResult stream(ChatRequest request, Consumer<String> onChunk) {
        String text = reply(request);
        for (String chunk : chunks(text)) {
            onChunk.accept(chunk);
            if (chunkDelayMillis > 0) {
                try {
                    Thread.sleep(chunkDelayMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return new ChatResult(
                text, Tokens.estimate(request.messages()), Tokens.estimate(text), true, "stop");
    }

    /** Word-sized pieces, because that is roughly what a real stream delivers. */
    private static List<String> chunks(String text) {
        String[] words = text.split(" ");
        List<String> out = new java.util.ArrayList<>(words.length);
        for (int i = 0; i < words.length; i++) {
            out.add(i == words.length - 1 ? words[i] : words[i] + " ");
        }
        return out;
    }

    private static String reply(ChatRequest request) {
        String system = request.messages().stream()
                .filter(message -> message.role() == ChatMessage.Role.SYSTEM)
                .map(ChatMessage::text)
                .findFirst()
                .orElse("");
        String asked = request.messages().stream()
                .filter(message -> message.role() == ChatMessage.Role.USER
                        && message.channel() == ChatMessage.Channel.USER)
                .map(ChatMessage::text)
                .reduce((first, second) -> second)
                .orElse("");

        if (system.contains(HINT_MARKER)) {
            return LABEL + "\n\n" + hint(asked);
        }
        if (system.contains(GUIDED_MARKER)) {
            return LABEL + "\n\n" + guided(asked);
        }
        return LABEL + "\n\n" + full(asked);
    }

    /** A question back, which is what the hint stage is for. */
    private static String hint(String asked) {
        return "Before I answer that, one question back: what do you already expect to happen, "
                + "and why?\n\nYou asked about " + quoted(asked) + ". Name the step you are least "
                + "sure of and we will start there — that is usually the step doing the work.";
    }

    private static String guided(String asked) {
        return "Here is the method, and I am leaving the last step to you.\n\n"
                + "1. Write down what you are given and what you want, in the same notation.\n"
                + "2. Find the one relationship that connects them. For " + quoted(asked)
                + ", that relationship is the thing the unit spends most of its words on.\n"
                + "3. Apply it once and check the units or the types before going further.\n\n"
                + "Your turn for step four: apply it to your own case and tell me what you get. "
                + "If the answer surprises you, the surprise is the interesting part.";
    }

    private static String full(String asked) {
        return "Taking " + quoted(asked) + " properly.\n\n"
                + "The short version: it behaves the way it does because of one rule, and every "
                + "surprising case is that rule applied somewhere you were not looking.\n\n"
                + "Why the reasoning works: the rule is about what the thing *is*, not about what "
                + "this particular operation does, so it holds for operations you have not met "
                + "yet. That is what makes it worth learning rather than memorising.\n\n"
                + "Where it stops working: at the boundary where the underlying assumption fails. "
                + "Find that boundary deliberately, in a REPL or on paper, and the rule stops "
                + "being a fact you were told and becomes one you can check.";
    }

    /** The learner's own words, trimmed, so the fixture is visibly about their question. */
    private static String quoted(String asked) {
        String cleaned = asked == null ? "" : asked.replace("\n", " ").trim();
        if (cleaned.isEmpty()) {
            return "this unit";
        }
        String shortened = cleaned.length() > 80 ? cleaned.substring(0, 77) + "…" : cleaned;
        return "\u201c" + shortened + "\u201d";
    }
}
