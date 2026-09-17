package com.aileapro.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Marking, as a pure function from an authored key and a learner's response to a result.
 *
 * <p>Pure because marking is the one thing in an assessment engine that must be inspectable
 * and repeatable: the same response and the same key give the same mark, with no clock, no
 * database, and no model in the way.
 *
 * <p>Three rules are load-bearing and each one exists because the obvious alternative lies
 * to the learner:
 *
 * <ul>
 *   <li><strong>Multi-select earns partial credit.</strong> Marking "select all that apply"
 *       all-or-nothing throws away the difference between someone who found three of four
 *       and someone who guessed, and it teaches the learner to fear the item type rather
 *       than the material.
 *   <li><strong>Wrong selections cost.</strong> Otherwise ticking every box scores full
 *       marks, which makes the item worthless as evidence.
 *   <li><strong>Code is never marked wrong.</strong> Comparing source text cannot tell a
 *       different correct solution from an incorrect one. A mismatch is reported as unmarked,
 *       excluded from the score, and disclosed — not scored zero and called a failure.
 * </ul>
 */
public final class ItemScoring {

    private ItemScoring() {
    }

    /** How an answer came out. Distinct from a boolean because "unmarked" is a real state. */
    public enum Outcome {
        CORRECT,
        /** Some of the right choices, on a multi-select. Earns a fraction of the points. */
        PARTIAL,
        WRONG,
        /** Could not be marked automatically. Earns nothing and claims nothing. */
        UNMARKED,
        /** Nothing was submitted. Distinguished from wrong so reports can say "skipped". */
        SKIPPED;

        public boolean isCorrect() {
            return this == CORRECT;
        }

        /** Null for {@link #UNMARKED}, which is what a "we do not know" boolean has to be. */
        public Boolean correctFlag() {
            return this == UNMARKED ? null : this == CORRECT;
        }
    }

    /**
     * The authored answer key.
     *
     * @param type          decides which fields below are read
     * @param correctIds    choice ids that are correct, for choice items
     * @param accepted      accepted answers for short text and code, before normalisation
     * @param numericAnswer set when a short-text answer is a number, which is compared with
     *                      tolerance instead of by string, so "0.96" and ".96" both pass
     * @param tolerance     absolute tolerance for the numeric comparison
     */
    public record Key(
            ItemType type,
            Set<String> correctIds,
            List<String> accepted,
            Double numericAnswer,
            Double tolerance) {

        public static Key choice(ItemType type, String... correctIds) {
            return new Key(type, new LinkedHashSet<>(List.of(correctIds)), List.of(), null, null);
        }

        public static Key text(String... accepted) {
            return new Key(ItemType.SHORT_TEXT, Set.of(), List.of(accepted), null, null);
        }

        public static Key number(double answer, double tolerance) {
            return new Key(ItemType.SHORT_TEXT, Set.of(), List.of(), answer, tolerance);
        }

        public static Key code(String... accepted) {
            return new Key(ItemType.CODING_STUB, Set.of(), List.of(accepted), null, null);
        }
    }

    /**
     * What the learner submitted. One shape for every item type, because a response is
     * either a set of choices or a string, and pretending otherwise would need a type
     * hierarchy to carry two fields.
     */
    public record Response(Set<String> selectedIds, String text) {

        public static Response of(String... selectedIds) {
            return new Response(new LinkedHashSet<>(List.of(selectedIds)), null);
        }

        public static Response text(String text) {
            return new Response(Set.of(), text);
        }

        public static Response empty() {
            return new Response(Set.of(), null);
        }

        public boolean isEmpty() {
            return selectedIds.isEmpty() && (text == null || text.isBlank());
        }
    }

    /**
     * @param fraction     share of the item's points earned, 0–1
     * @param missedIds    correct choices the learner did not select, for the feedback line
     * @param wrongIds     incorrect choices the learner selected, which is what raises a
     *                     misconception flag when the author tagged that distractor
     */
    public record Result(Outcome outcome, double fraction, List<String> missedIds, List<String> wrongIds) {

        public boolean earnedAnything() {
            return fraction > 0;
        }
    }

    public static Result mark(Key key, Response response) {
        Objects.requireNonNull(key, "key");
        Response answer = response == null ? Response.empty() : response;

        if (answer.isEmpty()) {
            // A skipped item is not a wrong answer. It earns nothing either way, but the
            // report needs to be able to tell the learner which one happened.
            return new Result(Outcome.SKIPPED, 0, List.copyOf(key.correctIds()), List.of());
        }

        return switch (key.type()) {
            case MCQ, TRUE_FALSE -> markSingleChoice(key, answer);
            case MULTI_SELECT -> markMultiSelect(key, answer);
            case SHORT_TEXT -> markShortText(key, answer);
            case CODING_STUB -> markCode(key, answer);
        };
    }

    /**
     * More than one selection on a single-choice item is wrong rather than an error: the UI
     * cannot produce it, so a request that contains it is either a bug or a client doing its
     * own thing, and neither deserves credit.
     */
    private static Result markSingleChoice(Key key, Response answer) {
        if (answer.selectedIds().size() != 1) {
            return new Result(Outcome.WRONG, 0, List.copyOf(key.correctIds()), List.copyOf(answer.selectedIds()));
        }
        String chosen = answer.selectedIds().iterator().next();
        if (key.correctIds().contains(chosen)) {
            return new Result(Outcome.CORRECT, 1, List.of(), List.of());
        }
        return new Result(Outcome.WRONG, 0, List.copyOf(key.correctIds()), List.of(chosen));
    }

    /**
     * Partial credit as (right selections − wrong selections) / number of right answers,
     * floored at zero. Picking everything therefore scores nothing on a two-of-four item,
     * which is the property that makes the item mean something.
     */
    private static Result markMultiSelect(Key key, Response answer) {
        Set<String> correct = key.correctIds();
        List<String> hits = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        for (String selected : answer.selectedIds()) {
            if (correct.contains(selected)) {
                hits.add(selected);
            } else {
                wrong.add(selected);
            }
        }
        List<String> missed = correct.stream().filter(id -> !answer.selectedIds().contains(id)).toList();

        if (correct.isEmpty()) {
            // An authored item with no correct answer is a content bug, and marking it
            // generously would hide it.
            return new Result(Outcome.WRONG, 0, List.of(), wrong);
        }

        double raw = (hits.size() - wrong.size()) / (double) correct.size();
        double fraction = Math.max(0, raw);

        if (missed.isEmpty() && wrong.isEmpty()) {
            return new Result(Outcome.CORRECT, 1, List.of(), List.of());
        }
        if (fraction == 0) {
            return new Result(Outcome.WRONG, 0, missed, wrong);
        }
        return new Result(Outcome.PARTIAL, fraction, missed, wrong);
    }

    private static Result markShortText(Key key, Response answer) {
        String given = answer.text() == null ? "" : answer.text();

        if (key.numericAnswer() != null) {
            Double parsed = parseNumber(given);
            if (parsed == null) {
                return new Result(Outcome.WRONG, 0, List.of(), List.of());
            }
            double tolerance = key.tolerance() == null ? 0 : Math.abs(key.tolerance());
            boolean close = Math.abs(parsed - key.numericAnswer()) <= tolerance;
            return close
                    ? new Result(Outcome.CORRECT, 1, List.of(), List.of())
                    : new Result(Outcome.WRONG, 0, List.of(), List.of());
        }

        String normalised = normaliseText(given);
        for (String accepted : key.accepted()) {
            if (normaliseText(accepted).equals(normalised)) {
                return new Result(Outcome.CORRECT, 1, List.of(), List.of());
            }
        }
        return new Result(Outcome.WRONG, 0, List.of(), List.of());
    }

    /**
     * A match earns the points; anything else is unmarked. This is the whole reason the
     * {@link Outcome#UNMARKED} state exists.
     */
    private static Result markCode(Key key, Response answer) {
        String normalised = normaliseCode(answer.text() == null ? "" : answer.text());
        for (String accepted : key.accepted()) {
            if (normaliseCode(accepted).equals(normalised)) {
                return new Result(Outcome.CORRECT, 1, List.of(), List.of());
            }
        }
        return new Result(Outcome.UNMARKED, 0, List.of(), List.of());
    }

    /**
     * Case, surrounding whitespace, internal runs of whitespace, and trailing sentence
     * punctuation are all noise for a one-or-two word answer. Anything more aggressive than
     * this starts accepting answers the author did not mean.
     */
    static String normaliseText(String value) {
        String collapsed = value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return collapsed.replaceAll("[.!?]+$", "");
    }

    /**
     * Code is normalised on whitespace only. Indentation and blank lines are formatting;
     * identifiers and structure are the answer, and rewriting those to force a match would
     * turn an unmarked answer into a false pass.
     */
    static String normaliseCode(String value) {
        StringBuilder out = new StringBuilder();
        for (String line : value.strip().split("\r?\n")) {
            String trimmed = line.strip().replaceAll("\\s+", " ");
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(trimmed);
        }
        return out.toString();
    }

    private static Double parseNumber(String value) {
        String cleaned = value.strip().replace(",", "").replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
