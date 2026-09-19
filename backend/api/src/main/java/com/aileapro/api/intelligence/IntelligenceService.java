package com.aileapro.api.intelligence;

import com.aileapro.api.admin.CurriculumChangeRepository;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.SourceRepository;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.CurriculumImpactScore;
import com.aileapro.persistence.AiUpdateEntity;
import com.aileapro.persistence.AiUpdateSourceEntity;
import com.aileapro.persistence.CurriculumChangeProposalEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import com.aileapro.persistence.SourceEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IntelligenceService {

    private final AiUpdateRepository updates;
    private final AiUpdateSourceRepository updateSources;
    private final CurriculumChangeProposalRepository proposals;
    private final SourceRepository sources;
    private final LearningUnitRepository units;
    private final MasteryRecordRepository mastery;
    private final CurriculumChangeRepository changes;
    private final ObjectMapper json;

    public IntelligenceService(
            AiUpdateRepository updates,
            AiUpdateSourceRepository updateSources,
            CurriculumChangeProposalRepository proposals,
            SourceRepository sources,
            LearningUnitRepository units,
            MasteryRecordRepository mastery,
            CurriculumChangeRepository changes,
            ObjectMapper json) {
        this.updates = updates;
        this.updateSources = updateSources;
        this.proposals = proposals;
        this.sources = sources;
        this.units = units;
        this.mastery = mastery;
        this.changes = changes;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public IntelligenceViews.Feed feed() {
        List<AiUpdateEntity> published = updates.findByStatusOrderByOccurredAtDesc("published");
        Map<UUID, List<String>> sourceSlugs = sourceSlugsByUpdate(
                published.stream().map(AiUpdateEntity::getId).toList());
        List<IntelligenceViews.FeedItem> items = published.stream()
                .map(u -> new IntelligenceViews.FeedItem(
                        u.getSlug(),
                        u.getTitle(),
                        u.getSummary(),
                        u.getOccurredAt(),
                        u.getLifecycle(),
                        u.getVerifiedAt(),
                        u.getVerifiedSourceCount(),
                        u.getCurrentnessNote(),
                        sourceSlugs.getOrDefault(u.getId(), List.of())))
                .toList();
        return new IntelligenceViews.Feed(
                items,
                "Never 'fully up to date.' Each item states how many sources it was verified against and when.");
    }

    @Transactional(readOnly = true)
    public List<IntelligenceViews.UpdateSummary> listUpdates() {
        return updates.findAllByOrderByOccurredAtDesc().stream()
                .map(u -> new IntelligenceViews.UpdateSummary(
                        u.getId(),
                        u.getSlug(),
                        u.getTitle(),
                        u.getStatus(),
                        u.getLifecycle(),
                        u.getOccurredAt(),
                        u.getVerifiedAt(),
                        u.getVerifiedSourceCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntelligenceViews.Proposal> listProposals(String status) {
        List<CurriculumChangeProposalEntity> rows =
                status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                        ? proposals.findAllByOrderByCreatedAtDesc()
                        : proposals.findByStatusOrderByCreatedAtDesc(status.trim().toLowerCase(Locale.ROOT));
        Map<UUID, String> slugs = new HashMap<>();
        for (CurriculumChangeProposalEntity row : rows) {
            slugs.computeIfAbsent(row.getUpdateId(), id -> updates.findById(id).map(AiUpdateEntity::getSlug).orElse("?"));
        }
        return rows.stream().map(p -> toProposal(p, slugs.get(p.getUpdateId()))).toList();
    }

    @Transactional
    public IntelligenceViews.UpdateSummary createUpdate(UUID actorId, IntelligenceViews.CreateUpdateBody body) {
        if (body.slug() == null || body.slug().isBlank()) {
            throw ApiException.validation("Slug is required");
        }
        String slug = body.slug().trim().toLowerCase(Locale.ROOT);
        if (updates.findBySlug(slug).isPresent()) {
            throw ApiException.conflict("Update slug already exists");
        }
        List<SourceEntity> linked = resolveSources(body.sourceSlugs());
        Instant now = Instant.now();
        AiUpdateEntity update = new AiUpdateEntity();
        update.setId(UUID.randomUUID());
        update.setSlug(slug);
        update.setTitle(require(body.title(), "Title"));
        update.setSummary(require(body.summary(), "Summary"));
        update.setOccurredAt(now);
        update.setLifecycle(body.lifecycle() == null || body.lifecycle().isBlank() ? "emerging" : body.lifecycle());
        update.setStatus(body.publish() ? "published" : "draft");
        update.setConfidence(new BigDecimal("0.700"));
        update.setVerifiedSourceCount(linked.size());
        update.setVerifiedAt(linked.isEmpty() ? null : now);
        update.setCurrentnessNote(currentnessNote(linked.size(), now));
        update.setCreatedBy(actorId);
        update.setCreatedAt(now);
        update.setUpdatedAt(now);
        updates.save(update);
        for (SourceEntity source : linked) {
            AiUpdateSourceEntity link = new AiUpdateSourceEntity();
            link.setUpdateId(update.getId());
            link.setSourceId(source.getId());
            updateSources.save(link);
        }
        return new IntelligenceViews.UpdateSummary(
                update.getId(),
                update.getSlug(),
                update.getTitle(),
                update.getStatus(),
                update.getLifecycle(),
                update.getOccurredAt(),
                update.getVerifiedAt(),
                update.getVerifiedSourceCount());
    }

    @Transactional
    public IntelligenceViews.Proposal createProposal(IntelligenceViews.CreateProposalBody body) {
        AiUpdateEntity update = updates.findBySlug(body.updateSlug())
                .orElseThrow(() -> ApiException.notFound("Update not found"));
        UUID unitId = null;
        if (body.unitSlug() != null && !body.unitSlug().isBlank()) {
            unitId = units.findBySlugAndLocale(body.unitSlug().trim(), "en")
                    .map(LearningUnitEntity::getId)
                    .orElseThrow(() -> ApiException.notFound("Unit not found"));
        }
        CurriculumImpactScore.Result impact = CurriculumImpactScore.score(new CurriculumImpactScore.Flags(
                body.newConcept(),
                body.changeExisting(),
                body.obsoleteTechnique(),
                body.toolChange(),
                body.apiChange(),
                body.patternChange(),
                body.newPrereq(),
                body.newSpecialization(),
                body.industryExpectation(),
                body.newExerciseOrAssessment(),
                body.careerSkillChange()));
        CurriculumChangeProposalEntity proposal = new CurriculumChangeProposalEntity();
        proposal.setId(UUID.randomUUID());
        proposal.setUpdateId(update.getId());
        proposal.setUnitId(unitId);
        proposal.setTitle(require(body.title(), "Title"));
        proposal.setDiffJson("{}");
        proposal.setImpactScore(BigDecimal.valueOf(impact.score()).setScale(3, RoundingMode.HALF_UP));
        try {
            proposal.setImpactReasons(json.writeValueAsString(impact.reasons()));
        } catch (JsonProcessingException e) {
            proposal.setImpactReasons("[]");
        }
        proposal.setStatus("pending");
        proposal.setCreatedAt(Instant.now());
        proposals.save(proposal);
        return toProposal(proposal, update.getSlug());
    }

    @Transactional
    public IntelligenceViews.Proposal reject(UUID actorId, UUID proposalId, IntelligenceViews.RejectBody body) {
        CurriculumChangeProposalEntity proposal = proposals.findById(proposalId)
                .orElseThrow(() -> ApiException.notFound("Proposal not found"));
        if (!"pending".equals(proposal.getStatus())) {
            throw ApiException.validation("Only pending proposals can be rejected");
        }
        proposal.setStatus("rejected");
        proposal.setRejectReason(body == null || body.reason() == null || body.reason().isBlank()
                ? "Rejected by admin"
                : body.reason().trim());
        proposal.setReviewedBy(actorId);
        proposal.setReviewedAt(Instant.now());
        proposals.save(proposal);
        String slug = updates.findById(proposal.getUpdateId()).map(AiUpdateEntity::getSlug).orElse("?");
        return toProposal(proposal, slug);
    }

    @Transactional
    public IntelligenceViews.Proposal approve(UUID actorId, UUID proposalId) {
        CurriculumChangeProposalEntity proposal = proposals.findById(proposalId)
                .orElseThrow(() -> ApiException.notFound("Proposal not found"));
        if (!"pending".equals(proposal.getStatus())) {
            throw ApiException.validation("Only pending proposals can be approved");
        }
        proposal.setStatus("approved");
        proposal.setReviewedBy(actorId);
        proposal.setReviewedAt(Instant.now());
        proposals.save(proposal);
        String slug = updates.findById(proposal.getUpdateId()).map(AiUpdateEntity::getSlug).orElse("?");
        return toProposal(proposal, slug);
    }

    @Transactional(readOnly = true)
    public IntelligenceViews.ChangesSince changesSince(UUID userId, String slug) {
        LearningUnitEntity unit = units.findBySlugAndLocale(slug, "en")
                .orElseThrow(() -> ApiException.notFound("Unit not found"));
        Instant since = mastery.findByUserIdAndUnitId(userId, unit.getId())
                .map(MasteryRecordEntity::getLastEventAt)
                .orElse(Instant.EPOCH);
        if (since == null) {
            since = Instant.EPOCH;
        }
        Instant cutoff = since;
        List<IntelligenceViews.UnitChange> rows = changes.findByUnitIdOrderByCreatedAtDesc(unit.getId()).stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(cutoff))
                .map(c -> new IntelligenceViews.UnitChange(
                        c.getId(),
                        c.getAction(),
                        c.getFromVersion(),
                        c.getToVersion(),
                        c.getCreatedAt(),
                        c.getReason()))
                .toList();
        return new IntelligenceViews.ChangesSince(
                slug,
                since,
                rows,
                "Curriculum publish/rollback rows after your last learning event on this unit. Empty means nothing changed since then.");
    }

    public static String currentnessNote(int sourceCount, Instant verifiedAt) {
        return "Verified against " + sourceCount + " source"
                + (sourceCount == 1 ? "" : "s")
                + " as of "
                + verifiedAt
                + ". This is not a claim that the field is fully up to date.";
    }

    private Map<UUID, List<String>> sourceSlugsByUpdate(List<UUID> updateIds) {
        if (updateIds.isEmpty()) {
            return Map.of();
        }
        List<AiUpdateSourceEntity> links = updateSources.findByUpdateIdIn(updateIds);
        Map<UUID, String> sourceSlug = sources.findAllById(
                        links.stream().map(AiUpdateSourceEntity::getSourceId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(SourceEntity::getId, SourceEntity::getSlug));
        Map<UUID, List<String>> out = new HashMap<>();
        for (AiUpdateSourceEntity link : links) {
            out.computeIfAbsent(link.getUpdateId(), id -> new ArrayList<>())
                    .add(sourceSlug.getOrDefault(link.getSourceId(), link.getSourceId().toString()));
        }
        return out;
    }

    private List<SourceEntity> resolveSources(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return List.of();
        }
        List<SourceEntity> linked = new ArrayList<>();
        for (String slug : slugs) {
            linked.add(sources.findBySlug(slug.trim())
                    .orElseThrow(() -> ApiException.notFound("Source not found: " + slug)));
        }
        return linked;
    }

    private IntelligenceViews.Proposal toProposal(CurriculumChangeProposalEntity p, String updateSlug) {
        List<String> reasons;
        try {
            reasons = json.readValue(p.getImpactReasons(), json.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            reasons = List.of();
        }
        return new IntelligenceViews.Proposal(
                p.getId(),
                p.getUpdateId(),
                updateSlug,
                p.getTitle(),
                p.getImpactScore(),
                reasons,
                p.getStatus(),
                p.getRejectReason(),
                p.getCreatedAt(),
                p.getReviewedAt());
    }

    private static String require(String value, String label) {
        if (value == null || value.isBlank()) {
            throw ApiException.validation(label + " is required");
        }
        return value.trim();
    }
}
