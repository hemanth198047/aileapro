package com.aileapro.api.intelligence;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Learner-facing Evolve feed. Only published updates with verification metadata.
 */
@RestController
@RequestMapping("/v1/evolve")
public class EvolveController {

    private final IntelligenceService intelligence;

    public EvolveController(IntelligenceService intelligence) {
        this.intelligence = intelligence;
    }

    @GetMapping("/feed")
    public IntelligenceViews.Feed feed() {
        return intelligence.feed();
    }
}
