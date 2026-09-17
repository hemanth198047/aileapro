"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Callout, IconArrowRight, IconMap } from "@aileapro/ui";
import { api, type Neighborhood, type Prerequisites } from "@/lib/api";
import { edgePhrase, isDeadEnd, masteryMeaning } from "@/components/graph-canvas";

/**
 * What a unit needs, and what else it touches.
 *
 * <p>The honest part is what this does not say. It knows the authored prerequisites and it now
 * knows which of them you have read, which is a weaker claim than knowing which you
 * understand. So an unread prerequisite is marked as unread, a read one is not marked as
 * understood, and neither blocks the page.
 */
export function PrereqPanel({ slug }: { slug: string }) {
  const [prereqs, setPrereqs] = useState<Prerequisites | null>(null);
  const [related, setRelated] = useState<Neighborhood | null>(null);

  useEffect(() => {
    let cancelled = false;
    setPrereqs(null);
    setRelated(null);
    api<Prerequisites>(`/v1/units/${slug}/prerequisites`)
      .then((loaded) => {
        if (!cancelled) setPrereqs(loaded);
      })
      .catch(() => {
        // The unit itself has already loaded; a graph failure must not blank the page.
      });
    api<Neighborhood>(`/v1/units/${slug}/graph?hops=1&types=RELATED_TO,APPLIED_IN,ALTERNATIVE_TO`)
      .then((loaded) => {
        if (!cancelled) setRelated(loaded);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [slug]);

  const requiredCount = prereqs?.required.length ?? 0;
  const advisoryCount = prereqs?.advisory.length ?? 0;
  const relatedEdges = related?.edges ?? [];

  if (prereqs && requiredCount === 0 && advisoryCount === 0 && relatedEdges.length === 0) {
    return null;
  }

  return (
    <section className="card">
      <div className="card__head">
        <span className="card__title">Before this, and around it</span>
        <Link className="btn btn--ghost" href={`/app/map?unit=${slug}`}>
          See it on the map
          <IconArrowRight />
        </Link>
      </div>

      {!prereqs ? <p className="muted">Reading the graph…</p> : null}

      {requiredCount > 0 ? (
        <>
          <p className="kicker">Required first</p>
          <ol className="prereqs">
            {prereqs!.required.map((step) => (
              <li className="prereq" key={step.unit.id}>
                <span className="prereq__depth" aria-hidden>
                  {step.depth}
                </span>
                <div className="prereq__body">
                  <Link className="prereq__title" href={`/app/learn/${step.unit.slug}`}>
                    {step.unit.title}
                    <IconArrowRight />
                  </Link>
                  <p className="prereq__meta">
                    <span className="badge">
                      {step.direct ? "directly required" : `${step.depth} steps back`}
                    </span>
                    {step.unit.estimateMinutes ? (
                      <span className="badge">{step.unit.estimateMinutes} min</span>
                    ) : null}
                    <span
                      className="badge"
                      data-mastery={step.masteryState}
                      title={masteryMeaning(step.masteryState)}
                    >
                      {step.masteryState === "UNKNOWN"
                        ? "not read yet"
                        : step.masteryState.toLowerCase()}
                    </span>
                  </p>
                </div>
              </li>
            ))}
          </ol>
          <p className="hint">
            Listed furthest-back first, which is study order. The states are your own reading:
            anything marked not read is a gap you can close today, and INTRODUCED means you read
            it, not that you can do it. Nothing here blocks the page, and a diagnostic that can
            skip what you already know arrives with the assessment engine.
          </p>
        </>
      ) : null}

      {advisoryCount > 0 ? (
        <>
          <p className="kicker" style={{ marginTop: 18 }}>
            Helps, but not required
          </p>
          <ul className="prereqs prereqs--soft">
            {prereqs!.advisory.map((step) => (
              <li className="prereq" key={step.unit.id}>
                <div className="prereq__body">
                  <Link className="prereq__title" href={`/app/learn/${step.unit.slug}`}>
                    {step.unit.title}
                    <IconArrowRight />
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        </>
      ) : null}

      {relatedEdges.length > 0 ? (
        <>
          <p className="kicker" style={{ marginTop: 18 }}>
            Connected to
          </p>
          <ul className="related">
            {relatedEdges.map((edge) => {
              const outgoing = edge.fromId === related!.focus.id;
              const other = related!.nodes.find(
                (node) => node.unit.id === (outgoing ? edge.toId : edge.fromId),
              );
              if (!other) return null;
              return (
                <li key={edge.id}>
                  <span className={`etag etag--${edge.type.toLowerCase().replace(/_/g, "-")}`}>
                    {edgePhrase(edge.type, outgoing)}
                  </span>{" "}
                  <Link href={`/app/learn/${other.unit.slug}`}>{other.unit.title}</Link>
                  {isDeadEnd(other.unit) ? (
                    <span className="badge badge--warn">nothing to read yet</span>
                  ) : null}
                  {edge.note ? <span className="related__note">{edge.note}</span> : null}
                </li>
              );
            })}
          </ul>
        </>
      ) : null}

      {prereqs && requiredCount === 0 && advisoryCount > 0 ? (
        <Callout icon={<IconMap />}>
          Nothing is required before this unit. The links above only make it easier.
        </Callout>
      ) : null}
    </section>
  );
}
