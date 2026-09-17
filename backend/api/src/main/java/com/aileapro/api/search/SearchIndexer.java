package com.aileapro.api.search;

import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.UnitBlockEntity;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.UnitBlockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Rebuilds the search corpus from published curriculum.
 *
 * <p>There is no separate publish pipeline yet: seeding writes {@code status=published}, and
 * this runs at the end of seed (and can be called again). Skipping exercise answer keys is
 * deliberate — the tutor must not retrieve them.
 */
@Service
public class SearchIndexer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

    /** Block kinds whose text is safe to retrieve and to show in search snippets. */
    private static final Set<BlockType> INDEXABLE = EnumSet.of(
            BlockType.WHY,
            BlockType.OBJECTIVES,
            BlockType.ELI5,
            BlockType.BEGINNER,
            BlockType.INTERMEDIATE,
            BlockType.ADVANCED,
            BlockType.EXPERT,
            BlockType.RESEARCH,
            BlockType.FORMAL_DEFINITION,
            BlockType.MATH_INTUITION,
            BlockType.MATH_FORMAL,
            BlockType.VISUAL,
            BlockType.CODE,
            BlockType.EXAMPLE,
            BlockType.COUNTEREXAMPLE,
            BlockType.APPLICATION,
            BlockType.COMMON_MISTAKE,
            BlockType.MISCONCEPTION,
            BlockType.REFERENCES,
            BlockType.RELATED);

    private final LearningUnitRepository units;
    private final UnitBlockRepository blocks;
    private final ContentIndex index;

    public SearchIndexer(
            LearningUnitRepository units, UnitBlockRepository blocks, ContentIndex index) {
        this.units = units;
        this.blocks = blocks;
        this.index = index;
    }

    @Transactional
    public IndexReport reindexAll() {
        List<LearningUnitEntity> published = units.findAll().stream()
                .filter(unit -> unit.getStatus() == ContentStatus.PUBLISHED)
                .toList();
        int unitCount = 0;
        int blockCount = 0;
        int skipped = 0;
        for (LearningUnitEntity unit : published) {
            IndexReport one = reindexUnit(unit);
            unitCount += one.units();
            blockCount += one.blocks();
            skipped += one.skippedBlocks();
        }
        log.info(
                "Search index rebuilt: {} units, {} blocks indexed, {} blocks skipped, {} chunks",
                unitCount,
                blockCount,
                skipped,
                index.chunkCount());
        return new IndexReport(unitCount, blockCount, skipped, index.chunkCount());
    }

    @Transactional
    public IndexReport reindexUnit(LearningUnitEntity unit) {
        if (unit.getStatus() != ContentStatus.PUBLISHED) {
            index.removeUnit(unit.getId());
            return new IndexReport(0, 0, 0, index.chunkCount());
        }
        List<UnitBlockEntity> rows = blocks.findByUnitIdOrderBySortAsc(unit.getId());
        List<ContentIndex.BlockDocument> docs = new ArrayList<>();
        int skipped = 0;
        for (UnitBlockEntity row : rows) {
            if (!INDEXABLE.contains(row.getBlockType())) {
                skipped++;
                continue;
            }
            String text = BlockText.fromPayloadJson(row.getPayload());
            if (text.isBlank()) {
                skipped++;
                continue;
            }
            docs.add(new ContentIndex.BlockDocument(
                    row.getId(),
                    unit.getId(),
                    row.getBlockType().code(),
                    text,
                    unit.getKnowledgeClass() == null ? "STABLE" : unit.getKnowledgeClass().name(),
                    row.getUpdatedAt()));
        }
        index.reindexUnit(
                new ContentIndex.UnitDocument(
                        unit.getId(),
                        unit.getSlug(),
                        unit.getTitle(),
                        unit.getDescription(),
                        unit.getUnitKind() == null ? "knowledge_unit" : unit.getUnitKind().code(),
                        unit.getKnowledgeClass() == null ? "STABLE" : unit.getKnowledgeClass().name(),
                        unit.getUpdatedAt()),
                docs);
        return new IndexReport(1, docs.size(), skipped, index.chunkCount());
    }

    public record IndexReport(int units, int blocks, int skippedBlocks, long chunks) {
    }
}
