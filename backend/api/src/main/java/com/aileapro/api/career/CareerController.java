package com.aileapro.api.career;

import com.aileapro.api.auth.AileaproUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me/career")
public class CareerController {

    private final CareerService career;

    public CareerController(CareerService career) {
        this.career = career;
    }

    /**
     * Skill gaps against the target path. Only demonstrated mastery counts — never
     * onboarding self-report.
     *
     * @param path optional path slug; defaults to the learner's target path
     */
    @GetMapping("/gaps")
    public CareerViews.Gaps gaps(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestParam(required = false) String path) {
        return career.gaps(principal.getId(), path);
    }
}
