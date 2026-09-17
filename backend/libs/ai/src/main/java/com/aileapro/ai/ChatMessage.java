package com.aileapro.ai;

/**
 * One message on the wire, with its channel.
 *
 * <p>The channel is the whole prompt-injection defence, and it is a type rather than a
 * convention so it cannot be forgotten. {@link Channel#SYSTEM} is ours. {@link Channel#USER}
 * is what the learner typed. {@link Channel#DATA} is retrieved or authored content that the
 * model may read and must never obey. Collapsing the last two into "the prompt" is how
 * injection works.
 */
public record ChatMessage(Role role, Channel channel, String text) {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT
    }

    public enum Channel {
        /** Our instructions. The only channel whose contents are instructions at all. */
        SYSTEM,
        /** The learner's own words. Trusted as intent, never as instruction. */
        USER,
        /** Unit text, retrieved passages, code output. Data to reason about, nothing more. */
        DATA
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(Role.SYSTEM, Channel.SYSTEM, text);
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(Role.USER, Channel.USER, text);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(Role.ASSISTANT, Channel.USER, text);
    }

    /**
     * Untrusted content, wrapped so the model can tell where it starts and stops.
     *
     * <p>Sent with the user role because providers only accept three, but fenced and labelled:
     * the system prompt tells the model that anything inside these tags is quoted material.
     */
    public static ChatMessage data(String label, String text) {
        return new ChatMessage(
                Role.USER,
                Channel.DATA,
                "<untrusted-data source=\"" + label + "\">\n" + text + "\n</untrusted-data>");
    }
}
