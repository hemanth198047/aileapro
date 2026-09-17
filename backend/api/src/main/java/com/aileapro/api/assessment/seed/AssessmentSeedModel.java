package com.aileapro.api.assessment.seed;

import java.util.List;
import java.util.Map;

/**
 * Shape of the authored files in {@code resources/seed/assessment}.
 *
 * <p>Items live in their own files rather than inside the unit files because an item is
 * reusable: the same question can appear in a practice set, a unit quiz, and the diagnostic.
 * Which of those it is is decided by the assessment that contains it, never by the item.
 */
public final class AssessmentSeedModel {

    private AssessmentSeedModel() {
    }

    public record SeedFile(
            List<SeedItem> items, List<SeedAssessment> assessments, List<SeedCard> cards) {
    }

    /**
     * One authored item.
     *
     * @param unit          slug of the page this item is asking about
     * @param type          {@code mcq}, {@code multi_select}, {@code true_false},
     *                      {@code short_text}, or {@code coding_stub}
     * @param bloom         what the item actually demands; recall items may not claim
     *                      understanding, which is why this is required rather than defaulted
     * @param choices       options for the choice types, each with an {@code id} and
     *                      {@code text}, and {@code correct: true} on the key
     * @param accepted      accepted strings for {@code short_text} and {@code coding_stub}
     * @param answer        numeric answer for {@code short_text}, with {@code tolerance}
     * @param misconceptions codes this item detects, which must exist in the catalog
     */
    public record SeedItem(
            String slug,
            String unit,
            String type,
            Short difficulty,
            String bloom,
            String stem,
            List<SeedChoice> choices,
            List<String> accepted,
            Double answer,
            Double tolerance,
            String explanation,
            List<String> misconceptions,
            Short estimateSeconds,
            String status,
            String version) {
    }

    /**
     * One option.
     *
     * @param misconception the code this distractor is built from, so choosing it is
     *                      diagnostic rather than merely wrong
     */
    public record SeedChoice(String id, String text, Boolean correct, String misconception) {
    }

    /**
     * A named set of items.
     *
     * @param items     item slugs in order, for a fixed assessment
     * @param blueprint selection and stop rules, for an assessment built per attempt
     */
    public record SeedAssessment(
            String slug,
            String title,
            String kind,
            String unit,
            String description,
            List<String> items,
            Map<String, Object> blueprint,
            Integer timeLimitSeconds,
            Double passMark,
            String status,
            String version) {
    }

    /** An authored review card. Locked cards cannot be displaced by generated ones. */
    public record SeedCard(
            String slug, String unit, String front, String back, Boolean locked, String status) {
    }
}
