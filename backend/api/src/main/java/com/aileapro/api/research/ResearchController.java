package com.aileapro.api.research;

import com.aileapro.api.auth.AileaproUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/research")
public class ResearchController {

    private final ResearchService research;

    public ResearchController(ResearchService research) {
        this.research = research;
    }

    public record ExplainBody(@NotBlank String question, String paperSlug) {
    }

    @GetMapping("/papers")
    public ResearchViews.ReadingList papers(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return research.readingList();
    }

    @GetMapping("/papers/{slug}")
    public ResearchViews.Paper paper(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String slug) {
        return research.getPaper(slug);
    }

    @PostMapping("/explain")
    public ResearchViews.Explainer explain(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @RequestBody ExplainBody body) {
        return research.explain(body.question(), body.paperSlug());
    }

    @GetMapping("/map")
    public ResearchViews.LiteratureMap map(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return research.literatureMap();
    }
}
