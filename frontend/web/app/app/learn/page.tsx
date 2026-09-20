"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Callout, ErrorAlert, IconArrowRight, IconMap, MasteryChips } from "@aileapro/ui";
import { api, type PathDetail, type PathSummary, type Roadmap, type RoadmapStop } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Learn's root is the roadmap: the published waypoints of your path with your own evidence
 * laid over them, followed by the other published paths as plain catalog.
 *
 * <p>The overlay is deliberately unflattering. A waypoint that asks for PRACTICED shows as
 * unmet no matter how thoroughly it has been read, because reading is not practice, and the
 * row says which of the two it is rather than colouring it green for effort.
 */
export default function LearnPage() {
  const [roadmap, setRoadmap] = useState<Roadmap | null>(null);
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
        return;
      }

      try {
        const mine = await api<Roadmap>("/v1/me/roadmap");
        if (!cancelled) setRoadmap(mine);
      } catch {
        // No target path and nothing published to fall back on. The catalog below is still
        // worth showing, so this is not an error the learner needs to read about.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const unitCount = paths?.reduce((total, path) => total + path.waypoints.length, 0) ?? 0;
  const others = paths?.filter((path) => path.slug !== roadmap?.pathSlug) ?? [];

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

      {roadmap ? <RoadmapCard roadmap={roadmap} /> : null}

      {others.map((path) => (
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
                </div>
              </li>
            ))}
          </ol>
        </section>
      ))}

      {paths ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Beyond the authored slice</span>
            <span className="badge badge--brand">Authoring continues</span>
          </div>
          <Callout icon={<IconMap />}>
            All 57 domains of the taxonomy exist with real descriptions, but only the units above
            are authored. The <Link href="/app/map">map</Link> shows how they connect.
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

function RoadmapCard({ roadmap }: { roadmap: Roadmap }) {
  const introduced = roadmap.stops.filter((stop) => stop.mastery.state !== "UNKNOWN").length;

  return (
    <section className="card">
      <div className="card__head">
        <span className="card__title">{roadmap.pathTitle}</span>
        <span className="muted mono">
          {introduced} of {roadmap.stops.length} started · {Math.round(roadmap.estimateMinutes / 60)}{" "}
          h reading · v{roadmap.pathVersion}
        </span>
      </div>
      <p className="hint">
        Your path, in the author&apos;s order, with what you have shown on each stop. The required
        state is the author&apos;s bar for the waypoint, not a box that reading can tick.
      </p>

      <ol className="waypoints">
        {roadmap.stops.map((stop, index) => (
          <RoadmapRow key={stop.unit.id} stop={stop} index={index} />
        ))}
      </ol>

      <p className="hint" style={{ marginTop: 16 }}>
        Every stop above will stay short of its required state until graded practice and
        assessment exist, because those are the only things that can prove it. That is the
        honest position, not a gap in the roadmap.
      </p>
    </section>
  );
}

function RoadmapRow({ stop, index }: { stop: RoadmapStop; index: number }) {
  const read = stop.mastery.evidence.read.state;

  return (
    <li className="waypoint" data-state={read}>
      <span className="waypoint__n" aria-hidden>
        {index + 1}
      </span>
      <div className="waypoint__body">
        <Link className="waypoint__title" href={`/app/learn/${stop.unit.slug}`}>
          {stop.unit.title}
          <IconArrowRight />
        </Link>
        {stop.unit.description ? <p className="waypoint__desc">{stop.unit.description}</p> : null}
        <p className="waypoint__meta">
          <span className="badge" title="What you have shown on this unit">
            {stop.mastery.state}
          </span>
          {stop.requiredMasteryState ? (
            <span
              className={stop.requirementMet ? "badge badge--ok" : "badge"}
              title="The author's bar for this waypoint"
            >
              needs {stop.requiredMasteryState}
            </span>
          ) : null}
          {stop.unit.estimateMinutes ? (
            <span className="badge">{stop.unit.estimateMinutes} min</span>
          ) : null}
        </p>
        <MasteryChips evidence={stop.mastery.evidence} />
      </div>
    </li>
  );
}
