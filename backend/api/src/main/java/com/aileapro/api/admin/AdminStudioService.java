package com.aileapro.api.admin;

import com.aileapro.api.AuditLogRepository;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.UnitBlockRepository;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.BlockType;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.KnowledgeClass;
import com.aileapro.domain.UnitKind;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.AuditLogEntity;
import com.aileapro.persistence.CurriculumChangeEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.UnitBlockEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin content studio: read any status, edit drafts, publish with snapshot + audit.
 *
 * <p>Learner APIs never call this. A draft remains 404 on {@code /v1/units/**} until publish.
 */
@Service
public class AdminStudioService {

    private static final String LOCALE = "en";
    public static final String AUDIT_PUBLISH = "curriculum.publish";
    public static final String AUDIT_ROLLBACK = "curriculum.rollback";

    private final LearningUnitRepository units;
    private final UnitBlockRepository blocks;
    private final CurriculumChangeRepository changes;
    private final AuditLogRepository audits;
    private final ObjectMapper json;

    public AdminStudioService(
            LearningUnitRepository units,
            UnitBlockRepository blocks,
            CurriculumChangeRepository changes,
            AuditLogRepository audits,
            ObjectMapper json) {
        this.units = units;
        this.blocks = blocks;
        this.changes = changes;
        this.audits = audits;
        this.json = json;
    }

    public record CreateBody(
            String slug,
            String title,
            String description,
            String kind,
            Short difficulty,
            Integer estimateMinutes,
            List<String> objectives,
            List<BlockInput> blocks) {
    }

    public record BlockInput(String type, String markdown, String payload) {
    }

    public record PatchBody(
            String title,
            String description,
            Short difficulty,
            Integer estimateMinutes,
            List<String> objectives,
            List<BlockInput> blocks) {
    }

    public record PublishBody(String reason) {
    }

    @Transactional(readOnly = true)
    public List<AdminViews.UnitSummary> list(String statusFilter) {
        List<LearningUnitEntity> rows;
        if (statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)) {
            rows = units.findAllByOrderByTitleAsc();
        } else {
            rows = units.findByStatusOrderByTitleAsc(ContentStatus.fromCode(statusFilter.trim().toLowerCase(Locale.ROOT)));
        }
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = blocks.countByUnitIds(rows.stream().map(LearningUnitEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        return rows.stream()
                .map(unit -> new AdminViews.UnitSummary(
                        unit.getId(),
                        unit.getSlug(),
                        unit.getTitle(),
                        unit.getStatus().code(),
                        unit.getVersion(),
                        unit.getUnitKind().code(),
                        unit.getDifficulty(),
                        counts.getOrDefault(unit.getId(), 0L).intValue(),
                        unit.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminViews.UnitDetail get(String slug) {
        LearningUnitEntity unit = bySlug(slug);
        List<UnitBlockEntity> unitBlocks = blocks.findByUnitIdAndLocaleOrderBySortAsc(unit.getId(), LOCALE);
        return detail(unit, unitBlocks);
    }

    @Transactional
    public AdminViews.UnitDetail create(UUID actorId, CreateBody body) {
        if (body.slug() == null || body.slug().isBlank()) {
            throw ApiException.validation("Slug is required");
        }
        String slug = body.slug().trim().toLowerCase(Locale.ROOT);
        if (units.findBySlugAndLocale(slug, LOCALE).isPresent()) {
            throw ApiException.conflict("Unit slug already exists");
        }
        LearningUnitEntity unit = new LearningUnitEntity();
        unit.setId(UUID.randomUUID());
        unit.setSlug(slug);
        unit.setLocale(LOCALE);
        unit.setTitle(requireTitle(body.title()));
        unit.setDescription(body.description());
        unit.setUnitKind(body.kind() == null || body.kind().isBlank()
                ? UnitKind.KNOWLEDGE_UNIT
                : UnitKind.fromCode(body.kind()));
        unit.setDepth(0);
        unit.setObjectives(toArray(body.objectives()));
        unit.setTags(new String[0]);
        unit.setDifficulty(body.difficulty());
        unit.setEstimateMinutes(body.estimateMinutes());
        unit.setVolatility(Volatility.LOW);
        unit.setKnowledgeClass(KnowledgeClass.STABLE);
        unit.setStatus(ContentStatus.DRAFT);
        unit.setVersion("0.1.0");
        unit.setCreatedBy(actorId);
        unit.setCreatedAt(Instant.now());
        unit.setUpdatedAt(Instant.now());
        units.save(unit);
        replaceBlocks(unit, body.blocks());
        return get(slug);
    }

    @Transactional
    public AdminViews.UnitDetail patch(String slug, PatchBody body) {
        LearningUnitEntity unit = bySlug(slug);
        if (unit.getStatus() == ContentStatus.PUBLISHED || unit.getStatus() == ContentStatus.DEPRECATED) {
            throw ApiException.validation("Published units cannot be patched here — unpublish or create a new draft.");
        }
        if (body.title() != null) {
            unit.setTitle(requireTitle(body.title()));
        }
        if (body.description() != null) {
            unit.setDescription(body.description());
        }
        if (body.difficulty() != null) {
            unit.setDifficulty(body.difficulty());
        }
        if (body.estimateMinutes() != null) {
            unit.setEstimateMinutes(body.estimateMinutes());
        }
        if (body.objectives() != null) {
            unit.setObjectives(toArray(body.objectives()));
        }
        unit.setUpdatedAt(Instant.now());
        units.save(unit);
        if (body.blocks() != null) {
            replaceBlocks(unit, body.blocks());
        }
        return get(slug);
    }

    @Transactional(readOnly = true)
    public ContentCritic.Report critic(String slug) {
        LearningUnitEntity unit = bySlug(slug);
        return ContentCritic.review(unit, blocks.findByUnitIdAndLocaleOrderBySortAsc(unit.getId(), LOCALE));
    }

    @Transactional
    public AdminViews.PublishResult publish(UUID actorId, String slug, PublishBody body) {
        LearningUnitEntity unit = bySlug(slug);
        List<UnitBlockEntity> unitBlocks = blocks.findByUnitIdAndLocaleOrderBySortAsc(unit.getId(), LOCALE);
        ContentCritic.Report report = ContentCritic.review(unit, unitBlocks);
        if (!report.publishable()) {
            throw ApiException.validation("Critic blocked publish: "
                    + report.findings().stream()
                            .filter(f -> "error".equals(f.severity()))
                            .map(ContentCritic.Finding::code)
                            .collect(Collectors.joining(", ")));
        }
        String fromVersion = unit.getVersion();
        String fromStatus = unit.getStatus().code();
        String toVersion = bumpVersion(fromVersion);
        String snapshot = snapshotJson(unit, unitBlocks);

        CurriculumChangeEntity change = new CurriculumChangeEntity();
        change.setId(UUID.randomUUID());
        change.setUnitId(unit.getId());
        change.setActorId(actorId);
        change.setAction("publish");
        change.setFromVersion(fromVersion);
        change.setToVersion(toVersion);
        change.setFromStatus(fromStatus);
        change.setToStatus(ContentStatus.PUBLISHED.code());
        change.setReason(body == null || body.reason() == null || body.reason().isBlank()
                ? "Published from content studio"
                : body.reason().trim());
        change.setSnapshot(snapshot);
        change.setCreatedAt(Instant.now());
        changes.save(change);

        unit.setVersion(toVersion);
        unit.setStatus(ContentStatus.PUBLISHED);
        unit.setEffectiveAt(Instant.now());
        unit.setSupersededAt(null);
        unit.setUpdatedAt(Instant.now());
        units.save(unit);

        UUID auditId = audit(actorId, AUDIT_PUBLISH, unit.getId().toString(), Map.of(
                "slug", unit.getSlug(),
                "fromVersion", fromVersion,
                "toVersion", toVersion,
                "changeId", change.getId().toString()));

        return new AdminViews.PublishResult(
                unit.getSlug(), fromVersion, toVersion, unit.getStatus().code(), change.getId(), auditId, report);
    }

    /**
     * Restores the unit fields and blocks from the most recent publish snapshot, then marks
     * the unit draft again so learners cannot see a rolled-back publish until re-reviewed.
     */
    @Transactional
    public AdminViews.RollbackResult rollback(UUID actorId, String slug) {
        LearningUnitEntity unit = bySlug(slug);
        CurriculumChangeEntity published = changes
                .findFirstByUnitIdAndActionOrderByCreatedAtDesc(unit.getId(), "publish")
                .orElseThrow(() -> ApiException.notFound("No publish snapshot to roll back to"));
        try {
            JsonNode snap = json.readTree(published.getSnapshot());
            unit.setTitle(text(snap, "title", unit.getTitle()));
            unit.setDescription(text(snap, "description", unit.getDescription()));
            unit.setDifficulty(snap.path("difficulty").isMissingNode() || snap.path("difficulty").isNull()
                    ? null
                    : (short) snap.path("difficulty").asInt());
            unit.setEstimateMinutes(snap.path("estimateMinutes").isMissingNode() || snap.path("estimateMinutes").isNull()
                    ? null
                    : snap.path("estimateMinutes").asInt());
            if (snap.has("objectives") && snap.get("objectives").isArray()) {
                List<String> objs = new ArrayList<>();
                snap.get("objectives").forEach(n -> objs.add(n.asText()));
                unit.setObjectives(toArray(objs));
            }
            unit.setVersion(published.getFromVersion() == null ? "0.1.0" : published.getFromVersion());
            unit.setStatus(ContentStatus.DRAFT);
            unit.setEffectiveAt(null);
            unit.setUpdatedAt(Instant.now());
            units.save(unit);

            List<BlockInput> restored = new ArrayList<>();
            if (snap.has("blocks") && snap.get("blocks").isArray()) {
                for (JsonNode block : snap.get("blocks")) {
                    restored.add(new BlockInput(
                            block.path("type").asText("beginner"),
                            null,
                            block.path("payload").isTextual()
                                    ? block.path("payload").asText()
                                    : block.path("payload").toString()));
                }
            }
            replaceBlocks(unit, restored);

            CurriculumChangeEntity change = new CurriculumChangeEntity();
            change.setId(UUID.randomUUID());
            change.setUnitId(unit.getId());
            change.setActorId(actorId);
            change.setAction("rollback");
            change.setFromVersion(published.getToVersion());
            change.setToVersion(unit.getVersion());
            change.setFromStatus(ContentStatus.PUBLISHED.code());
            change.setToStatus(ContentStatus.DRAFT.code());
            change.setReason("Rollback to snapshot " + published.getId());
            change.setSnapshot(published.getSnapshot());
            change.setCreatedAt(Instant.now());
            changes.save(change);

            audit(actorId, AUDIT_ROLLBACK, unit.getId().toString(), Map.of(
                    "slug", unit.getSlug(),
                    "restoredFromChangeId", published.getId().toString(),
                    "toVersion", unit.getVersion()));

            return new AdminViews.RollbackResult(
                    unit.getSlug(),
                    unit.getVersion(),
                    unit.getStatus().code(),
                    change.getId(),
                    "Restored snapshot and set status to draft. Learners see 404 until republish.");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read publish snapshot", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminViews.Change> changelog(String slug) {
        LearningUnitEntity unit = bySlug(slug);
        return changes.findByUnitIdOrderByCreatedAtDesc(unit.getId()).stream()
                .map(c -> new AdminViews.Change(
                        c.getId(),
                        c.getAction(),
                        c.getFromVersion(),
                        c.getToVersion(),
                        c.getFromStatus(),
                        c.getToStatus(),
                        c.getReason(),
                        c.getCreatedAt()))
                .toList();
    }

    private AdminViews.UnitDetail detail(LearningUnitEntity unit, List<UnitBlockEntity> unitBlocks) {
        return new AdminViews.UnitDetail(
                unit.getId(),
                unit.getSlug(),
                unit.getTitle(),
                unit.getDescription(),
                unit.getStatus().code(),
                unit.getVersion(),
                unit.getUnitKind().code(),
                unit.getDifficulty(),
                unit.getEstimateMinutes(),
                List.copyOf(java.util.Arrays.asList(
                        unit.getObjectives() == null ? new String[0] : unit.getObjectives())),
                List.copyOf(java.util.Arrays.asList(
                        unit.getTags() == null ? new String[0] : unit.getTags())),
                unitBlocks.stream()
                        .map(b -> new AdminViews.Block(b.getId(), b.getBlockType().code(), b.getSort(), b.getPayload()))
                        .toList(),
                ContentCritic.review(unit, unitBlocks),
                unit.getEffectiveAt(),
                unit.getUpdatedAt());
    }

    private void replaceBlocks(LearningUnitEntity unit, List<BlockInput> inputs) {
        blocks.deleteByUnitId(unit.getId());
        blocks.flush();
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        int sort = 0;
        for (BlockInput input : inputs) {
            UnitBlockEntity block = new UnitBlockEntity();
            block.setId(UUID.randomUUID());
            block.setUnitId(unit.getId());
            block.setSort(sort++);
            block.setBlockType(BlockType.fromCode(input.type() == null ? "beginner" : input.type()));
            block.setLocale(LOCALE);
            block.setPayload(blockPayload(input));
            block.setVersion(unit.getVersion());
            block.setCreatedAt(Instant.now());
            block.setUpdatedAt(Instant.now());
            blocks.save(block);
        }
    }

    private String blockPayload(BlockInput input) {
        if (input.payload() != null && !input.payload().isBlank()) {
            return input.payload();
        }
        String markdown = input.markdown() == null ? "" : input.markdown();
        try {
            return json.writeValueAsString(Map.of("markdown", markdown));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String snapshotJson(LearningUnitEntity unit, List<UnitBlockEntity> unitBlocks) {
        try {
            ObjectNode root = json.createObjectNode();
            root.put("slug", unit.getSlug());
            root.put("title", unit.getTitle());
            root.put("description", unit.getDescription());
            if (unit.getDifficulty() != null) {
                root.put("difficulty", unit.getDifficulty());
            }
            if (unit.getEstimateMinutes() != null) {
                root.put("estimateMinutes", unit.getEstimateMinutes());
            }
            ArrayNode objectives = root.putArray("objectives");
            for (String o : unit.getObjectives() == null ? new String[0] : unit.getObjectives()) {
                objectives.add(o);
            }
            ArrayNode blockArr = root.putArray("blocks");
            for (UnitBlockEntity block : unitBlocks) {
                ObjectNode b = blockArr.addObject();
                b.put("type", block.getBlockType().code());
                b.put("sort", block.getSort());
                b.put("payload", block.getPayload());
            }
            return json.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private UUID audit(UUID actorId, String action, String resourceId, Map<String, String> meta) {
        AuditLogEntity row = new AuditLogEntity();
        row.setId(UUID.randomUUID());
        row.setActorId(actorId);
        row.setAction(action);
        row.setResourceType("learning_unit");
        row.setResourceId(resourceId);
        try {
            row.setMeta(json.writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            row.setMeta("{}");
        }
        row.setAt(Instant.now());
        audits.save(row);
        return row.getId();
    }

    private LearningUnitEntity bySlug(String slug) {
        return units.findBySlugAndLocale(slug, LOCALE)
                .orElseThrow(() -> ApiException.notFound("Unit not found"));
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw ApiException.validation("Title is required");
        }
        return title.trim();
    }

    private static String[] toArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new String[0];
        }
        return values.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private static String bumpVersion(String current) {
        if (current == null || current.isBlank()) {
            return "1.0.0";
        }
        String[] parts = current.split("\\.");
        try {
            if (parts.length >= 3) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]);
                if (major == 0) {
                    return "1.0.0";
                }
                return major + "." + minor + "." + (patch + 1);
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return current + ".published";
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback);
    }
}
