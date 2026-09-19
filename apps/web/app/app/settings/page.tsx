"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { Button, ErrorAlert, ThemeToggle, useTheme } from "@aileapro/ui";
import { api, csrfHeaders } from "@/lib/api";
import { useSession } from "@/components/session";
import { VerifyEmailForm } from "@/components/verify-email";
import { SpaceHeader } from "@/components/space-view";

export default function SettingsPage() {
  const { me, isAdmin, signOut } = useSession();
  const { theme, toggle } = useTheme();
  const [exportBusy, setExportBusy] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [privacyMsg, setPrivacyMsg] = useState("");
  const [privacyErr, setPrivacyErr] = useState("");

  async function downloadExport() {
    setPrivacyErr("");
    setPrivacyMsg("");
    setExportBusy(true);
    try {
      const data = await api<Record<string, unknown>>("/v1/me/export");
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `aileapro-export-${me.id}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setPrivacyMsg("Export downloaded.");
    } catch (err) {
      setPrivacyErr(err instanceof Error ? err.message : "Export failed");
    } finally {
      setExportBusy(false);
    }
  }

  async function deleteAccount(e: FormEvent) {
    e.preventDefault();
    setPrivacyErr("");
    setPrivacyMsg("");
    if (!window.confirm("Delete and anonymize this account? You will not be able to sign in again.")) {
      return;
    }
    setDeleteBusy(true);
    try {
      await api("/v1/me", {
        method: "DELETE",
        headers: await csrfHeaders(),
      });
      setPrivacyMsg("Account anonymized. Signing out…");
      await signOut();
    } catch (err) {
      setPrivacyErr(err instanceof Error ? err.message : "Delete failed");
      setDeleteBusy(false);
    }
  }

  return (
    <>
      <SpaceHeader id="settings" />

      <section className="card">
        <div className="card__head">
          <span className="card__title">Account</span>
          {me.emailVerified ? (
            <span className="badge badge--ok">Verified</span>
          ) : (
            <span className="badge badge--warn">Unverified</span>
          )}
        </div>
        <dl className="facts">
          <div>
            <dt>Email</dt>
            <dd>{me.email}</dd>
          </div>
          <div>
            <dt>Roles</dt>
            <dd>{me.roles.join(", ") || "learner"}</dd>
          </div>
          <div>
            <dt>Admin access</dt>
            <dd>{isAdmin ? "Granted" : "Not granted"}</dd>
          </div>
          <div>
            <dt>User id</dt>
            <dd className="mono">{me.id}</dd>
          </div>
        </dl>
        {me.emailVerified ? null : (
          <div style={{ marginTop: 16 }}>
            <VerifyEmailForm />
          </div>
        )}
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Appearance</span>
          <span className="muted mono">{theme}</span>
        </div>
        <p className="hint">
          Your choice is stored on this device and applied before the first paint, so the page never
          flashes the wrong theme.
        </p>
        <div className="space__actions" style={{ marginTop: 12 }}>
          <ThemeToggle theme={theme} onToggle={toggle} />
          <Button onClick={toggle}>Switch to {theme === "dark" ? "light" : "dark"}</Button>
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Privacy</span>
        </div>
        <p className="hint">
          Download a copy of your account and learning data, or anonymize the account (email
          replaced, free text scrubbed, login blocked). See the{" "}
          <Link href="/privacy">privacy policy</Link> and <Link href="/terms">terms</Link>.
        </p>
        {privacyMsg ? <p className="hint" style={{ marginTop: 8 }}>{privacyMsg}</p> : null}
        <ErrorAlert message={privacyErr} />
        <div className="space__actions" style={{ marginTop: 12 }}>
          <Button onClick={downloadExport} loading={exportBusy} disabled={exportBusy || deleteBusy}>
            Download my data
          </Button>
          <form onSubmit={deleteAccount}>
            <Button type="submit" loading={deleteBusy} disabled={exportBusy || deleteBusy}>
              Delete my account
            </Button>
          </form>
        </div>
      </section>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Session</span>
        </div>
        <p className="hint">
          Signing out clears your server-side session immediately; it is not only a cookie removal.
        </p>
        <div className="space__actions" style={{ marginTop: 12 }}>
          <Button onClick={signOut}>Sign out</Button>
        </div>
      </section>
    </>
  );
}
