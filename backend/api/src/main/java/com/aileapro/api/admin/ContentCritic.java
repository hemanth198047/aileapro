package com.aileapro.api.admin;

import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.UnitBlockEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Rule-based critic for draft units. Errors block publish; warnings do not.
 */
public final class ContentCritic {

    private ContentCritic() {
    }

    public record Finding(String severity, String code, String message) {
    }

    public record Report(List<Finding> findings, boolean publishable) {
    }

    public static Report review(LearningUnitEntity unit, List<UnitBlockEntity> blocks) {
        List<Finding> findings = new ArrayList<>();
        if (unit.getObjectives() == null || unit.getObjectives().length == 0) {
            findings.add(new Finding("error", "empty_objectives", "Objectives are empty."));
        }
        if (unit.getDifficulty() == null) {
            findings.add(new Finding("error", "missing_difficulty", "Difficulty is not set."));
        }
        if (unit.getTitle() == null || unit.getTitle().isBlank()) {
            findings.add(new Finding("error", "missing_title", "Title is required."));
        }
        Set<BlockType> types = EnumSet.noneOf(BlockType.class);
        for (UnitBlockEntity block : blocks) {
            if (block.getBlockType() != null) {
                types.add(block.getBlockType());
            }
        }
        if (!types.contains(BlockType.WHY)) {
            findings.add(new Finding("error", "missing_why", "A why block is required before publish."));
        }
        if (types.stream().noneMatch(BlockType::isExplanationLevel)) {
            findings.add(new Finding("error", "missing_explanation",
                    "At least one explanation level block is required."));
        }
        if (!types.contains(BlockType.EXAMPLE)) {
            findings.add(new Finding("warn", "missing_example", "No example block yet."));
        }
        if (!types.contains(BlockType.MISCONCEPTION)) {
            findings.add(new Finding("warn", "missing_misconception", "No misconception block yet."));
        }
        if (!types.contains(BlockType.EXERCISE_SET)) {
            findings.add(new Finding("warn", "missing_exercise", "No exercise set block yet."));
        }
        if (unit.getStatus() == ContentStatus.PUBLISHED) {
            findings.add(new Finding("warn", "already_published", "Unit is already published."));
        }
        boolean publishable = findings.stream().noneMatch(f -> "error".equals(f.severity()));
        return new Report(List.copyOf(findings), publishable);
    }
}
