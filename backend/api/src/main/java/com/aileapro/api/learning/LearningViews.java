package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read models for learner state: evidence, recommendations, notes. */
public final class LearningViews {

    private LearningViews() {
    }

    /**
     * One evidence chip. The date is what makes the chip honest — a claim with no date behind
     * it is just a colour.
     *
     * @param state empty, partial or solid
     * @param at    when this chip last moved, null while empty
     */
    public record Chip(String state, Instant at) {

        public static Chip empty() {
            return new Chip("empty", null);
        }
    }

    public record Evidence(Chip read, Chip practice, Chip assess, Chip apply) {

        public static Evidence none() {
            return new Evidence(Chip.empty(), Chip.empty(), Chip.empty(), Chip.empty());
        }
    }

    /**
     * A learner's standing on one unit.
     *
     * @param state          the mastery state machine's current position
     * @param score          internal ranking score, exposed for the dashboard's diagnostics
     * @param dwellSeconds   visible reading time counted so far
     * @param readFloorSeconds what dwell has to reach for the Read chip to go solid
     * @param levelsSeen     explanation levels the learner has actually opened
     */
    public record Mastery(
            UUID unitId,
            String unitSlug,
            String state,
            double score,
            Evidence evidence,
            int dwellSeconds,
            int readFloorSeconds,
            List<String> levelsSeen,
            int openedCount,
            Instant firstOpenedAt,
            Instant lastEventAt) {
    }

    /**
     * The recommendation, in the form the UI is required to render: a verb, an object, and a
     * number of minutes, plus the sentence explaining why this and not something else.
     *
     * @param kind   what sort of work this is: read, practice, assess, apply
     * @param reason one sentence the learner can argue with
     * @param rule   which policy rule fired, for debugging and for the dashboard
     */
    public record NextAction(
            String kind,
            String verb,
            String object,
            Integer minutes,
            String reason,
            String rule,
            String href,
            UnitRef unit,
            Mastery mastery) {
    }

    /**
     * Today's shape. Reviews are a list so the UI does not have to special-case their
     * arrival in Phase 6; until then it is empty and {@code reviewsNote} says why.
     */
    public record Today(
            NextAction next,
            String pathSlug,
            String pathTitle,
            int waypointCount,
            int waypointsStarted,
            int waypointsIntroduced,
            List<NextAction> reviews,
            String reviewsNote,
            boolean onboarded) {
    }

    public record Profile(
            UUID targetPathId,
            String targetPathSlug,
            String targetPathTitle,
            Integer hoursPerWeek,
            List<String> modalities,
            String experience,
            String goals,
            Instant onboardedAt) {
    }

    public record Note(UUID id, UUID unitId, String unitSlug, String unitTitle, String body, Instant createdAt) {
    }

    /** A path waypoint with the learner's evidence laid over it, for the roadmap. */
    public record RoadmapStop(
            int sort,
            String requiredMasteryState,
            boolean requirementMet,
            UnitRef unit,
            Mastery mastery) {
    }

    public record Roadmap(
            String pathSlug,
            String pathTitle,
            String pathVersion,
            int estimateMinutes,
            List<RoadmapStop> stops) {
    }

    /**
     * Learner analytics from mastery_records / reviews — never random, never vanity XP.
     *
     * @param velocityNote explains the window in one sentence so the UI does not invent meaning
     */
    public record Dashboard(
            NextAction next,
            int reviewsDue,
            String reviewsNote,
            int timeOnTaskSeconds,
            int velocityWindowDays,
            int practicedOrHigherInWindow,
            String velocityNote,
            List<WeakUnit> weak,
            List<DomainRollup> domains,
            String pathSlug,
            String pathTitle,
            int waypointCount,
            int waypointsStarted,
            int waypointsIntroduced,
            boolean onboarded,
            String note) {
    }

    public record WeakUnit(
            String unitSlug,
            String unitTitle,
            String state,
            int openMisconceptions,
            String reason,
            String href) {
    }

    public record DomainRollup(
            String domainSlug,
            String domainTitle,
            int trackedUnits,
            int introducedOrHigher,
            int practicedOrHigher) {
    }
}
