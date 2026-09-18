package com.aileapro.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator analytics. Requires ROLE_ADMIN via SecurityConfig /v1/admin/**.
 */
@RestController
@RequestMapping("/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analytics;

    public AdminAnalyticsController(AdminAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping
    public AdminAnalyticsViews.Overview overview(@RequestParam(required = false) Integer days) {
        return analytics.overview(days);
    }

    @GetMapping("/funnel")
    public AdminAnalyticsViews.Funnel funnel() {
        return analytics.funnel();
    }

    @GetMapping("/cost")
    public AdminAnalyticsViews.CostBurn cost(@RequestParam(required = false) Integer days) {
        return analytics.cost(days);
    }
}
