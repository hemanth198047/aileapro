package com.aileapro.api.mastery;

import com.aileapro.domain.AssessmentKind;
import com.aileapro.domain.BloomLevel;
import com.aileapro.domain.Evidence;
import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.GradedEvidencePolicy;
import com.aileapro.domain.GradedEvidencePolicy.Graded;
import com.aileapro.domain.GradedEvidencePolicy.Marked;
import com.aileapro.domain.GradedEvidencePolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules that decide what graded work is worth. The tests that matter most are the ones
 * asserting that something does <em>not</em> happen.
 */
class GradedEvidencePolicyTest {

    /**
     * A well-stocked unit: enough practice items that answering a handful correctly does not
     * complete the set, which is what lets these tests tell one rung from the next.
     */
    private static final UnitFacts FACTS = new UnitFacts(10, 4, true);

    @Test
    void oneCorrectAnswerIsNotRecall() {
        Graded graded = GradedEvidencePolicy.apply(Graded.empty(), practice(true, BloomLevel.REMEMBER), true);

        assertThat(GradedEvidencePolicy.recallDemonstrated(graded))
                .as("a single right answer is as likely to be a lucky guess as knowledge")
                .isFalse();
        assertThat(state(graded)).isEqualTo(MasteryState.INTRODUCED);
    }

    @Test
    void twoCorrectAnswersAtAGoodRateReachFamiliar() {
        Graded graded = Graded.empty();
        graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.REMEMBER), true);
        graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.REMEMBER), false);

        assertThat(GradedEvidencePolicy.recallDemonstrated(graded)).isTrue();
        assertThat(state(graded)).isEqualTo(MasteryState.FAMILIAR);
    }

    @Test
    void recallAtALowAccuracyIsNotRecall() {
        Graded graded = Graded.empty();
        graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.REMEMBER), true);
        graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.REMEMBER), false);
        for (int i = 0; i < 4; i++) {
            graded = GradedEvidencePolicy.apply(graded, practice(false, BloomLevel.REMEMBER), false);
        }

        assertThat(graded.totalCorrect()).isEqualTo(2);
        assertThat(GradedEvidencePolicy.recallDemonstrated(graded))
                .as("two right out of six is not above the threshold")
                .isFalse();
    }

    @Test
    void understandingNeedsItemsAboveRecall() {
        Graded onlyRecall = Graded.empty();
        for (int i = 0; i < 4; i++) {
            onlyRecall = GradedEvidencePolicy.apply(onlyRecall, practice(true, BloomLevel.REMEMBER), i == 0);
        }
        assertThat(state(onlyRecall))
                .as("four definitions recalled is still only recall")
                .isEqualTo(MasteryState.FAMILIAR);

        Graded withApplication = onlyRecall;
        withApplication = GradedEvidencePolicy.apply(withApplication, practice(true, BloomLevel.APPLY), false);
        withApplication = GradedEvidencePolicy.apply(withApplication, practice(true, BloomLevel.APPLY), false);

        assertThat(state(withApplication)).isEqualTo(MasteryState.UNDERSTOOD);
    }

    @Test
    void anOpenMisconceptionHoldsTheStateDown() {
        Graded graded = Graded.empty();
        for (int i = 0; i < 4; i++) {
            graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.APPLY), i == 0);
        }
        assertThat(state(graded)).isEqualTo(MasteryState.UNDERSTOOD);

        Graded flagged = GradedEvidencePolicy.withOpenMisconceptions(graded, 1);

        assertThat(state(flagged))
                .as("reciting the rule while holding a named false belief is not understanding")
                .isEqualTo(MasteryState.FAMILIAR);
    }

    @Test
    void thePracticeChipIsMeasuredAgainstTheAuthoredSetNotTheAttempted() {
        UnitFacts fiveAuthored = new UnitFacts(5, 4, true);
        Graded one = GradedEvidencePolicy.apply(Graded.empty(), practice(true, BloomLevel.APPLY), true);

        assertThat(GradedEvidencePolicy.practiceState(one, fiveAuthored))
                .as("one of five authored items is a start, not a finished set")
                .isEqualTo(EvidenceState.PARTIAL);

        Graded four = one;
        for (int i = 0; i < 3; i++) {
            four = GradedEvidencePolicy.apply(four, practice(true, BloomLevel.APPLY), false);
        }

        assertThat(GradedEvidencePolicy.practiceState(four, fiveAuthored)).isEqualTo(EvidenceState.SOLID);
        assertThat(GradedEvidencePolicy.stateFor(evidence(four, fiveAuthored), four, fiveAuthored))
                .isEqualTo(MasteryState.PRACTICED);
    }

    @Test
    void theLadderCannotBeSkipped() {
        // The whole authored practice set, correct, but every item is a definition.
        UnitFacts recallOnlySet = new UnitFacts(4, 2, false);
        Graded graded = Graded.empty();
        for (int i = 0; i < 4; i++) {
            graded = GradedEvidencePolicy.apply(graded, practice(true, BloomLevel.REMEMBER), i == 0);
        }

        assertThat(GradedEvidencePolicy.practiceState(graded, recallOnlySet)).isEqualTo(EvidenceState.SOLID);
        assertThat(GradedEvidencePolicy.stateFor(evidence(graded, recallOnlySet), graded, recallOnlySet))
                .as("finishing a set of definitions cannot reach PRACTICED, because nothing asked")
                .isEqualTo(MasteryState.FAMILIAR);
        assertThat(GradedEvidencePolicy.reachableCeiling(recallOnlySet))
                .as("the ceiling belongs to the content, and the UI has to be able to say so")
                .isEqualTo(MasteryState.FAMILIAR);
    }

    @Test
    void aUnitWithNoItemsAtAllStopsAtIntroduced() {
        assertThat(GradedEvidencePolicy.reachableCeiling(UnitFacts.none())).isEqualTo(MasteryState.INTRODUCED);
        assertThat(GradedEvidencePolicy.reachableCeiling(new UnitFacts(6, 4, true)))
                .as("a full item bank plus projects can support MASTERED")
                .isEqualTo(MasteryState.MASTERED);
    }

    @Test
    void anAttemptedQuizIsNotAPassedOne() {
        Graded attempted = Graded.empty();
        attempted = GradedEvidencePolicy.apply(attempted, quiz(true), true);
        attempted = GradedEvidencePolicy.apply(attempted, quiz(false), false);
        attempted = GradedEvidencePolicy.applySubmission(attempted, AssessmentKind.QUIZ, 0.5, false);

        assertThat(GradedEvidencePolicy.assessState(attempted)).isEqualTo(EvidenceState.PARTIAL);
        assertThat(attempted.assessBestFraction()).isEqualTo(0.5);
    }

    @Test
    void proficientNeedsAPassOnMoreThanOneDay() {
        // Everything below PROFICIENT is in place: understanding shown, practice set done,
        // and a quiz passed — all on one day.
        Graded sameDay = Graded.empty();
        for (int i = 0; i < 10; i++) {
            sameDay = GradedEvidencePolicy.apply(sameDay, practice(true, BloomLevel.APPLY), i == 0);
        }
        for (int i = 0; i < 4; i++) {
            sameDay = GradedEvidencePolicy.apply(sameDay, quiz(true), false);
        }
        sameDay = GradedEvidencePolicy.applySubmission(sameDay, AssessmentKind.QUIZ, 1.0, true);

        assertThat(GradedEvidencePolicy.assessState(sameDay)).isEqualTo(EvidenceState.SOLID);
        assertThat(sameDay.gradedDays()).isEqualTo(1);
        assertThat(state(sameDay))
                .as("one good afternoon is not consistency across days")
                .isEqualTo(MasteryState.PRACTICED);

        Graded secondDay = GradedEvidencePolicy.apply(sameDay, quiz(true), true);
        assertThat(secondDay.gradedDays()).isEqualTo(2);
        assertThat(state(secondDay)).isEqualTo(MasteryState.PROFICIENT);
    }

    @Test
    void masteredCannotBeReachedWithoutApplication() {
        Graded everything = Graded.empty();
        for (int i = 0; i < 6; i++) {
            everything = GradedEvidencePolicy.apply(everything, quiz(true), i < 3);
        }
        for (int i = 0; i < 10; i++) {
            everything = GradedEvidencePolicy.apply(everything, practice(true, BloomLevel.APPLY), false);
        }
        everything = GradedEvidencePolicy.applySubmission(everything, AssessmentKind.QUIZ, 1.0, true);
        everything = GradedEvidencePolicy.applyReview(everything, true, true);
        everything = GradedEvidencePolicy.applyReview(everything, true, true);

        Evidence evidence = evidence(everything, FACTS);
        assertThat(evidence.practice()).isEqualTo(EvidenceState.SOLID);
        assertThat(evidence.assess()).isEqualTo(EvidenceState.SOLID);
        assertThat(evidence.apply())
                .as("quizzes and reviews never fill Apply — only a finished project does")
                .isEqualTo(EvidenceState.EMPTY);
        assertThat(GradedEvidencePolicy.stateFor(evidence, everything, FACTS))
                .as("no amount of quizzing is application")
                .isEqualTo(MasteryState.PROFICIENT);
    }

    @Test
    void masteredNeedsApplicationAndRetention() {
        Graded everything = Graded.empty();
        for (int i = 0; i < 6; i++) {
            everything = GradedEvidencePolicy.apply(everything, quiz(true), i < 3);
        }
        for (int i = 0; i < 10; i++) {
            everything = GradedEvidencePolicy.apply(everything, practice(true, BloomLevel.APPLY), false);
        }
        everything = GradedEvidencePolicy.applySubmission(everything, AssessmentKind.QUIZ, 1.0, true);
        everything = GradedEvidencePolicy.applyReview(everything, true, true);
        everything = GradedEvidencePolicy.applyReview(everything, true, true);

        Evidence withApply = new Evidence(
                EvidenceState.SOLID, EvidenceState.SOLID, EvidenceState.SOLID, EvidenceState.SOLID);
        assertThat(GradedEvidencePolicy.stateFor(withApply, everything, FACTS))
                .as("Apply solid plus retention is the only path to MASTERED")
                .isEqualTo(MasteryState.MASTERED);
    }

    @Test
    void theDiagnosticGradesNothing() {
        Graded graded = Graded.empty();
        for (int i = 0; i < 6; i++) {
            graded = GradedEvidencePolicy.apply(
                    graded,
                    new Marked(AssessmentKind.DIAGNOSTIC, BloomLevel.APPLY, (short) 3, 1, true, true),
                    i == 0);
        }

        assertThat(graded).isEqualTo(Graded.empty());
        assertThat(state(graded))
                .as("placement is a guess taken before teaching, not evidence of competence")
                .isEqualTo(MasteryState.INTRODUCED);
    }

    @Test
    void anUnmarkedItemChangesNothing() {
        Graded graded = GradedEvidencePolicy.apply(
                Graded.empty(),
                new Marked(AssessmentKind.PRACTICE, BloomLevel.APPLY, (short) 3, 0, false, false),
                true);

        assertThat(graded).isEqualTo(Graded.empty());
    }

    @Test
    void reviewsOnlyCountForRetentionWhenTheyWereDue() {
        Graded early = GradedEvidencePolicy.applyReview(Graded.empty(), false, true);
        assertThat(early.reviewsDuePassed())
                .as("answering a card before it is due is re-reading with extra steps")
                .isZero();

        Graded due = GradedEvidencePolicy.applyReview(Graded.empty(), true, true);
        assertThat(due.reviewsDuePassed()).isEqualTo(1);
    }

    @Test
    void accuracyRewardsHardItemsMoreThanEasyOnes() {
        Graded easy = GradedEvidencePolicy.apply(
                Graded.empty(), new Marked(AssessmentKind.QUIZ, BloomLevel.REMEMBER, (short) 1, 1, true, true), true);
        Graded hard = GradedEvidencePolicy.apply(
                Graded.empty(), new Marked(AssessmentKind.QUIZ, BloomLevel.ANALYZE, (short) 5, 1, true, true), true);

        assertThat(hard.accuracyEma())
                .as("performing as expected on a definition is average; a transfer item is not")
                .isGreaterThan(easy.accuracyEma());
    }

    private static Marked practice(boolean correct, BloomLevel bloom) {
        return new Marked(AssessmentKind.PRACTICE, bloom, (short) 2, correct ? 1 : 0, correct, true);
    }

    private static Marked quiz(boolean correct) {
        return new Marked(AssessmentKind.QUIZ, BloomLevel.UNDERSTAND, (short) 3, correct ? 1 : 0, correct, true);
    }

    /** Read is solid throughout: these tests are about what grading adds on top of reading. */
    private static Evidence evidence(Graded graded, UnitFacts facts) {
        Evidence read = new Evidence(
                EvidenceState.SOLID, EvidenceState.EMPTY, EvidenceState.EMPTY, EvidenceState.EMPTY);
        return GradedEvidencePolicy.evidenceFor(read, graded, facts);
    }

    private static MasteryState state(Graded graded) {
        return GradedEvidencePolicy.stateFor(evidence(graded, FACTS), graded, FACTS);
    }
}
