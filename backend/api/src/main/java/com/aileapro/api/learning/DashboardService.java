package com.aileapro.api.learning;

import com.aileapro.api.assessment.ReviewService;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.learning.LearningViews.Dashboard;
import com.aileapro.api.learning.LearningViews.DomainRollup;
import com.aileapro.api.learning.LearningViews.Today;
import com.aileapro.api.learning.LearningViews.WeakUnit;
import com.aileapro.domain.MasteryState;
import com.aileapro.domain.UnitKind;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Learner dashboard numbers from mastery_records and the review queue — never invented.
 */
@Service
public class DashboardService {

    public static final int VELOCITY_WINDOW_DAYS = 7;
    private static final int WEAK_LIMIT = 8;

    private final RecommendationService recommendations;
    private final ReviewService reviews;
    private final MasteryRecordRepository records;
    private final LearningUnitRepository units;

    public DashboardService(
            RecommendationService recommendations,
            ReviewService reviews,
            MasteryRecordRepository records,
            LearningUnitRepository units) {
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.records = records;
        this.units = units;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID userId) {
        Today today = recommendations.today(userId);
        var queue = reviews.due(userId);
        List<MasteryRecordEntity> rows = records.findByUserId(userId);

        int timeOnTask = rows.stream().mapToInt(MasteryRecordEntity::getDwellSeconds).sum();
        Instant since = Instant.now().minus(VELOCITY_WINDOW_DAYS, ChronoUnit.DAYS);
        int velocity = (int) rows.stream()
                .filter(r -> r.getState().atLeast(MasteryState.PRACTICED))
                .filter(r -> {
                    Instant at = r.getLastGradedAt() != null ? r.getLastGradedAt() : r.getUpdatedAt();
                    return at != null && !at.isBefore(since);
                })
                .count();

        Map<UUID, LearningUnitEntity> byId = loadUnits(rows);
        List<WeakUnit> weak = weakList(rows, byId);
        List<DomainRollup> domains = domainRollups(rows, byId);

        return new Dashboard(
                today.next(),
                queue.dueCount(),
                today.reviewsNote(),
                timeOnTask,
                VELOCITY_WINDOW_DAYS,
                velocity,
                "Units that reached PRACTICED or higher in the last "
                        + VELOCITY_WINDOW_DAYS
                        + " days, counted from mastery_records — not a streak.",
                weak,
                domains,
                today.pathSlug(),
                today.pathTitle(),
                today.waypointCount(),
                today.waypointsStarted(),
                today.waypointsIntroduced(),
                today.onboarded(),
                "No XP. Numbers come from mastery_records dwell, state, open misconceptions, and the review queue.");
    }

    private Map<UUID, LearningUnitEntity> loadUnits(List<MasteryRecordEntity> rows) {
        Map<UUID, LearningUnitEntity> byId = new HashMap<>();
        if (rows.isEmpty()) {
            return byId;
        }
        for (LearningUnitEntity unit : units.findAllById(rows.stream().map(MasteryRecordEntity::getUnitId).toList())) {
            byId.put(unit.getId(), unit);
        }
        // Parents for domain rollup.
        List<UUID> parents = byId.values().stream()
                .map(LearningUnitEntity::getParentId)
                .filter(id -> id != null && !byId.containsKey(id))
                .distinct()
                .toList();
        while (!parents.isEmpty()) {
            List<LearningUnitEntity> batch = units.findAllById(parents);
            List<UUID> next = new ArrayList<>();
            for (LearningUnitEntity unit : batch) {
                byId.put(unit.getId(), unit);
                if (unit.getParentId() != null && !byId.containsKey(unit.getParentId())) {
                    next.add(unit.getParentId());
                }
            }
            parents = next;
        }
        return byId;
    }

    private static List<WeakUnit> weakList(List<MasteryRecordEntity> rows, Map<UUID, LearningUnitEntity> byId) {
        return rows.stream()
                .filter(r -> r.getOpenMisconceptions() > 0
                        || (r.getOpenedCount() > 0 && !r.getState().atLeast(MasteryState.PRACTICED)))
                .sorted(Comparator
                        .comparingInt(MasteryRecordEntity::getOpenMisconceptions).reversed()
                        .thenComparingInt(r -> r.getState().rank()))
                .limit(WEAK_LIMIT)
                .map(r -> {
                    LearningUnitEntity unit = byId.get(r.getUnitId());
                    String slug = unit == null ? r.getUnitId().toString() : unit.getSlug();
                    String title = unit == null ? slug : unit.getTitle();
                    String reason = r.getOpenMisconceptions() > 0
                            ? r.getOpenMisconceptions() + " open misconception(s)"
                            : "Opened but below PRACTICED (" + r.getState().name() + ")";
                    return new WeakUnit(
                            slug,
                            title,
                            r.getState().name(),
                            r.getOpenMisconceptions(),
                            reason,
                            "/app/learn/" + slug);
                })
                .toList();
    }

    private static List<DomainRollup> domainRollups(
            List<MasteryRecordEntity> rows, Map<UUID, LearningUnitEntity> byId) {
        record Acc(String slug, String title, int tracked, int introduced, int practiced) {
        }
        Map<UUID, Acc> acc = new LinkedHashMap<>();
        for (MasteryRecordEntity row : rows) {
            LearningUnitEntity unit = byId.get(row.getUnitId());
            if (unit == null) {
                continue;
            }
            LearningUnitEntity domain = domainOf(unit, byId);
            Acc cur = acc.getOrDefault(
                    domain.getId(),
                    new Acc(domain.getSlug(), domain.getTitle(), 0, 0, 0));
            int introduced = cur.introduced + (row.getState().atLeast(MasteryState.INTRODUCED) ? 1 : 0);
            int practiced = cur.practiced + (row.getState().atLeast(MasteryState.PRACTICED) ? 1 : 0);
            acc.put(domain.getId(), new Acc(cur.slug, cur.title, cur.tracked + 1, introduced, practiced));
        }
        return acc.values().stream()
                .sorted(Comparator.comparing(Acc::title))
                .map(a -> new DomainRollup(a.slug, a.title, a.tracked, a.introduced, a.practiced))
                .toList();
    }

    private static LearningUnitEntity domainOf(LearningUnitEntity unit, Map<UUID, LearningUnitEntity> byId) {
        LearningUnitEntity cursor = unit;
        while (cursor.getParentId() != null) {
            LearningUnitEntity parent = byId.get(cursor.getParentId());
            if (parent == null) {
                break;
            }
            if (parent.getUnitKind() == UnitKind.DOMAIN) {
                return parent;
            }
            cursor = parent;
        }
        return cursor.getUnitKind() == UnitKind.DOMAIN ? cursor : unit;
    }
}
