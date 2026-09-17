package com.aileapro.ai;

/**
 * A provider could not answer.
 *
 * <p>Checked, because the caller has a decision to make: try the next model in the chain, or
 * tell the learner the tutor is unavailable and leave the authored content standing. Making it
 * unchecked would let that decision be skipped by accident.
 */
public class ProviderException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean retryable;

    public ProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public ProviderException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * Whether trying again could plausibly work.
     *
     * <p>A timeout or a 503 is retryable. A malformed request or a rejected key is not, and
     * retrying it just spends the learner's wait on the same failure three times.
     */
    public boolean retryable() {
        return retryable;
    }
}
