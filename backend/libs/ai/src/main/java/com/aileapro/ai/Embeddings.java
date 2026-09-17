package com.aileapro.ai;

/**
 * Vector helpers shared by the fake embedder and the in-memory index.
 */
public final class Embeddings {

    private Embeddings() {
    }

    public static float[] l2Normalize(float[] input) {
        double sum = 0;
        for (float v : input) {
            sum += (double) v * v;
        }
        if (sum == 0) {
            float[] out = input.clone();
            out[0] = 1f;
            return out;
        }
        float scale = (float) (1.0 / Math.sqrt(sum));
        float[] out = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            out[i] = input[i] * scale;
        }
        return out;
    }

    /** Cosine similarity for already-normalised vectors. */
    public static float cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("dimension mismatch: " + a.length + " vs " + b.length);
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return (float) sum;
    }

    /** Postgres {@code vector} literal: {@code [0.1,0.2,...]}. */
    public static String toPgVector(float[] values) {
        StringBuilder out = new StringBuilder(values.length * 8);
        out.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(values[i]);
        }
        out.append(']');
        return out.toString();
    }

    public static float[] fromPgVector(String literal) {
        String body = literal.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Float.parseFloat(parts[i].trim());
        }
        return out;
    }
}
