package com.aileapro.ai;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Deterministic development embedder.
 *
 * <p>Hashes each token into a fixed 64-dimensional bag-of-words vector and L2-normalises
 * it. Similar short texts land near each other often enough for retrieval tests; it is not
 * a substitute for a real model and is labelled as such on every chunk it writes.
 */
public final class FakeEmbeddingClient implements EmbeddingPort {

    public static final int DIMENSIONS = 64;
    public static final String MODEL_SLUG = "fake-embed-64";

    @Override
    public String providerSlug() {
        return "fake";
    }

    @Override
    public String modelSlug() {
        return MODEL_SLUG;
    }

    @Override
    public boolean development() {
        return true;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            vector[0] = 1f;
            return vector;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.isBlank() || token.length() < 2) {
                continue;
            }
            int hash = murmur(token);
            int index = Math.floorMod(hash, DIMENSIONS);
            float sign = (hash & 1) == 0 ? 1f : -1f;
            vector[index] += sign;
            // A second bucket keeps rare tokens from collapsing onto one axis.
            int secondary = Math.floorMod(hash >>> 8, DIMENSIONS);
            vector[secondary] += 0.5f * sign;
        }
        return Embeddings.l2Normalize(vector);
    }

    /** Small non-cryptographic hash; stable across JVMs. */
    private static int murmur(String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        int h = 0x9747b28c;
        for (byte b : bytes) {
            h ^= b;
            h *= 0x5bd1e995;
            h ^= h >>> 15;
        }
        return h;
    }
}
