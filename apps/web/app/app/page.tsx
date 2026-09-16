"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError, api, csrfHeaders, type Me } from "@/lib/api";
import { Brand } from "@/components/brand";
import {
  IconArrowRight,
  IconChart,
  IconCompass,
  IconLab,
  IconLogout,
  IconPath,
  IconSearch,
  IconSpark,
} from "@/components/icons";
import { ThemeToggle, useTheme } from "@/components/theme";
import { Button, ErrorAlert } from "@/components/ui";
import { CommandPalette } from "./palette";

const navigation = [
  { id: "today", label: "Today", icon: <IconCompass />, ready: true },
  { id: "path", label: "Path", icon: <IconPath />, phase: "Phase 3" },
  { id: "lab", label: "Lab", icon: <IconLab />, phase: "Phase 5" },
  { id: "tutor", label: "Tutor", icon: <IconSpark />, phase: "Phase 7" },
  { id: "mastery", label: "Mastery", icon: <IconChart />, phase: "Phase 4" },
];

const milestones = [
  { label: "Account and session", state: "done", note: "Done" },
  { label: "Placement diagnostic", state: "active", note: "Phase 2" },
  { label: "Your first path", state: "todo", note: "Phase 3" },
  { label: "Lab environment", state: "todo", note: "Phase 5" },
];

export default function AppShell() {
  const router = useRouter();
  const { theme, toggle } = useTheme();
  const [me, setMe] = useState<Me | null>(null);
  const [loadError, setLoadError] = useState("");
  const [paletteOpen, setPaletteOpen] = useState(false);

  useEffect(() => {
    api<Me>("/v1/me")
      .then(setMe)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login");
          return;
        }
        setLoadError(err instanceof Error ? err.message : "Could not load your session");
      });
  }, [router]);

  const logout = useCallback(async () => {
    try {
      await api("/v1/auth/logout", { method: "POST", headers: await csrfHeaders() });
    } finally {
      router.replace("/login");
    }
  }, [router]);

  if (!me) {
    return (
      <div className="splash">
        <Brand large />
        {loadError ? (
          <>
            <ErrorAlert message={loadError} />
            <Button onClick={() => location.reload()}>Try again</Button>
          </>
        ) : (
          <p className="muted">Preparing your studio…</p>
        )}
      </div>
    );
  }

  const role = me.roles[0] ?? "learner";

  return (
    <div className="shell">
      <header className="topbar">
        <Brand />
        <span className="topbar__spacer" />
        <button type="button" className="searchbtn" onClick={() => setPaletteOpen(true)}>
          <IconSearch />
          <span className="searchbtn__label">Search or jump to…</span>
          <span className="searchbtn__spacer" />
          <span className="kbd">Ctrl K</span>
        </button>
        <div className="topbar__actions">
          <ThemeToggle theme={theme} onToggle={toggle} />
          <Button variant="ghost" onClick={logout} title="Sign out">
            <IconLogout />
            <span className="searchbtn__label">Sign out</span>
          </Button>
          <div className="user">
            <span className="avatar" aria-hidden>
              {me.email.slice(0, 2).toUpperCase()}
            </span>
            <span className="user__meta">
              <span className="user__email">{me.email}</span>
              <span className="user__role">{role}</span>
            </span>
          </div>
        </div>
      </header>

      <div className="panes">
        <nav className="pane pane--nav" aria-label="Sections">
          <div className="navgroup">
            <p className="kicker navgroup__label">Studio</p>
            {navigation.map((item) => (
              <button
                key={item.id}
                type="button"
                className="navitem"
                aria-current={item.ready ? "page" : undefined}
                disabled={!item.ready}
                title={item.ready ? undefined : `Ships in ${item.phase}`}
              >
                {item.icon}
                {item.label}
                {item.phase ? <span className="badge navitem__tail">{item.phase}</span> : null}
              </button>
            ))}
          </div>
          <div className="navgroup">
            <p className="kicker navgroup__label">Account</p>
            <div className="dockmsg">
              <strong>{role}</strong>
              <br />
              {me.emailVerified ? (
                <span className="muted">Email verified</span>
              ) : (
                <span className="muted">
                  Email not verified yet — the dev token is printed in the API log.
                </span>
              )}
            </div>
          </div>
        </nav>

        <main className="pane pane--main">
          <section className="card card--hero">
            <div className="card__head" style={{ marginBottom: 0 }}>
              <p className="kicker">Today</p>
              {me.emailVerified ? (
                <span className="badge badge--ok">Verified</span>
              ) : (
                <span className="badge badge--warn">Verify email</span>
              )}
            </div>
            <h1>Welcome to AILEPRO.</h1>
            <p className="lede">
              Your account is live and your session is secure. The placement diagnostic opens next,
              and it decides where your path begins — no generic beginner track.
            </p>
            <div>
              <Button variant="primary" disabled title="Opens in Phase 2">
                Start placement
                <IconArrowRight />
              </Button>
            </div>
          </section>

          <section className="stats">
            <div className="stat">
              <span className="stat__value">0</span>
              <span className="stat__label">Skills assessed</span>
            </div>
            <div className="stat">
              <span className="stat__value">0</span>
              <span className="stat__label">Lab runs</span>
            </div>
            <div className="stat">
              <span className="stat__value">—</span>
              <span className="stat__label">Current unit</span>
            </div>
          </section>

          <section className="card">
            <div className="card__head">
              <span className="card__title">Getting to your first path</span>
              <span className="muted mono">1 of 4</span>
            </div>
            <div className="progress" role="progressbar" aria-valuenow={25} aria-valuemin={0} aria-valuemax={100}>
              <span className="progress__fill" style={{ width: "25%" }} />
            </div>
            <ul className="timeline" style={{ marginTop: 14 }}>
              {milestones.map((m) => (
                <li className="timeline__item" key={m.label}>
                  <span className="timeline__dot" data-state={m.state} />
                  <span>{m.label}</span>
                  <span className="muted mono">{m.note}</span>
                </li>
              ))}
            </ul>
          </section>

          <section className="empty">
            <strong>Curriculum, labs, and the tutor arrive in later phases.</strong>
            <span>
              Press <span className="kbd">Ctrl K</span> to open the command palette.
            </span>
          </section>
        </main>

        <aside className="pane pane--dock" aria-label="Tutor">
          <div className="card__head">
            <span className="card__title">Tutor dock</span>
            <span className="badge badge--brand">Phase 7</span>
          </div>
          <div className="dockmsg">
            The teaching companion will sit here: it sees the unit you are on, the code in your
            lab, and the mistakes you have made before.
          </div>
          <div className="dockmsg">
            It explains, questions, and refuses to hand over answers you are close to reaching
            yourself.
          </div>
          <div className="empty" style={{ padding: 16 }}>
            Conversation starts once the curriculum ships.
          </div>
        </aside>
      </div>

      <CommandPalette
        open={paletteOpen}
        onOpenChange={setPaletteOpen}
        onToggleTheme={toggle}
        onSignOut={logout}
      />
    </div>
  );
}
