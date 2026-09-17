package com.aileapro.domain;

import java.time.LocalDate;

/**
 * The spaced-repetition schedule: an SM-2 variant, as a pure function.
 *
 * <p>SM-2 is the right starting point because it is simple, published, and predictable, and
 * because a learner can be told what it is doing. The variations here are all in one
 * direction — refusing to let the schedule become a treadmill:
 *
 * <ul>
 *   <li><strong>Intervals are capped.</strong> Beyond a year the schedule is guessing, and a
 *       card that says "next due in 2031" is not a plan.
 *   <li><strong>Lapses flag a leech instead of resetting forever.</strong> A card failed
 *       repeatedly is a teaching problem, not a memory problem: 07 says reteach, not
 *       infinite cram, so the card is flagged and taken out of the rotation.
 *   <li><strong>Volatility shortens intervals.</strong> Remembering a framework's current
 *       API for a year is not worth scheduling for, because the answer will have changed.
 * </ul>
 *
 * <p>Grades follow SM-2: 0–2 is a failure, 3 is a correct answer that was hard work, 4 is
 * correct, 5 is instant.
 */
public final class ReviewScheduler {

    private ReviewScheduler() {
    }

    public static final int MIN_GRADE = 0;
    public static final int MAX_GRADE = 5;

    /** Below this the answer was not recalled, whatever the learner felt about it. */
    public static final int PASSING_GRADE = 3;

    public static final double DEFAULT_EASE = 2.5;
    public static final double MIN_EASE = 1.3;
    public static final double MAX_EASE = 3.0;

    /** First and second successful intervals, in days, before ease takes over. */
    private static final int FIRST_INTERVAL = 1;
    private static final int SECOND_INTERVAL = 6;

    /** A year is as far ahead as this schedule is willing to claim anything. */
    public static final int MAX_INTERVAL_DAYS = 365;

    /** Failures before the card is treated as a teaching failure rather than a memory one. */
    public static final int LEECH_LAPSES = 4;

    /**
     * @param intervalDays days until due, from the last review
     * @param leech        true once the card has lapsed too often to keep drilling
     */
    public record State(double ease, int intervalDays, int reps, int lapses, boolean leech, LocalDate due) {

        /** A new card is due today: there is no reason to make someone wait to start. */
        public static State newCard(LocalDate today) {
            return new State(DEFAULT_EASE, 0, 0, 0, false, today);
        }
    }

    /**
     * Applies one graded answer.
     *
     * @param today      the day the answer was given, which is also the day intervals count
     *                   from — scheduling from the due date would punish answering late
     * @param volatility the unit's volatility, which caps how far ahead a card can be pushed
     */
    public static State apply(State current, int grade, LocalDate today, Volatility volatility) {
        int clamped = Math.max(MIN_GRADE, Math.min(MAX_GRADE, grade));

        if (clamped < PASSING_GRADE) {
            return lapse(current, today);
        }

        double ease = nextEase(current.ease(), clamped);
        int reps = current.reps() + 1;
        int interval = nextInterval(current, reps, ease, volatility);
        return new State(ease, interval, reps, current.lapses(), current.leech(), today.plusDays(interval));
    }

    /**
     * A failure returns the card to the start of the ladder but keeps its ease history, so a
     * card that was easy before one bad day is not treated as brand new. Enough failures and
     * it stops being scheduled at all.
     */
    private static State lapse(State current, LocalDate today) {
        int lapses = current.lapses() + 1;
        boolean leech = lapses >= LEECH_LAPSES;
        double ease = clampEase(current.ease() - 0.20);
        return new State(ease, leech ? 0 : FIRST_INTERVAL, 0, lapses, leech, today.plusDays(leech ? 0 : FIRST_INTERVAL));
    }

    /** The SM-2 ease adjustment, which rewards effortless recall and punishes struggle. */
    private static double nextEase(double ease, int grade) {
        double adjusted = ease + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02));
        return clampEase(adjusted);
    }

    private static int nextInterval(State current, int reps, double ease, Volatility volatility) {
        int interval = switch (reps) {
            case 1 -> FIRST_INTERVAL;
            case 2 -> SECOND_INTERVAL;
            default -> (int) Math.round(Math.max(current.intervalDays(), 1) * ease);
        };
        return Math.max(FIRST_INTERVAL, Math.min(cap(volatility), interval));
    }

    /**
     * How far ahead a card of this kind may be scheduled. A fast-moving subject is capped
     * tightly because the material will have changed before the interval elapses, and
     * drilling a stale answer is worse than not drilling.
     */
    public static int cap(Volatility volatility) {
        return switch (volatility) {
            case LOW -> MAX_INTERVAL_DAYS;
            case MEDIUM -> 120;
            case HIGH -> 45;
        };
    }

    public static boolean isPass(int grade) {
        return grade >= PASSING_GRADE;
    }

    private static double clampEase(double ease) {
        return Math.max(MIN_EASE, Math.min(MAX_EASE, ease));
    }
}
