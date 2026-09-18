package com.aileapro.api.project.seed;

import com.aileapro.api.curriculum.LearningUnitRepository;
import com.aileapro.api.project.ProjectApplyUnitRepository;
import com.aileapro.api.project.ProjectMilestoneRepository;
import com.aileapro.api.project.ProjectRepository;
import com.aileapro.persistence.LearningUnitEntity;
import com.aileapro.persistence.ProjectApplyUnitEntity;
import com.aileapro.persistence.ProjectEntity;
import com.aileapro.persistence.ProjectMilestoneEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Loads authored project briefs. Idempotent by slug. Outline projects keep an empty milestone
 * list on purpose — the Studio must show the brief without inventing a finishable path.
 */
@Component
public class ProjectSeeder {

    private static final Logger log = LoggerFactory.getLogger(ProjectSeeder.class);
    private static final String LOCATION = "classpath*:seed/projects/*.yml";
    private static final String LOCALE = "en";

    private final ProjectRepository projects;
    private final ProjectMilestoneRepository milestones;
    private final ProjectApplyUnitRepository applyUnits;
    private final LearningUnitRepository units;
    private final ObjectMapper yaml;

    public ProjectSeeder(
            ProjectRepository projects,
            ProjectMilestoneRepository milestones,
            ProjectApplyUnitRepository applyUnits,
            LearningUnitRepository units) {
        this.projects = projects;
        this.milestones = milestones;
        this.applyUnits = applyUnits;
        this.units = units;
        this.yaml = JsonMapperForYaml.create();
    }

    @Transactional
    public String seed() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
            int projectCount = 0;
            int milestoneCount = 0;
            for (Resource resource : resources) {
                if (resource.getFilename() == null || !resource.getFilename().endsWith(".yml")) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    ProjectSeedModel file = yaml.readValue(in, ProjectSeedModel.class);
                    if (file.projects() == null) {
                        continue;
                    }
                    for (ProjectSeedModel.SeedProject seed : file.projects()) {
                        milestoneCount += upsert(seed);
                        projectCount++;
                    }
                }
            }
            return projectCount + " projects, " + milestoneCount + " milestones";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int upsert(ProjectSeedModel.SeedProject seed) {
        ProjectEntity project = projects.findBySlug(seed.slug()).orElseGet(() -> {
            ProjectEntity fresh = new ProjectEntity();
            fresh.setId(UUID.randomUUID());
            fresh.setSlug(seed.slug());
            fresh.setCreatedAt(Instant.now());
            return fresh;
        });
        project.setTitle(seed.title());
        project.setSummary(nullToEmpty(seed.summary()));
        project.setObjective(nullToEmpty(seed.objective()));
        project.setStatus(seed.status() == null ? "outline" : seed.status());
        project.setSortOrder(seed.sortOrder());
        project.setStarterSource(seed.starterSource());
        project.setUpdatedAt(Instant.now());

        if (seed.unit() != null && !seed.unit().isBlank()) {
            LearningUnitEntity unit = units.findBySlugAndLocale(seed.unit(), LOCALE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Project " + seed.slug() + " links unknown unit " + seed.unit()));
            project.setUnitId(unit.getId());
        } else {
            project.setUnitId(null);
        }
        projects.save(project);

        // Replace apply links and milestones wholesale — the file is the source of truth.
        applyUnits.findByProjectId(project.getId()).forEach(applyUnits::delete);
        applyUnits.flush();
        List<String> applySlugs = seed.applyUnits() == null ? List.of() : seed.applyUnits();
        for (String slug : applySlugs) {
            LearningUnitEntity unit = units.findBySlugAndLocale(slug, LOCALE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Project " + seed.slug() + " apply unit unknown: " + slug));
            ProjectApplyUnitEntity link = new ProjectApplyUnitEntity();
            link.setProjectId(project.getId());
            link.setUnitId(unit.getId());
            applyUnits.save(link);
        }

        milestones.findByProjectIdOrderBySortOrderAsc(project.getId()).forEach(milestones::delete);
        milestones.flush();
        int count = 0;
        if (seed.milestones() != null) {
            for (ProjectSeedModel.SeedMilestone ms : seed.milestones()) {
                ProjectMilestoneEntity row = new ProjectMilestoneEntity();
                row.setId(UUID.randomUUID());
                row.setProjectId(project.getId());
                row.setSlug(ms.slug());
                row.setTitle(ms.title());
                row.setDescription(nullToEmpty(ms.description()));
                row.setCriteria(nullToEmpty(ms.criteria()));
                row.setHint(nullToEmpty(ms.hint()));
                row.setSortOrder(ms.sortOrder());
                row.setCheckKey(ms.checkKey());
                row.setStarterSource(ms.starterSource());
                milestones.save(row);
                count++;
            }
        }
        log.info("Seeded project {} ({})", seed.slug(), project.getStatus());
        return count;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class JsonMapperForYaml {
        private JsonMapperForYaml() {
        }

        static ObjectMapper create() {
            return new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
        }
    }
}
