package com.aileapro.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits authored block text into retrieval chunks.
 *
 * <p>Pure: same input, same chunks, same hashes. Idempotent reindexing depends on that.
 * Paragraph-aware with a hard character ceiling so one long code block cannot become a
 * single unusable vector.
 */
public final class Chunker {

    public static final int DEFAULT_MAX_CHARS = 900;

    private Chunker() {
    }

    public record Chunk(int index, String text, String contentHash, int tokenEstimate) {
    }

    public static List<Chunk> chunk(String text) {
        return chunk(text, DEFAULT_MAX_CHARS);
    }

    public static List<Chunk> chunk(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalised = text.replace("\r\n", "\n").trim();
        List<String> paragraphs = splitParagraphs(normalised);
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > maxChars) {
                flush(current, pieces);
                for (String slice : hardSplit(paragraph, maxChars)) {
                    pieces.add(slice);
                }
                continue;
            }
            if (current.length() > 0 && current.length() + 1 + paragraph.length() > maxChars) {
                flush(current, pieces);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(paragraph);
        }
        flush(current, pieces);

        List<Chunk> out = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            String body = pieces.get(i);
            out.add(new Chunk(i, body, sha256(body), estimateTokens(body)));
        }
        return out;
    }

    private static List<String> splitParagraphs(String text) {
        String[] raw = text.split("\n{2,}");
        List<String> out = new ArrayList<>(raw.length);
        for (String part : raw) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        if (out.isEmpty() && !text.isBlank()) {
            out.add(text.trim());
        }
        return out;
    }

    private static List<String> hardSplit(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > start + maxChars / 2) {
                    end = space;
                }
            }
            out.add(text.substring(start, end).trim());
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        return out.stream().filter(s -> !s.isBlank()).toList();
    }

    private static void flush(StringBuilder current, List<String> pieces) {
        if (current.length() == 0) {
            return;
        }
        pieces.add(current.toString().trim());
        current.setLength(0);
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
