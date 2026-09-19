package com.aileapro.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Explainable curriculum impact (0–1) for update proposals. Internal — never a learner score.
 *
 * <p>Checklist from continuous-intelligence: new concept, change existing, obsolete technique,
 * tool/API/pattern change, new prereq/specialization, industry expectation, new exercise/project/
 * assessment, career skill change.
 */
public final class CurriculumImpactScore {

    private CurriculumImpactScore() {
    }

    public record Flags(
            boolean newConcept,
            boolean changeExisting,
            boolean obsoleteTechnique,
            boolean toolChange,
            boolean apiChange,
            boolean patternChange,
            boolean newPrereq,
            boolean newSpecialization,
            boolean industryExpectation,
            boolean newExerciseOrAssessment,
            boolean careerSkillChange) {
    }

    public record Result(double score, List<String> reasons) {
    }

    public static Result score(Flags flags) {
        List<String> reasons = new ArrayList<>();
        int hits = 0;
        hits += flag(flags.newConcept(), "new_concept", reasons);
        hits += flag(flags.changeExisting(), "change_existing", reasons);
        hits += flag(flags.obsoleteTechnique(), "obsolete_technique", reasons);
        hits += flag(flags.toolChange(), "tool_change", reasons);
        hits += flag(flags.apiChange(), "api_change", reasons);
        hits += flag(flags.patternChange(), "pattern_change", reasons);
        hits += flag(flags.newPrereq(), "new_prereq", reasons);
        hits += flag(flags.newSpecialization(), "new_specialization", reasons);
        hits += flag(flags.industryExpectation(), "industry_expectation", reasons);
        hits += flag(flags.newExerciseOrAssessment(), "new_exercise_or_assessment", reasons);
        hits += flag(flags.careerSkillChange(), "career_skill_change", reasons);
        double score = Math.min(1.0, hits / 11.0);
        return new Result(score, List.copyOf(reasons));
    }

    private static int flag(boolean on, String code, List<String> reasons) {
        if (!on) {
            return 0;
        }
        reasons.add(code);
        return 1;
    }
}
