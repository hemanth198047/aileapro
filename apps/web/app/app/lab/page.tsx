"use client";

import { useCallback, useEffect, useState } from "react";
import { Button, Callout, ErrorAlert } from "@aileapro/ui";
import { ApiError, api, csrfHeaders, type LabHistory, type LabRun, type LabStatus } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * The coding lab: write Python, run it in an isolated sandbox, read what came back.
 *
 * <p>The browser and the API never execute the source. When fixtures are serving, the banner
 * says so — a canned print presented as a real sandbox would be the worst failure this page
 * could have.
 */
export default function LabPage() {
  const [status, setStatus] = useState<LabStatus | null>(null);
  const [history, setHistory] = useState<LabHistory | null>(null);
  const [source, setSource] = useState("");
  const [current, setCurrent] = useState<LabRun | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const refresh = useCallback(async () => {
    const [next, hist] = await Promise.all([
      api<LabStatus>("/v1/lab/status"),
      api<LabHistory>("/v1/lab/runs"),
    ]);
    setStatus(next);
    setHistory(hist);
    if (!source) setSource(next.demoSource);
  }, [source]);

  useEffect(() => {
    let live = true;
    refresh()
      .catch((err) => live && setError(err instanceof Error ? err.message : "Could not load the lab"))
      .finally(() => undefined);
    return () => {
      live = false;
    };
  }, [refresh]);

  const run = async () => {
    if (busy || !source.trim()) return;
    setBusy(true);
    setError("");
    try {
      const headers = await csrfHeaders();
      const res = await fetch("/v1/lab/runs", {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({ language: "python", source }),
      });
      const body = await res.json();
      if (!res.ok) {
        throw new ApiError(res.status, body?.error?.message ?? "Run failed", body?.error?.code);
      }
      setCurrent(body as LabRun);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Run failed");
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    if (!current || current.finishedAt) return;
    try {
      const headers = await csrfHeaders();
      const res = await fetch(`/v1/lab/runs/${current.id}/cancel`, {
        method: "POST",
        credentials: "include",
        headers,
      });
      if (res.ok) setCurrent((await res.json()) as LabRun);
    } catch {
      // Ambient; the next poll/history refresh will show the truth.
    }
  };

  return (
    <>
      <SpaceHeader
        id="lab"
        action={
          status ? (
            <span className="badge">{status.remainingToday}/{status.dailyCap} today</span>
          ) : null
        }
      />

      {status?.development ? (
        <Callout tone="warn">
          <strong>Fixtures.</strong> No Docker sandbox is wired for this session, so output comes
          from development fixtures. They exercise the run path — timeout, quota, history — and
          they are not a real interpreter.
        </Callout>
      ) : null}

      {error ? <ErrorAlert message={error} /> : null}

      <section className="card lab__editor">
        <div className="card__head">
          <span className="card__title">Python</span>
          <span className="muted mono">{status?.executorKind ?? "…"}</span>
        </div>
        <textarea
          className="lab__source"
          spellCheck={false}
          value={source}
          onChange={(e) => setSource(e.target.value)}
          rows={14}
          aria-label="Python source"
        />
        <div className="lab__actions">
          <Button type="button" disabled={busy || !source.trim()} onClick={() => void run()}>
            {busy ? "Running…" : "Run"}
          </Button>
          <Button type="button" variant="ghost" disabled={!current || !!current.finishedAt} onClick={() => void cancel()}>
            Stop
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => status && setSource(status.demoSource)}
          >
            Load demo
          </Button>
        </div>
      </section>

      {current ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Output</span>
            <span className={`badge ${current.timedOut || current.status === "failed" ? "badge--warn" : "badge--ok"}`}>
              {current.status}
              {current.durationMs != null ? ` · ${current.durationMs}ms` : ""}
            </span>
          </div>
          {current.stdout ? <pre className="lab__out">{current.stdout}</pre> : null}
          {current.stderr ? <pre className="lab__err">{current.stderr}</pre> : null}
          {current.errorMessage ? <p className="hint">{current.errorMessage}</p> : null}
        </section>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">History</span>
          <span className="muted mono">{history ? history.runs.length : "…"}</span>
        </div>
        <p className="hint">{history?.note}</p>
        {history && history.runs.length > 0 ? (
          <ul className="lab__history">
            {history.runs.map((run) => (
              <li key={run.id}>
                <button type="button" className="lab__historyItem" onClick={() => { setCurrent(run); setSource(run.source); }}>
                  <span className="mono">{run.status}</span>
                  <span className="hint">{new Date(run.createdAt).toLocaleString()}</span>
                  <span className="hint truncate">{run.source.split("\n")[0]}</span>
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <div className="empty">
            <strong>No runs yet.</strong>
            <span>Load the demo and press Run. The API will not execute this code itself.</span>
          </div>
        )}
      </section>
    </>
  );
}
