"use client";

import { Button, ThemeToggle, useTheme } from "@aileapro/ui";
import { useSession } from "@/components/session";
import { VerifyEmailForm } from "@/components/verify-email";
import { SpaceHeader } from "@/components/space-view";

export default function SettingsPage() {
  const { me, isAdmin, signOut } = useSession();
  const { theme, toggle } = useTheme();

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
          <span className="card__title">AI budget, notifications, privacy</span>
          <span className="badge badge--warn">Later phases</span>
        </div>
        <p className="hint">
          Model spend caps arrive with the tutor in Phase 7. Notification channels need email
          delivery. Data export and deletion are part of production hardening in Phase 16. None of
          these controls are shown as switches until they actually do something.
        </p>
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
