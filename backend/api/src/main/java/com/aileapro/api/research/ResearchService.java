package com.aileapro.api.research;

import com.aileapro.ai.ResearchPolicy;
import com.aileapro.ai.ResearchPolicy.Decision;
import com.aileapro.ai.ResearchPolicy.PaperRef;
import com.aileapro.api.curriculum.LearningResourceRepository;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.curriculum.SourceRepository;
import com.aileapro.api.curriculum.UnitResourceRepository;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.SourceType;
import com.aileapro.persistence.LearningResourceEntity;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.SourceEntity;
import com.aileapro.persistence.UnitResourceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Research space: paper reading list, forced-citation explainer, literature map v0.
 *
 * <p>Citations can only come from attached PAPER sources. An empty retrieval set refuses
 * rather than inventing a reference — that is the whole product promise of this phase.
 */
@Service
public class ResearchService {

    private final SourceRepository sources;
    private final LearningResourceRepository resources;
    private final UnitResourceRepository unitResources;
    private final LearningUnitRepository units;

    public ResearchService(
            SourceRepository sources,
            LearningResourceRepository resources,
            UnitResourceRepository unitResources,
            LearningUnitRepository units) {
        this.sources = sources;
        this.resources = resources;
        this.unitResources = unitResources;
        this.units = units;
    }

    @Transactional(readOnly = true)
    public ResearchViews.ReadingList readingList() {
        List<ResearchViews.Paper> papers = paperEntities().stream().map(this::toPaper).toList();
        String note = papers.isEmpty()
                ? "No papers are seeded yet."
                : papers.size() + " seeded papers. Explainers may only cite from this list.";
        return new ResearchViews.ReadingList(papers, note);
    }

    @Transactional(readOnly = true)
    public ResearchViews.Paper getPaper(String slug) {
        SourceEntity source = sources.findBySlug(slug)
                .filter(s -> s.getSourceType() == SourceType.PAPER)
                .orElseThrow(() -> ApiException.notFound("Paper not found"));
        return toPaper(source);
    }

    @Transactional(readOnly = true)
    public ResearchViews.Explainer explain(String question, String paperSlug) {
        if (question == null || question.isBlank()) {
            throw ApiException.validation("Question is required");
        }
        List<PaperRef> corpus = paperEntities().stream().map(this::toRef).toList();
        List<PaperRef> attached = ResearchPolicy.retrieve(question, paperSlug, corpus, 3);
        Decision decision = ResearchPolicy.decide(attached);
        if (decision.refused()) {
            return new ResearchViews.Explainer(
                    question.trim(),
                    decision.refusal().reply(),
                    true,
                    decision.refusal().code(),
                    true,
                    List.of());
        }
        String raw = ResearchPolicy.fixtureReply(question.trim(), decision.attached());
        String answer = ResearchPolicy.enforceCitations(raw, decision.attached());
        List<ResearchViews.Citation> citations = decision.attached().stream()
                .map(p -> new ResearchViews.Citation(p.slug(), p.title(), p.doi(), p.url()))
                .toList();
        return new ResearchViews.Explainer(question.trim(), answer, false, null, true, citations);
    }

    /**
     * Test/helper path: runs an untrusted draft through citation enforcement. Used by the
     * gate to prove invented titles blow up and unsourced DOIs are stripped.
     */
    @Transactional(readOnly = true)
    public String sanitizeDraft(String draft, List<String> attachedSlugs) {
        List<PaperRef> attached = new ArrayList<>();
        for (String slug : attachedSlugs) {
            SourceEntity source = sources.findBySlug(slug)
                    .filter(s -> s.getSourceType() == SourceType.PAPER)
                    .orElseThrow(() -> ApiException.notFound("Paper not found: " + slug));
            attached.add(toRef(source));
        }
        return ResearchPolicy.enforceCitations(draft, attached);
    }

    @Transactional(readOnly = true)
    public ResearchViews.LiteratureMap literatureMap() {
        List<SourceEntity> papers = paperEntities();
        Map<String, ResearchViews.MapNode> nodes = new LinkedHashMap<>();
        List<ResearchViews.MapEdge> edges = new ArrayList<>();

        for (SourceEntity paper : papers) {
            String paperNode = "paper:" + paper.getSlug();
            nodes.put(paperNode, new ResearchViews.MapNode(paperNode, "paper", paper.getSlug(), paper.getTitle()));
            LearningResourceEntity resource = resources.findBySourceId(paper.getId()).orElse(null);
            if (resource == null) {
                continue;
            }
            for (UnitResourceEntity link : unitResources.findByResourceId(resource.getId())) {
                units.findById(link.getUnitId()).ifPresent(unit -> {
                    String unitNode = "unit:" + unit.getSlug();
                    nodes.putIfAbsent(
                            unitNode,
                            new ResearchViews.MapNode(unitNode, "unit", unit.getSlug(), unit.getTitle()));
                    edges.add(new ResearchViews.MapEdge(unitNode, paperNode, "cites"));
                });
            }
        }

        String note = edges.isEmpty()
                ? "Papers are listed; unit citation edges appear when a unit resource points at a paper."
                : nodes.size() + " nodes, " + edges.size() + " citation edges.";
        return new ResearchViews.LiteratureMap(List.copyOf(nodes.values()), edges, note);
    }

    private List<SourceEntity> paperEntities() {
        return sources.findBySourceTypeOrderByPublishedOnDescTitleAsc(SourceType.PAPER);
    }

    private ResearchViews.Paper toPaper(SourceEntity source) {
        return new ResearchViews.Paper(
                source.getId(),
                source.getSlug(),
                source.getTitle(),
                source.getAuthors(),
                source.getPublisher(),
                source.getUrl(),
                source.getDoi(),
                source.getAbstractText(),
                source.getPublishedOn(),
                relatedUnitSlugs(source.getId()));
    }

    private PaperRef toRef(SourceEntity source) {
        return new PaperRef(
                source.getSlug(),
                source.getTitle(),
                source.getDoi(),
                source.getUrl(),
                source.getAbstractText());
    }

    private List<String> relatedUnitSlugs(UUID sourceId) {
        LearningResourceEntity resource = resources.findBySourceId(sourceId).orElse(null);
        if (resource == null) {
            return List.of();
        }
        Set<String> slugs = new LinkedHashSet<>();
        for (UnitResourceEntity link : unitResources.findByResourceId(resource.getId())) {
            units.findById(link.getUnitId()).map(LearningUnitEntity::getSlug).ifPresent(slugs::add);
        }
        return List.copyOf(slugs);
    }
}
