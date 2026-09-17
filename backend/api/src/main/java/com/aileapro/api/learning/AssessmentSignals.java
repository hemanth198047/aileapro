package com.aileapro.api.learning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What the recommendation rules need to know about graded work, declared here and supplied by
 * the assessment code.
 *
 * <p>The dependency points one way on purpose. Recommendations are a learner concern and the
 * assessment engine is a mechanism; if the learner package imported the assessment package,
 * every future mechanism — a tutor, a lab, a project — would have to be imported here too, and
 * this file would become the list of everything in the product.
 *
 * <p>It is also what makes {@link NextActionPolicy} testable: the rules take these records as
 * arguments, so "an overdue review outranks a new unit" can be asserted without a database.
 */
public interface AssessmentSignals {

    /**
     * The most overdue card waiting, if any.
     *
     * @param dueCount     how many are due in total
     * @param daysOverdue  days past the due date for the worst one; zero when due today
     */
    record DueReviews(int dueCount, int overdueCount, int daysOverdue, String unitTitle, String unitSlug) {
    }

    /** A named false belief currently held open, with the page that teaches against it. */
    record OpenMisconception(String code, String claim, String unitTitle, String unitSlug, String href) {
    }

    /**
     * The graded work authored for one unit.
     *
     * @param ceiling       the highest mastery state this unit's content can support
     * @param ceilingReason why, in one sentence, when the ceiling is below PROFICIENT
     */
    record GradedOffer(
            String practiceSlug,
            int practiceItemCount,
            String quizSlug,
            int quizItemCount,
            String ceiling,
            String ceilingReason) {
    }

    DueReviews dueReviews(UUID userId);

    List<OpenMisconception> openMisconceptions(UUID userId);

    Optional<GradedOffer> offerFor(UUID userId, UUID unitId);
}
