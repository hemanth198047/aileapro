package com.aileapro.api.intelligence.seed;

import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.SourceRepository;
import com.aileapro.api.intelligence.AiUpdateRepository;
import com.aileapro.api.intelligence.AiUpdateSourceRepository;
import com.aileapro.api.intelligence.CurriculumChangeProposalRepository;
import com.aileapro.api.intelligence.IntelligenceService;
import com.aileapro.domain.CurriculumImpactScore;
import com.aileapro.persistence.AiUpdateEntity;
import com.aileapro.persistence.AiUpdateSourceEntity;
import com.aileapro.persistence.CurriculumChangeProposalEntity;
import com.aileapro.persistence.SourceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Two manual AIUpdate examples (model release + deprecation proposal) — no scrapers.
 */
@Component
public class IntelligenceSeeder {

    private final AiUpdateRepository updates;
    private final AiUpdateSourceRepository updateSources;
    private final CurriculumChangeProposalRepository proposals;
    private final SourceRepository sources;
    private final LearningUnitRepository units;
    private final ObjectMapper json;

    public IntelligenceSeeder(
            AiUpdateRepository updates,
            AiUpdateSourceRepository updateSources,
            CurriculumChangeProposalRepository proposals,
            SourceRepository sources,
            LearningUnitRepository units,
            ObjectMapper json) {
        this.updates = updates;
        this.updateSources = updateSources;
        this.proposals = proposals;
        this.sources = sources;
        this.units = units;
        this.json = json;
    }

    public record SeedReport(int updates, int proposals) {
    }

    @Transactional
    public SeedReport seed() {
        int updateCount = 0;
        int proposalCount = 0;
        if (updates.findBySlug("attention-paper-still-foundational").isEmpty()) {
            seedPublishedAttention();
            updateCount++;
        }
        if (updates.findBySlug("legacy-bag-of-words-deprecation").isEmpty()) {
            seedDeprecationWithPendingProposal();
            updateCount++;
            proposalCount++;
        } else if (proposals.findByStatusOrderByCreatedAtDesc("pending").isEmpty()) {
            // Ensure at least one pending proposal exists for admin reject demos.
            AiUpdateEntity dep = updates.findBySlug("legacy-bag-of-words-deprecation").orElseThrow();
            seedPendingProposal(dep);
            proposalCount++;
        }
        return new SeedReport(updateCount, proposalCount);
    }

    private void seedPublishedAttention() {
        Instant verified = Instant.now().minus(2, ChronoUnit.DAYS);
        SourceEntity attention = sources.findBySlug("attention-2017").orElse(null);
        AiUpdateEntity update = new AiUpdateEntity();
        update.setId(UUID.randomUUID());
        update.setSlug("attention-paper-still-foundational");
        update.setTitle("Attention is still the foundation for transformer units");
        update.setSummary(
                "The 2017 Attention Is All You Need paper remains the cited foundation for our "
                        + "attention units. No curriculum rewrite required; verification refreshed.");
        update.setOccurredAt(verified);
        update.setLifecycle("established");
        update.setStatus("published");
        update.setConfidence(new BigDecimal("0.900"));
        update.setVerifiedAt(verified);
        update.setVerifiedSourceCount(attention == null ? 0 : 1);
        update.setCurrentnessNote(IntelligenceService.currentnessNote(attention == null ? 0 : 1, verified));
        update.setCreatedAt(verified);
        update.setUpdatedAt(verified);
        updates.save(update);
        if (attention != null) {
            AiUpdateSourceEntity link = new AiUpdateSourceEntity();
            link.setUpdateId(update.getId());
            link.setSourceId(attention.getId());
            updateSources.save(link);
        }
    }

    private void seedDeprecationWithPendingProposal() {
        Instant occurred = Instant.now().minus(5, ChronoUnit.DAYS);
        SourceEntity numpy = sources.findBySlug("numpy-absolute-basics").orElse(null);
        AiUpdateEntity update = new AiUpdateEntity();
        update.setId(UUID.randomUUID());
        update.setSlug("legacy-bag-of-words-deprecation");
        update.setTitle("Bag-of-words baselines marked for deprecation guidance");
        update.setSummary(
                "Industry hiring screens still mention bag-of-words, but production NLP paths "
                        + "should steer learners toward embeddings. A proposal awaits review.");
        update.setOccurredAt(occurred);
        update.setLifecycle("deprecated");
        update.setStatus("published");
        update.setConfidence(new BigDecimal("0.650"));
        update.setVerifiedAt(occurred);
        update.setVerifiedSourceCount(numpy == null ? 0 : 1);
        update.setCurrentnessNote(IntelligenceService.currentnessNote(numpy == null ? 0 : 1, occurred));
        update.setCreatedAt(occurred);
        update.setUpdatedAt(occurred);
        updates.save(update);
        if (numpy != null) {
            AiUpdateSourceEntity link = new AiUpdateSourceEntity();
            link.setUpdateId(update.getId());
            link.setSourceId(numpy.getId());
            updateSources.save(link);
        }
        seedPendingProposal(update);
    }

    private void seedPendingProposal(AiUpdateEntity update) {
        CurriculumImpactScore.Result impact = CurriculumImpactScore.score(new CurriculumImpactScore.Flags(
                false, true, true, false, false, true, false, false, true, true, true));
        CurriculumChangeProposalEntity proposal = new CurriculumChangeProposalEntity();
        proposal.setId(UUID.randomUUID());
        proposal.setUpdateId(update.getId());
        units.findBySlugAndLocale("python-values-and-types", "en")
                .ifPresent(u -> proposal.setUnitId(u.getId()));
        proposal.setTitle("Add deprecation callout to bag-of-words mentions");
        proposal.setDiffJson("{\"note\":\"pending human review\"}");
        proposal.setImpactScore(BigDecimal.valueOf(impact.score()).setScale(3, RoundingMode.HALF_UP));
        try {
            proposal.setImpactReasons(json.writeValueAsString(impact.reasons()));
        } catch (Exception e) {
            proposal.setImpactReasons("[]");
        }
        proposal.setStatus("pending");
        proposal.setCreatedAt(Instant.now());
        proposals.save(proposal);
    }
}
