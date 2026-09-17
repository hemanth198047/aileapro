package com.aileapro.domain;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The reading half of the mastery rules, as a pure function.
 *
 * <p>It is pure so that the one rule this product cannot afford to get wrong is testable
 * without a database: <strong>consuming a page is not evidence of competence</strong>.
 * Reading can fill the Read chip and reach {@link MasteryState#INTRODUCED}, and that is the
 * end of what it can do. Practice, Assess, and Apply are left untouched here by
 * construction — this class has no code path that can raise them.
 *
 * <p>Dwell time is a weak signal and is treated like one: heartbeats are clamped, they only
 * arrive while the tab is visible, and the floor scales with the author's own estimate
 * rather than being a flat number that suits short units and flatters long ones.
 */
public final class MasteryPolicy {

    /** A heartbeat cannot claim more than two minutes, whatever the client says. */
    public static final int MAX_DWELL_SECONDS_PER_EVENT = 120;

    /** Fraction of the authored estimate that counts as having plausibly read the unit. */
    private static final double READ_FLOOR_FRACTION = 0.4;

    private static final int READ_FLOOR_MIN_SECONDS = 90;
    private static final int READ_FLOOR_MAX_SECONDS = 720;
    private static final int READ_FLOOR_DEFAULT_SECONDS = 180;

    /**
     * Ceiling on the internal score while reading is the only evidence. The composite in the
     * spec is mostly accuracy, retention, and application; none of those exist yet, so a
     * read-only record stays near the floor instead of impersonating progress.
     */
    public static final double READ_ONLY_SCORE_CEILING = 0.10;

    private MasteryPolicy() {
    }

    /**
     * What the rules need to know about the unit itself.
     *
     * @param estimateMinutes the author's reading estimate, or null when unstated
     * @param authoredLevels  how many explanation levels exist; zero for a container unit
     */
    public record UnitFacts(Integer estimateMinutes, int authoredLevels) {
    }

    public record Evidence(
            EvidenceState read, EvidenceState practice, EvidenceState assess, EvidenceState apply) {

        public static Evidence none() {
            return new Evidence(
                    EvidenceState.EMPTY, EvidenceState.EMPTY, EvidenceState.EMPTY, EvidenceState.EMPTY);
        }
    }

    /** The projection these rules read and rewrite. Immutable, like the rules themselves. */
    public record Snapshot(
            MasteryState state,
            Evidence evidence,
            int dwellSeconds,
            Set<String> levelsSeen,
            int openedCount,
            double score) {

        public static Snapshot empty() {
            return new Snapshot(MasteryState.UNKNOWN, Evidence.none(), 0, Set.of(), 0, 0);
        }
    }

    /**
     * @param dwellSeconds seconds claimed by a {@link LearningEventKind#DWELL} heartbeat
     * @param level        the explanation level code for {@link LearningEventKind#EXPLANATION_LEVEL}
     */
    public record Event(LearningEventKind kind, int dwellSeconds, String level) {

        public static Event of(LearningEventKind kind) {
            return new Event(kind, 0, null);
        }

        public static Event dwell(int seconds) {
            return new Event(LearningEventKind.DWELL, seconds, null);
        }

        public static Event level(String level) {
            return new Event(LearningEventKind.EXPLANATION_LEVEL, 0, level);
        }
    }

    public static Snapshot apply(Snapshot current, Event event, UnitFacts facts) {
        int dwell = current.dwellSeconds();
        Set<String> levels = new LinkedHashSet<>(current.levelsSeen());
        int opened = current.openedCount();

        switch (event.kind()) {
            case UNIT_OPENED -> opened = opened + 1;
            case DWELL -> dwell += clampDwell(event.dwellSeconds());
            case EXPLANATION_LEVEL -> {
                if (event.level() != null && !event.level().isBlank()) {
                    levels.add(event.level());
                }
            }
            // Revealing an answer and looking at exercises are recorded as history and credited
            // to nothing. Neither one demonstrates recall or production.
            case SELF_CHECK_REVEALED, EXERCISE_VIEWED -> {
            }
        }

        EvidenceState read = readState(opened, dwell, levels.size(), facts);
        Evidence evidence = new Evidence(
                // Only Read is derived here. The other three are carried through untouched,
                // because nothing in this class is allowed to raise them.
                maxOf(current.evidence().read(), read),
                current.evidence().practice(),
                current.evidence().assess(),
                current.evidence().apply());

        return new Snapshot(
                stateFor(current.state(), evidence),
                evidence,
                dwell,
                Set.copyOf(levels),
                opened,
                scoreFor(current.score(), evidence));
    }

    /** Seconds of visible reading that count as plausibly having read this unit. */
    public static int readFloorSeconds(UnitFacts facts) {
        if (facts.estimateMinutes() == null || facts.estimateMinutes() <= 0) {
            return READ_FLOOR_DEFAULT_SECONDS;
        }
        int scaled = (int) Math.round(facts.estimateMinutes() * 60 * READ_FLOOR_FRACTION);
        return Math.max(READ_FLOOR_MIN_SECONDS, Math.min(READ_FLOOR_MAX_SECONDS, scaled));
    }

    private static EvidenceState readState(int opened, int dwellSeconds, int levelsSeen, UnitFacts facts) {
        if (opened == 0) {
            return EvidenceState.EMPTY;
        }
        // A container unit has no explanation levels to choose, so opening it is all there is.
        boolean sawAnExplanation = facts.authoredLevels() == 0 || levelsSeen > 0;
        if (!sawAnExplanation) {
            return EvidenceState.PARTIAL;
        }
        return dwellSeconds >= readFloorSeconds(facts) ? EvidenceState.SOLID : EvidenceState.PARTIAL;
    }

    /**
     * States only move up, and reading can only ever reach the reading ceiling. Note that a
     * <em>partial</em> read earns nothing at all: the spec's INTRODUCED means an explanation was
     * read for a plausible length of time, so a bare page open leaves the state at UNKNOWN. A
     * record already sitting higher from graded evidence is never pulled down by a page view.
     */
    private static MasteryState stateFor(MasteryState current, Evidence evidence) {
        MasteryState earned = evidence.read() == EvidenceState.SOLID
                ? MasteryState.readingCeiling()
                : MasteryState.UNKNOWN;
        return current.atLeast(earned) ? current : earned;
    }

    private static double scoreFor(double current, Evidence evidence) {
        double read = switch (evidence.read()) {
            case EMPTY -> 0;
            case PARTIAL -> READ_ONLY_SCORE_CEILING / 2;
            case SOLID -> READ_ONLY_SCORE_CEILING;
        };
        return Math.max(current, read);
    }

    private static int clampDwell(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.min(seconds, MAX_DWELL_SECONDS_PER_EVENT);
    }

    private static EvidenceState maxOf(EvidenceState a, EvidenceState b) {
        return a.atLeast(b) ? a : b;
    }
}
