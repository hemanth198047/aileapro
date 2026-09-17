"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { Callout, ErrorAlert, IconArrowRight, IconMap } from "@aileapro/ui";
import { SpaceHeader } from "@/components/space-view";
import { GraphCanvas, GraphList } from "@/components/graph-canvas";
import { api, type Neighborhood, type PathDetail, type PathSummary } from "@/lib/api";

const DEFAULT_FOCUS = "matrix-multiplication";

/**
 * The map reads one neighbourhood at a time. It never asks for the whole graph: the server
 * clamps the radius and caps the node count, and this page reports both so a trimmed view
 * cannot be mistaken for the complete picture.
 */
export default function MapPage() {
  return (
    <Suspense
      fallback={
        <section className="card">
          <p className="muted">Loading the map…</p>
        </section>
      }
    >
      <MapView />
    </Suspense>
  );
}

/** Split out because the focus and hop count live in the URL, which needs a Suspense boundary. */
function MapView() {
  const router = useRouter();
  const params = useSearchParams();
  const focus = params.get("unit") ?? DEFAULT_FOCUS;
  const hops = clampHops(Number(params.get("hops") ?? 2));

  const [graph, setGraph] = useState<Neighborhood | null>(null);
  const [path, setPath] = useState<PathDetail | null>(null);
  const [error, setError] = useState("");
  const [types, setTypes] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const summaries = await api<PathSummary[]>("/v1/paths");
        if (summaries.length === 0 || cancelled) return;
        const detail = await api<PathDetail>(`/v1/paths/${summaries[0].slug}`);
        if (!cancelled) setPath(detail);
      } catch {
        // The picker is a convenience; losing it must not take the map down with it.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setGraph(null);
    setError("");
    const query = new URLSearchParams({ hops: String(hops) });
    if (types.length > 0) query.set("types", types.join(","));
    api<Neighborhood>(`/v1/units/${focus}/graph?${query}`)
      .then((loaded) => {
        if (!cancelled) setGraph(loaded);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load the graph");
      });
    return () => {
      cancelled = true;
    };
  }, [focus, hops, types]);

  const move = useCallback(
    (next: { unit?: string; hops?: number }) => {
      const query = new URLSearchParams(params.toString());
      if (next.unit) query.set("unit", next.unit);
      if (next.hops) query.set("hops", String(next.hops));
      router.replace(`/app/map?${query}`);
    },
    [params, router],
  );

  // Only the types actually present in this neighbourhood are offered, so the filter cannot
  // suggest relationships the authors have not drawn.
  const availableTypes = useMemo(() => {
    const present = new Set(graph?.edges.map((edge) => edge.type) ?? []);
    return [...present].sort();
  }, [graph]);

  const toggleType = (type: string) => {
    setTypes((current) =>
      current.includes(type) ? current.filter((t) => t !== type) : [...current, type],
    );
  };

  return (
    <>
      <SpaceHeader
        id="map"
        action={
          graph ? (
            <span className="badge badge--ok">
              {graph.nodes.length} of max {graph.nodeBudget} nodes
            </span>
          ) : null
        }
      />

      {error ? <ErrorAlert message={error} /> : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">Centre</span>
          <span className="muted mono">
            {hops} hop{hops === 1 ? "" : "s"} · max {graph?.maxHops ?? 3}
          </span>
        </div>
        <div className="mapctl">
          <label className="mapctl__field">
            <span className="kicker">Unit</span>
            <select
              value={focus}
              onChange={(event) => move({ unit: event.target.value })}
              aria-label="Unit at the centre of the map"
            >
              {(path?.waypoints ?? []).map((waypoint) => (
                <option key={waypoint.unit.id} value={waypoint.unit.slug}>
                  {waypoint.unit.title}
                </option>
              ))}
              {path?.waypoints.some((w) => w.unit.slug === focus) ? null : (
                <option value={focus}>{focus}</option>
              )}
            </select>
          </label>
          <fieldset className="mapctl__field">
            <legend className="kicker">Hops</legend>
            <div className="filters">
              {[1, 2, 3].map((value) => (
                <button
                  key={value}
                  type="button"
                  className={`filter${value === hops ? " filter--on" : ""}`}
                  aria-pressed={value === hops}
                  onClick={() => move({ hops: value })}
                >
                  {value}
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset className="mapctl__field mapctl__field--wide">
            <legend className="kicker">Edge types</legend>
            <div className="filters">
              {availableTypes.length === 0 ? <span className="muted">—</span> : null}
              {availableTypes.map((type) => (
                <button
                  key={type}
                  type="button"
                  className={`filter${types.includes(type) ? " filter--on" : ""}`}
                  aria-pressed={types.includes(type)}
                  onClick={() => toggleType(type)}
                >
                  {type.replace(/_/g, " ").toLowerCase()}
                </button>
              ))}
              {types.length > 0 ? (
                <button type="button" className="filter" onClick={() => setTypes([])}>
                  show all
                </button>
              ) : null}
            </div>
          </fieldset>
        </div>
        <p className="hint">
          The edges are authored claims about the subject; the node fills are yours. A fill says
          what you have shown on that unit, so it moves when you do the work and not before.
        </p>
      </section>

      {graph ? (
        <>
          <section className="card">
            <div className="card__head">
              <span className="card__title">{graph.focus.title}</span>
              <Link className="btn btn--ghost" href={`/app/learn/${graph.focus.slug}`}>
                Open the unit
                <IconArrowRight />
              </Link>
            </div>
            {graph.edges.length === 0 ? (
              <Callout icon={<IconMap />}>
                {types.length > 0
                  ? "No edges of the selected types touch this unit. Widen the filter."
                  : "No edges have been authored at this unit yet, so there is nothing to draw. It is on the map, and that is all anyone can honestly say about it today."}
              </Callout>
            ) : (
              <GraphCanvas graph={graph} />
            )}
            {graph.truncated ? (
              <Callout tone="warn">
                The node budget of {graph.nodeBudget} stopped this walk, so the view is partial.
                Centre the map on a nearer unit to see the rest.
              </Callout>
            ) : null}
          </section>

          <section className="card">
            <div className="card__head">
              <span className="card__title">Nodes and edges</span>
              <span className="muted mono">
                {graph.nodes.length} nodes · {graph.edges.length} edges
              </span>
            </div>
            <p className="hint">
              This list is the map, not a summary of it: it carries every node and edge in the
              response, works with a keyboard and a screen reader, and each title opens the unit.
            </p>
            <GraphList graph={graph} />
          </section>
        </>
      ) : error ? null : (
        <section className="card">
          <p className="muted">Loading the neighbourhood…</p>
        </section>
      )}

      <section className="card">
        <div className="card__head">
          <span className="card__title">What this map does not show yet</span>
        </div>
        <p className="prose">
          Only authored units are nodes. Technologies, papers, and misconceptions get their own
          node kinds later, and the 50-odd domains with no units yet have no edges to draw. The
          fills can currently only reach INTRODUCED, because reading is the only evidence the
          platform collects until graded work arrives; impact analysis on content change is
          Phase 15.
        </p>
      </section>
    </>
  );
}

function clampHops(value: number) {
  if (!Number.isFinite(value)) return 2;
  return Math.max(1, Math.min(3, Math.round(value)));
}
