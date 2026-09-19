package com.aileapro.api.ai;

import com.aileapro.api.platform.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * The daily ceiling on what one learner can spend.
 *
 * <p>Two ceilings, because one is not enough. The dollar cap is the one that matters on a real
 * provider. The turn cap is the one that matters everywhere else: the development provider is
 * free, so a cost-only budget would mean no limit at all in development and in CI — and a
 * runaway loop that costs nothing still fills a database and a learner's transcript.
 *
 * <p>Both are checked before the call and measured from {@code ai_usage}, so the budget is
 * enforced from what was actually metered rather than from a counter that can drift.
 */
@Service
@Transactional(readOnly = true)
public class AiBudget {

    private final AiUsageRepository usage;
    private final BigDecimal dailyUsdPerUser;
    private final int dailyTurnsPerUser;
    private final boolean enabled;

    public AiBudget(
            AiUsageRepository usage,
            @Value("${aileapro.ai.budget.daily-usd-per-user:0.50}") BigDecimal dailyUsdPerUser,
            @Value("${aileapro.ai.budget.daily-turns-per-user:120}") int dailyTurnsPerUser,
            @Value("${aileapro.ai.enabled:true}") boolean enabled) {
        this.usage = usage;
        this.dailyUsdPerUser = dailyUsdPerUser;
        this.dailyTurnsPerUser = dailyTurnsPerUser;
        this.enabled = enabled;
    }

    /** What is left today. Shown in the UI, so a learner is never surprised by a wall. */
    public record Remaining(BigDecimal usdSpent, BigDecimal usdCap, long turnsUsed, int turnsCap) {

        public boolean exhausted() {
            return (usdCap.signum() > 0 && usdSpent.compareTo(usdCap) >= 0)
                    || (turnsCap > 0 && turnsUsed >= turnsCap);
        }
    }

    public Remaining remaining(UUID userId) {
        Instant since = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal spent = usage.spentSince(userId, since);
        return new Remaining(
                spent == null ? BigDecimal.ZERO : spent,
                dailyUsdPerUser,
                usage.turnsSince(userId, since),
                dailyTurnsPerUser);
    }

    /**
     * Throws when AI is globally disabled or the learner has spent their day.
     */
    public void require(UUID userId) {
        if (!enabled) {
            throw new ApiException(
                    503,
                    "ai_disabled",
                    "AI features are temporarily disabled by an operator kill switch. "
                            + "Units, practice, and reviews still work without a model.");
        }
        Remaining remaining = remaining(userId);
        if (!remaining.exhausted()) {
            return;
        }
        String detail = remaining.usdCap().signum() > 0
                        && remaining.usdSpent().compareTo(remaining.usdCap()) >= 0
                ? "You have used today's tutor budget of $" + remaining.usdCap().toPlainString() + "."
                : "You have used today's limit of " + remaining.turnsCap() + " tutor turns.";
        throw new ApiException(
                429,
                "budget_exceeded",
                detail + " It resets at midnight UTC. The units, practice sets, and reviews are "
                        + "unaffected — none of them need a model.");
    }

    public boolean enabled() {
        return enabled;
    }
}
