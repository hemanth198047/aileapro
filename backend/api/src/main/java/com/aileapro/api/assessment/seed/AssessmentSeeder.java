package com.aileapro.api.assessment.seed;

import com.aileapro.api.assessment.AssessmentItemRepository;
import com.aileapro.api.assessment.AssessmentRepository;
import com.aileapro.api.assessment.Blueprint;
import com.aileapro.api.assessment.FlashcardRepository;
import com.aileapro.api.assessment.MisconceptionCatalogRepository;
import com.aileapro.api.assessment.QuestionRepository;
import com.aileapro.api.assessment.seed.AssessmentSeedModel.SeedAssessment;
import com.aileapro.api.assessment.seed.AssessmentSeedModel.SeedCard;
import com.aileapro.api.assessment.seed.AssessmentSeedModel.SeedChoice;
import com.aileapro.api.assessment.seed.AssessmentSeedModel.SeedFile;
import com.aileapro.api.assessment.seed.AssessmentSeedModel.SeedItem;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.UnitBlockRepository;
import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.BlockType;
import com.aileapro.domain.BloomLevel;
import com.aileapro.domain.ContentStatus;
import com.aileapro.domain.ItemType;
import com.aileapro.persistence.AssessmentEntity;
import com.aileapro.persistence.AssessmentItemEntity;
import com.aileapro.persistence.FlashcardEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.MisconceptionCatalogEntity;
import com.aileapro.persistence.QuestionEntity;
import com.aileapro.persistence.UnitBlockEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loads the authored question bank, assessments, and review cards.
 *
 * <p>Runs after the curriculum seeder because everything here hangs off a unit slug, and it
 * derives the misconception catalog from the {@code misconception} blocks the curriculum
 * seeder just wrote. That direction matters: a misconception code exists because a page
 * teaches against it, so the page is the source and the catalog is the index. A code with no
 * block behind it would let the system label a learner with something it cannot then explain,
 * which is why an item citing an unknown code fails the seed.
 *
 * <p>Idempotent by slug, like the curriculum seeder. An assessment's item list is replaced
 * wholesale, because the file is the source of truth for what the assessment contains.
 */
@Component
public class AssessmentSeeder {

    private static final Logger log = LoggerFactory.getLogger(AssessmentSeeder.class);
    private static final String LOCATION = "classpath*:seed/assessment/*.yml";
    private static final String LOCALE = "en";

    private final LearningUnitRepository units;
    private final UnitBlockRepository blocks;
    private final QuestionRepository questions;
    private final AssessmentRepository assessments;
    private final AssessmentItemRepository assessmentItems;
    private final MisconceptionCatalogRepository catalog;
    private final FlashcardRepository flashcards;
    private final ObjectMapper yaml;
    private final ObjectMapper json;

    public AssessmentSeeder(
            LearningUnitRepository units,
            UnitBlockRepository blocks,
            QuestionRepository questions,
            AssessmentRepository assessments,
            AssessmentItemRepository assessmentItems,
            MisconceptionCatalogRepository catalog,
            FlashcardRepository flashcards) {
        this.units = units;
        this.blocks = blocks;
        this.questions = questions;
        this.assessments = assessments;
        this.assessmentItems = assessmentItems;
        this.catalog = catalog;
        this.flashcards = flashcards;
        this.yaml = JsonMapper.builder(new YAMLFactory()).addModule(new JavaTimeModule()).build();
        this.json = new ObjectMapper();
    }

    public record SeedReport(int misconceptions, int items, int assessments, int assessmentItems, int cards) {
        @Override
        public String toString() {
            return "misconceptions=%d items=%d assessments=%d assessmentItems=%d cards=%d"
                    .formatted(misconceptions, items, assessments, assessmentItems, cards);
        }
    }

    @Transactional
    public SeedReport seed() {
        List<SeedFile> files = read();
        List<SeedItem> seedItems = files.stream().flatMap(f -> nullSafe(f.items()).stream()).toList();
        List<SeedAssessment> seedAssessments =
                files.stream().flatMap(f -> nullSafe(f.assessments()).stream()).toList();
        List<SeedCard> seedCards = files.stream().flatMap(f -> nullSafe(f.cards()).stream()).toList();

        Set<String> codes = rewriteCatalog();

        Map<String, UUID> itemIdsBySlug = new LinkedHashMap<>();
        for (SeedItem item : seedItems) {
            itemIdsBySlug.put(item.slug(), upsertItem(item, codes).getId());
        }

        int linked = 0;
        for (SeedAssessment assessment : seedAssessments) {
            linked += upsertAssessment(assessment, itemIdsBySlug);
        }

        for (SeedCard card : seedCards) {
            upsertCard(card);
        }

        SeedReport report = new SeedReport(
                codes.size(), seedItems.size(), seedAssessments.size(), linked, seedCards.size());
        log.info("Assessment seed applied: {}", report);
        return report;
    }

    private List<SeedFile> read() {
        try {
            Resource[] found = new PathMatchingResourcePatternResolver().getResources(LOCATION);
            if (found.length == 0) {
                throw new IllegalStateException("No assessment seed files found at " + LOCATION);
            }
            List<Resource> ordered = new ArrayList<>(Arrays.asList(found));
            ordered.sort(Comparator.comparing(resource -> String.valueOf(resource.getFilename())));
            List<SeedFile> parsed = new ArrayList<>(ordered.size());
            for (Resource resource : ordered) {
                try (InputStream in = resource.getInputStream()) {
                    parsed.add(yaml.readValue(in, SeedFile.class));
                } catch (IOException broken) {
                    throw new IllegalStateException(
                            "Could not read assessment seed file " + resource.getFilename(), broken);
                }
            }
            return parsed;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Rebuilds the catalog from the misconception blocks in the curriculum, and keeps the id
     * of the block that reteaches each one so a flag can always be turned back into a page.
     */
    private Set<String> rewriteCatalog() {
        Set<String> codes = new LinkedHashSet<>();
        for (LearningUnitEntity unit : units.findAll()) {
            for (UnitBlockEntity block : blocks.findByUnitIdAndLocaleOrderBySortAsc(unit.getId(), LOCALE)) {
                if (block.getBlockType() != BlockType.MISCONCEPTION) {
                    continue;
                }
                JsonNode payload = parse(block.getPayload(), unit.getSlug());
                String code = text(payload, "code");
                String claim = text(payload, "claim");
                if (code == null || claim == null) {
                    throw new IllegalStateException(
                            "Misconception block on unit '" + unit.getSlug() + "' needs a code and a claim");
                }
                if (!codes.add(code)) {
                    throw new IllegalStateException("Misconception code '" + code + "' is claimed by two blocks");
                }
                MisconceptionCatalogEntity row = catalog.findById(code).orElseGet(() -> {
                    MisconceptionCatalogEntity fresh = new MisconceptionCatalogEntity();
                    fresh.setCode(code);
                    return fresh;
                });
                row.setUnitId(unit.getId());
                row.setClaim(claim);
                row.setReteachBlockId(block.getId());
                row.setUpdatedAt(Instant.now());
                catalog.save(row);
            }
        }
        List<MisconceptionCatalogEntity> stale =
                catalog.findAll().stream().filter(row -> !codes.contains(row.getCode())).toList();
        catalog.deleteAll(stale);
        return codes;
    }

    private QuestionEntity upsertItem(SeedItem seed, Set<String> codes) {
        LearningUnitEntity unit = requireUnit(seed.unit(), "Item '" + seed.slug() + "'");
        ItemType type = ItemType.fromCode(seed.type());
        BloomLevel bloom = BloomLevel.fromCode(require(seed.bloom(), "bloom", seed.slug()));

        for (String code : nullSafe(seed.misconceptions())) {
            requireCode(code, codes, "Item '" + seed.slug() + "'");
        }
        for (SeedChoice choice : nullSafe(seed.choices())) {
            if (choice.misconception() != null) {
                requireCode(choice.misconception(), codes, "Choice '" + choice.id() + "' of item '" + seed.slug() + "'");
            }
        }

        QuestionEntity item = questions.findBySlug(seed.slug()).orElseGet(() -> {
            QuestionEntity fresh = new QuestionEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        item.setUnitId(unit.getId());
        item.setItemType(type);
        item.setDifficulty(seed.difficulty() == null ? (short) 2 : seed.difficulty());
        item.setBloom(bloom);
        item.setStem(require(seed.stem(), "stem", seed.slug()));
        item.setData(itemData(seed, type));
        item.setExplanation(require(seed.explanation(), "explanation", seed.slug()));
        item.setMisconceptionCodes(nullSafe(seed.misconceptions()).toArray(String[]::new));
        item.setEstimateSeconds(seed.estimateSeconds());
        item.setStatus(ContentStatus.fromCode(seed.status() == null ? "published" : seed.status()));
        item.setVersion(seed.version() == null ? "1.0.0" : seed.version());
        item.setUpdatedAt(Instant.now());
        return questions.save(item);
    }

    /**
     * Builds the stored answer key and validates it for the type, because a malformed key is
     * not a display bug: it marks a correct answer wrong, and the learner has no way to tell.
     */
    private String itemData(SeedItem seed, ItemType type) {
        ObjectNode data = json.createObjectNode();
        List<SeedChoice> choices = nullSafe(seed.choices());
        List<String> accepted = nullSafe(seed.accepted());

        switch (type) {
            case MCQ, TRUE_FALSE, MULTI_SELECT -> {
                if (choices.size() < 2) {
                    throw new IllegalStateException("Item '" + seed.slug() + "' needs at least two choices");
                }
                ArrayNode options = data.putArray("choices");
                Set<String> ids = new LinkedHashSet<>();
                int correct = 0;
                for (SeedChoice choice : choices) {
                    String id = require(choice.id(), "choice id", seed.slug());
                    if (!ids.add(id)) {
                        throw new IllegalStateException("Item '" + seed.slug() + "' repeats choice id '" + id + "'");
                    }
                    ObjectNode option = options.addObject();
                    option.put("id", id);
                    option.put("text", require(choice.text(), "choice text", seed.slug()));
                    boolean isCorrect = Boolean.TRUE.equals(choice.correct());
                    option.put("correct", isCorrect);
                    if (choice.misconception() != null) {
                        option.put("misconception", choice.misconception());
                    }
                    if (isCorrect) {
                        correct++;
                    }
                }
                if (correct == 0) {
                    throw new IllegalStateException("Item '" + seed.slug() + "' has no correct choice");
                }
                if (type != ItemType.MULTI_SELECT && correct > 1) {
                    throw new IllegalStateException(
                            "Item '" + seed.slug() + "' is a " + type.code() + " with " + correct + " correct choices");
                }
                if (type == ItemType.MULTI_SELECT && correct == choices.size()) {
                    throw new IllegalStateException(
                            "Item '" + seed.slug() + "' marks every choice correct, so it tests nothing");
                }
                if (type == ItemType.TRUE_FALSE && choices.size() != 2) {
                    throw new IllegalStateException("Item '" + seed.slug() + "' is true/false with " + choices.size()
                            + " choices");
                }
            }
            case SHORT_TEXT -> {
                if (seed.answer() != null) {
                    data.put("answer", seed.answer());
                    // A numeric answer with no tolerance is a trap: 0.333 fails against 1/3.
                    data.put("tolerance", seed.tolerance() == null ? 0.0001 : Math.abs(seed.tolerance()));
                } else if (accepted.isEmpty()) {
                    throw new IllegalStateException(
                            "Item '" + seed.slug() + "' is short text with neither accepted answers nor a number");
                }
                if (!accepted.isEmpty()) {
                    ArrayNode list = data.putArray("accepted");
                    accepted.forEach(list::add);
                }
            }
            case CODING_STUB -> {
                if (accepted.isEmpty()) {
                    throw new IllegalStateException("Item '" + seed.slug() + "' is a coding stub with no reference answer");
                }
                ArrayNode list = data.putArray("accepted");
                accepted.forEach(list::add);
            }
        }

        try {
            return json.writeValueAsString(data);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the key for item '" + seed.slug() + "'", e);
        }
    }

    private int upsertAssessment(SeedAssessment seed, Map<String, UUID> itemIdsBySlug) {
        AssessmentKind kind = AssessmentKind.fromCode(seed.kind());
        UUID unitId = null;
        if (seed.unit() != null) {
            unitId = requireUnit(seed.unit(), "Assessment '" + seed.slug() + "'").getId();
        } else if (kind != AssessmentKind.DIAGNOSTIC) {
            throw new IllegalStateException(
                    "Assessment '" + seed.slug() + "' is a " + kind.code() + " with no unit, so nothing it grades "
                            + "can be attributed to a page");
        }

        AssessmentEntity assessment = assessments.findBySlug(seed.slug()).orElseGet(() -> {
            AssessmentEntity fresh = new AssessmentEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        assessment.setTitle(require(seed.title(), "title", seed.slug()));
        assessment.setKind(kind);
        assessment.setUnitId(unitId);
        assessment.setDescription(seed.description());
        assessment.setBlueprint(blueprintJson(seed, itemIdsBySlug));
        assessment.setTimeLimitSeconds(seed.timeLimitSeconds());
        assessment.setPassMark(seed.passMark() == null
                ? new BigDecimal("0.70")
                : BigDecimal.valueOf(seed.passMark()));
        assessment.setStatus(ContentStatus.fromCode(seed.status() == null ? "published" : seed.status()));
        assessment.setVersion(seed.version() == null ? "1.0.0" : seed.version());
        assessment.setUpdatedAt(Instant.now());
        assessments.save(assessment);

        assessmentItems.deleteByIdAssessmentId(assessment.getId());
        assessmentItems.flush();
        List<String> itemSlugs = nullSafe(seed.items());
        if (itemSlugs.isEmpty() && kind != AssessmentKind.DIAGNOSTIC) {
            throw new IllegalStateException("Assessment '" + seed.slug() + "' contains no items");
        }
        List<AssessmentItemEntity> rows = new ArrayList<>(itemSlugs.size());
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < itemSlugs.size(); i++) {
            String slug = itemSlugs.get(i);
            if (!seen.add(slug)) {
                throw new IllegalStateException(
                        "Assessment '" + seed.slug() + "' lists item '" + slug + "' twice");
            }
            UUID itemId = itemIdsBySlug.get(slug);
            if (itemId == null) {
                throw new IllegalStateException(
                        "Assessment '" + seed.slug() + "' references unknown item '" + slug + "'");
            }
            rows.add(new AssessmentItemEntity(assessment.getId(), itemId, i, BigDecimal.ONE));
        }
        assessmentItems.saveAll(rows);
        return rows.size();
    }

    /**
     * Serialises the blueprint and checks that everything it names exists. An adaptive
     * assessment resolves its items while a learner is sitting in front of it, so a typo
     * here would surface as a broken attempt rather than as a failed build.
     */
    private String blueprintJson(SeedAssessment seed, Map<String, UUID> itemIdsBySlug) {
        Map<String, Object> raw = seed.blueprint() == null ? Map.of() : seed.blueprint();
        String serialised;
        try {
            serialised = json.writeValueAsString(raw);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the blueprint for '" + seed.slug() + "'", e);
        }

        Blueprint blueprint = Blueprint.parse(serialised, json);
        for (Blueprint.Domain domain : blueprint.domains()) {
            requireUnit(domain.unit(), "Blueprint of '" + seed.slug() + "'");
            if (domain.items().size() < blueprint.stopRules().minPerDomain()) {
                throw new IllegalStateException("Blueprint of '" + seed.slug() + "' offers domain '" + domain.unit()
                        + "' fewer items than its own minimum of " + blueprint.stopRules().minPerDomain());
            }
            for (String slug : domain.items()) {
                if (!itemIdsBySlug.containsKey(slug)) {
                    throw new IllegalStateException("Blueprint of '" + seed.slug() + "' references unknown item '"
                            + slug + "'");
                }
            }
        }
        return serialised;
    }

    private void upsertCard(SeedCard seed) {
        LearningUnitEntity unit = requireUnit(seed.unit(), "Card '" + seed.slug() + "'");
        FlashcardEntity card = flashcards.findBySlug(seed.slug()).orElseGet(() -> {
            FlashcardEntity fresh = new FlashcardEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            return fresh;
        });
        card.setUnitId(unit.getId());
        card.setUserId(null);
        card.setFront(require(seed.front(), "front", seed.slug()));
        card.setBack(require(seed.back(), "back", seed.slug()));
        card.setOrigin("author");
        card.setLocked(!Boolean.FALSE.equals(seed.locked()));
        card.setStatus(ContentStatus.fromCode(seed.status() == null ? "published" : seed.status()));
        card.setUpdatedAt(Instant.now());
        flashcards.save(card);
    }

    private LearningUnitEntity requireUnit(String slug, String who) {
        if (slug == null) {
            throw new IllegalStateException(who + " names no unit");
        }
        return units.findBySlugAndLocale(slug, LOCALE)
                .orElseThrow(() -> new IllegalStateException(who + " references unknown unit '" + slug + "'"));
    }

    private static void requireCode(String code, Set<String> codes, String who) {
        if (!codes.contains(code)) {
            throw new IllegalStateException(who + " cites misconception '" + code
                    + "', which no unit teaches against. Write the misconception block first.");
        }
    }

    private JsonNode parse(String payload, String unitSlug) {
        try {
            return json.readTree(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unreadable block payload on unit '" + unitSlug + "'", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String require(String value, String field, String slug) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("'" + slug + "' is missing " + field);
        }
        return value;
    }

    private static <T> List<T> nullSafe(List<T> value) {
        return value == null ? List.of() : value;
    }
}
