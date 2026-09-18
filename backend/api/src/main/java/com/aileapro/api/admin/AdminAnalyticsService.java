package com.aileapro.api.admin;

import com.aileapro.api.ai.AiUsageRepository;
import com.aileapro.api.assessment.DiagnosticPriorRepository;
import com.aileapro.api.learning.LearnerProfileRepository;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.api.UserRepository;
import com.aileapro.domain.MasteryState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

/**
 * Operator funnel + AI cost burn from existing tables. Costs are estimated from model prices.
 */
@Service
public class AdminAnalyticsService {

    public static final int DEFAULT_COST_DAYS = 30;

    private final UserRepository users;
    private final LearnerProfileRepository profiles;
    private final DiagnosticPriorRepository priors;
    private final MasteryRecordRepository mastery;
    private final AiUsageRepository usage;

    public AdminAnalyticsService(
            UserRepository users,
            LearnerProfileRepository profiles,
            DiagnosticPriorRepository priors,
            MasteryRecordRepository mastery,
            AiUsageRepository usage) {
        this.users = users;
        this.profiles = profiles;
        this.priors = priors;
        this.mastery = mastery;
        this.usage = usage;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsViews.Overview overview(Integer days) {
        return new AdminAnalyticsViews.Overview(funnel(), cost(days));
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsViews.Funnel funnel() {
        long registered = users.count();
        long onboarded = profiles.countOnboarded();
        long diagnostic = priors.countDistinctUsers();
        long practiced = mastery.countDistinctUsersWithStateIn(
                EnumSet.of(MasteryState.PRACTICED, MasteryState.PROFICIENT, MasteryState.MASTERED));
        return new AdminAnalyticsViews.Funnel(
                registered,
                onboarded,
                diagnostic,
                practiced,
                "Register → onboarded_at → diagnostic_priors → mastery_records at PRACTICED+. "
                        + "Self-reported experience never moves these counts.");
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsViews.CostBurn cost(Integer days) {
        int window = days == null || days < 1 ? DEFAULT_COST_DAYS : Math.min(days, 365);
        Instant since = Instant.now().minus(window, ChronoUnit.DAYS);
        BigDecimal total = usage.totalSpentSince(since);
        List<AdminAnalyticsViews.FeatureSpend> byFeature = usage.spendByFeatureSince(since).stream()
                .map(row -> new AdminAnalyticsViews.FeatureSpend(
                        (String) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();
        return new AdminAnalyticsViews.CostBurn(
                window,
                since,
                total,
                usage.tokensInSince(since),
                usage.tokensOutSince(since),
                usage.callCountSince(since),
                byFeature,
                "Estimated USD from model price tables in ai_usage — not an invoice reconciliation.");
    }
}
