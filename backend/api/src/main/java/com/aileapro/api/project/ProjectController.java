package com.aileapro.api.project;

import com.aileapro.api.auth.AileaproUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    public record SubmitBody(@NotBlank String source) {
    }

    public record PortfolioPatch(String visibility, String reflection) {
    }

    @GetMapping("/projects")
    public List<ProjectViews.Summary> list(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return projects.list(principal.getId());
    }

    @GetMapping("/projects/{slug}")
    public ProjectViews.Detail get(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String slug) {
        return projects.get(principal.getId(), slug);
    }

    @PostMapping("/projects/{slug}/milestones/{milestoneSlug}/submit")
    public ProjectViews.SubmissionResult submit(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String slug,
            @PathVariable String milestoneSlug,
            @RequestBody SubmitBody body) {
        return projects.submit(principal.getId(), slug, milestoneSlug, body.source());
    }

    @GetMapping("/portfolio")
    public ProjectViews.Portfolio portfolio(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return projects.portfolio(principal.getId());
    }

    @PatchMapping("/portfolio/{id}")
    public ProjectViews.PortfolioItem patchPortfolio(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody PortfolioPatch body) {
        return projects.updatePortfolio(principal.getId(), id, body.visibility(), body.reflection());
    }
}
