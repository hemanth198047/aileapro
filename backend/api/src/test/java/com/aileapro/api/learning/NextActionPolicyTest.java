package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Chip;
import com.aileapro.api.learning.LearningViews.Evidence;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.LearningViews.NextAction;
import com.aileapro.api.learning.NextActionPolicy.Prereq;
import com.aileapro.api.learning.NextActionPolicy.Stop;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ranking rules, tested without a database. What matters here is that the recommendation
 * is always a specific piece of work with a reason attached, and that the reason is true.
 */
class NextActionPolicyTest {

    @Test
    void theCurrentStopIsTheFirstOneReadingHasNotFinished() {
        Stop first = stop(1, "PRACTICED", "vectors", "Vectors", 20, solidRead());
        Stop second = stop(2, "PRACTICED", "matrices", "Matrices", 25, partialRead());
        Stop third = stop(3, "PRACTICED", "matmul", "Matrix multiplication", 30, noEvidence());

        assertThat(NextActionPolicy.currentStop(List.of(first, second, third)))
                .contains(second);
        assertThat(NextActionPolicy.currentStop(List.of(first)))
                .as("a fully read path has no current stop, which is what triggers the honest dead end")
                .isEmpty();
    }

    @Test
    void anUnreadPrerequisiteOutranksTheWaypointItBlocks() {
        Stop blocked = stop(6, "MASTERED", "how-a-neuron-learns", "How a neuron learns", 45, noEvidence());
        // Study order: the deepest ancestor comes first.
        Prereq vectors = prereq("vectors", "Vectors", 20, solidRead());
        Prereq matrices = prereq("matrices", "Matrices", 25, noEvidence());
        Prereq matmul = prereq("matmul", "Matrix multiplication", 30, noEvidence());

        Optional<Prereq> blocking =
                NextActionPolicy.blockingPrerequisite(List.of(vectors, matrices, matmul));
        assertThat(blocking).contains(matrices);

        NextAction action = NextActionPolicy.readPrerequisite(blocking.orElseThrow(), blocked.unit());
        assertThat(action.rule()).isEqualTo(NextActionPolicy.RULE_PREREQUISITE_GAP);
        assertThat(action.verb()).isEqualTo("Read");
        assertThat(action.object()).isEqualTo("Matrices");
        assertThat(action.minutes()).isEqualTo(25);
        assertThat(action.reason())
                .as("the sentence has to name the thing being unblocked")
                .isEqualTo("It is a prerequisite of How a neuron learns, and you have not read it yet.");
        assertThat(action.href()).isEqualTo("/app/learn/matrices");
    }

    @Test
    void aWaypointWithEverythingReadBeforeItSaysSo() {
        Stop stop = stop(2, "PRACTICED", "matrices", "Matrices", 25, noEvidence());

        assertThat(NextActionPolicy.readWaypoint(stop, "AI Engineer", true).reason())
                .isEqualTo("It is the next unread waypoint on AI Engineer, and its prerequisites are read.");
        assertThat(NextActionPolicy.readWaypoint(stop, "AI Engineer", false).reason())
                .as("claiming prerequisites are cleared when there were none would be a small lie")
                .isEqualTo("It is the next unread waypoint on AI Engineer, and nothing has to come before it.");
    }

    @Test
    void aStartedUnitIsResumedAndTheEstimateShrinks() {
        Mastery started = mastery(readChip("partial"), 300, 480, 2);
        Stop stop = stop(1, "PRACTICED", "vectors", "Vectors", 20, started);

        NextAction action = NextActionPolicy.readWaypoint(stop, "AI Engineer", false);

        assertThat(action.verb()).isEqualTo("Finish reading");
        assertThat(action.minutes())
                .as("three minutes of reading are outstanding, not the full twenty")
                .isEqualTo(3);
        assertThat(action.reason()).contains("started it and stopped partway");
    }

    @Test
    void aFreshUnitUsesTheAuthorsEstimate() {
        Stop stop = stop(1, "PRACTICED", "vectors", "Vectors", 20, noEvidence());
        assertThat(NextActionPolicy.readWaypoint(stop, "AI Engineer", false).minutes()).isEqualTo(20);
    }

    @Test
    void whenReadingRunsOutTheActionSaysWhatIsActuallyNeeded() {
        NextAction action = NextActionPolicy.readingExhausted("AI Engineer");

        assertThat(action.kind()).isEqualTo("blocked");
        assertThat(action.rule()).isEqualTo(NextActionPolicy.RULE_READING_EXHAUSTED);
        assertThat(action.minutes()).as("no estimate is offered for work that does not exist yet").isNull();
        assertThat(action.reason())
                .contains("as far as reading counts as evidence")
                .contains("work you produce");
    }

    @Test
    void withoutAGoalTheRecommendationIsToSetOne() {
        NextAction action = NextActionPolicy.onboarding();

        assertThat(action.rule()).isEqualTo(NextActionPolicy.RULE_ONBOARDING);
        assertThat(action.href()).isEqualTo("/app/onboarding");
        assertThat(action.verb()).isEqualTo("Choose");
        assertThat(action.mastery()).as("nothing has been shown, so no evidence is attached").isNull();
    }

    @Test
    void everyActionIsAVerbAnObjectAndAReason() {
        List<NextAction> all = List.of(
                NextActionPolicy.onboarding(),
                NextActionPolicy.emptyPath("AI Engineer"),
                NextActionPolicy.readingExhausted("AI Engineer"),
                NextActionPolicy.readWaypoint(
                        stop(1, "PRACTICED", "vectors", "Vectors", 20, noEvidence()), "AI Engineer", false));

        assertThat(all).allSatisfy(action -> {
            assertThat(action.verb()).isNotBlank();
            assertThat(action.object()).isNotBlank();
            assertThat(action.reason()).isNotBlank();
            assertThat(action.href()).startsWith("/app/");
            assertThat(action.verb()).isNotEqualTo("Continue");
        });
    }

    private static Stop stop(
            int sort, String required, String slug, String title, int minutes, Mastery mastery) {
        return new Stop(sort, required, ref(slug, title, minutes), mastery);
    }

    private static Prereq prereq(String slug, String title, int minutes, Mastery mastery) {
        return new Prereq(ref(slug, title, minutes), mastery);
    }

    private static UnitRef ref(String slug, String title, int minutes) {
        return new UnitRef(
                UUID.nameUUIDFromBytes(slug.getBytes()),
                slug,
                title,
                null,
                "knowledge_unit",
                (short) 2,
                minutes,
                "LOW",
                "FOUNDATIONAL",
                true,
                0);
    }

    private static Evidence readChip(String state) {
        return new Evidence(new Chip(state, Instant.now()), Chip.empty(), Chip.empty(), Chip.empty());
    }

    private static Mastery noEvidence() {
        return mastery(Evidence.none(), 0, 480, 0);
    }

    private static Mastery partialRead() {
        return mastery(readChip("partial"), 120, 480, 1);
    }

    private static Mastery solidRead() {
        return mastery(readChip("solid"), 600, 480, 1);
    }

    private static Mastery mastery(Evidence evidence, int dwell, int floor, int opened) {
        return new Mastery(
                UUID.randomUUID(),
                "slug",
                opened == 0 ? "UNKNOWN" : "INTRODUCED",
                0,
                evidence,
                dwell,
                floor,
                List.of(),
                opened,
                null,
                null);
    }
}
