package com.aileapro.api.admin;

import com.aileapro.api.platform.FeatureFlagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/flags")
public class AdminFlagController {

    private final FeatureFlagService flags;

    public AdminFlagController(FeatureFlagService flags) {
        this.flags = flags;
    }

    public record SetBody(boolean enabled) {
    }

    @GetMapping
    public List<FeatureFlagService.FlagView> list() {
        return flags.all();
    }

    @PutMapping("/{code}")
    public FeatureFlagService.FlagView set(@PathVariable String code, @RequestBody SetBody body) {
        return flags.set(code, body.enabled());
    }
}
