package com.aileapro.api.graph;

import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.EdgeType;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.ConceptEdgeEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prerequisite health for the composite score: how much of what a unit rests on is itself in
 * good standing.
 *
 * <p>Only the direct prerequisites are counted. Health is transitive through the state
 * machine already — a prerequisite cannot be UNDERSTOOD unless its own prerequisites were
 * cleared on the way — so walking the whole ancestry would weight the deep, long-settled
 * foundations more heavily than the thing actually about to bite.
 *
 * <p>A unit with no prerequisites scores 1. That is not a free point: it means nothing is
 * propping this unit up, so there is nothing that can be shaky.
 */
@Component
class GraphPrerequisiteHealth implements GradedMasteryService.PrerequisiteHealth {

    private final ConceptEdgeRepository edges;
    private final MasteryRecordRepository records;

    GraphPrerequisiteHealth(ConceptEdgeRepository edges, MasteryRecordRepository records) {
        this.edges = edges;
        this.records = records;
    }

    @Override
    @Transactional(readOnly = true)
    public double healthFor(UUID userId, UUID unitId) {
        List<UUID> prerequisites = new ArrayList<>();
        for (ConceptEdgeEntity edge : edges.findByToIdInAndEdgeTypeAndStatus(
                List.of(unitId), EdgeType.PREREQUISITE_OF, ContentStatus.PUBLISHED)) {
            prerequisites.add(edge.getFromId());
        }
        if (prerequisites.isEmpty()) {
            return 1.0;
        }

        Map<UUID, MasteryState> states = new HashMap<>();
        for (MasteryRecordEntity record : records.findByUserIdAndUnitIdIn(userId, prerequisites)) {
            states.put(record.getUnitId(), record.getState());
        }

        double total = 0;
        for (UUID prerequisite : prerequisites) {
            total += credit(states.getOrDefault(prerequisite, MasteryState.UNKNOWN));
        }
        return total / prerequisites.size();
    }

    /**
     * Partial credit up the ladder rather than a pass/fail gate. A prerequisite the learner
     * has read but not practised is genuinely better than one they have never opened, and a
     * step function here would make the score jump for reasons the learner cannot see.
     */
    private static double credit(MasteryState state) {
        return switch (state) {
            case UNKNOWN -> 0.0;
            case INTRODUCED -> 0.25;
            case FAMILIAR -> 0.45;
            case UNDERSTOOD -> 0.70;
            case PRACTICED -> 0.85;
            case PROFICIENT, MASTERED -> 1.0;
        };
    }
}
