package com.aileapro.ai;

import java.util.function.Consumer;

/**
 * The only way anything in this product talks to a language model.
 *
 * <p>One interface, two methods, no vendor types anywhere in the signature. That is what makes
 * "never couple features to one vendor" a structural property rather than a good intention: a
 * feature that wanted to depend on a particular provider would have to import it, and nothing
 * above this line is allowed to.
 *
 * <p>Implementations are expected to be slow and to fail. Callers get told which of those
 * happened — {@link ProviderException} for a provider that could not answer — and the caller
 * decides whether to fall back, because only the caller knows what the learner is waiting for.
 */
public interface ChatPort {

    /** How this port identifies itself in logs and usage rows. */
    String providerSlug();

    /**
     * True when answers come from fixtures rather than a model.
     *
     * <p>This is on the interface rather than left to configuration because the answer has to
     * reach the screen. A development reply displayed as teaching is worse than no reply.
     */
    boolean development();

    /** One completion, waited for. Used where there is nothing to stream to. */
    ChatResult complete(ChatRequest request) throws ProviderException;

    /**
     * A completion delivered as it is produced.
     *
     * <p>The consumer is called on the calling thread. Streaming exists because a learner
     * watching a blank panel for six seconds concludes the thing is broken, and tokens arriving
     * are the only honest progress indicator available.
     *
     * @param onChunk receives each piece of text in order; never called after this returns
     * @return the assembled result, including the token counts the meter needs
     */
    ChatResult stream(ChatRequest request, Consumer<String> onChunk) throws ProviderException;
}
