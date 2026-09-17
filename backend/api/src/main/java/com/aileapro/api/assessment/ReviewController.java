package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.ReviewQueue;
import com.aileapro.api.assessment.AssessmentViews.ReviewResult;
import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.platform.ApiException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The spaced review queue. */
@RestController
@RequestMapping("/v1/reviews")
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    /** @param grade 0–5; 3 and above is a pass, as in SM-2 */
    public record GradeRequest(Integer grade) {
    }

    @GetMapping("/due")
    public ReviewQueue due(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return reviews.due(principal.getId());
    }

    /**
     * Grading returns the back of the card along with the new interval. The back is withheld
     * from the queue and released here, so the learner has to commit to a grade before seeing
     * whether they were right — which is the only version of this that measures recall.
     */
    @PostMapping("/{scheduleId}")
    public ReviewResult grade(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable UUID scheduleId,
            @RequestBody GradeRequest request) {
        if (request == null || request.grade() == null) {
            throw ApiException.validation("grade is required");
        }
        return reviews.answer(principal.getId(), scheduleId, request.grade());
    }
}
