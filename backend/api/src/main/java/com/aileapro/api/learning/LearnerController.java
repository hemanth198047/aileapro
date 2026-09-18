package com.aileapro.api.learning;

import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.learning.LearnerProfileService.ProfileUpdate;
import com.aileapro.api.learning.LearningViews.Dashboard;
import com.aileapro.api.learning.LearningViews.NextAction;
import com.aileapro.api.learning.LearningViews.Note;
import com.aileapro.api.learning.LearningViews.Profile;
import com.aileapro.api.learning.LearningViews.Roadmap;
import com.aileapro.api.learning.LearningViews.Today;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Everything scoped to "me": the stated goal, the recommendation, the roadmap, the notes.
 *
 * <p>The user is always taken from the session principal. No route here accepts a user id,
 * so there is no shape of request that could read somebody else's learning.
 */
@RestController
@RequestMapping("/v1/me")
public class LearnerController {

    private final LearnerProfileService profiles;
    private final RecommendationService recommendations;
    private final NoteService notes;
    private final DashboardService dashboards;

    public LearnerController(
            LearnerProfileService profiles,
            RecommendationService recommendations,
            NoteService notes,
            DashboardService dashboards) {
        this.profiles = profiles;
        this.recommendations = recommendations;
        this.notes = notes;
        this.dashboards = dashboards;
    }

    /** Null until onboarding has been through once, which the client treats as "not set up". */
    @GetMapping("/profile")
    public Profile profile(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return profiles.profile(principal.getId()).orElse(null);
    }

    @PutMapping("/profile")
    public Profile saveProfile(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @RequestBody ProfileUpdate update) {
        return profiles.save(principal.getId(), update);
    }

    @GetMapping("/next")
    public NextAction next(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return recommendations.next(principal.getId());
    }

    @GetMapping("/today")
    public Today today(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return recommendations.today(principal.getId());
    }

    /** Evidence-backed analytics for the dashboard — dwell, weak list, velocity from tables. */
    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return dashboards.dashboard(principal.getId());
    }

    /** @param path optional; defaults to the learner's target path */
    @GetMapping("/roadmap")
    public Roadmap roadmap(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestParam(required = false) String path) {
        return recommendations.roadmap(principal.getId(), path);
    }

    @GetMapping("/notes")
    public List<Note> notes(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return notes.all(principal.getId());
    }
}
