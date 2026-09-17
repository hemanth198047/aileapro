package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.AttemptState;
import com.aileapro.api.assessment.AssessmentViews.Marked;
import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.assessment.AssessmentViews.Report;
import com.aileapro.api.assessment.AssessmentViews.StudyPlan;
import com.aileapro.api.assessment.AssessmentViews.UnitAssessments;
import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.platform.ApiException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Attempts, reports, and the diagnostic.
 *
 * <p>Answering returns the mark and the explanation in the same response. There is no route
 * that reveals an answer key without an answer having been recorded first, which is the
 * property that makes server-side marking worth the round trip.
 */
@RestController
public class AssessmentController {

    private static final String ENTRY_DIAGNOSTIC = "entry-diagnostic";

    private final AttemptService attempts;
    private final ReportService reports;
    private final DiagnosticService diagnostics;
    private final MisconceptionService misconceptions;

    public AssessmentController(
            AttemptService attempts,
            ReportService reports,
            DiagnosticService diagnostics,
            MisconceptionService misconceptions) {
        this.attempts = attempts;
        this.reports = reports;
        this.diagnostics = diagnostics;
        this.misconceptions = misconceptions;
    }

    /** What is authored for one unit, plus the learner's standing and the unit's honest ceiling. */
    @GetMapping("/v1/units/{idOrSlug}/assessments")
    public UnitAssessments forUnit(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String idOrSlug) {
        return attempts.forUnit(principal.getId(), idOrSlug);
    }

    @PostMapping("/v1/assessments/{slug}/attempts")
    public AttemptState start(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String slug) {
        return attempts.start(principal.getId(), slug);
    }

    @GetMapping("/v1/attempts/{attemptId}")
    public AttemptState state(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID attemptId) {
        return attempts.state(principal.getId(), attemptId);
    }

    @PostMapping("/v1/attempts/{attemptId}/items/{itemId}")
    public Marked answer(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable UUID attemptId,
            @PathVariable UUID itemId,
            @RequestBody AttemptService.AnswerRequest request) {
        if (request == null) {
            throw ApiException.validation("An answer is required");
        }
        return attempts.answer(principal.getId(), attemptId, itemId, request);
    }

    /**
     * Submits and returns the report in one call. Splitting them would let a client submit and
     * never read the report, which is exactly the score-only experience ASM-06 forbids.
     */
    @PostMapping("/v1/attempts/{attemptId}/submit")
    public Report submit(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID attemptId) {
        attempts.submit(principal.getId(), attemptId);
        return reports.reportFor(principal.getId(), attemptId);
    }

    @GetMapping("/v1/attempts/{attemptId}/report")
    public Report report(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID attemptId) {
        return reports.reportFor(principal.getId(), attemptId);
    }

    /** Turns a submitted diagnostic into priors and a versioned plan. */
    @PostMapping("/v1/attempts/{attemptId}/plan")
    public StudyPlan plan(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID attemptId) {
        return diagnostics.planFrom(principal.getId(), attemptId);
    }

    @GetMapping("/v1/plan")
    public StudyPlan currentPlan(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return diagnostics.latestPlan(principal.getId())
                .orElseThrow(() -> ApiException.notFound("No plan yet. Take the diagnostic to generate one."));
    }

    @GetMapping("/v1/misconceptions")
    public List<Misconception> open(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return misconceptions.openFor(principal.getId());
    }

    /** Starts or resumes the entry diagnostic, whose slug is fixed so clients need not hunt for it. */
    @PostMapping("/v1/diagnostic")
    public AttemptState diagnostic(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return attempts.start(principal.getId(), ENTRY_DIAGNOSTIC);
    }
}
