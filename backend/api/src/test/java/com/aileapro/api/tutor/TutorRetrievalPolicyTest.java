package com.aileapro.api.tutor;

import com.aileapro.ai.TutorPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Phase 8 additions to the teaching rules. */
class TutorRetrievalPolicyTest {

    @Test
    void askingForLatestIsDetected() {
        assertThat(TutorPolicy.asksForLatest("What is the latest state of the art on RAG?")).isTrue();
        assertThat(TutorPolicy.asksForLatest("Why does changing b also change a?")).isFalse();
    }

    @Test
    void integrityWithRetrievalAllowsCitingRetrievedBlockIds() {
        TutorPolicy.Turn turn = TutorPolicy.decide(new TutorPolicy.Ask(
                "Explain aliasing",
                null,
                null,
                TutorPolicy.UnitContext.none(),
                0,
                false,
                true));

        assertThat(turn.slots().get("integrity")).containsIgnoringCase("block id");
        assertThat(turn.slots().get("integrity")).containsIgnoringCase("timestamp");
        assertThat(turn.slots().get("integrity")).doesNotContain("You have no retrieval for this turn");
    }

    @Test
    void integrityWithoutRetrievalForbidsCitationsAndHandlesLatestAsks() {
        TutorPolicy.Turn turn = TutorPolicy.decide(new TutorPolicy.Ask(
                "What is the latest approach?",
                null,
                null,
                TutorPolicy.UnitContext.none(),
                0,
                false,
                false));

        String integrity = turn.slots().get("integrity");
        assertThat(integrity).containsIgnoringCase("no retrieval");
        assertThat(integrity).containsIgnoringCase("foundational");
    }
}
