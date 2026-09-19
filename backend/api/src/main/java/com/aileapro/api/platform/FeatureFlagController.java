package com.aileapro.api.platform;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public feature flags for the web shell (authenticated). */
@RestController
@RequestMapping("/v1/flags")
public class FeatureFlagController {

    private final FeatureFlagService flags;

    public FeatureFlagController(FeatureFlagService flags) {
        this.flags = flags;
    }

    @GetMapping
    public Map<String, Boolean> flags() {
        return flags.publicMap();
    }
}
