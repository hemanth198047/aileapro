package com.aileapro.api.mastery;

import com.aileapro.domain.ItemScoring;
import com.aileapro.domain.ItemScoring.Key;
import com.aileapro.domain.ItemScoring.Outcome;
import com.aileapro.domain.ItemScoring.Response;
import com.aileapro.domain.ItemScoring.Result;
import com.aileapro.domain.ItemType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Marking rules. Every case here is one a learner could reasonably argue about. */
class ItemScoringTest {

    @Test
    void singleChoiceIsRightOrWrong() {
        Key key = Key.choice(ItemType.MCQ, "b");

        assertThat(ItemScoring.mark(key, Response.of("b")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.of("a")).outcome()).isEqualTo(Outcome.WRONG);
        assertThat(ItemScoring.mark(key, Response.of("a")).wrongIds())
                .as("the chosen distractor is what a misconception flag is raised from")
                .containsExactly("a");
    }

    @Test
    void tickingEveryBoxOnAMultiSelectEarnsNothing() {
        Key key = Key.choice(ItemType.MULTI_SELECT, "a", "c");

        Result everything = ItemScoring.mark(key, Response.of("a", "b", "c", "d"));

        assertThat(everything.outcome()).isEqualTo(Outcome.WRONG);
        assertThat(everything.fraction())
                .as("two right and two wrong cancel out, otherwise the item is free marks")
                .isZero();
    }

    @Test
    void halfOfAMultiSelectEarnsHalfTheCredit() {
        Key key = Key.choice(ItemType.MULTI_SELECT, "a", "c");

        Result half = ItemScoring.mark(key, Response.of("a"));

        assertThat(half.outcome()).isEqualTo(Outcome.PARTIAL);
        assertThat(half.fraction()).isEqualTo(0.5);
        assertThat(half.missedIds()).containsExactly("c");
        assertThat(half.wrongIds()).isEmpty();
    }

    @Test
    void oneWrongTickCostsOneRightTick() {
        Key key = Key.choice(ItemType.MULTI_SELECT, "a", "b", "c");

        Result mixed = ItemScoring.mark(key, Response.of("a", "b", "d"));

        assertThat(mixed.outcome()).isEqualTo(Outcome.PARTIAL);
        assertThat(mixed.fraction()).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(mixed.wrongIds()).containsExactly("d");
    }

    @Test
    void anExactMultiSelectIsFullMarks() {
        Key key = Key.choice(ItemType.MULTI_SELECT, "a", "c");
        Result exact = ItemScoring.mark(key, Response.of("c", "a"));

        assertThat(exact.outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(exact.fraction()).isEqualTo(1);
    }

    @Test
    void shortTextIgnoresCaseSpacingAndTrailingPunctuation() {
        Key key = Key.text("perpendicular", "orthogonal");

        assertThat(ItemScoring.mark(key, Response.text("Perpendicular")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.text("  orthogonal. ")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.text("parallel")).outcome()).isEqualTo(Outcome.WRONG);
    }

    @Test
    void aNumericAnswerIsComparedAsANumber() {
        Key key = Key.number(0.96, 0.01);

        assertThat(ItemScoring.mark(key, Response.text(".96")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.text("0.960")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.text("0.96 ")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.text("0.9")).outcome()).isEqualTo(Outcome.WRONG);
        assertThat(ItemScoring.mark(key, Response.text("nearly one")).outcome())
                .as("a number was asked for, and prose is not one")
                .isEqualTo(Outcome.WRONG);
    }

    @Test
    void codeThatDoesNotMatchIsUnmarkedRatherThanWrong() {
        Key key = Key.code("np.dot(a, b)");

        Result matched = ItemScoring.mark(key, Response.text("np.dot(a,  b)"));
        assertThat(matched.outcome())
                .as("whitespace is formatting, not an answer")
                .isEqualTo(Outcome.CORRECT);

        Result different = ItemScoring.mark(key, Response.text("sum(x * y for x, y in zip(a, b))"));
        assertThat(different.outcome())
                .as("text comparison cannot tell a different correct answer from a wrong one")
                .isEqualTo(Outcome.UNMARKED);
        assertThat(different.fraction()).isZero();
        assertThat(different.outcome().correctFlag())
                .as("unmarked has to be null on the wire, not false")
                .isNull();
    }

    @Test
    void nothingSubmittedIsSkippedNotWrong() {
        Key key = Key.choice(ItemType.MCQ, "b");

        Result skipped = ItemScoring.mark(key, Response.empty());

        assertThat(skipped.outcome()).isEqualTo(Outcome.SKIPPED);
        assertThat(skipped.fraction()).isZero();
        assertThat(skipped.outcome().correctFlag()).isFalse();
        assertThat(ItemScoring.mark(key, null).outcome())
                .as("a missing response object is the same as an empty one")
                .isEqualTo(Outcome.SKIPPED);
    }

    @Test
    void severalSelectionsOnASingleChoiceItemEarnNothing() {
        Key key = Key.choice(ItemType.MCQ, "b");

        assertThat(ItemScoring.mark(key, Response.of("a", "b")).outcome()).isEqualTo(Outcome.WRONG);
    }

    @Test
    void trueFalseIsMarkedLikeAnyOtherSingleChoice() {
        Key key = Key.choice(ItemType.TRUE_FALSE, "false");

        assertThat(ItemScoring.mark(key, Response.of("false")).outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(ItemScoring.mark(key, Response.of("true")).outcome()).isEqualTo(Outcome.WRONG);
    }

    @Test
    void anItemAuthoredWithNoCorrectAnswerFailsLoudlyRatherThanGenerously() {
        Key broken = new Key(ItemType.MULTI_SELECT, java.util.Set.of(), java.util.List.of(), null, null);

        assertThat(ItemScoring.mark(broken, Response.of("a")).outcome()).isEqualTo(Outcome.WRONG);
    }
}
