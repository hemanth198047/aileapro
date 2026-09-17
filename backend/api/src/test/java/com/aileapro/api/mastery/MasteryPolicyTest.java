package com.aileapro.api.mastery;

import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.LearningEventKind;
import com.aileapro.domain.MasteryPolicy;
import com.aileapro.domain.MasteryPolicy.Event;
import com.aileapro.domain.MasteryPolicy.Snapshot;
import com.aileapro.domain.MasteryPolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule this product is built on: reading is not mastery. These tests exist so that the
 * rule cannot be softened by accident later.
 */
class MasteryPolicyTest {

    private static final UnitFacts UNIT = new UnitFacts(30, 3);
    private static final UnitFacts CONTAINER = new UnitFacts(null, 0);

    @Test
    void openingAUnitDoesNotCompleteIt() {
        Snapshot after = MasteryPolicy.apply(
                Snapshot.empty(), Event.of(LearningEventKind.UNIT_OPENED), UNIT);

        assertThat(after.state()).isEqualTo(MasteryState.UNKNOWN);
        assertThat(after.evidence().read()).isEqualTo(EvidenceState.PARTIAL);
        assertThat(after.evidence().practice()).isEqualTo(EvidenceState.EMPTY);
        assertThat(after.evidence().assess()).isEqualTo(EvidenceState.EMPTY);
        assertThat(after.evidence().apply()).isEqualTo(EvidenceState.EMPTY);
    }

    @Test
    void readingEverythingStillOnlyReachesIntroduced() {
        Snapshot state = Snapshot.empty();
        state = MasteryPolicy.apply(state, Event.of(LearningEventKind.UNIT_OPENED), UNIT);
        state = MasteryPolicy.apply(state, Event.level("beginner"), UNIT);
        state = MasteryPolicy.apply(state, Event.level("intermediate"), UNIT);
        state = MasteryPolicy.apply(state, Event.level("advanced"), UNIT);
        for (int beat = 0; beat < 40; beat++) {
            state = MasteryPolicy.apply(state, Event.dwell(120), UNIT);
        }

        assertThat(state.evidence().read()).isEqualTo(EvidenceState.SOLID);
        assertThat(state.state())
                .as("no amount of reading gets past the reading ceiling")
                .isEqualTo(MasteryState.INTRODUCED);
        assertThat(state.state().atLeast(MasteryState.FAMILIAR)).isFalse();
        assertThat(state.score()).isLessThanOrEqualTo(MasteryPolicy.READ_ONLY_SCORE_CEILING);
    }

    @Test
    void readGoesSolidOnlyOnceTheFloorIsMet() {
        Snapshot state = MasteryPolicy.apply(
                Snapshot.empty(), Event.of(LearningEventKind.UNIT_OPENED), UNIT);
        state = MasteryPolicy.apply(state, Event.level("beginner"), UNIT);

        int floor = MasteryPolicy.readFloorSeconds(UNIT);
        assertThat(floor).isEqualTo(720);

        // One second short of the floor is still partial.
        for (int seconds = 0; seconds + 120 <= floor - 120; seconds += 120) {
            state = MasteryPolicy.apply(state, Event.dwell(120), UNIT);
        }
        state = MasteryPolicy.apply(state, Event.dwell(119), UNIT);
        assertThat(state.dwellSeconds()).isEqualTo(floor - 1);
        assertThat(state.evidence().read()).isEqualTo(EvidenceState.PARTIAL);

        state = MasteryPolicy.apply(state, Event.dwell(1), UNIT);
        assertThat(state.evidence().read()).isEqualTo(EvidenceState.SOLID);
    }

    @Test
    void dwellFloorScalesWithTheAuthorsEstimate() {
        assertThat(MasteryPolicy.readFloorSeconds(new UnitFacts(5, 2)))
                .as("a five minute unit cannot demand ten minutes of reading")
                .isEqualTo(120);
        assertThat(MasteryPolicy.readFloorSeconds(new UnitFacts(1, 1)))
                .as("floor has a minimum so a one minute estimate is not trivially solid")
                .isEqualTo(90);
        assertThat(MasteryPolicy.readFloorSeconds(new UnitFacts(600, 1)))
                .as("and a cap, so a long unit is not unreadable")
                .isEqualTo(720);
        assertThat(MasteryPolicy.readFloorSeconds(new UnitFacts(null, 1))).isEqualTo(180);
    }

    @Test
    void aSingleHeartbeatCannotClaimHoursOfReading() {
        Snapshot state = MasteryPolicy.apply(
                Snapshot.empty(), Event.of(LearningEventKind.UNIT_OPENED), UNIT);
        state = MasteryPolicy.apply(state, Event.level("beginner"), UNIT);

        state = MasteryPolicy.apply(state, Event.dwell(86_400), UNIT);

        assertThat(state.dwellSeconds()).isEqualTo(MasteryPolicy.MAX_DWELL_SECONDS_PER_EVENT);
        assertThat(state.evidence().read()).isEqualTo(EvidenceState.PARTIAL);
    }

    @Test
    void negativeDwellIsIgnored() {
        Snapshot state = MasteryPolicy.apply(
                Snapshot.empty(), Event.dwell(-500), UNIT);

        assertThat(state.dwellSeconds()).isZero();
        assertThat(state.evidence().read())
                .as("a unit that was never opened has no read evidence")
                .isEqualTo(EvidenceState.EMPTY);
    }

    @Test
    void revealingSelfCheckAnswersEarnsNothing() {
        Snapshot state = MasteryPolicy.apply(
                Snapshot.empty(), Event.of(LearningEventKind.UNIT_OPENED), UNIT);
        Snapshot before = state;

        for (int i = 0; i < 10; i++) {
            state = MasteryPolicy.apply(state, Event.of(LearningEventKind.SELF_CHECK_REVEALED), UNIT);
            state = MasteryPolicy.apply(state, Event.of(LearningEventKind.EXERCISE_VIEWED), UNIT);
        }

        assertThat(state.evidence()).isEqualTo(before.evidence());
        assertThat(state.state()).isEqualTo(before.state());
        assertThat(state.evidence().assess()).isEqualTo(EvidenceState.EMPTY);
        assertThat(state.evidence().practice()).isEqualTo(EvidenceState.EMPTY);
    }

    @Test
    void aContainerUnitHasNoLevelsToRead() {
        Snapshot state = MasteryPolicy.apply(
                Snapshot.empty(), Event.of(LearningEventKind.UNIT_OPENED), CONTAINER);
        assertThat(state.evidence().read()).isEqualTo(EvidenceState.PARTIAL);

        state = MasteryPolicy.apply(state, Event.dwell(120), CONTAINER);
        state = MasteryPolicy.apply(state, Event.dwell(60), CONTAINER);

        assertThat(state.evidence().read())
                .as("with no explanation levels authored, dwell alone can finish the Read chip")
                .isEqualTo(EvidenceState.SOLID);
    }

    @Test
    void readingNeverPullsDownAStateEarnedElsewhere() {
        // A record that reached PRACTICED through graded evidence.
        Snapshot earned = new Snapshot(
                MasteryState.PRACTICED,
                new com.aileapro.domain.Evidence(
                        EvidenceState.SOLID, EvidenceState.SOLID, EvidenceState.PARTIAL, EvidenceState.EMPTY),
                5_000,
                java.util.Set.of("beginner"),
                3,
                0.62);

        Snapshot after = MasteryPolicy.apply(earned, Event.of(LearningEventKind.UNIT_OPENED), UNIT);

        assertThat(after.state()).isEqualTo(MasteryState.PRACTICED);
        assertThat(after.evidence().practice()).isEqualTo(EvidenceState.SOLID);
        assertThat(after.evidence().assess()).isEqualTo(EvidenceState.PARTIAL);
        assertThat(after.score()).isEqualTo(0.62);
    }
}
