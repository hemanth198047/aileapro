package com.aileapro.api.lab;

import com.aileapro.api.auth.AileaproUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/lab")
public class LabController {

    private final LabService lab;

    public LabController(LabService lab) {
        this.lab = lab;
    }

    public record StartBody(
            String language,
            @NotBlank String source,
            String stdin,
            Integer timeoutMs) {
    }

    @GetMapping("/status")
    public LabViews.Status status(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return lab.status(principal.getId());
    }

    @GetMapping("/runs")
    public LabViews.History history(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return lab.history(principal.getId());
    }

    @PostMapping("/runs")
    public LabViews.Run start(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @RequestBody StartBody body) {
        return lab.start(principal.getId(), body.language(), body.source(), body.stdin(), body.timeoutMs());
    }

    @GetMapping("/runs/{id}")
    public LabViews.Run get(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        return lab.get(principal.getId(), id);
    }

    @PostMapping("/runs/{id}/cancel")
    public LabViews.Run cancel(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        return lab.cancel(principal.getId(), id);
    }
}
