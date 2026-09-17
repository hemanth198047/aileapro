package com.aileapro.api.ai.seed;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The shape of the authored AI configuration.
 *
 * <p>Unknown fields are rejected rather than ignored: a typo in a price or a model slug would
 * otherwise be silently dropped and only noticed on an invoice.
 */
public final class AiSeedModel {

    private AiSeedModel() {
    }

    public record Catalogue(
            List<SeedProvider> providers,
            List<SeedModel> models,
            List<SeedProfile> profiles) {
    }

    public record SeedProvider(
            String slug,
            String title,
            String apiKind,
            String baseUrl,
            String apiKeyEnv) {
    }

    public record SeedModel(
            String slug,
            String provider,
            String wireName,
            List<String> capabilities,
            Integer contextTokens,
            Integer maxOutputTokens,
            BigDecimal priceInPerMtok,
            BigDecimal priceOutPerMtok) {
    }

    public record SeedProfile(
            String slug,
            String title,
            String purpose,
            Map<String, Object> params,
            List<String> models) {
    }

    public record PromptFile(List<SeedPrompt> prompts) {
    }

    public record SeedPrompt(String name, String purpose, String owner, List<SeedPromptVersion> versions) {
    }

    public record SeedPromptVersion(Integer version, String template, String notes) {
    }
}
