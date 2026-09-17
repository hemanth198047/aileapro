package com.aileapro.ai;

/**
 * The named things a learner can ask for without typing a sentence.
 *
 * <p>These are buttons in the dock, and they exist because "I don't understand" is hard to say
 * usefully. Each one is a different request with a different shape of answer, and each routes
 * to the profile that can actually do it — asking a cheap chat model to review code is how a
 * product acquires a reputation for being confidently wrong.
 */
public enum TutorAction {

    /** Same idea, fewer moving parts. Not shorter: simpler. */
    SIMPLER("simpler", "teach"),

    /** The version for someone who already followed the first one. */
    DEEPER("deeper", "teach"),

    /** The formal statement, with notation defined rather than assumed. */
    MATH("math", "teach"),

    /** Show it as code that runs, not as pseudocode that cannot be checked. */
    CODE("code", "code"),

    /** A comparison to something the learner already has, with its limits stated. */
    ANALOGY("analogy", "teach"),

    /** Ask me questions instead of telling me things. */
    QUIZ_ME("quiz_me", "cheap_chat"),

    /** A harder case than the page covers, to find the edge of what I understand. */
    CHALLENGE_ME("challenge_me", "teach"),

    /** Debug or review what I wrote. */
    REVIEW_CODE("review_code", "code");

    private final String slug;
    private final String profile;

    TutorAction(String slug, String profile) {
        this.slug = slug;
        this.profile = profile;
    }

    public String slug() {
        return slug;
    }

    /** The routing profile this kind of request needs. */
    public String profile() {
        return profile;
    }

    public static TutorAction fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        for (TutorAction action : values()) {
            if (action.slug.equals(slug)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown tutor action: " + slug);
    }
}
