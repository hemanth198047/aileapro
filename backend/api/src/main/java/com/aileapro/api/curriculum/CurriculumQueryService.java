package com.aileapro.api.curriculum;

import com.aileapro.api.curriculum.CurriculumViews.Block;
import com.aileapro.api.curriculum.CurriculumViews.PathDetail;
import com.aileapro.api.curriculum.CurriculumViews.PathSummary;
import com.aileapro.api.curriculum.CurriculumViews.Resource;
import com.aileapro.api.curriculum.CurriculumViews.UnitDetail;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.curriculum.CurriculumViews.Waypoint;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.ContentStatus;
import com.aileapro.persistence.LearningPathEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.UnitBlockEntity;
import com.aileapro.persistence.UnitResourceEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The learner-facing read side of the curriculum.
 *
 * <p>Every lookup here filters on {@link ContentStatus#PUBLISHED}. Anything in draft,
 * review, or validate does not exist as far as this service is concerned — a learner
 * asking for it gets a 404, not a 403, because telling them a draft exists is itself
 * a leak. Authoring reads arrive with the admin studio in Phase 13.
 */
@Service
@Transactional(readOnly = true)
public class CurriculumQueryService {

    private static final String DEFAULT_LOCALE = "en";

    private final LearningUnitRepository units;
    private final UnitBlockRepository blocks;
    private final UnitClosureRepository closure;
    private final LearningPathRepository paths;
    private final PathWaypointRepository waypoints;
    private final UnitResourceRepository unitResources;
    private final LearningResourceRepository resources;
    private final SourceRepository sources;
    private final ObjectMapper json;

    public CurriculumQueryService(
            LearningUnitRepository units,
            UnitBlockRepository blocks,
            UnitClosureRepository closure,
            LearningPathRepository paths,
            PathWaypointRepository waypoints,
            UnitResourceRepository unitResources,
            LearningResourceRepository resources,
            SourceRepository sources,
            ObjectMapper json) {
        this.units = units;
        this.blocks = blocks;
        this.closure = closure;
        this.paths = paths;
        this.waypoints = waypoints;
        this.unitResources = unitResources;
        this.resources = resources;
        this.sources = sources;
        this.json = json;
    }

    public List<PathSummary> listPaths() {
        return paths.findByStatusOrderByTitleAsc(ContentStatus.PUBLISHED).stream()
                .map(path -> {
                    List<LearningUnitEntity> pathUnits = publishedWaypointUnits(path.getId());
                    return new PathSummary(
                            path.getId(),
                            path.getSlug(),
                            path.getTitle(),
                            path.getDescription(),
                            path.getRoleTarget(),
                            path.getVersion(),
                            pathUnits.size(),
                            totalMinutes(pathUnits));
                })
                .toList();
    }

    public PathDetail path(String idOrSlug) {
        LearningPathEntity path = resolvePath(idOrSlug)
                .filter(p -> p.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("No published path for '" + idOrSlug + "'"));

        var ordered = waypoints.findByPathIdOrderBySortAsc(path.getId());
        Map<UUID, LearningUnitEntity> published = publishedById(
                ordered.stream().map(w -> w.getUnitId()).toList());
        Map<UUID, Integer> blockCounts = blockCounts(published.keySet());
        Map<UUID, Integer> childCounts = childCounts(published.keySet());

        List<Waypoint> views = new ArrayList<>();
        for (var waypoint : ordered) {
            LearningUnitEntity unit = published.get(waypoint.getUnitId());
            // A waypoint whose unit is still a draft is simply not part of the published path.
            if (unit == null) {
                continue;
            }
            views.add(new Waypoint(
                    waypoint.getSort(),
                    waypoint.getRequiredMasteryState(),
                    toRef(unit, blockCounts, childCounts)));
        }

        return new PathDetail(
                path.getId(),
                path.getSlug(),
                path.getTitle(),
                path.getDescription(),
                path.getRoleTarget(),
                path.getVersion(),
                totalMinutes(views.stream().map(w -> published.get(w.unit().id())).toList()),
                views);
    }

    public UnitDetail unit(String idOrSlug) {
        LearningUnitEntity unit = publishedUnit(idOrSlug);

        List<UnitBlockEntity> unitBlocks =
                blocks.findByUnitIdAndLocaleOrderBySortAsc(unit.getId(), unit.getLocale());

        List<LearningUnitEntity> children =
                units.findByParentIdAndStatusOrderByTitleAsc(unit.getId(), ContentStatus.PUBLISHED);
        Map<UUID, Integer> childBlockCounts = blockCounts(children.stream().map(LearningUnitEntity::getId).toList());
        Map<UUID, Integer> grandchildCounts = childCounts(children.stream().map(LearningUnitEntity::getId).toList());

        return new UnitDetail(
                unit.getId(),
                unit.getSlug(),
                unit.getTitle(),
                unit.getDescription(),
                unit.getUnitKind().code(),
                unit.getDifficulty(),
                unit.getEstimateMinutes(),
                unit.getVolatility().name(),
                unit.getKnowledgeClass().name(),
                unit.getVersion(),
                unit.getLastReviewedAt(),
                unit.getLastVerifiedAt(),
                Arrays.asList(unit.getObjectives()),
                Arrays.asList(unit.getTags()),
                breadcrumb(unit),
                children.stream().map(child -> toRef(child, childBlockCounts, grandchildCounts)).toList(),
                unitBlocks.stream().map(this::toBlock).toList(),
                resourcesFor(unit.getId()));
    }

    public List<Resource> resources(String idOrSlug) {
        return resourcesFor(publishedUnit(idOrSlug).getId());
    }

    /**
     * A single published unit as a reference, or 404. The graph resolves its focus through
     * here so that "published only" is decided in one place.
     */
    public UnitRef publishedRef(String idOrSlug) {
        LearningUnitEntity unit = publishedUnit(idOrSlug);
        return toRef(unit, blockCounts(List.of(unit.getId())), childCounts(List.of(unit.getId())));
    }

    /** References for the ids that are published, keyed by id. Ids in draft are simply absent. */
    public Map<UUID, UnitRef> publishedRefs(Collection<UUID> ids) {
        Map<UUID, LearningUnitEntity> found = publishedById(ids);
        Map<UUID, Integer> blockCounts = blockCounts(found.keySet());
        Map<UUID, Integer> childCounts = childCounts(found.keySet());
        Map<UUID, UnitRef> refs = new HashMap<>(found.size());
        found.forEach((id, unit) -> refs.put(id, toRef(unit, blockCounts, childCounts)));
        return refs;
    }

    /** Ancestors of a published unit, top-down, skipping any ancestor that is not published. */
    private List<UnitRef> breadcrumb(LearningUnitEntity unit) {
        List<UUID> ancestorIds = closure.findByIdDescendantIdOrderByDepthDesc(unit.getId()).stream()
                .filter(row -> row.getDepth() > 0)
                .map(row -> row.getId().getAncestorId())
                .toList();
        if (ancestorIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, LearningUnitEntity> byId = publishedById(ancestorIds);
        return ancestorIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(ancestor -> toRef(ancestor, Map.of(), Map.of()))
                .toList();
    }

    private List<Resource> resourcesFor(UUID unitId) {
        List<UnitResourceEntity> links = unitResources.findByUnitIdOrderBySortAsc(unitId);
        List<Resource> out = new ArrayList<>(links.size());
        for (UnitResourceEntity link : links) {
            var resource = resources.findById(link.getResourceId()).orElse(null);
            if (resource == null) {
                continue;
            }
            var source = sources.findById(resource.getSourceId()).orElse(null);
            // Provenance is not optional: a resource without a source is not shown at all.
            if (source == null) {
                continue;
            }
            out.add(new Resource(
                    source.getTitle(),
                    source.getUrl(),
                    source.getPublisher(),
                    source.getAuthors(),
                    source.getSourceType().name(),
                    source.getLicense(),
                    source.getPublishedOn(),
                    source.getLastVerifiedAt(),
                    link.getPurpose(),
                    link.getExcerpt()));
        }
        return out;
    }

    private LearningUnitEntity publishedUnit(String idOrSlug) {
        return resolveUnit(idOrSlug)
                .filter(u -> u.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("No published unit for '" + idOrSlug + "'"));
    }

    private Optional<LearningUnitEntity> resolveUnit(String idOrSlug) {
        return asUuid(idOrSlug)
                .map(units::findById)
                .orElseGet(() -> units.findBySlugAndLocale(idOrSlug, DEFAULT_LOCALE));
    }

    private Optional<LearningPathEntity> resolvePath(String idOrSlug) {
        return asUuid(idOrSlug)
                .map(paths::findById)
                .orElseGet(() -> paths.findBySlug(idOrSlug));
    }

    /** Slugs and UUIDs are both accepted on catalog routes; slugs keep URLs readable. */
    private static Optional<UUID> asUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }

    private List<LearningUnitEntity> publishedWaypointUnits(UUID pathId) {
        var ids = waypoints.findByPathIdOrderBySortAsc(pathId).stream().map(w -> w.getUnitId()).toList();
        Map<UUID, LearningUnitEntity> byId = publishedById(ids);
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    private Map<UUID, LearningUnitEntity> publishedById(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return units.findByIdInAndStatus(ids, ContentStatus.PUBLISHED).stream()
                .collect(Collectors.toMap(LearningUnitEntity::getId, Function.identity()));
    }

    private Map<UUID, Integer> blockCounts(Collection<UUID> unitIds) {
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        return tally(blocks.countByUnitIds(unitIds));
    }

    private Map<UUID, Integer> childCounts(Collection<UUID> unitIds) {
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        return tally(units.countPublishedChildren(unitIds, ContentStatus.PUBLISHED));
    }

    private static Map<UUID, Integer> tally(List<Object[]> rows) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private static int totalMinutes(Collection<LearningUnitEntity> unitList) {
        return unitList.stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(u -> u.getEstimateMinutes() == null ? 0 : u.getEstimateMinutes())
                .sum();
    }

    private UnitRef toRef(
            LearningUnitEntity unit, Map<UUID, Integer> blockCounts, Map<UUID, Integer> childCounts) {
        return new UnitRef(
                unit.getId(),
                unit.getSlug(),
                unit.getTitle(),
                unit.getDescription(),
                unit.getUnitKind().code(),
                unit.getDifficulty(),
                unit.getEstimateMinutes(),
                unit.getVolatility().name(),
                unit.getKnowledgeClass().name(),
                blockCounts.getOrDefault(unit.getId(), 0) > 0,
                childCounts.getOrDefault(unit.getId(), 0));
    }

    private Block toBlock(UnitBlockEntity block) {
        JsonNode payload;
        try {
            payload = json.readTree(block.getPayload());
        } catch (JsonProcessingException corrupt) {
            // Stored payloads are written by the seed loader, so this means the row is broken,
            // not that the learner did something wrong.
            throw new IllegalStateException(
                    "Block " + block.getId() + " has an unreadable payload", corrupt);
        }
        return new Block(block.getId(), block.getBlockType().code(), block.getSort(), payload);
    }
}
