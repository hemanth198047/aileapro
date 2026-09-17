package com.aileapro.api.search;

import java.time.Instant;
import java.util.UUID;

/**
 * One retrieved or searched piece of curriculum, ready to show or to cite.
 *
 * @param knowledgeClass FOUNDATIONAL / CURRENT / … — drives the "latest needs retrieval" rule
 * @param updatedAt      when the underlying block last changed; shown next to the citation
 */
public record ContentHit(
        UUID chunkId,
        UUID blockId,
        UUID unitId,
        String unitSlug,
        String unitTitle,
        String blockType,
        String snippet,
        String knowledgeClass,
        Instant updatedAt,
        double score,
        String match) {

    public ContentHit {
        if (snippet == null) {
            snippet = "";
        }
        if (knowledgeClass == null) {
            knowledgeClass = "STABLE";
        }
        if (match == null) {
            match = "hybrid";
        }
    }
}
