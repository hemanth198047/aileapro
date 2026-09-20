"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button, Callout, ErrorAlert } from "@aileapro/ui";
import { api, csrfHeaders, type CareerGaps } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Career: skill gaps from demonstrated mastery only, plus interview practice via the tutor.
 */
export default function CareerPage() {
  const [gaps, setGaps] = useState<CareerGaps | null>(null);
  const [error, setError] = useState("");
  const [starting, setStarting] = useState(false);

  const refresh = useCallback(async () => {
    const next = await api<CareerGaps>("/v1/me/career/gaps");
    setGaps(next);
  }, []);

  useEffect(() => {
    let live = true;
    refresh().catch((err) =>
      live && setError(err instanceof Error ? err.message : "Could not load career gaps"),
    );
    return () => {
      live = false;
    };
  }, [refresh]);

  const startInterview = async () => {
    if (!gaps || starting) return;
    const first = gaps.gaps[0];
    const unitSlug = first?.unitSlug ?? "python-values-and-types";
    setStarting(true);
    setError("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch("/v1/tutor/conversations", {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({ unitSlug, persona: gaps.interviewPersona }),
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body?.error?.message ?? "Could not start interview practice");
      }
      window.location.href = `/app/learn/${unitSlug}`;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start interview practice");
    } finally {
      setStarting(false);
    }
  };

  return (
    <>
      <SpaceHeader id="career" />

      {error ? <ErrorAlert message={error} /> : null}

      <Callout tone="neutral">
        <strong>Evidence only.</strong> Gaps come from mastery records against your path. What you
        typed at onboarding never counts as mastered.
      </Callout>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Role readiness</span>
          <span className="muted mono">
            {gaps
              ? gaps.readyForRole
                ? "Path bar met"
                : "Not calculable as a percentage"
              : "…"}
          </span>
        </div>
        {gaps ? (
          <>
            <p className="prose">{gaps.honesty}</p>
            <p className="hint" style={{ marginTop: 8 }}>
              Path: {gaps.pathTitle}
              {gaps.roleTarget ? ` · Role target: ${gaps.roleTarget}` : ""} · Bar:{" "}
              {gaps.readyBar} · {gaps.readyUnitCount}/{gaps.requiredUnitCount} units ready
            </p>
            <p className="hint" style={{ marginTop: 8 }}>
              {gaps.note}
            </p>
          </>
        ) : (
          <p className="hint">Loading…</p>
        )}
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Skill gaps</span>
          <span className="muted mono">{gaps?.gapCount ?? "…"}</span>
        </div>
        {gaps && gaps.gaps.length === 0 ? (
          <div className="empty" style={{ marginTop: 12 }}>
            <strong>No gaps on this path.</strong>
            <span>Every required unit is at least {gaps.readyBar} from demonstrated evidence.</span>
          </div>
        ) : (
          <ul className="studio__list" style={{ marginTop: 12 }}>
            {gaps?.gaps.map((gap) => (
              <li key={gap.unitId}>
                <Link className="studio__pick" href={gap.href}>
                  <strong>{gap.unitTitle}</strong>
                  <span className="muted mono">
                    {gap.currentState} → {gap.requiredBar}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Interview practice</span>
          <span className="badge">{gaps?.interviewPersona ?? "…"}</span>
        </div>
        <p className="hint">
          Opens the tutor as an interview coach on a unit you still need. Rehearsal answers are
          not mastery — they will not close a gap by themselves.
        </p>
        <div className="lab__actions">
          <Button type="button" disabled={starting || !gaps} onClick={() => void startInterview()}>
            {starting ? "Starting…" : "Start interview practice"}
          </Button>
        </div>
      </section>
    </>
  );
}
