package com.aileapro.api.platform;

import com.aileapro.persistence.FeatureFlagEntity;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ensures launch flags exist when Flyway did not run (H2 tests) or a row was missing.
 */
@Component
public class FeatureFlagSeedRunner implements ApplicationListener<ApplicationReadyEvent> {

    private final FeatureFlagRepository flags;

    public FeatureFlagSeedRunner(FeatureFlagRepository flags) {
        this.flags = flags;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ensure(FeatureFlagService.LAB, true, "Learner Python lab runs");
        ensure(FeatureFlagService.TUTOR, true, "AI tutor conversations and turns");
        ensure(FeatureFlagService.CI_AGENTS, false, "Automated continuous-intelligence watchers");
    }

    private void ensure(String code, boolean enabled, String description) {
        if (flags.existsById(code)) {
            return;
        }
        FeatureFlagEntity row = new FeatureFlagEntity();
        row.setCode(code);
        row.setEnabled(enabled);
        row.setDescription(description);
        row.setUpdatedAt(Instant.now());
        flags.save(row);
    }
}
