package com.aileapro.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumImpactScoreTest {

    @Test
    void emptyFlagsScoreZero() {
        CurriculumImpactScore.Result result = CurriculumImpactScore.score(new CurriculumImpactScore.Flags(
                false, false, false, false, false, false, false, false, false, false, false));
        assertThat(result.score()).isZero();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void hitsAccumulateExplainably() {
        CurriculumImpactScore.Result result = CurriculumImpactScore.score(new CurriculumImpactScore.Flags(
                true, true, false, false, false, false, false, false, false, false, false));
        assertThat(result.reasons()).containsExactly("new_concept", "change_existing");
        assertThat(result.score()).isGreaterThan(0).isLessThanOrEqualTo(1);
    }
}
