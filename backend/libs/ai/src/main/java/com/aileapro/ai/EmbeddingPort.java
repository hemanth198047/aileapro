package com.aileapro.ai;

/**
 * Vendor-neutral embedding port.
 *
 * <p>Same rule as {@link ChatPort}: features never import a provider SDK. The fake
 * implementation is deterministic so CI and local retrieval tests never need a key.
 */
public interface EmbeddingPort {

    /** Provider slug recorded on each chunk, so a model change is visible later. */
    String providerSlug();

    /** Model slug recorded on each chunk. */
    String modelSlug();

    /** True when this is a development fixture rather than a real model. */
    boolean development();

    /** Fixed dimensionality of every vector this port returns. */
    int dimensions();

    /** One dense vector for the text. Always L2-normalised. */
    float[] embed(String text);
}
