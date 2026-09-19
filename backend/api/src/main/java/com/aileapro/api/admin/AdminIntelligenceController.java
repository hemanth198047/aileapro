package com.aileapro.api.admin;

import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.intelligence.IntelligenceService;
import com.aileapro.api.intelligence.IntelligenceViews;
import com.aileapro.api.intelligence.LinkCheckService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
public class AdminIntelligenceController {

    private final IntelligenceService intelligence;
    private final LinkCheckService linkChecks;

    public AdminIntelligenceController(IntelligenceService intelligence, LinkCheckService linkChecks) {
        this.intelligence = intelligence;
        this.linkChecks = linkChecks;
    }

    @GetMapping("/updates")
    public List<IntelligenceViews.UpdateSummary> updates() {
        return intelligence.listUpdates();
    }

    @PostMapping("/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public IntelligenceViews.UpdateSummary createUpdate(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestBody IntelligenceViews.CreateUpdateBody body) {
        return intelligence.createUpdate(principal.getId(), body);
    }

    @GetMapping("/proposals")
    public List<IntelligenceViews.Proposal> proposals(@RequestParam(required = false) String status) {
        return intelligence.listProposals(status);
    }

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public IntelligenceViews.Proposal createProposal(@RequestBody IntelligenceViews.CreateProposalBody body) {
        return intelligence.createProposal(body);
    }

    @PostMapping("/proposals/{id}/reject")
    public IntelligenceViews.Proposal reject(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) IntelligenceViews.RejectBody body) {
        return intelligence.reject(principal.getId(), id, body == null
                ? new IntelligenceViews.RejectBody(null)
                : body);
    }

    @PostMapping("/proposals/{id}/approve")
    public IntelligenceViews.Proposal approve(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        return intelligence.approve(principal.getId(), id);
    }

    @PostMapping("/jobs/link-check")
    public IntelligenceViews.LinkCheckReport linkCheck(@RequestParam(required = false) Integer limit) {
        return linkChecks.checkAll(limit == null ? 20 : limit);
    }
}
