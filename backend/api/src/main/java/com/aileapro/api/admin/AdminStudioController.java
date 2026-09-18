package com.aileapro.api.admin;

import com.aileapro.api.auth.AileaproUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Content studio. Requires ROLE_ADMIN (see SecurityConfig). Learners never hit these routes.
 */
@RestController
@RequestMapping("/v1/admin/units")
public class AdminStudioController {

    private final AdminStudioService studio;

    public AdminStudioController(AdminStudioService studio) {
        this.studio = studio;
    }

    @GetMapping
    public List<AdminViews.UnitSummary> list(@RequestParam(required = false) String status) {
        return studio.list(status);
    }

    @GetMapping("/{slug}")
    public AdminViews.UnitDetail get(@PathVariable String slug) {
        return studio.get(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminViews.UnitDetail create(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestBody AdminStudioService.CreateBody body) {
        return studio.create(principal.getId(), body);
    }

    @PatchMapping("/{slug}")
    public AdminViews.UnitDetail patch(
            @PathVariable String slug, @RequestBody AdminStudioService.PatchBody body) {
        return studio.patch(slug, body);
    }

    @GetMapping("/{slug}/critic")
    public ContentCritic.Report critic(@PathVariable String slug) {
        return studio.critic(slug);
    }

    @PostMapping("/{slug}/publish")
    public AdminViews.PublishResult publish(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String slug,
            @RequestBody(required = false) AdminStudioService.PublishBody body) {
        return studio.publish(principal.getId(), slug, body == null
                ? new AdminStudioService.PublishBody(null)
                : body);
    }

    @PostMapping("/{slug}/rollback")
    public AdminViews.RollbackResult rollback(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String slug) {
        return studio.rollback(principal.getId(), slug);
    }

    @GetMapping("/{slug}/changelog")
    public List<AdminViews.Change> changelog(@PathVariable String slug) {
        return studio.changelog(slug);
    }
}
