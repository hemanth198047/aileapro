package com.aileapro.api.admin;

import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.UnitKind;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.UnitBlockEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentCriticTest {

    @Test
    void emptyDraftIsNotPublishable() {
        LearningUnitEntity unit = base();
        unit.setObjectives(new String[0]);
        unit.setDifficulty(null);
        ContentCritic.Report report = ContentCritic.review(unit, List.of());
        assertThat(report.publishable()).isFalse();
        assertThat(report.findings()).extracting(ContentCritic.Finding::code)
                .contains("empty_objectives", "missing_difficulty", "missing_why", "missing_explanation");
    }

    @Test
    void completeDraftIsPublishableWithWarnings() {
        LearningUnitEntity unit = base();
        unit.setObjectives(new String[]{"Explain attention"});
        unit.setDifficulty((short) 3);
        List<UnitBlockEntity> blocks = List.of(
                block(BlockType.WHY),
                block(BlockType.BEGINNER));
        ContentCritic.Report report = ContentCritic.review(unit, blocks);
        assertThat(report.publishable()).isTrue();
        assertThat(report.findings()).extracting(ContentCritic.Finding::severity)
                .containsOnly("warn");
    }

    private static LearningUnitEntity base() {
        LearningUnitEntity unit = new LearningUnitEntity();
        unit.setId(UUID.randomUUID());
        unit.setSlug("critic-test");
        unit.setTitle("Critic Test");
        unit.setUnitKind(UnitKind.KNOWLEDGE_UNIT);
        unit.setStatus(ContentStatus.DRAFT);
        return unit;
    }

    private static UnitBlockEntity block(BlockType type) {
        UnitBlockEntity block = new UnitBlockEntity();
        block.setId(UUID.randomUUID());
        block.setBlockType(type);
        block.setPayload("{\"markdown\":\"x\"}");
        return block;
    }
}
