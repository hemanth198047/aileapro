package com.aileapro.api.project;

import com.aileapro.api.assessment.UnitFactsService;
import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.lab.LabService;
import com.aileapro.api.lab.LabViews;
import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.PortfolioItemEntity;
import com.aileapro.persistence.ProjectApplyUnitEntity;
import com.aileapro.persistence.ProjectEntity;
import com.aileapro.persistence.ProjectMilestoneEntity;
import com.aileapro.persistence.SubmissionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Project Studio: briefs, milestone submissions through the lab sandbox, portfolio artifacts,
 * and Apply evidence on linked units when a published project is finished.
 */
@Service
public class ProjectService {

    private static final String LOCALE = "en";

    private final ProjectRepository projects;
    private final ProjectMilestoneRepository milestones;
    private final ProjectApplyUnitRepository applyUnits;
    private final SubmissionRepository submissions;
    private final PortfolioItemRepository portfolio;
    private final LearningUnitRepository units;
    private final LabService lab;
    private final GradedMasteryService graded;
    private final UnitFactsService unitFacts;

    public ProjectService(
            ProjectRepository projects,
            ProjectMilestoneRepository milestones,
            ProjectApplyUnitRepository applyUnits,
            SubmissionRepository submissions,
            PortfolioItemRepository portfolio,
            LearningUnitRepository units,
            LabService lab,
            GradedMasteryService graded,
            UnitFactsService unitFacts) {
        this.projects = projects;
        this.milestones = milestones;
        this.applyUnits = applyUnits;
        this.submissions = submissions;
        this.portfolio = portfolio;
        this.units = units;
        this.lab = lab;
        this.graded = graded;
        this.unitFacts = unitFacts;
    }

    @Transactional(readOnly = true)
    public List<ProjectViews.Summary> list(UUID userId) {
        return projects.findAllByOrderBySortOrderAscTitleAsc().stream()
                .map(project -> summary(project, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectViews.Detail get(UUID userId, String slug) {
        ProjectEntity project = bySlug(slug);
        List<ProjectMilestoneEntity> ms = milestones.findByProjectIdOrderBySortOrderAsc(project.getId());
        long passedCount = submissions.countPassedMilestones(userId, project.getId());
        boolean completed = !ms.isEmpty() && passedCount >= ms.size() && "published".equals(project.getStatus());
        UUID portfolioId = portfolio.findByUserIdAndProjectId(userId, project.getId())
                .map(PortfolioItemEntity::getId)
                .orElse(null);

        List<ProjectViews.Milestone> views = ms.stream()
                .map(m -> {
                    Optional<SubmissionEntity> pass =
                            submissions.findFirstByUserIdAndMilestoneIdAndPassedTrueOrderByCreatedAtDesc(
                                    userId, m.getId());
                    return new ProjectViews.Milestone(
                            m.getId(),
                            m.getSlug(),
                            m.getTitle(),
                            m.getDescription(),
                            m.getCriteria(),
                            m.getHint(),
                            m.getSortOrder(),
                            m.getCheckKey(),
                            m.getStarterSource(),
                            pass.isPresent(),
                            pass.map(SubmissionEntity::getCreatedAt).orElse(null));
                })
                .toList();

        return new ProjectViews.Detail(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getSummary(),
                project.getObjective(),
                project.getStatus(),
                project.getStarterSource(),
                applyUnitSlugs(project.getId()),
                views,
                (int) passedCount,
                completed,
                "published".equals(project.getStatus()) && !ms.isEmpty(),
                portfolioId);
    }

    @Transactional
    public ProjectViews.SubmissionResult submit(
            UUID userId, String projectSlug, String milestoneSlug, String source) {
        ProjectEntity project = bySlug(projectSlug);
        if (!"published".equals(project.getStatus())) {
            throw ApiException.validation("This project is an outline only — submissions are not open yet.");
        }
        ProjectMilestoneEntity milestone = milestones
                .findByProjectIdAndSlug(project.getId(), milestoneSlug)
                .orElseThrow(() -> ApiException.notFound("Milestone not found"));
        if (milestone.getCheckKey() == null || milestone.getCheckKey().isBlank()) {
            throw ApiException.validation("This milestone has no automated check yet.");
        }
        if (source == null || source.isBlank()) {
            throw ApiException.validation("Source is required");
        }

        String harness = ProjectCheck.harness(milestone.getCheckKey(), source);
        LabViews.Run run = lab.start(userId, "python", harness, null, 8000);
        boolean passed = ProjectCheck.passed(run.stdout());
        String mentor = ProjectCheck.mentorComment(passed, milestone.getHint());
        String review = passed
                ? "Automated check passed for milestone '" + milestone.getSlug() + "'."
                : "Automated check failed for milestone '" + milestone.getSlug() + "'.";

        SubmissionEntity row = new SubmissionEntity();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        row.setProjectId(project.getId());
        row.setMilestoneId(milestone.getId());
        row.setSource(source);
        row.setSourceHash(ProjectCheck.sha256(source));
        row.setLabRunId(run.id());
        row.setPassed(passed);
        row.setReview(review);
        row.setMentorComment(mentor);
        row.setCreatedAt(Instant.now());
        submissions.save(row);

        boolean projectCompleted = false;
        UUID portfolioItemId = null;
        List<String> applyUpdated = List.of();
        if (passed) {
            long milestoneTotal = milestones.countByProjectId(project.getId());
            long passedTotal = submissions.countPassedMilestones(userId, project.getId());
            if (milestoneTotal > 0 && passedTotal >= milestoneTotal) {
                PortfolioItemEntity item = ensurePortfolio(userId, project, row.getId());
                portfolioItemId = item.getId();
                applyUpdated = fillApply(userId, project, row.getId());
                projectCompleted = true;
            } else {
                portfolioItemId = portfolio.findByUserIdAndProjectId(userId, project.getId())
                        .map(PortfolioItemEntity::getId)
                        .orElse(null);
            }
        }

        return new ProjectViews.SubmissionResult(
                row.getId(),
                project.getSlug(),
                milestone.getSlug(),
                passed,
                review,
                mentor,
                run.stdout(),
                run.development(),
                projectCompleted,
                portfolioItemId,
                applyUpdated);
    }

    @Transactional(readOnly = true)
    public ProjectViews.Portfolio portfolio(UUID userId) {
        List<ProjectViews.PortfolioItem> items = portfolio.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::portfolioView)
                .toList();
        String note = items.isEmpty()
                ? "No artifacts yet. Completing a published project puts something here — private by default."
                : items.size() + " artifact" + (items.size() == 1 ? "" : "s") + ".";
        return new ProjectViews.Portfolio(items, note);
    }

    @Transactional
    public ProjectViews.PortfolioItem updatePortfolio(
            UUID userId, UUID id, String visibility, String reflection) {
        PortfolioItemEntity item = portfolio.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Portfolio item not found"));
        if (visibility != null && !visibility.isBlank()) {
            String v = visibility.trim().toLowerCase(Locale.ROOT);
            if (!"private".equals(v) && !"public".equals(v)) {
                throw ApiException.validation("Visibility must be private or public");
            }
            item.setVisibility(v);
        }
        if (reflection != null) {
            item.setReflection(reflection);
        }
        item.setUpdatedAt(Instant.now());
        portfolio.save(item);
        return portfolioView(item);
    }

    private List<String> fillApply(UUID userId, ProjectEntity project, UUID submissionId) {
        List<ProjectApplyUnitEntity> links = applyUnits.findByProjectId(project.getId());
        Set<UUID> unitIds = new HashSet<>();
        for (ProjectApplyUnitEntity link : links) {
            unitIds.add(link.getUnitId());
        }
        if (project.getUnitId() != null) {
            unitIds.add(project.getUnitId());
        }
        List<String> updated = new ArrayList<>();
        for (UUID unitId : unitIds) {
            LearningUnitEntity unit = units.findById(unitId).orElse(null);
            if (unit == null) {
                continue;
            }
            UnitFacts facts = unitFacts.factsFor(unitId);
            graded.recordProjectCompleted(
                    userId, unitId, facts, unit.getVolatility(), project.getId(), submissionId);
            updated.add(unit.getSlug());
        }
        return updated;
    }

    private PortfolioItemEntity ensurePortfolio(UUID userId, ProjectEntity project, UUID submissionId) {
        return portfolio.findByUserIdAndProjectId(userId, project.getId()).orElseGet(() -> {
            PortfolioItemEntity item = new PortfolioItemEntity();
            item.setId(UUID.randomUUID());
            item.setUserId(userId);
            item.setProjectId(project.getId());
            item.setSubmissionId(submissionId);
            item.setTitle(project.getTitle());
            item.setSummary(project.getSummary());
            item.setReflection("");
            item.setVisibility("private");
            item.setCreatedAt(Instant.now());
            item.setUpdatedAt(Instant.now());
            return portfolio.save(item);
        });
    }

    private ProjectViews.Summary summary(ProjectEntity project, UUID userId) {
        long total = milestones.countByProjectId(project.getId());
        long passed = submissions.countPassedMilestones(userId, project.getId());
        boolean accepts = "published".equals(project.getStatus()) && total > 0;
        boolean completed = accepts && passed >= total;
        return new ProjectViews.Summary(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getSummary(),
                project.getStatus(),
                (int) total,
                (int) passed,
                completed,
                accepts);
    }

    private ProjectViews.PortfolioItem portfolioView(PortfolioItemEntity item) {
        String slug = projects.findById(item.getProjectId()).map(ProjectEntity::getSlug).orElse("");
        return new ProjectViews.PortfolioItem(
                item.getId(),
                slug,
                item.getTitle(),
                item.getSummary(),
                item.getReflection(),
                item.getVisibility(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private List<String> applyUnitSlugs(UUID projectId) {
        List<String> slugs = new ArrayList<>();
        for (ProjectApplyUnitEntity link : applyUnits.findByProjectId(projectId)) {
            units.findById(link.getUnitId()).ifPresent(u -> slugs.add(u.getSlug()));
        }
        return slugs;
    }

    private ProjectEntity bySlug(String slug) {
        return projects.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Project not found"));
    }
}
