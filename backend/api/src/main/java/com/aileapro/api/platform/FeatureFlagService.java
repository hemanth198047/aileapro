package com.aileapro.api.platform;

import com.aileapro.persistence.FeatureFlagEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureFlagService {

    public static final String LAB = "lab_enabled";
    public static final String TUTOR = "tutor_enabled";
    public static final String CI_AGENTS = "ci_agents";

    private final FeatureFlagRepository flags;

    public FeatureFlagService(FeatureFlagRepository flags) {
        this.flags = flags;
    }

    public record FlagView(String code, boolean enabled, String description) {
    }

    @Transactional(readOnly = true)
    public boolean enabled(String code) {
        return flags.findById(code).map(FeatureFlagEntity::isEnabled).orElse(true);
    }

    @Transactional(readOnly = true)
    public void require(String code, String message) {
        if (!enabled(code)) {
            throw ApiException.validation(message);
        }
    }

    @Transactional(readOnly = true)
    public List<FlagView> all() {
        return flags.findAll().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .map(f -> new FlagView(f.getCode(), f.isEnabled(), f.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> publicMap() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (FeatureFlagEntity flag : flags.findAll()) {
            map.put(flag.getCode(), flag.isEnabled());
        }
        // Defaults if migration not applied in an odd env
        map.putIfAbsent(LAB, true);
        map.putIfAbsent(TUTOR, true);
        map.putIfAbsent(CI_AGENTS, false);
        return map;
    }

    @Transactional
    public FlagView set(String code, boolean enabled) {
        FeatureFlagEntity flag = flags.findById(code)
                .orElseThrow(() -> ApiException.notFound("Unknown feature flag: " + code));
        flag.setEnabled(enabled);
        flag.setUpdatedAt(Instant.now());
        flags.save(flag);
        return new FlagView(flag.getCode(), flag.isEnabled(), flag.getDescription());
    }
}
