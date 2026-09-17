package com.aileapro.ai;

/**
 * Tone and rigour. Same tools, same safety rules, same refusals.
 *
 * <p>A persona is allowed to change how something is said and how hard it pushes. It is not
 * allowed to change what is true, what gets refused, or whether hints come before answers —
 * otherwise "strict examiner" becomes a way to get the answer key by asking in a costume.
 */
public enum Persona {

    PATIENT_TEACHER(
            "patient_teacher",
            "Patient teacher",
            "Warm and unhurried. Assumes the learner is capable and the explanation was at "
                    + "fault. Checks understanding before moving on."),

    STRICT_EXAMINER(
            "strict_examiner",
            "Strict examiner",
            "Terse and demanding. Asks for precision, names imprecision as imprecision, and "
                    + "does not accept an answer that only sounds right."),

    CODING_MENTOR(
            "coding_mentor",
            "Coding mentor",
            "Talks in code and tradeoffs. Reads what the learner wrote before suggesting "
                    + "anything, and explains why a change matters rather than just making it.");

    private final String slug;
    private final String title;
    private final String instruction;

    Persona(String slug, String title, String instruction) {
        this.slug = slug;
        this.title = title;
        this.instruction = instruction;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    /** The line that goes into the system prompt. Tone only. */
    public String instruction() {
        return instruction;
    }

    public static Persona fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return PATIENT_TEACHER;
        }
        for (Persona persona : values()) {
            if (persona.slug.equals(slug)) {
                return persona;
            }
        }
        throw new IllegalArgumentException("Unknown persona: " + slug);
    }
}
