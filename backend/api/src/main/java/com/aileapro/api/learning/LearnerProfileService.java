package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PathSummary;
import com.aileapro.api.learning.LearningViews.Profile;
import com.aileapro.api.platform.ApiException;
import com.aileapro.persistence.LearnerProfileEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The learner's stated goal. Everything here is self-reported and is used only to aim
 * recommendations — it is kept well away from mastery, because a claim about what you know
 * is not evidence that you know it.
 */
@Service
@Transactional
public class LearnerProfileService {

    /** Lenses the UI offers. Preferences, not capabilities. */
    private static final Set<String> MODALITIES = Set.of("visual", "code", "math", "analogy", "text");

    private static final Set<String> EXPERIENCE = Set.of(
            "none", "some_programming", "professional_developer", "some_ml", "working_in_ml");

    private static final int MAX_GOALS_LENGTH = 2_000;

    private final LearnerProfileRepository profiles;
    private final CurriculumQueryService curriculum;
    private final ObjectMapper json;

    public LearnerProfileService(
            LearnerProfileRepository profiles, CurriculumQueryService curriculum, ObjectMapper json) {
        this.profiles = profiles;
        this.curriculum = curriculum;
        this.json = json;
    }

    /** The request body for onboarding. Every field is optional so a step can be skipped. */
    public record ProfileUpdate(
            String targetPath, Integer hoursPerWeek, List<String> modalities, String experience, String goals) {
    }

    @Transactional(readOnly = true)
    public Optional<Profile> profile(UUID userId) {
        return profiles.findById(userId).map(this::view);
    }

    public Profile save(UUID userId, ProfileUpdate update) {
        LearnerProfileEntity profile = profiles.findById(userId).orElseGet(() -> {
            LearnerProfileEntity fresh = new LearnerProfileEntity();
            fresh.setUserId(userId);
            return fresh;
        });

        if (update.targetPath() != null && !update.targetPath().isBlank()) {
            // Resolved through the catalog so an unpublished or invented path cannot be stored.
            profile.setTargetPathId(curriculum.path(update.targetPath()).id());
        }
        if (update.hoursPerWeek() != null) {
            if (update.hoursPerWeek() < 1 || update.hoursPerWeek() > 80) {
                throw ApiException.validation("hoursPerWeek must be between 1 and 80");
            }
            profile.setHoursPerWeek(update.hoursPerWeek().shortValue());
        }
        if (update.modalities() != null) {
            List<String> chosen = update.modalities().stream().map(String::trim).distinct().toList();
            for (String modality : chosen) {
                if (!MODALITIES.contains(modality)) {
                    throw ApiException.validation("Unknown modality '" + modality + "'");
                }
            }
            profile.setModalities(write(chosen));
        }
        if (update.experience() != null && !update.experience().isBlank()) {
            if (!EXPERIENCE.contains(update.experience())) {
                throw ApiException.validation("Unknown experience '" + update.experience() + "'");
            }
            profile.setExperience(update.experience());
        }
        if (update.goals() != null) {
            String goals = update.goals().trim();
            if (goals.length() > MAX_GOALS_LENGTH) {
                throw ApiException.validation("goals is longer than " + MAX_GOALS_LENGTH + " characters");
            }
            profile.setGoals(goals.isEmpty() ? null : goals);
        }

        // Onboarding counts as done once there is a path and an amount of time to fit it into;
        // the rest is colour.
        if (profile.getOnboardedAt() == null
                && profile.getTargetPathId() != null
                && profile.getHoursPerWeek() != null) {
            profile.setOnboardedAt(Instant.now());
        }
        profile.setUpdatedAt(Instant.now());
        return view(profiles.save(profile));
    }

    /**
     * The path recommendations should aim at: the learner's choice, or the only sensible
     * default when they have not chosen yet.
     */
    @Transactional(readOnly = true)
    public Optional<PathSummary> targetPath(UUID userId) {
        UUID chosen = profiles.findById(userId).map(LearnerProfileEntity::getTargetPathId).orElse(null);
        List<PathSummary> published = curriculum.listPaths();
        if (chosen != null) {
            Optional<PathSummary> match =
                    published.stream().filter(path -> path.id().equals(chosen)).findFirst();
            if (match.isPresent()) {
                return match;
            }
            // The chosen path was unpublished since. Fall through rather than 500.
        }
        return published.stream().findFirst();
    }

    private Profile view(LearnerProfileEntity profile) {
        PathSummary path = profile.getTargetPathId() == null
                ? null
                : curriculum.listPaths().stream()
                        .filter(candidate -> candidate.id().equals(profile.getTargetPathId()))
                        .findFirst()
                        .orElse(null);
        return new Profile(
                profile.getTargetPathId(),
                path == null ? null : path.slug(),
                path == null ? null : path.title(),
                profile.getHoursPerWeek() == null ? null : profile.getHoursPerWeek().intValue(),
                read(profile.getModalities()),
                profile.getExperience(),
                profile.getGoals(),
                profile.getOnboardedAt());
    }

    private String write(List<String> modalities) {
        try {
            return json.writeValueAsString(modalities);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise modalities", impossible);
        }
    }

    private List<String> read(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(stored, json.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException corrupt) {
            throw new IllegalStateException("Stored modalities are unreadable", corrupt);
        }
    }
}
