"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Callout, ErrorAlert } from "@aileapro/ui";
import {
  ApiError,
  api,
  csrfHeaders,
  type LiteratureMap,
  type ResearchExplainer,
  type ResearchPaper,
  type ResearchReadingList,
} from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Research: seeded papers, explainers that may only cite attached sources, literature map v0.
 */
export default function ResearchPage() {
  const [list, setList] = useState<ResearchReadingList | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [paper, setPaper] = useState<ResearchPaper | null>(null);
  const [question, setQuestion] = useState("What is retrieval-augmented generation?");
  const [explainer, setExplainer] = useState<ResearchExplainer | null>(null);
  const [map, setMap] = useState<LiteratureMap | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    const [reading, lit] = await Promise.all([
      api<ResearchReadingList>("/v1/research/papers"),
      api<LiteratureMap>("/v1/research/map"),
    ]);
    setList(reading);
    setMap(lit);
    if (!selected && reading.papers.length > 0) {
      const prefer = reading.papers.find((p) => p.slug === "rag-2020") ?? reading.papers[0];
      setSelected(prefer.slug);
    }
  }, [selected]);

  useEffect(() => {
    let live = true;
    refresh().catch((err) => live && setError(err instanceof Error ? err.message : "Could not load research"));
    return () => {
      live = false;
    };
  }, [refresh]);

  useEffect(() => {
    if (!selected) return;
    let live = true;
    api<ResearchPaper>(`/v1/research/papers/${selected}`)
      .then((p) => live && setPaper(p))
      .catch((err) => live && setError(err instanceof Error ? err.message : "Could not load paper"));
    return () => {
      live = false;
    };
  }, [selected]);

  const explain = async () => {
    if (busy || !question.trim()) return;
    setBusy(true);
    setError("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch("/v1/research/explain", {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({ question, paperSlug: selected }),
      });
      const body = await res.json();
      if (!res.ok) {
        throw new ApiError(res.status, body?.error?.message ?? "Explain failed", body?.error?.code);
      }
      setExplainer(body as ResearchExplainer);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Explain failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <SpaceHeader id="research" />

      {error ? <ErrorAlert message={error} /> : null}

      <Callout tone="neutral">
        <strong>Forced citations.</strong> Explainers may only cite papers from the seeded reading
        list. If nothing matches, the API refuses rather than inventing a reference.
      </Callout>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Reading list</span>
          <span className="muted mono">{list?.papers.length ?? "…"}</span>
        </div>
        <p className="hint">{list?.note ?? "Loading…"}</p>
        <ul className="studio__list" style={{ marginTop: 12 }}>
          {list?.papers.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                className={`studio__pick${selected === p.slug ? " is-active" : ""}`}
                onClick={() => {
                  setSelected(p.slug);
                  setExplainer(null);
                }}
              >
                <strong>{p.title}</strong>
                <span className="muted mono">{p.doi ?? p.slug}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      {paper ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">{paper.title}</span>
            <span className="badge">PAPER</span>
          </div>
          <p className="hint">
            {paper.authors}
            {paper.publishedOn ? ` · ${paper.publishedOn}` : ""}
          </p>
          <p className="prose" style={{ marginTop: 8 }}>
            {paper.abstractText}
          </p>
          <p className="hint" style={{ marginTop: 8 }}>
            DOI {paper.doi ?? "—"} ·{" "}
            <a href={paper.url} target="_blank" rel="noreferrer">
              Open source
            </a>
            {paper.relatedUnitSlugs.length > 0
              ? ` · Units: ${paper.relatedUnitSlugs.join(", ")}`
              : null}
          </p>

          <h3 className="studio__msTitle">Explainer</h3>
          <textarea
            className="lab__source"
            rows={3}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            aria-label="Research question"
          />
          <div className="lab__actions">
            <Button type="button" disabled={busy || !question.trim()} onClick={() => void explain()}>
              {busy ? "Explaining…" : "Explain with citations"}
            </Button>
          </div>

          {explainer ? (
            <div className="studio__result">
              {explainer.refused ? (
                <Callout tone="warn">
                  <strong>Refused ({explainer.refusalCode}).</strong> {explainer.answer}
                </Callout>
              ) : (
                <>
                  {explainer.development ? (
                    <Callout tone="warn">
                      <strong>Fixture.</strong> No vendor model is wired; the reply is grounded only
                      in attached paper metadata.
                    </Callout>
                  ) : null}
                  <pre className="lab__out">{explainer.answer}</pre>
                  <ul className="studio__list" style={{ marginTop: 12 }}>
                    {explainer.citations.map((c) => (
                      <li key={c.slug} className="hint">
                        <strong>{c.title}</strong>
                        <span className="muted mono">
                          {" "}
                          {c.doi ?? c.slug}
                        </span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">Literature map</span>
          <span className="muted mono">v0</span>
        </div>
        <p className="hint">{map?.note ?? "Loading…"}</p>
        {map ? (
          <div className="research__map">
            <div>
              <strong className="muted">Papers</strong>
              <ul className="studio__list" style={{ marginTop: 8 }}>
                {map.nodes
                  .filter((n) => n.kind === "paper")
                  .map((n) => (
                    <li key={n.id} className="hint">
                      {n.title}
                    </li>
                  ))}
              </ul>
            </div>
            <div>
              <strong className="muted">Units citing them</strong>
              <ul className="studio__list" style={{ marginTop: 8 }}>
                {map.nodes
                  .filter((n) => n.kind === "unit")
                  .map((n) => (
                    <li key={n.id} className="hint">
                      {n.title}
                    </li>
                  ))}
              </ul>
            </div>
            <div>
              <strong className="muted">Edges</strong>
              <ul className="studio__list" style={{ marginTop: 8 }}>
                {map.edges.map((e, i) => (
                  <li key={`${e.from}-${e.to}-${i}`} className="muted mono">
                    {e.from} → {e.to}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ) : null}
      </section>
    </>
  );
}
