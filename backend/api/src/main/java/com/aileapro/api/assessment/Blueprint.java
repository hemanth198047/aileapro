package com.aileapro.api.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * The typed form of an assessment's authored blueprint.
 *
 * <p>Only the diagnostic uses one: a fixed quiz already knows its items, while the
 * diagnostic chooses as it goes. Parsing it into a record rather than reading the JSON at
 * each decision keeps the stop rules in one place, where they can be read as a set of
 * intentions instead of being rediscovered from field lookups.
 */
public record Blueprint(int maxItems, StopRules stopRules, List<Domain> domains) {

    /**
     * When to stop probing.
     *
     * @param minPerDomain    floor below which an estimate is noise rather than a guess
     * @param maxPerDomain    ceiling past which we are measuring, not placing
     * @param stopAfterWrong  consecutive wrong answers that end a domain, because the
     *                        ceiling is clearly below it and continuing only demoralises
     * @param stopAfterCorrect consecutive correct answers that end a domain, because the
     *                        remaining budget is better spent where the answer is unknown
     */
    public record StopRules(int minPerDomain, int maxPerDomain, int stopAfterWrong, int stopAfterCorrect) {

        public static StopRules defaults() {
            return new StopRules(2, 4, 2, 3);
        }
    }

    /** One domain to place the learner in, with its item ladder ordered easiest first. */
    public record Domain(String unit, List<String> items) {
    }

    public static Blueprint empty() {
        return new Blueprint(0, StopRules.defaults(), List.of());
    }

    public boolean isAdaptive() {
        return !domains.isEmpty();
    }

    /** Every item slug the blueprint can serve, in domain order. */
    public List<String> allItemSlugs() {
        List<String> slugs = new ArrayList<>();
        domains.forEach(domain -> slugs.addAll(domain.items()));
        return slugs;
    }

    public static Blueprint parse(String json, ObjectMapper mapper) {
        JsonNode root;
        try {
            root = mapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unreadable assessment blueprint", e);
        }
        if (!root.hasNonNull("domains")) {
            return empty();
        }

        JsonNode rules = root.get("stopRules");
        StopRules defaults = StopRules.defaults();
        StopRules stopRules = rules == null
                ? defaults
                : new StopRules(
                        rules.path("minPerDomain").asInt(defaults.minPerDomain()),
                        rules.path("maxPerDomain").asInt(defaults.maxPerDomain()),
                        rules.path("stopAfterWrong").asInt(defaults.stopAfterWrong()),
                        rules.path("stopAfterCorrect").asInt(defaults.stopAfterCorrect()));

        List<Domain> domains = new ArrayList<>();
        for (JsonNode domain : root.get("domains")) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : domain.path("items")) {
                items.add(item.asText());
            }
            domains.add(new Domain(domain.path("unit").asText(), items));
        }

        int maxItems = root.path("maxItems").asInt(0);
        return new Blueprint(maxItems, stopRules, List.copyOf(domains));
    }
}
