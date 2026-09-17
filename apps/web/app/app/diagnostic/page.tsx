"use client";

import Link from "next/link";
import { useState } from "react";
import { Button, Callout, ErrorAlert, IconAlert, IconArrowRight } from "@aileapro/ui";
import {
  ApiError,
  api,
  csrfHeaders,
  type AttemptState,
  type Report,
  type StudyPlan,
} from "@/lib/api";
import { QuizRunner } from "@/components/quiz-runner";
import { ReportView } from "@/components/report-view";
import { PlanView } from "@/components/plan-view";

/**
 * Placement, in three steps: sit it, read what it found, take the plan.
 *
 * <p>Nothing answered here counts as evidence of mastery. That is not a caveat in the copy,
 * it is enforced by the assessment kind — a diagnostic cannot move an evidence chip, because
 * two questions on a topic are enough to decide where to start and nowhere near enough to
 * claim somebody knows it.
 */
export default function DiagnosticPage() {
  const [attempt, setAttempt] = useState<AttemptState | null>(null);
  const [report, setReport] = useState<Report | null>(null);
  const [plan, setPlan] = useState<StudyPlan | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [needsGoal, setNeedsGoal] = useState(false);

  const start = async () => {
    setBusy(true);
    setError("");
    try {
      setAttempt(
        await api<AttemptState>("/v1/diagnostic", {
          method: "POST",
          headers: await csrfHeaders(),
        }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start the diagnostic");
    } finally {
      setBusy(false);
    }
  };

  const generatePlan = async () => {
    if (!report) return;
    setBusy(true);
    setError("");
    try {
      setPlan(
        await api<StudyPlan>(`/v1/attempts/${report.attemptId}/plan`, {
          method: "POST",
          headers: await csrfHeaders(),
        }),
      );
    } catch (err) {
      // A missing goal is a fixable situation, not a failure: the diagnostic is still
      // submitted and the plan is one form away, so say that instead of showing an error.
      if (err instanceof ApiError && err.code === "goal_required") {
        setNeedsGoal(true);
      } else {
        setError(err instanceof Error ? err.message : "Could not generate your plan");
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <p className="kicker">Assess</p>
          <h1>{plan ? "Your plan" : "Where should you start?"}</h1>
          <p className="lede">
            {plan
              ? "Built from what the diagnostic found, with the reason for every line."
              : "Twelve to sixteen questions across the curriculum, adapting as you go. It places you; it does not grade you."}
          </p>
        </div>
        <div className="space__meta">
          <span className="badge badge--brand">Not scored</span>
        </div>
      </header>

      {error ? <ErrorAlert message={error} /> : null}

      {plan ? (
        <PlanView plan={plan} />
      ) : report ? (
        <>
          <ReportView report={report} />
          <section className="card card--hero">
            <p className="kicker">One step left</p>
            <h2>{needsGoal ? "Choose a goal, and the plan follows." : "Turn this into a plan."}</h2>
            <p className="lede">
              {needsGoal
                ? "A plan needs a destination — without one it is only a reading list. Set your goal and weekly hours, then come back here; this diagnostic is submitted and waiting."
                : "The plan orders what to do from where the diagnostic placed you, and every line carries the reason it is on the list. It is versioned, so changing your goal later produces a new one rather than quietly rewriting this."}
            </p>
            <div className="space__actions">
              {needsGoal ? (
                <Link className="btn btn--primary" href="/app/onboarding">
                  Choose your goal
                  <IconArrowRight />
                </Link>
              ) : null}
              <Button
                variant={needsGoal ? "outline" : "primary"}
                onClick={generatePlan}
                disabled={busy}
              >
                {busy ? "Building your plan…" : needsGoal ? "Try again" : "Generate my plan"}
                <IconArrowRight />
              </Button>
            </div>
          </section>
        </>
      ) : attempt ? (
        <QuizRunner attempt={attempt} onSubmitted={setReport} />
      ) : (
        <Intro busy={busy} onStart={start} />
      )}
    </>
  );
}

function Intro({ busy, onStart }: { busy: boolean; onStart: () => void }) {
  return (
    <>
      <section className="card card--hero">
        <p className="kicker">Before you start</p>
        <h2>It gets harder where you are strong and stops where you are not.</h2>
        <p className="lede">
          Each domain asks at least two questions and at most four. Two wrong in a row ends that
          domain — the ceiling is clearly below it, and pushing further would be demoralising
          without telling us anything we would act on. Three right in a row ends it too, because
          the remaining questions are better spent where the answer is still unknown.
        </p>
        <div className="space__actions">
          <Button variant="primary" onClick={onStart} disabled={busy}>
            {busy ? "Preparing your questions…" : "Start the diagnostic"}
            <IconArrowRight />
          </Button>
          <Link className="btn btn--outline" href="/app/assess">
            Not now
          </Link>
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">What it does and does not do</span>
        </div>
        <Callout icon={<IconAlert />}>
          A right answer here does not fill an evidence chip, and a wrong one does not count
          against you.
        </Callout>
        <p className="prose" style={{ marginTop: 12 }}>
          It produces a starting point per domain with a confidence attached, so thin evidence
          stays visibly thin. You will meet all of this material properly, and be assessed on it
          then — which is the only assessment that can claim you know something.
        </p>
        <p className="hint" style={{ marginTop: 12 }}>
          There is a thirty minute limit. If it runs out, what you have answered is still
          submitted and still produces a plan.
        </p>
      </section>
    </>
  );
}
