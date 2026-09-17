package com.aileapro.ai;

/**
 * What came back, including what it cost in tokens.
 *
 * <p>Token counts are reported by the provider where it reports them and estimated where it
 * does not, and {@code estimatedTokens} says which. A cost table that cannot distinguish a
 * measurement from a guess will be trusted as though every row were a measurement.
 *
 * @param finishReason provider's own word for why it stopped: {@code stop}, {@code length}
 */
public record ChatResult(
        String text,
        int tokensIn,
        int tokensOut,
        boolean estimatedTokens,
        String finishReason) {

    /** True when the answer was cut off by the output ceiling rather than finished. */
    public boolean truncated() {
        return "length".equals(finishReason);
    }
}
