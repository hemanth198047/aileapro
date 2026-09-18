"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Callout, ErrorAlert } from "@aileapro/ui";
import {
  ApiError,
  api,
  csrfHeaders,
  type Portfolio,
  type PortfolioItem,
  type ProjectDetail,
  type ProjectSubmissionResult,
  type ProjectSummary,
} from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Project Studio: finishable briefs, milestone submissions through the lab sandbox, and a
 * portfolio that stays private until you choose otherwise. Completing a project is what fills
 * Apply — quizzes never do.
 */
export default function StudioPage() {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [detail, setDetail] = useState<ProjectDetail | null>(null);
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [milestoneSlug, setMilestoneSlug] = useState<string | null>(null);
  const [source, setSource] = useState("");
  const [result, setResult] = useState<ProjectSubmissionResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const refreshList = useCallback(async () => {
    const [list, folio] = await Promise.all([
      api<ProjectSummary[]>("/v1/projects"),
      api<Portfolio>("/v1/portfolio"),
    ]);
    setProjects(list);
    setPortfolio(folio);
    if (!selected && list.length > 0) {
      const first = list.find((p) => p.acceptsSubmissions) ?? list[0];
      setSelected(first.slug);
    }
  }, [selected]);

  const refreshDetail = useCallback(async (slug: string) => {
    const next = await api<ProjectDetail>(`/v1/projects/${slug}`);
    setDetail(next);
    const open = next.milestones.find((m) => !m.passed) ?? next.milestones[0] ?? null;
    if (open) {
      setMilestoneSlug(open.slug);
      setSource(open.starterSource ?? next.starterSource ?? "");
    } else {
      setMilestoneSlug(null);
      setSource(next.starterSource ?? "");
    }
  }, []);

  useEffect(() => {
    let live = true;
    refreshList().catch((err) => live && setError(err instanceof Error ? err.message : "Could not load Studio"));
    return () => {
      live = false;
    };
  }, [refreshList]);

  useEffect(() => {
    if (!selected) return;
    let live = true;
    refreshDetail(selected).catch((err) => live && setError(err instanceof Error ? err.message : "Could not load project"));
    return () => {
      live = false;
    };
  }, [selected, refreshDetail]);

  const pickMilestone = (slug: string) => {
    if (!detail) return;
    const m = detail.milestones.find((x) => x.slug === slug);
    if (!m) return;
    setMilestoneSlug(slug);
    setSource(m.starterSource ?? detail.starterSource ?? "");
    setResult(null);
  };

  const submit = async () => {
    if (!detail || !milestoneSlug || busy || !source.trim()) return;
    setBusy(true);
    setError("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch(
        `/v1/projects/${detail.slug}/milestones/${milestoneSlug}/submit`,
        {
          method: "POST",
          credentials: "include",
          headers,
          body: JSON.stringify({ source }),
        },
      );
      const body = await res.json();
      if (!res.ok) {
        throw new ApiError(res.status, body?.error?.message ?? "Submit failed", body?.error?.code);
      }
      setResult(body as ProjectSubmissionResult);
      await Promise.all([refreshDetail(detail.slug), refreshList()]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Submit failed");
    } finally {
      setBusy(false);
    }
  };

  const setVisibility = async (item: PortfolioItem, visibility: "private" | "public") => {
    try {
      const headers = await csrfHeaders();
      const res = await fetch(`/v1/portfolio/${item.id}`, {
        method: "PATCH",
        credentials: "include",
        headers,
        body: JSON.stringify({ visibility }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(res.status, body?.error?.message ?? "Update failed", body?.error?.code);
      }
      await refreshList();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update visibility");
    }
  };

  const active = detail?.milestones.find((m) => m.slug === milestoneSlug) ?? null;

  return (
    <>
      <SpaceHeader id="studio" />

      {error ? <ErrorAlert message={error} /> : null}

      {result?.development ? (
        <Callout tone="warn">
          <strong>Fixtures.</strong> Milestone checks ran through the development lab executor.
          A canned pass is still a pass on the fixture path — wire Docker for a real interpreter.
        </Callout>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">Projects</span>
          <span className="muted mono">{projects.length}</span>
        </div>
        <ul className="studio__list">
          {projects.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                className={`studio__pick${selected === p.slug ? " is-active" : ""}`}
                onClick={() => {
                  setSelected(p.slug);
                  setResult(null);
                }}
              >
                <strong>{p.title}</strong>
                <span className="muted">
                  {p.status === "outline"
                    ? "Outline only"
                    : p.completed
                      ? "Completed"
                      : `${p.passedMilestones}/${p.milestoneCount} milestones`}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      {detail ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">{detail.title}</span>
            <span className="badge">{detail.status}</span>
          </div>
          <p className="prose">{detail.summary}</p>
          <p className="hint" style={{ marginTop: 8 }}>
            {detail.objective}
          </p>
          {detail.applyUnitSlugs.length > 0 ? (
            <p className="hint" style={{ marginTop: 8 }}>
              Completing this fills Apply on: {detail.applyUnitSlugs.join(", ")}
            </p>
          ) : null}

          {!detail.acceptsSubmissions ? (
            <Callout tone="neutral">
              <strong>Not finishable yet.</strong> This brief is visible on purpose. Milestones and
              sandbox checks are not authored, so Studio will not invent a submission path.
            </Callout>
          ) : (
            <>
              <ol className="studio__milestones">
                {detail.milestones.map((m) => (
                  <li key={m.id}>
                    <button
                      type="button"
                      className={`studio__milestone${milestoneSlug === m.slug ? " is-active" : ""}`}
                      onClick={() => pickMilestone(m.slug)}
                    >
                      <span>
                        {m.passed ? "✓ " : ""}
                        {m.title}
                      </span>
                      <span className="muted mono">{m.slug}</span>
                    </button>
                  </li>
                ))}
              </ol>

              {active ? (
                <>
                  <h3 className="studio__msTitle">{active.title}</h3>
                  <p className="prose">{active.description}</p>
                  <p className="hint">
                    <strong>Criteria.</strong> {active.criteria}
                  </p>
                  {active.hint ? (
                    <p className="hint">
                      <strong>Hint.</strong> {active.hint}
                    </p>
                  ) : null}
                  <textarea
                    className="lab__source"
                    spellCheck={false}
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                    rows={14}
                    aria-label="Milestone source"
                  />
                  <div className="lab__actions">
                    <Button
                      type="button"
                      disabled={busy || !source.trim() || active.passed}
                      onClick={() => void submit()}
                    >
                      {active.passed ? "Passed" : busy ? "Checking…" : "Submit milestone"}
                    </Button>
                  </div>
                </>
              ) : null}
            </>
          )}

          {result ? (
            <div className="studio__result">
              <p className={result.passed ? "hint" : "error"}>
                <strong>{result.passed ? "Passed." : "Not yet."}</strong> {result.mentorComment}
              </p>
              {result.stdout ? <pre className="lab__out">{result.stdout}</pre> : null}
              {result.projectCompleted ? (
                <p className="hint">
                  Project complete. Apply updated on {result.applyUnitsUpdated.join(", ") || "linked units"}.
                  A private portfolio artifact was created.
                </p>
              ) : null}
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">Portfolio</span>
          <span className="muted mono">Private by default</span>
        </div>
        <p className="hint">{portfolio?.note ?? "Loading…"}</p>
        {portfolio && portfolio.items.length === 0 ? (
          <div className="empty" style={{ marginTop: 12 }}>
            <strong>No artifacts.</strong>
            <span>Finishing a published project is what puts something here.</span>
          </div>
        ) : (
          <ul className="studio__portfolio">
            {portfolio?.items.map((item) => (
              <li key={item.id} className="studio__artifact">
                <div>
                  <strong>{item.title}</strong>
                  <p className="hint">{item.summary}</p>
                  {item.reflection ? <p className="prose">{item.reflection}</p> : null}
                </div>
                <div className="lab__actions">
                  <span className="badge">{item.visibility}</span>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() =>
                      void setVisibility(item, item.visibility === "public" ? "private" : "public")
                    }
                  >
                    Make {item.visibility === "public" ? "private" : "public"}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
