"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Callout, IconArrowRight, IconShield } from "@aileapro/ui";
import { ApiError, api } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

type Access = "checking" | "granted" | "denied" | "error";

export default function AdminPage() {
  const [access, setAccess] = useState<Access>("checking");
  const [message, setMessage] = useState("");

  // The API is the authority on this, not the role we happen to hold client-side.
  useEffect(() => {
    api<{ ok: string }>("/v1/admin/ping")
      .then(() => setAccess("granted"))
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 403 || err.status === 401)) {
          setAccess("denied");
          return;
        }
        setMessage(err instanceof Error ? err.message : "Could not reach the admin API");
        setAccess("error");
      });
  }, []);

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
            The API refused this request. Content authoring, version workflow, and the update review
            queue are restricted to the admin role, and the check happens on the server — hiding the
            link would not have been enough.
          </p>
          <p className="hint" style={{ marginTop: 12 }}>
            If you should have access, an existing admin has to grant your account the role. There
            is no self-service escalation.
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
          <section className="card">
            <div className="card__head">
              <span className="card__title">Admin API reachable</span>
              <span className="badge badge--brand">Phase 13</span>
            </div>
            <Callout icon={<IconShield />}>
              <code className="mono">GET /v1/admin/ping</code> answered for your session. The
              editors themselves are not built yet.
            </Callout>
            <p className="hint" style={{ marginTop: 12 }}>
              Phase 13 brings unit and block editors, graph edge editing, the item bank, the
              generation critic, and the update queue. A draft will never be visible in Learn until
              it is explicitly published.
            </p>
          </section>

          <section className="empty">
            <strong>No content to administer.</strong>
            <span>The curriculum tables are empty until the Phase 3 seed loader runs.</span>
          </section>
        </>
      ) : null}
    </>
  );
}
