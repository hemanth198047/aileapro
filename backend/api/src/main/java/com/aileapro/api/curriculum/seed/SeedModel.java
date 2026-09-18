package com.aileapro.api.curriculum.seed;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Shape of the authored seed files in {@code resources/seed/curriculum}.
 *
 * <p>Unknown properties are rejected on purpose: a typo in a block type or a misspelled
 * field in authored content should fail the seed run, not be silently dropped.
 */
public final class SeedModel {

    private SeedModel() {
    }

    public record SeedFile(
            List<SeedSource> sources,
            List<SeedUnit> units,
            List<SeedEdge> edges,
            List<SeedPath> paths) {
    }

    /**
     * A typed edge between two units, by slug.
     *
     * @param note    why the author drew it, shown beside the edge in the map
     * @param sources slugs of seeded sources that back the claim
     */
    public record SeedEdge(
            String from,
            String to,
            String type,
            Double weight,
            String status,
            String note,
            List<String> sources) {
    }

    public record SeedSource(
            String slug,
            String title,
            String authors,
            String publisher,
            String url,
            String doi,
            String abstractText,
            LocalDate publishedOn,
            String sourceType,
            String license,
            LocalDate lastVerifiedAt,
            Integer httpStatus,
            Short difficulty) {
    }

    public record SeedUnit(
            String slug,
            String title,
            String description,
            String kind,
            String parent,
            String status,
            Short difficulty,
            Integer estimateMinutes,
            String volatility,
            String knowledgeClass,
            String version,
            LocalDate lastReviewedAt,
            LocalDate lastVerifiedAt,
            List<String> objectives,
            List<String> tags,
            List<SeedBlock> blocks,
            List<SeedResourceLink> resources) {
    }

    /** {@code markdown} is shorthand for a payload of {@code {"markdown": "..."}}. */
    public record SeedBlock(String type, String markdown, Map<String, Object> payload) {
    }

    public record SeedResourceLink(String source, String purpose, String excerpt, Double relevance) {
    }

    public record SeedPath(
            String slug,
            String title,
            String description,
            String roleTarget,
            String status,
            String version,
            List<SeedWaypoint> waypoints) {
    }

    public record SeedWaypoint(String unit, String requiredMasteryState) {
    }
}
