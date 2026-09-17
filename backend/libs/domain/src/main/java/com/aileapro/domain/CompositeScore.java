package com.aileapro.domain;

/**
 * The internal 0–1 composite from 07-MASTERY_AND_ADAPTIVE_LEARNING, used for ranking and
 * diagnostics and <strong>never</strong> shown as the product's headline number.
 *
 * <pre>
 * S = w1·accuracy + w2·consistency + w3·retention + w4·application
 *   + w5·prerequisite_health + w6·recency − w7·misconception_penalty
 * </pre>
 *
 * <p>Two things about this are worth stating plainly rather than discovering later.
 *
 * <p>First, the positive weights sum to 1, but the terms that can currently be earned do
 * not: application is always zero until the lab and projects exist, and retention only moves
 * once cards have come due. A learner who has done everything available today tops out well
 * below 1. That is correct — the ceiling is low because the evidence is thin, and inflating
 * the number to make the bar look full would be the exact failure this design exists to
 * avoid. {@link #reachableCeiling} exposes the ceiling so the UI can explain it.
 *
 * <p>Second, the misconception penalty can drive the result to zero on its own. A learner
 * who answers everything correctly while holding a named false belief is not 90% of the way
 * there, and the ranking should send them to the reteach block rather than onward.
 */
public final class CompositeScore {

    private CompositeScore() {
    }

    public static final double W_ACCURACY = 0.22;
    public static final double W_CONSISTENCY = 0.15;
    public static final double W_RETENTION = 0.18;
    public static final double W_APPLICATION = 0.20;
    public static final double W_PREREQUISITE_HEALTH = 0.15;
    public static final double W_RECENCY = 0.10;
    public static final double W_MISCONCEPTION = 0.25;

    /** Config version, so a stored score can be read against the rules that produced it. */
    public static final String WEIGHTS_VERSION = "1.0.0";

    /**
     * @param accuracy            difficulty-weighted moving accuracy, 0–1
     * @param consistency         spread of evidence across days, 0–1
     * @param retention           success on cards answered at or after their due date, 0–1
     * @param application         produced work: labs, projects, teach-backs. Zero for now.
     * @param prerequisiteHealth  the weakest prerequisite's score, since a chain is as strong
     *                            as its worst link rather than as its average
     * @param recency             how fresh the evidence is, judged against the unit's own
     *                            volatility: a foundation decays slowly, an API does not
     * @param misconceptionLoad   0–1 measure of open named false beliefs
     */
    public record Signals(
            double accuracy,
            double consistency,
            double retention,
            double application,
            double prerequisiteHealth,
            double recency,
            double misconceptionLoad) {

        public static Signals none() {
            return new Signals(0, 0, 0, 0, 0, 0, 0);
        }
    }

    public static double of(Signals signals) {
        double positive = W_ACCURACY * clamp(signals.accuracy())
                + W_CONSISTENCY * clamp(signals.consistency())
                + W_RETENTION * clamp(signals.retention())
                + W_APPLICATION * clamp(signals.application())
                + W_PREREQUISITE_HEALTH * clamp(signals.prerequisiteHealth())
                + W_RECENCY * clamp(signals.recency());
        return clamp(positive - W_MISCONCEPTION * clamp(signals.misconceptionLoad()));
    }

    /**
     * The highest score currently obtainable, given that application cannot be demonstrated
     * and retention needs time to pass. Exposed so the dashboard can say "0.58 of a possible
     * 0.80" instead of implying a learner is failing at something unavailable.
     */
    public static double reachableCeiling(boolean applicationAvailable, boolean retentionAvailable) {
        double ceiling = W_ACCURACY + W_CONSISTENCY + W_PREREQUISITE_HEALTH + W_RECENCY;
        if (retentionAvailable) {
            ceiling += W_RETENTION;
        }
        if (applicationAvailable) {
            ceiling += W_APPLICATION;
        }
        return ceiling;
    }

    /** Consistency as days of evidence against the target, so two days is full marks. */
    public static double consistencyFrom(int gradedDays) {
        if (gradedDays <= 0) {
            return 0;
        }
        return clamp(gradedDays / (double) GradedEvidencePolicy.CONSISTENCY_DAYS);
    }

    /** Retention from due-date recalls only. No due cards means no claim either way. */
    public static double retentionFrom(int duePassed, int dueFailed) {
        int total = duePassed + dueFailed;
        if (total == 0) {
            return 0;
        }
        return clamp(duePassed / (double) total);
    }

    /**
     * Misconception load. The first open flag is most of the penalty: holding one named
     * false belief about a unit is qualitatively different from holding none, and the
     * difference between two and three matters much less.
     */
    public static double misconceptionLoadFrom(int openFlags) {
        if (openFlags <= 0) {
            return 0;
        }
        return switch (openFlags) {
            case 1 -> 0.70;
            case 2 -> 0.90;
            default -> 1.0;
        };
    }

    /**
     * Recency, judged against how fast the unit's subject moves. Evidence about matrix
     * multiplication does not go stale; evidence about a framework's current API does.
     *
     * @param daysSinceEvidence days since the last graded evidence, or null if there is none
     */
    public static double recencyFrom(Integer daysSinceEvidence, Volatility volatility) {
        if (daysSinceEvidence == null) {
            return 0;
        }
        int halfLife = switch (volatility) {
            case LOW -> 365;
            case MEDIUM -> 120;
            case HIGH -> 45;
        };
        double decayed = Math.pow(0.5, Math.max(0, daysSinceEvidence) / (double) halfLife);
        return clamp(decayed);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
