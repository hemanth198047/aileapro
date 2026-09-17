package com.aileapro.api.mastery;

import com.aileapro.domain.ReviewScheduler;
import com.aileapro.domain.ReviewScheduler.State;
import com.aileapro.domain.Volatility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** The schedule. The interesting cases are the ones where it refuses to keep drilling. */
class ReviewSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Test
    void aNewCardIsDueImmediately() {
        State fresh = State.newCard(TODAY);

        assertThat(fresh.due()).isEqualTo(TODAY);
        assertThat(fresh.reps()).isZero();
        assertThat(fresh.ease()).isEqualTo(ReviewScheduler.DEFAULT_EASE);
    }

    @Test
    void theFirstTwoSuccessesUseFixedIntervals() {
        State first = ReviewScheduler.apply(State.newCard(TODAY), 4, TODAY, Volatility.LOW);
        assertThat(first.intervalDays()).isEqualTo(1);
        assertThat(first.due()).isEqualTo(TODAY.plusDays(1));

        State second = ReviewScheduler.apply(first, 4, TODAY.plusDays(1), Volatility.LOW);
        assertThat(second.intervalDays()).isEqualTo(6);
        assertThat(second.due()).isEqualTo(TODAY.plusDays(7));
    }

    @Test
    void afterThatTheIntervalGrowsByEase() {
        State state = State.newCard(TODAY);
        state = ReviewScheduler.apply(state, 5, TODAY, Volatility.LOW);
        state = ReviewScheduler.apply(state, 5, TODAY.plusDays(1), Volatility.LOW);
        State third = ReviewScheduler.apply(state, 5, TODAY.plusDays(7), Volatility.LOW);

        assertThat(third.intervalDays()).isGreaterThan(6);
        assertThat(third.reps()).isEqualTo(3);
        assertThat(third.ease()).isGreaterThan(ReviewScheduler.DEFAULT_EASE);
    }

    @Test
    void aFailureSendsTheCardBackToTomorrowButKeepsItsHistory() {
        State state = State.newCard(TODAY);
        state = ReviewScheduler.apply(state, 5, TODAY, Volatility.LOW);
        state = ReviewScheduler.apply(state, 5, TODAY.plusDays(1), Volatility.LOW);
        double easeBefore = state.ease();

        State lapsed = ReviewScheduler.apply(state, 1, TODAY.plusDays(7), Volatility.LOW);

        assertThat(lapsed.reps()).as("the ladder restarts").isZero();
        assertThat(lapsed.intervalDays()).isEqualTo(1);
        assertThat(lapsed.lapses()).isEqualTo(1);
        assertThat(lapsed.ease())
                .as("ease drops but is not reset: one bad day does not erase the card's history")
                .isLessThan(easeBefore)
                .isGreaterThan(ReviewScheduler.MIN_EASE);
        assertThat(lapsed.leech()).isFalse();
    }

    @Test
    void aCardThatKeepsFailingIsFlaggedForReteachingInsteadOfDrilledForever() {
        State state = State.newCard(TODAY);
        for (int i = 0; i < ReviewScheduler.LEECH_LAPSES; i++) {
            state = ReviewScheduler.apply(state, 0, TODAY.plusDays(i), Volatility.LOW);
        }

        assertThat(state.lapses()).isEqualTo(ReviewScheduler.LEECH_LAPSES);
        assertThat(state.leech())
                .as("07 asks for reteach, not infinite cram")
                .isTrue();
        assertThat(state.intervalDays())
                .as("a leech is taken out of the rotation rather than scheduled again")
                .isZero();
    }

    @Test
    void easeIsBoundedSoNoCardEscapesOrCollapses() {
        State easy = State.newCard(TODAY);
        for (int i = 0; i < 40; i++) {
            easy = ReviewScheduler.apply(easy, 5, TODAY.plusDays(i), Volatility.LOW);
        }
        assertThat(easy.ease()).isLessThanOrEqualTo(ReviewScheduler.MAX_EASE);

        State hard = State.newCard(TODAY);
        for (int i = 0; i < 40; i++) {
            hard = ReviewScheduler.apply(hard, 3, TODAY.plusDays(i), Volatility.LOW);
        }
        assertThat(hard.ease()).isGreaterThanOrEqualTo(ReviewScheduler.MIN_EASE);
    }

    @Test
    void intervalsAreCappedAndTheCapFollowsVolatility() {
        State foundation = drillTenTimes(Volatility.LOW);
        State framework = drillTenTimes(Volatility.HIGH);

        assertThat(foundation.intervalDays()).isEqualTo(ReviewScheduler.MAX_INTERVAL_DAYS);
        assertThat(framework.intervalDays())
                .as("there is no point scheduling a recall for after the API has changed")
                .isEqualTo(ReviewScheduler.cap(Volatility.HIGH));
        assertThat(framework.intervalDays()).isLessThan(foundation.intervalDays());
    }

    @Test
    void schedulingCountsFromTheDayAnsweredNotTheDayDue() {
        State state = ReviewScheduler.apply(State.newCard(TODAY), 4, TODAY, Volatility.LOW);
        LocalDate late = TODAY.plusDays(30);

        State answeredLate = ReviewScheduler.apply(state, 4, late, Volatility.LOW);

        assertThat(answeredLate.due())
                .as("answering late must not stack up a backlog of overdue dates")
                .isEqualTo(late.plusDays(answeredLate.intervalDays()));
    }

    @Test
    void gradeThreeIsAPassAndTwoIsNot() {
        assertThat(ReviewScheduler.isPass(3)).isTrue();
        assertThat(ReviewScheduler.isPass(2)).isFalse();

        State hardPass = ReviewScheduler.apply(State.newCard(TODAY), 3, TODAY, Volatility.LOW);
        assertThat(hardPass.lapses()).isZero();
        assertThat(hardPass.reps()).isEqualTo(1);
    }

    @Test
    void gradesOutsideTheScaleAreClampedRatherThanTrusted() {
        State tooHigh = ReviewScheduler.apply(State.newCard(TODAY), 99, TODAY, Volatility.LOW);
        State tooLow = ReviewScheduler.apply(State.newCard(TODAY), -5, TODAY, Volatility.LOW);

        assertThat(tooHigh.reps()).isEqualTo(1);
        assertThat(tooHigh.ease()).isLessThanOrEqualTo(ReviewScheduler.MAX_EASE);
        assertThat(tooLow.lapses()).isEqualTo(1);
    }

    private static State drillTenTimes(Volatility volatility) {
        State state = State.newCard(TODAY);
        LocalDate day = TODAY;
        for (int i = 0; i < 12; i++) {
            state = ReviewScheduler.apply(state, 5, day, volatility);
            day = state.due();
        }
        return state;
    }
}
