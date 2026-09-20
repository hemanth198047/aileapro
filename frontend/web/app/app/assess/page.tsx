"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ErrorAlert, IconArrowRight } from "@aileapro/ui";
import { ApiError, api, type Misconception, type ReviewQueue, type StudyPlan } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * The assessment hub: what is due, what is held against you, and where to be placed.
 *
 * <p>It shows counts and routes; the work itself happens in the runner, the review queue, and
 * the unit pages. Nothing on this page is a summary statistic of the learner — every number
 * here is a queue length, which is a fact about work outstanding rather than a claim about
 * what they know.
 */
export default function AssessPage() {
  const [queue, setQueue] = useState<ReviewQueue | null>(null);
  const [misconceptions, setMisconceptions] = useState<Misconception[] | null>(null);
  const [plan, setPlan] = useState<StudyPlan | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api<ReviewQueue>("/v1/reviews/due")
      .then((loaded) => !cancelled && setQueue(loaded))
      .catch((err) => !cancelled && setError(err instanceof Error ? err.message : "Could not load reviews"));
    api<Misconception[]>("/v1/misconceptions")
      .then((loaded) => !cancelled && setMisconceptions(loaded))
      .catch(() => !cancelled && setMisconceptions([]));
    // No plan yet is the normal state before the diagnostic, so a 404 here is not an error.
    api<StudyPlan>("/v1/plan")
      .then((loaded) => !cancelled && setPlan(loaded))
      .catch((err) => {
        if (!cancelled && !(err instanceof ApiError && err.status === 404)) {
          setError(err instanceof Error ? err.message : "Could not load your plan");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <SpaceHeader
        id="assess"
        action={
          queue && queue.dueCount > 0 ? (
            <span className="badge badge--warn">{queue.dueCount} due</span>
          ) : (
            <span className="badge badge--ok">Nothing due</span>
          )
        }
      />

      {error ? <ErrorAlert message={error} /> : null}

      <div className="grid-2">
        <section className="card">
          <div className="card__head">
            <span className="card__title">Review due</span>
            <span className="muted mono">{queue ? queue.dueCount : "…"}</span>
          </div>
          {queue && queue.dueCount > 0 ? (
            <>
              <p className="prose">
                {queue.overdueCount > 0
                  ? `${queue.overdueCount} of them are past their date, which is where forgetting actually happens.`
                  : "All of them are due today, none are late."}
              </p>
              <div className="space__actions" style={{ marginTop: 16 }}>
                <Link className="btn btn--primary" href="/app/review">
                  Start reviewing
                  <IconArrowRight />
                </Link>
              </div>
            </>
          ) : (
            <div className="empty">
              <strong>Nothing is due.</strong>
              <span>{queue?.note ?? "Checking your schedule…"}</span>
            </div>
          )}
        </section>

        <section className="card">
          <div className="card__head">
            <span className="card__title">Open misconceptions</span>
            <span className="muted mono">{misconceptions ? misconceptions.length : "…"}</span>
          </div>
          {misconceptions && misconceptions.length > 0 ? (
            <>
              <ul className="misconceptions">
                {misconceptions.map((raised) => (
                  <li key={raised.code}>
                    <strong>{raised.claim}</strong>
                    {raised.href ? (
                      <Link className="inline-link" href={raised.href}>
                        Read the page that argues against it
                        <IconArrowRight />
                      </Link>
                    ) : null}
                  </li>
                ))}
              </ul>
              <p className="hint">
                Each of these came from a specific answer, and each holds your state on its unit
                down until you answer something that depends on the opposite.
              </p>
            </>
          ) : (
            <div className="empty">
              <strong>None held against you.</strong>
              <span>
                A misconception is raised by choosing a distractor built from it, and cleared by
                getting the same idea right later.
              </span>
            </div>
          )}
        </section>
      </div>

      <section className="card card--hero">
        <p className="kicker">{plan ? `Plan v${plan.version}` : "Placement"}</p>
        <h2>{plan ? "You have a plan from your diagnostic." : "Find out where to start."}</h2>
        <p className="lede">
          {plan
            ? plan.reason
            : "The diagnostic adapts as you go and stops as soon as it can place you. It is not scored, and nothing in it can raise your mastery — it decides where you begin."}
        </p>
        <div className="space__actions">
          <Link className="btn btn--primary" href="/app/diagnostic">
            {plan ? "Retake the diagnostic" : "Take the diagnostic"}
            <IconArrowRight />
          </Link>
          {plan ? (
            <Link className="btn btn--outline" href="/app/today">
              See the next line of it
            </Link>
          ) : null}
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">How assessment works here</span>
        </div>
        <ul className="objectives">
          <li>
            Practice sets and unit quizzes are authored per unit, and they are marked on the
            server — the item never reaches your browser carrying its answer.
          </li>
          <li>
            Every attempt produces a written report: what held up, what did not, which belief a
            wrong answer revealed, and the page to reread for each one.
          </li>
          <li>
            A quiz can fill the Practice and Assess chips. It cannot make you MASTERED, because
            answering questions about something is not the same as having built with it.
          </li>
          <li>
            Passing a unit enrols its cards for spaced review, and answering one early does not
            count towards retention.
          </li>
        </ul>
        <p className="hint" style={{ marginTop: 12 }}>
          Exams across a whole path, and the code-graded items the Lab will run, come later. Open
          a unit to find its practice set and quiz.
        </p>
      </section>
    </>
  );
}
