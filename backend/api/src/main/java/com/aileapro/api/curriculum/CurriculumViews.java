package com.aileapro.api.curriculum;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read models for the catalog API. Nothing learner-specific lives here yet — that starts in Phase 5. */
public final class CurriculumViews {

    private CurriculumViews() {
    }

    /** A unit reduced to what a list or a link needs. */
    public record UnitRef(
            UUID id,
            String slug,
            String title,
            String description,
            String kind,
            Short difficulty,
            Integer estimateMinutes,
            String volatility,
            String knowledgeClass,
            boolean hasContent,
            int childCount) {
    }

    public record PathSummary(
            UUID id,
            String slug,
            String title,
            String description,
            String roleTarget,
            String version,
            int waypointCount,
            int estimateMinutes) {
    }

    public record Waypoint(int sort, String requiredMasteryState, UnitRef unit) {
    }

    public record PathDetail(
            UUID id,
            String slug,
            String title,
            String description,
            String roleTarget,
            String version,
            int estimateMinutes,
            List<Waypoint> waypoints) {
    }

    public record Block(UUID id, String type, int sort, JsonNode payload) {
    }

    public record Resource(
            String title,
            String url,
            String publisher,
            String authors,
            String sourceType,
            String license,
            LocalDate publishedOn,
            Instant lastVerifiedAt,
            String purpose,
            String excerpt) {
    }

    public record UnitDetail(
            UUID id,
            String slug,
            String title,
            String description,
            String kind,
            Short difficulty,
            Integer estimateMinutes,
            String volatility,
            String knowledgeClass,
            String version,
            Instant lastReviewedAt,
            Instant lastVerifiedAt,
            List<String> objectives,
            List<String> tags,
            List<UnitRef> breadcrumb,
            List<UnitRef> children,
            List<Block> blocks,
            List<Resource> resources) {
    }
}
