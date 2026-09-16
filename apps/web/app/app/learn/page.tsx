"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Callout, ErrorAlert, IconArrowRight, IconMap } from "@aileapro/ui";
import { api, type PathDetail, type PathSummary } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Learn's root is the catalog: the published paths and their waypoints. Everything shown
 * comes from /v1/paths, which returns published content only — a draft unit is absent from
 * the response, so there is nothing to filter client-side.
 */
export default function LearnPage() {
  const [paths, setPaths] = useState<PathDetail[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const summaries = await api<PathSummary[]>("/v1/paths");
        const details = await Promise.all(
          summaries.map((summary) => api<PathDetail>(`/v1/paths/${summary.slug}`)),
        );
        if (!cancelled) setPaths(details);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load the catalog");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const unitCount = paths?.reduce((total, path) => total + path.waypoints.length, 0) ?? 0;

  return (
    <>
      <SpaceHeader
        id="learn"
        action={paths ? <span className="badge badge--ok">{unitCount} units published</span> : null}
      />

      {error ? <ErrorAlert message={error} /> : null}

      {!paths && !error ? (
        <section className="card">
          <p className="muted">Loading the catalog…</p>
        </section>
      ) : null}

      {paths?.map((path) => (
        <section className="card" key={path.id}>
          <div className="card__head">
            <span className="card__title">{path.title}</span>
            <span className="muted mono">
              {path.waypoints.length} units · {Math.round(path.estimateMinutes / 60)} h reading · v
              {path.version}
            </span>
          </div>
          {path.description ? <p className="prose">{path.description}</p> : null}

          <ol className="waypoints">
            {path.waypoints.map((waypoint, index) => (
              <li className="waypoint" key={waypoint.unit.id}>
                <span className="waypoint__n" aria-hidden>
                  {index + 1}
                </span>
                <div className="waypoint__body">
                  <Link className="waypoint__title" href={`/app/learn/${waypoint.unit.slug}`}>
                    {waypoint.unit.title}
                    <IconArrowRight />
                  </Link>
                  {waypoint.unit.description ? (
                    <p className="waypoint__desc">{waypoint.unit.description}</p>
                  ) : null}
                  <p className="waypoint__meta">
                    {waypoint.unit.estimateMinutes ? (
                      <span className="badge">{waypoint.unit.estimateMinutes} min</span>
                    ) : null}
                    {waypoint.unit.difficulty ? (
                      <span className="badge">Difficulty {waypoint.unit.difficulty}/5</span>
                    ) : null}
                    <span className="badge">{waypoint.unit.volatility} volatility</span>
                  </p>
                </div>
              </li>
            ))}
          </ol>

          <p className="hint" style={{ marginTop: 16 }}>
            The order is the author&apos;s. Prerequisite edges arrive in Phase 4 and a plan shaped
            by what you already know in Phase 5 — nothing here is personalised yet, and reading a
            unit does not yet record progress.
          </p>
        </section>
      ))}

      {paths ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Beyond the authored slice</span>
            <span className="badge badge--brand">Phase 4</span>
          </div>
          <Callout icon={<IconMap />}>
            All 57 domains of the taxonomy exist with real descriptions, but only the units above
            are authored. Browsing the whole map, with prerequisites, is Phase 4.
          </Callout>
          <p className="hint" style={{ marginTop: 12 }}>
            To see what an unauthored domain looks like, open{" "}
            <Link href="/app/learn/ai-governance">AI Governance</Link>: a real description, a real
            volatility rating, and an explicit note that nothing is published in it yet. That is
            the honest state of the other 52 domains.
          </p>
        </section>
      ) : null}
    </>
  );
}
