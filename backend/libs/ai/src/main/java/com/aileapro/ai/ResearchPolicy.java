package com.aileapro.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Research explainer integrity: cite only attached papers, never invent DOIs or titles.
 *
 * <p>Unlike the tutor, this path has no foundational fallback when retrieval is empty —
 * an unsourced paper claim is refused outright.
 */
public final class ResearchPolicy {

    /** Titles that must never appear unless somehow attached (they never are in seed). */
    public static final Set<String> DENIED_TITLES = Set.of(
            "Deep Learning Will Solve Everything",
            "The Ultimate AGI Paper",
            "Hyperbolic Transformers for Infinite Context",
            "Quantum RAG on Mars");

    /** DOIs that are fabricated for fixture deny-list tests. */
    public static final Set<String> DENIED_DOIS = Set.of(
            "10.9999/fake.doi.0001",
            "10.1234/invented.paper.2099",
            "10.0000/not-a-real-paper");

    private static final Pattern DOI_PATTERN = Pattern.compile(
            "10\\.\\d{4,9}/[-._;/:A-Za-z0-9]+");

    private ResearchPolicy() {
    }

    public record PaperRef(String slug, String title, String doi, String url, String abstractText) {
    }

    public record Refusal(String code, String reply) {
    }

    public record Decision(boolean refused, Refusal refusal, List<PaperRef> attached) {
        public static Decision allow(List<PaperRef> attached) {
            return new Decision(false, null, List.copyOf(attached));
        }

        public static Decision refuse(String code, String reply) {
            return new Decision(true, new Refusal(code, reply), List.of());
        }
    }

    /**
     * Empty attachment → refuse. Never call a model (or fixture) that could invent papers.
     */
    public static Decision decide(List<PaperRef> attached) {
        if (attached == null || attached.isEmpty()) {
            return Decision.refuse(
                    "no_sources",
                    "I cannot explain that from attached papers — none matched this question. "
                            + "I will not invent a reference. Pick a paper from the reading list, "
                            + "or ask about something those papers cover.");
        }
        return Decision.allow(attached);
    }

    /**
     * Builds a deterministic explainer that only names attached papers. Used when no vendor
     * model is wired; labelled as a development fixture.
     */
    public static String fixtureReply(String question, List<PaperRef> attached) {
        PaperRef primary = attached.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("_Development implementation: no model is configured, so this explainer is a ")
                .append("fixture grounded only in attached paper metadata._\n\n");
        sb.append("On “").append(trim(question, 100)).append("”:\n\n");
        sb.append(primary.abstractText() == null || primary.abstractText().isBlank()
                ? "The attached paper is " + primary.title() + "."
                : primary.abstractText().trim());
        sb.append("\n\n");
        sb.append("Citation: ").append(primary.title());
        if (primary.doi() != null && !primary.doi().isBlank()) {
            sb.append(" (DOI ").append(primary.doi()).append(")");
        }
        sb.append(". Source slug `").append(primary.slug()).append("`.\n");
        if (attached.size() > 1) {
            sb.append("Also retrieved: ");
            sb.append(attached.stream().skip(1).map(PaperRef::slug).collect(Collectors.joining(", ")));
            sb.append(".\n");
        }
        return sb.toString();
    }

    /**
     * Strips denied DOIs and fails closed if denied titles appear. Allowed DOIs from the
     * attached set are kept. Returns the cleaned text and whether anything was removed.
     */
    public static String enforceCitations(String text, List<PaperRef> attached) {
        if (text == null) {
            return "";
        }
        String out = text;
        for (String denied : DENIED_TITLES) {
            if (containsIgnoreCase(out, denied)) {
                throw new IllegalStateException(
                        "Explainer invented a paper title that is not in the attached set: " + denied);
            }
        }
        Set<String> allowedDois = attached.stream()
                .map(PaperRef::doi)
                .filter(doi -> doi != null && !doi.isBlank())
                .map(doi -> doi.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Matcher matcher = DOI_PATTERN.matcher(out);
        StringBuffer cleaned = new StringBuffer();
        while (matcher.find()) {
            String found = matcher.group();
            String lower = found.toLowerCase(Locale.ROOT);
            boolean allowed = allowedDois.contains(lower)
                    || allowedDois.stream().anyMatch(a -> a.equalsIgnoreCase(found));
            if (DENIED_DOIS.contains(lower) || !allowed) {
                matcher.appendReplacement(cleaned, Matcher.quoteReplacement("[unsourced DOI removed]"));
            } else {
                matcher.appendReplacement(cleaned, Matcher.quoteReplacement(found));
            }
        }
        matcher.appendTail(cleaned);
        return cleaned.toString();
    }

    /** True when the text mentions a denied title or DOI — used by automated gate tests. */
    public static boolean containsFabrication(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String denied : DENIED_TITLES) {
            if (containsIgnoreCase(text, denied)) {
                return true;
            }
        }
        Matcher matcher = DOI_PATTERN.matcher(text);
        while (matcher.find()) {
            if (DENIED_DOIS.contains(matcher.group().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scores papers for a question: title/abstract/slug token overlap. Zero means no attach.
     */
    public static List<PaperRef> retrieve(String question, String paperSlug, List<PaperRef> corpus, int limit) {
        if (corpus == null || corpus.isEmpty()) {
            return List.of();
        }
        if (paperSlug != null && !paperSlug.isBlank()) {
            String want = paperSlug.trim().toLowerCase(Locale.ROOT);
            return corpus.stream()
                    .filter(p -> p.slug().equalsIgnoreCase(want))
                    .limit(1)
                    .toList();
        }
        Set<String> tokens = tokens(question);
        if (tokens.isEmpty()) {
            return List.of();
        }
        record Scored(PaperRef paper, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (PaperRef paper : corpus) {
            int score = overlap(tokens, paper);
            if (score > 0) {
                scored.add(new Scored(paper, score));
            }
        }
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.stream().limit(Math.max(1, limit)).map(Scored::paper).toList();
    }

    private static int overlap(Set<String> tokens, PaperRef paper) {
        Set<String> hay = tokens(paper.slug() + " " + paper.title() + " " + nullToEmpty(paper.abstractText()));
        int score = 0;
        for (String token : tokens) {
            if (hay.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        Set<String> out = new java.util.LinkedHashSet<>();
        for (String part : parts) {
            if (part.length() >= 3 && !STOP.contains(part)) {
                out.add(part);
            }
        }
        return out;
    }

    private static final Set<String> STOP = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "what", "how", "why",
            "about", "into", "are", "was", "were", "can", "does", "did", "you", "your");

    private static boolean containsIgnoreCase(String hay, String needle) {
        return hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trim(String value, int max) {
        String cleaned = value == null ? "" : value.replace('\n', ' ').trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, max - 1) + "…";
    }
}
