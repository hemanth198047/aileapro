package com.aileapro.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rendering and hashing for authored prompts.
 *
 * <p>Placeholders are {@code {{name}}} and every one of them must be filled. A prompt that
 * renders with an empty slot produces a system message with a hole in it, the model invents
 * something plausible to fill the gap, and the resulting answer looks fine — which is the
 * worst kind of bug this feature can have.
 */
public final class PromptTemplate {

    private PromptTemplate() {
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-z_]+)\\s*}}");

    /** Every placeholder the template expects, in the order it uses them. */
    public static Set<String> placeholders(String template) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template == null ? "" : template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * Fills the template.
     *
     * @throws IllegalArgumentException if any placeholder has no value, naming the ones missing
     */
    public static String render(String template, Map<String, String> slots) {
        Set<String> missing = new LinkedHashSet<>();
        for (String name : placeholders(template)) {
            String value = slots.get(name);
            if (value == null || value.isBlank()) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Prompt is missing values for " + missing);
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(slots.get(matcher.group(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** SHA-256, hex. The identity of a prompt's text, so an edit cannot hide. */
    public static String hash(String template) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((template == null ? "" : template).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by every JVM we support", impossible);
        }
    }
}
