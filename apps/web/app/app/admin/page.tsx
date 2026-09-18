"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { Button, Callout, ErrorAlert, IconArrowRight, IconShield } from "@aileapro/ui";
import {
  ApiError,
  api,
  csrfHeaders,
  type AdminAnalytics,
  type AdminChange,
  type AdminUnitDetail,
  type AdminUnitSummary,
} from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

type Access = "checking" | "granted" | "denied" | "error";

/**
 * Thin content studio: list any status, open a draft, run critic, publish with audit.
 * Learner Learn routes stay published-only — drafts never leak through this UI.
 */
export default function AdminPage() {
  const [access, setAccess] = useState<Access>("checking");
  const [message, setMessage] = useState("");
  const [units, setUnits] = useState<AdminUnitSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState("all");
  const [selected, setSelected] = useState<AdminUnitDetail | null>(null);
  const [changelog, setChangelog] = useState<AdminChange[]>([]);
  const [busy, setBusy] = useState(false);
  const [createSlug, setCreateSlug] = useState("");
  const [createTitle, setCreateTitle] = useState("");
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);

  const loadList = useCallback(async (filter: string) => {
    const q = filter === "all" ? "" : `?status=${encodeURIComponent(filter)}`;
    const rows = await api<AdminUnitSummary[]>(`/v1/admin/units${q}`);
    setUnits(rows);
  }, []);

  const loadAnalytics = useCallback(async () => {
    const overview = await api<AdminAnalytics>("/v1/admin/analytics?days=30");
    setAnalytics(overview);
  }, []);

  const openUnit = useCallback(async (slug: string) => {
    const detail = await api<AdminUnitDetail>(`/v1/admin/units/${slug}`);
    const changes = await api<AdminChange[]>(`/v1/admin/units/${slug}/changelog`);
    setSelected(detail);
    setChangelog(changes);
  }, []);

  useEffect(() => {
    let live = true;
    api<{ ok: string }>("/v1/admin/ping")
      .then(async () => {
        if (!live) return;
        setAccess("granted");
        await Promise.all([loadList("all"), loadAnalytics()]);
      })
      .catch((err) => {
        if (!live) return;
        if (err instanceof ApiError && (err.status === 403 || err.status === 401)) {
          setAccess("denied");
          return;
        }
        setMessage(err instanceof Error ? err.message : "Could not reach the admin API");
        setAccess("error");
      });
    return () => {
      live = false;
    };
  }, [loadList, loadAnalytics]);

  const onFilter = async (next: string) => {
    setStatusFilter(next);
    setMessage("");
    try {
      await loadList(next);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Could not load units");
    }
  };

  const onPublish = async () => {
    if (!selected || busy) return;
    setBusy(true);
    setMessage("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch(`/v1/admin/units/${selected.slug}/publish`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({ reason: "Published from content studio" }),
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body?.error?.message ?? "Publish failed");
      }
      await openUnit(selected.slug);
      await loadList(statusFilter);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Publish failed");
    } finally {
      setBusy(false);
    }
  };

  const onRollback = async () => {
    if (!selected || busy) return;
    setBusy(true);
    setMessage("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch(`/v1/admin/units/${selected.slug}/rollback`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body?.error?.message ?? "Rollback failed");
      }
      await openUnit(selected.slug);
      await loadList(statusFilter);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Rollback failed");
    } finally {
      setBusy(false);
    }
  };

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    if (busy || !createSlug.trim() || !createTitle.trim()) return;
    setBusy(true);
    setMessage("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch("/v1/admin/units", {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          slug: createSlug.trim(),
          title: createTitle.trim(),
          difficulty: 2,
          objectives: ["Write the learning objective"],
          blocks: [
            { type: "why", markdown: "Why this unit matters." },
            { type: "beginner", markdown: "First explanation." },
          ],
        }),
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body?.error?.message ?? "Could not create draft");
      }
      setCreateSlug("");
      setCreateTitle("");
      await loadList(statusFilter);
      await openUnit(body.slug);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Could not create draft");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <SpaceHeader
        id="admin"
        action={
          access === "granted" ? (
            <span className="badge badge--ok">Access granted</span>
          ) : access === "denied" ? (
            <span className="badge badge--warn">403 Forbidden</span>
          ) : null
        }
      />

      {access === "checking" ? (
        <section className="card">
          <p className="muted">Checking your access…</p>
        </section>
      ) : null}

      {access === "error" ? (
        <section className="card">
          <Callout tone="danger">{message}</Callout>
        </section>
      ) : null}

      {access === "denied" ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">You do not have admin access</span>
            <span className="muted mono">403</span>
          </div>
          <p className="prose">
            The API refused this request. Content authoring and publish are restricted to the admin
            role on the server — hiding the link would not have been enough.
          </p>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--outline" href="/app/today">
              Back to Today
              <IconArrowRight />
            </Link>
          </div>
        </section>
      ) : null}

      {access === "granted" ? (
        <>
          {message ? <ErrorAlert message={message} /> : null}

          <Callout icon={<IconShield />} tone="neutral">
            Drafts listed here never appear in Learn until publish. Funnel and AI spend below are
            read from users, diagnostic_priors, mastery_records, and ai_usage.
          </Callout>

          {analytics ? (
            <section className="card">
              <div className="card__head">
                <span className="card__title">Operator analytics</span>
                <span className="badge badge--brand">Phase 14</span>
              </div>
              <dl className="facts">
                <div>
                  <dt>Registered</dt>
                  <dd>{analytics.funnel.registered}</dd>
                </div>
                <div>
                  <dt>Onboarded</dt>
                  <dd>{analytics.funnel.onboarded}</dd>
                </div>
                <div>
                  <dt>Diagnostic done</dt>
                  <dd>{analytics.funnel.diagnosticCompleted}</dd>
                </div>
                <div>
                  <dt>First PRACTICED+</dt>
                  <dd>{analytics.funnel.firstPracticed}</dd>
                </div>
                <div>
                  <dt>AI spend ({analytics.cost.windowDays}d)</dt>
                  <dd>${Number(analytics.cost.costUsd).toFixed(4)}</dd>
                </div>
                <div>
                  <dt>AI calls</dt>
                  <dd>{analytics.cost.calls}</dd>
                </div>
              </dl>
              <p className="hint" style={{ marginTop: 12 }}>
                {analytics.funnel.note}
              </p>
              <p className="hint">{analytics.cost.note}</p>
              {analytics.cost.byFeature.length > 0 ? (
                <ul className="studio__list" style={{ marginTop: 12 }}>
                  {analytics.cost.byFeature.map((f) => (
                    <li key={f.feature} className="hint">
                      <strong>{f.feature}</strong>
                      <span className="muted mono">
                        {" "}
                        ${Number(f.costUsd).toFixed(4)} · {f.tokensIn}+{f.tokensOut} tok · {f.calls}{" "}
                        calls
                      </span>
                    </li>
                  ))}
                </ul>
              ) : null}
            </section>
          ) : null}

          <section className="card">
            <div className="card__head">
              <span className="card__title">New draft</span>
              <span className="badge badge--brand">Studio</span>
            </div>
            <form className="stack" onSubmit={onCreate} style={{ gap: 12 }}>
              <label className="field">
                <span className="field__label">Slug</span>
                <input
                  className="input"
                  value={createSlug}
                  onChange={(e) => setCreateSlug(e.target.value)}
                  placeholder="my-new-unit"
                  required
                />
              </label>
              <label className="field">
                <span className="field__label">Title</span>
                <input
                  className="input"
                  value={createTitle}
                  onChange={(e) => setCreateTitle(e.target.value)}
                  placeholder="Unit title"
                  required
                />
              </label>
              <Button type="submit" disabled={busy}>
                Create draft
              </Button>
            </form>
          </section>

          <section className="card">
            <div className="card__head">
              <span className="card__title">Units</span>
              <select
                className="input"
                style={{ width: "auto" }}
                value={statusFilter}
                onChange={(e) => void onFilter(e.target.value)}
              >
                <option value="all">All statuses</option>
                <option value="draft">Draft</option>
                <option value="published">Published</option>
                <option value="deprecated">Deprecated</option>
              </select>
            </div>
            {units.length === 0 ? (
              <p className="hint">No units match this filter.</p>
            ) : (
              <ul className="studio__list" style={{ marginTop: 12 }}>
                {units.map((unit) => (
                  <li key={unit.id}>
                    <button
                      type="button"
                      className={`studio__pick${selected?.slug === unit.slug ? " is-active" : ""}`}
                      onClick={() => void openUnit(unit.slug).catch((err) =>
                        setMessage(err instanceof Error ? err.message : "Could not open unit"),
                      )}
                    >
                      <strong>{unit.title}</strong>
                      <span className="muted mono">
                        {unit.slug} · {unit.status} · v{unit.version} · {unit.blockCount} blocks
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {selected ? (
            <section className="card">
              <div className="card__head">
                <span className="card__title">{selected.title}</span>
                <span className="muted mono">
                  {selected.status} · v{selected.version}
                </span>
              </div>
              <p className="hint">
                {selected.blocks.length} blocks · critic{" "}
                {selected.critic.publishable ? "ready" : "blocked"}
              </p>
              <ul className="studio__list" style={{ marginTop: 12 }}>
                {selected.critic.findings.map((f) => (
                  <li key={f.code} className="hint">
                    <span className="mono">{f.severity}</span> — {f.message}
                  </li>
                ))}
              </ul>
              <div className="space__actions" style={{ marginTop: 16 }}>
                {selected.status !== "published" ? (
                  <Button onClick={() => void onPublish()} disabled={busy || !selected.critic.publishable}>
                    Publish
                  </Button>
                ) : (
                  <Button onClick={() => void onRollback()} disabled={busy}>
                    Rollback to draft
                  </Button>
                )}
                <Link className="btn btn--outline" href={`/app/learn/${selected.slug}`}>
                  Open in Learn
                  <IconArrowRight />
                </Link>
              </div>
              {changelog.length > 0 ? (
                <>
                  <h3 className="studio__msTitle">Changelog</h3>
                  <ul className="studio__list">
                    {changelog.map((c) => (
                      <li key={c.id} className="hint">
                        <span className="mono">{c.action}</span> {c.fromVersion ?? "—"} → {c.toVersion}{" "}
                        ({c.fromStatus ?? "—"} → {c.toStatus})
                        {c.reason ? ` — ${c.reason}` : ""}
                      </li>
                    ))}
                  </ul>
                </>
              ) : null}
            </section>
          ) : null}
        </>
      ) : null}
    </>
  );
}
