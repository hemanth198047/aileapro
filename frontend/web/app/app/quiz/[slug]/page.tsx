"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Callout, ErrorAlert, IconAlert, IconArrowRight } from "@aileapro/ui";
import { ApiError, api, csrfHeaders, type AttemptState } from "@/lib/api";
import { QuizRunner } from "@/components/quiz-runner";

/**
 * One graded sitting, addressed by the assessment's slug.
 *
 * <p>Starting is a POST on arrival, and the API resumes an attempt that is already open
 * rather than opening a second one. So reloading this page mid-quiz puts the learner back
 * where they were instead of handing out a fresh set of questions, which would turn every
 * hard item into a reroll.
 */
export default function QuizPage() {
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [attempt, setAttempt] = useState<AttemptState | null>(null);
  const [missing, setMissing] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const started = await api<AttemptState>(`/v1/assessments/${slug}/attempts`, {
          method: "POST",
          headers: await csrfHeaders(),
        });
        if (!cancelled) setAttempt(started);
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setMissing(true);
          return;
        }
        setError(err instanceof Error ? err.message : "Could not start this assessment");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [slug]);

  if (missing) {
    return (
      <>
        <header className="space__head">
          <div className="space__titles">
            <p className="kicker">Assess</p>
            <h1>Not published</h1>
            <p className="lede">
              There is no published assessment at <code className="mono">{slug}</code>.
            </p>
          </div>
          <div className="space__meta">
            <span className="badge badge--warn">404</span>
          </div>
        </header>
        <section className="card">
          <Callout tone="warn" icon={<IconAlert />}>
            Either the slug is wrong, or the assessment exists but is still in draft. Unpublished
            items can never be attempted, because an attempt on one would produce evidence
            nobody reviewed.
          </Callout>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--outline" href="/app/assess">
              Back to Assess
              <IconArrowRight />
            </Link>
          </div>
        </section>
      </>
    );
  }

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <p className="kicker">Assess</p>
          <h1>{attempt ? attempt.assessmentTitle : "Opening your attempt…"}</h1>
          {attempt?.unit ? (
            <p className="lede">
              On <Link href={`/app/learn/${attempt.unit.slug}`}>{attempt.unit.title}</Link>. Every
              answer is marked the moment you give it, and each one comes back with the reasoning.
            </p>
          ) : null}
        </div>
        <div className="space__meta">
          {attempt ? <span className="badge">{attempt.kind}</span> : null}
        </div>
      </header>

      {error ? <ErrorAlert message={error} /> : null}

      {attempt ? (
        <QuizRunner attempt={attempt} />
      ) : (
        <section className="card">
          <p className="muted">Serving your questions…</p>
        </section>
      )}
    </>
  );
}
