package com.aileapro.api.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Operator analytics views — funnel and estimated AI spend. */
public final class AdminAnalyticsViews {

    private AdminAnalyticsViews() {
    }

    public record Funnel(
            long registered,
            long onboarded,
            long diagnosticCompleted,
            long firstPracticed,
            String note) {
    }

    public record FeatureSpend(
            String feature,
            BigDecimal costUsd,
            long tokensIn,
            long tokensOut,
            long calls) {
    }

    public record CostBurn(
            int windowDays,
            Instant since,
            BigDecimal costUsd,
            long tokensIn,
            long tokensOut,
            long calls,
            List<FeatureSpend> byFeature,
            String note) {
    }

    public record Overview(Funnel funnel, CostBurn cost) {
    }
}
