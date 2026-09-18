package com.aileapro.api.curriculum.seed;

import com.aileapro.api.curriculum.LearningPathRepository;
import com.aileapro.api.curriculum.LearningResourceRepository;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.PathWaypointRepository;
import com.aileapro.api.curriculum.SourceRepository;
import com.aileapro.api.curriculum.UnitBlockRepository;
import com.aileapro.api.curriculum.UnitClosureRepository;
import com.aileapro.api.curriculum.UnitResourceRepository;
import com.aileapro.api.curriculum.seed.SeedModel.SeedBlock;
import com.aileapro.api.curriculum.seed.SeedModel.SeedEdge;
import com.aileapro.api.curriculum.seed.SeedModel.SeedFile;
import com.aileapro.api.curriculum.seed.SeedModel.SeedPath;
import com.aileapro.api.curriculum.seed.SeedModel.SeedResourceLink;
import com.aileapro.api.curriculum.seed.SeedModel.SeedSource;
import com.aileapro.api.curriculum.seed.SeedModel.SeedUnit;
import com.aileapro.api.graph.ConceptEdgeRepository;
import com.aileapro.api.graph.PrerequisiteDag;
import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.EdgeType;
import com.aileapro.domain.KnowledgeClass;
import com.aileapro.domain.SourceType;
import com.aileapro.domain.UnitKind;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.ConceptEdgeEntity;
import com.aileapro.persistence.LearningPathEntity;
import com.aileapro.persistence.LearningResourceEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.PathWaypointEntity;
import com.aileapro.persistence.SourceEntity;
import com.aileapro.persistence.UnitBlockEntity;
import com.aileapro.persistence.UnitClosureEntity;
import com.aileapro.persistence.UnitResourceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loads the authored seed files into the curriculum tables.
 *
 * <p>Idempotent by slug: running it twice produces the same rows, not duplicates. Blocks,
 * resource links, waypoints, and closure rows are replaced wholesale for each seeded
 * entity, because the file is the source of truth for them and partial merging would
 * leave content that no author can see.
 *
 * <p>It only ever writes what the files contain. Nothing is generated, so there is no way
 * for this to invent thousands of empty topics.
 */
@Component
public class CurriculumSeeder {

    private static final Logger log = LoggerFactory.getLogger(CurriculumSeeder.class);
    private static final String LOCATION = "classpath*:seed/curriculum/*.yml";
    private static final String LOCALE = "en";

    private final LearningUnitRepository units;
    private final UnitBlockRepository blocks;
    private final UnitClosureRepository closure;
    private final LearningPathRepository paths;
    private final PathWaypointRepository waypoints;
    private final SourceRepository sources;
    private final LearningResourceRepository resources;
    private final UnitResourceRepository unitResources;
    private final ConceptEdgeRepository edges;
    private final ObjectMapper yaml;
    private final ObjectMapper json;

    public CurriculumSeeder(
            LearningUnitRepository units,
            UnitBlockRepository blocks,
            UnitClosureRepository closure,
            LearningPathRepository paths,
            PathWaypointRepository waypoints,
            SourceRepository sources,
            LearningResourceRepository resources,
            UnitResourceRepository unitResources,
            ConceptEdgeRepository edges) {
        this.units = units;
        this.blocks = blocks;
        this.closure = closure;
        this.paths = paths;
        this.waypoints = waypoints;
        this.sources = sources;
        this.resources = resources;
        this.unitResources = unitResources;
        this.edges = edges;
        this.yaml = JsonMapper.builder(new YAMLFactory()).addModule(new JavaTimeModule()).build();
        this.json = new ObjectMapper();
    }

    public record SeedReport(
            int sources, int units, int blocks, int resourceLinks, int edges, int paths, int waypoints) {
        @Override
        public String toString() {
            return "sources=%d units=%d blocks=%d resourceLinks=%d edges=%d paths=%d waypoints=%d"
                    .formatted(sources, units, blocks, resourceLinks, edges, paths, waypoints);
        }
    }

    @Transactional
    public SeedReport seed() {
        List<SeedFile> files = read();

        List<SeedSource> seedSources = files.stream().flatMap(f -> nullSafe(f.sources()).stream()).toList();
        List<SeedUnit> seedUnits = files.stream().flatMap(f -> nullSafe(f.units()).stream()).toList();
        List<SeedEdge> seedEdges = files.stream().flatMap(f -> nullSafe(f.edges()).stream()).toList();
        List<SeedPath> seedPaths = files.stream().flatMap(f -> nullSafe(f.paths()).stream()).toList();

        Map<String, UUID> resourceIdsBySourceSlug = new LinkedHashMap<>();
        Map<String, UUID> sourceIdsBySlug = new LinkedHashMap<>();
        for (SeedSource source : seedSources) {
            SeededSource seeded = upsertSource(source);
            resourceIdsBySourceSlug.put(source.slug(), seeded.resourceId());
            sourceIdsBySlug.put(source.slug(), seeded.sourceId());
        }

        // Pass 1: every unit exists before any parent link is resolved, so seed files can be
        // ordered by topic rather than by hierarchy.
        Map<String, LearningUnitEntity> bySlug = new LinkedHashMap<>();
        for (SeedUnit unit : seedUnits) {
            bySlug.put(unit.slug(), upsertUnit(unit));
        }

        // Pass 2: parents, cached depth, containment closure.
        Map<String, String> parentSlugs = new HashMap<>();
        for (SeedUnit unit : seedUnits) {
            if (unit.parent() != null) {
                LearningUnitEntity parent = bySlug.get(unit.parent());
                if (parent == null) {
                    throw new IllegalStateException(
                            "Unit '" + unit.slug() + "' names unknown parent '" + unit.parent() + "'");
                }
                parentSlugs.put(unit.slug(), unit.parent());
                bySlug.get(unit.slug()).setParentId(parent.getId());
            } else {
                bySlug.get(unit.slug()).setParentId(null);
            }
        }
        for (SeedUnit unit : seedUnits) {
            List<String> ancestors = ancestorChain(unit.slug(), parentSlugs);
            LearningUnitEntity entity = bySlug.get(unit.slug());
            entity.setDepth(ancestors.size());
            units.save(entity);
            rewriteClosure(entity, ancestors, bySlug);
        }

        int blockCount = 0;
        int linkCount = 0;
        for (SeedUnit unit : seedUnits) {
            LearningUnitEntity entity = bySlug.get(unit.slug());
            blockCount += rewriteBlocks(entity, nullSafe(unit.blocks()));
            linkCount += rewriteResourceLinks(entity, nullSafe(unit.resources()), resourceIdsBySourceSlug);
        }

        checkEveryPublishedUnitIsContained(seedUnits, bySlug);
        int edgeCount = rewriteEdges(seedEdges, bySlug, sourceIdsBySlug);

        int waypointCount = 0;
        for (SeedPath path : seedPaths) {
            waypointCount += upsertPath(path, bySlug);
        }

        SeedReport report = new SeedReport(
                seedSources.size(),
                seedUnits.size(),
                blockCount,
                linkCount,
                edgeCount,
                seedPaths.size(),
                waypointCount);
        log.info("Curriculum seed applied: {}", report);
        return report;
    }

    private List<SeedFile> read() {
        try {
            Resource[] found = new PathMatchingResourcePatternResolver().getResources(LOCATION);
            if (found.length == 0) {
                throw new IllegalStateException("No seed files found at " + LOCATION);
            }
            List<Resource> ordered = new ArrayList<>(Arrays.asList(found));
            ordered.sort(Comparator.comparing(r -> String.valueOf(r.getFilename())));
            List<SeedFile> parsed = new ArrayList<>(ordered.size());
            for (Resource resource : ordered) {
                try (InputStream in = resource.getInputStream()) {
                    parsed.add(yaml.readValue(in, SeedFile.class));
                } catch (IOException broken) {
                    throw new IllegalStateException("Could not read seed file " + resource.getFilename(), broken);
                }
                log.debug("Read seed file {}", resource.getFilename());
            }
            return parsed;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A seeded source and the teaching-side resource row that wraps it. */
    private record SeededSource(UUID sourceId, UUID resourceId) {
    }

    private SeededSource upsertSource(SeedSource seed) {
        SourceEntity source = sources.findBySlug(seed.slug()).orElseGet(() -> {
            SourceEntity fresh = new SourceEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        source.setTitle(seed.title());
        source.setAuthors(seed.authors());
        source.setPublisher(seed.publisher());
        source.setUrl(seed.url());
        source.setDoi(blankToNull(seed.doi()));
        source.setAbstractText(blankToNull(seed.abstractText()));
        source.setPublishedOn(seed.publishedOn());
        source.setSourceType(SourceType.valueOf(seed.sourceType()));
        source.setLicense(seed.license());
        source.setLastVerifiedAt(atStartOfDay(seed.lastVerifiedAt()));
        source.setHttpStatus(seed.httpStatus());
        source.setUpdatedAt(Instant.now());
        sources.save(source);

        LearningResourceEntity resource = resources.findBySourceId(source.getId()).orElseGet(() -> {
            LearningResourceEntity fresh = new LearningResourceEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSourceId(source.getId());
            return fresh;
        });
        resource.setDifficulty(seed.difficulty());
        resources.save(resource);
        return new SeededSource(source.getId(), resource.getId());
    }

    private LearningUnitEntity upsertUnit(SeedUnit seed) {
        LearningUnitEntity unit = units.findBySlugAndLocale(seed.slug(), LOCALE).orElseGet(() -> {
            LearningUnitEntity fresh = new LearningUnitEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            fresh.setLocale(LOCALE);
            return fresh;
        });
        unit.setTitle(seed.title());
        unit.setDescription(seed.description());
        unit.setUnitKind(UnitKind.fromCode(seed.kind()));
        unit.setStatus(ContentStatus.fromCode(seed.status()));
        unit.setDifficulty(seed.difficulty());
        unit.setEstimateMinutes(seed.estimateMinutes());
        unit.setVolatility(seed.volatility() == null ? Volatility.LOW : Volatility.valueOf(seed.volatility()));
        unit.setKnowledgeClass(seed.knowledgeClass() == null
                ? KnowledgeClass.STABLE
                : KnowledgeClass.valueOf(seed.knowledgeClass()));
        unit.setVersion(seed.version() == null ? "0.1.0" : seed.version());
        unit.setObjectives(nullSafe(seed.objectives()).toArray(String[]::new));
        unit.setTags(nullSafe(seed.tags()).toArray(String[]::new));
        unit.setLastReviewedAt(atStartOfDay(seed.lastReviewedAt()));
        unit.setLastVerifiedAt(atStartOfDay(seed.lastVerifiedAt()));
        // Published means visible to learners, so it needs a point from which it is in effect.
        if (unit.getStatus() == ContentStatus.PUBLISHED && unit.getEffectiveAt() == null) {
            unit.setEffectiveAt(Instant.now());
        }
        unit.setUpdatedAt(Instant.now());
        return units.save(unit);
    }

    /** Parent slugs from the root down, with a cycle guard so a bad seed file fails loudly. */
    private List<String> ancestorChain(String slug, Map<String, String> parentSlugs) {
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(slug);
        String current = parentSlugs.get(slug);
        while (current != null) {
            if (!seen.add(current)) {
                throw new IllegalStateException("Containment cycle through unit '" + current + "'");
            }
            chain.add(current);
            current = parentSlugs.get(current);
        }
        return chain.reversed();
    }

    private void rewriteClosure(
            LearningUnitEntity unit, List<String> ancestorSlugs, Map<String, LearningUnitEntity> bySlug) {
        closure.deleteByIdDescendantId(unit.getId());
        closure.flush();
        List<UnitClosureEntity> rows = new ArrayList<>(ancestorSlugs.size() + 1);
        rows.add(new UnitClosureEntity(unit.getId(), unit.getId(), 0));
        int total = ancestorSlugs.size();
        for (int i = 0; i < total; i++) {
            UUID ancestorId = bySlug.get(ancestorSlugs.get(i)).getId();
            rows.add(new UnitClosureEntity(ancestorId, unit.getId(), total - i));
        }
        closure.saveAll(rows);
    }

    private int rewriteBlocks(LearningUnitEntity unit, List<SeedBlock> seedBlocks) {
        blocks.deleteByUnitId(unit.getId());
        blocks.flush();
        List<UnitBlockEntity> rows = new ArrayList<>(seedBlocks.size());
        for (int i = 0; i < seedBlocks.size(); i++) {
            SeedBlock seed = seedBlocks.get(i);
            UnitBlockEntity block = new UnitBlockEntity();
            block.setId(UUID.randomUUID());
            block.setUnitId(unit.getId());
            block.setSort(i);
            block.setBlockType(BlockType.fromCode(seed.type()));
            block.setLocale(LOCALE);
            block.setPayload(payloadJson(unit.getSlug(), seed));
            block.setVersion(unit.getVersion());
            rows.add(block);
        }
        blocks.saveAll(rows);
        return rows.size();
    }

    private String payloadJson(String unitSlug, SeedBlock seed) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (seed.payload() != null) {
            payload.putAll(seed.payload());
        }
        if (seed.markdown() != null) {
            payload.put("markdown", seed.markdown());
        }
        if (payload.isEmpty()) {
            throw new IllegalStateException(
                    "Block '" + seed.type() + "' on unit '" + unitSlug + "' has no markdown and no payload");
        }
        if (payload.get("markdown") instanceof String markdown) {
            checkDisplayMath(unitSlug, seed.type(), markdown);
        }
        try {
            return json.writeValueAsString(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise block payload for unit " + unitSlug, e);
        }
    }

    /**
     * Display maths must be either balanced on one line ({@code $$x$$}) or fenced with
     * {@code $$} alone on its own lines. A line that opens {@code $$} and continues with
     * content is treated by the Markdown maths extension as an info string, so the equation
     * silently renders as raw LaTeX in front of the learner. Failing the seed is better.
     */
    private static void checkDisplayMath(String unitSlug, String blockType, String markdown) {
        boolean inCodeFence = false;
        int lineNumber = 0;
        for (String raw : markdown.split("\n", -1)) {
            lineNumber++;
            String line = raw.strip();
            if (line.startsWith("```")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence || line.equals("$$")) {
                continue;
            }
            int fences = 0;
            for (int at = line.indexOf("$$"); at >= 0; at = line.indexOf("$$", at + 2)) {
                fences++;
            }
            if (fences % 2 != 0) {
                throw new IllegalStateException(
                        "Unbalanced display maths in block '%s' of unit '%s' at line %d: %s%n"
                                .formatted(blockType, unitSlug, lineNumber, line)
                                + "Put $$ on its own line above and below a multi-line equation.");
            }
        }
    }

    private int rewriteResourceLinks(
            LearningUnitEntity unit, List<SeedResourceLink> links, Map<String, UUID> resourceIdsBySourceSlug) {
        unitResources.deleteByUnitId(unit.getId());
        unitResources.flush();
        List<UnitResourceEntity> rows = new ArrayList<>(links.size());
        for (int i = 0; i < links.size(); i++) {
            SeedResourceLink seed = links.get(i);
            UUID resourceId = resourceIdsBySourceSlug.get(seed.source());
            if (resourceId == null) {
                throw new IllegalStateException(
                        "Unit '" + unit.getSlug() + "' cites unknown source '" + seed.source() + "'");
            }
            UnitResourceEntity link = new UnitResourceEntity();
            link.setId(UUID.randomUUID());
            link.setUnitId(unit.getId());
            link.setResourceId(resourceId);
            link.setPurpose(seed.purpose());
            link.setExcerpt(seed.excerpt());
            link.setRelevance(seed.relevance() == null ? null : BigDecimal.valueOf(seed.relevance()));
            link.setSort(i);
            rows.add(link);
        }
        unitResources.saveAll(rows);
        return rows.size();
    }

    /**
     * The seed owns {@code concept_edges} for now: edges in the files are upserted by
     * (from, to, type), and any edge no longer in the files is removed, so deleting a line
     * from a seed file actually deletes the relationship. When the studio in Phase 13 lets
     * anyone else author edges, this needs an authorship column to tell the two apart.
     */
    private int rewriteEdges(
            List<SeedEdge> seedEdges,
            Map<String, LearningUnitEntity> bySlug,
            Map<String, UUID> sourceIdsBySlug) {
        rejectPrerequisiteCycles(seedEdges);

        Set<UUID> keep = new HashSet<>();
        for (SeedEdge seed : seedEdges) {
            LearningUnitEntity from = requireUnit(bySlug, seed.from(), seed);
            LearningUnitEntity to = requireUnit(bySlug, seed.to(), seed);
            if (from.getId().equals(to.getId())) {
                throw new IllegalStateException("Edge '" + seed.type() + "' points '" + seed.from() + "' at itself");
            }
            EdgeType type = parseEdgeType(seed);

            ConceptEdgeEntity edge = edges
                    .findByFromIdAndToIdAndEdgeType(from.getId(), to.getId(), type)
                    .orElseGet(() -> {
                        ConceptEdgeEntity fresh = new ConceptEdgeEntity();
                        fresh.setId(UUID.randomUUID());
                        fresh.setFromId(from.getId());
                        fresh.setToId(to.getId());
                        fresh.setEdgeType(type);
                        return fresh;
                    });
            edge.setWeight(seed.weight() == null ? BigDecimal.ONE : BigDecimal.valueOf(seed.weight()));
            edge.setStatus(ContentStatus.fromCode(seed.status() == null ? "published" : seed.status()));
            edge.setNote(seed.note());
            edge.setEvidenceSourceIds(evidenceIds(seed, sourceIdsBySlug));
            edge.setUpdatedAt(Instant.now());
            edges.save(edge);
            keep.add(edge.getId());
        }

        List<ConceptEdgeEntity> stale = edges.findAll().stream()
                .filter(edge -> !keep.contains(edge.getId()))
                .toList();
        edges.deleteAll(stale);
        return keep.size();
    }

    /**
     * Prerequisites must stay acyclic, and the check runs before anything is written so a bad
     * file cannot leave half a ring in the database. Checked on slugs, so the message names
     * the units rather than their identifiers.
     */
    static void rejectPrerequisiteCycles(List<SeedEdge> seedEdges) {
        Map<String, List<String>> precedes = new LinkedHashMap<>();
        for (SeedEdge seed : seedEdges) {
            if (parseEdgeType(seed).isGating()) {
                precedes.computeIfAbsent(seed.from(), key -> new ArrayList<>()).add(seed.to());
            }
        }
        PrerequisiteDag.findCycle(precedes).ifPresent(cycle -> {
            throw new IllegalStateException(
                    "Prerequisite cycle in the seed: " + String.join(" -> ", cycle)
                            + ". A learner could never start any unit on that ring.");
        });
    }

    /**
     * Every published unit that is not a domain must sit inside something. An orphan would be
     * unreachable from the map and from any breadcrumb, which is a content bug rather than a
     * design choice.
     */
    private static void checkEveryPublishedUnitIsContained(
            List<SeedUnit> seedUnits, Map<String, LearningUnitEntity> bySlug) {
        for (SeedUnit seed : seedUnits) {
            LearningUnitEntity unit = bySlug.get(seed.slug());
            if (unit.getStatus() == ContentStatus.PUBLISHED
                    && unit.getUnitKind() != UnitKind.DOMAIN
                    && unit.getParentId() == null) {
                throw new IllegalStateException(
                        "Published unit '" + seed.slug() + "' has no parent, so nothing contains it");
            }
        }
    }

    private static LearningUnitEntity requireUnit(
            Map<String, LearningUnitEntity> bySlug, String slug, SeedEdge seed) {
        LearningUnitEntity unit = bySlug.get(slug);
        if (unit == null) {
            throw new IllegalStateException(
                    "Edge '" + seed.from() + "' " + seed.type() + " '" + seed.to()
                            + "' references unknown unit '" + slug + "'");
        }
        return unit;
    }

    private static EdgeType parseEdgeType(SeedEdge seed) {
        try {
            return EdgeType.valueOf(seed.type());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalStateException(
                    "Unknown edge type '" + seed.type() + "' on '" + seed.from() + "' -> '" + seed.to() + "'");
        }
    }

    private static UUID[] evidenceIds(SeedEdge seed, Map<String, UUID> sourceIdsBySlug) {
        List<String> slugs = nullSafe(seed.sources());
        UUID[] ids = new UUID[slugs.size()];
        for (int i = 0; i < slugs.size(); i++) {
            UUID id = sourceIdsBySlug.get(slugs.get(i));
            if (id == null) {
                throw new IllegalStateException("Edge '" + seed.from() + "' -> '" + seed.to()
                        + "' cites unknown source '" + slugs.get(i) + "'");
            }
            ids[i] = id;
        }
        return ids;
    }

    private int upsertPath(SeedPath seed, Map<String, LearningUnitEntity> bySlug) {
        LearningPathEntity path = paths.findBySlug(seed.slug()).orElseGet(() -> {
            LearningPathEntity fresh = new LearningPathEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        path.setTitle(seed.title());
        path.setDescription(seed.description());
        path.setRoleTarget(seed.roleTarget());
        path.setStatus(ContentStatus.fromCode(seed.status()));
        path.setVersion(seed.version() == null ? "0.1.0" : seed.version());
        path.setUpdatedAt(Instant.now());
        paths.save(path);

        waypoints.deleteByPathId(path.getId());
        waypoints.flush();
        List<SeedWaypointRow> ordered = new ArrayList<>();
        List<SeedModel.SeedWaypoint> seedWaypoints = nullSafe(seed.waypoints());
        for (int i = 0; i < seedWaypoints.size(); i++) {
            SeedModel.SeedWaypoint seedWaypoint = seedWaypoints.get(i);
            LearningUnitEntity unit = bySlug.get(seedWaypoint.unit());
            if (unit == null) {
                throw new IllegalStateException(
                        "Path '" + seed.slug() + "' references unknown unit '" + seedWaypoint.unit() + "'");
            }
            ordered.add(new SeedWaypointRow(unit.getId(), i, seedWaypoint.requiredMasteryState()));
        }
        List<PathWaypointEntity> rows = new ArrayList<>(ordered.size());
        for (SeedWaypointRow row : ordered) {
            PathWaypointEntity waypoint = new PathWaypointEntity();
            waypoint.setId(UUID.randomUUID());
            waypoint.setPathId(path.getId());
            waypoint.setUnitId(row.unitId());
            waypoint.setSort(row.sort());
            waypoint.setRequiredMasteryState(row.requiredMasteryState());
            rows.add(waypoint);
        }
        waypoints.saveAll(rows);
        return rows.size();
    }

    private record SeedWaypointRow(UUID unitId, int sort, String requiredMasteryState) {
    }

    private static Instant atStartOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static <T> List<T> nullSafe(List<T> value) {
        return value == null ? List.of() : value;
    }
}
