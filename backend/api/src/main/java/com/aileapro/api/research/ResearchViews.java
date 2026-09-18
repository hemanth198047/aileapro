package com.aileapro.api.research;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ResearchViews {

    private ResearchViews() {
    }

    public record Paper(
            UUID id,
            String slug,
            String title,
            String authors,
            String publisher,
            String url,
            String doi,
            String abstractText,
            LocalDate publishedOn,
            List<String> relatedUnitSlugs) {
    }

    public record ReadingList(List<Paper> papers, String note) {
    }

    public record Citation(String slug, String title, String doi, String url) {
    }

    public record Explainer(
            String question,
            String answer,
            boolean refused,
            String refusalCode,
            boolean development,
            List<Citation> citations) {
    }

    public record MapNode(String id, String kind, String slug, String title) {
    }

    public record MapEdge(String from, String to, String type) {
    }

    public record LiteratureMap(List<MapNode> nodes, List<MapEdge> edges, String note) {
    }
}
