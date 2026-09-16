"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api, csrfHeaders, type Me } from "@/lib/api";
import { CommandPalette } from "./palette";

export default function AppShell() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState("");
  const [theme, setTheme] = useState<"dark" | "light">("dark");

  useEffect(() => {
    api<Me>("/v1/me")
      .then(setMe)
      .catch(() => router.replace("/login"));
  }, [router]);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  async function logout() {
    try {
      await api("/v1/auth/logout", { method: "POST", headers: await csrfHeaders() });
    } finally {
      router.replace("/login");
    }
  }

  if (!me) {
    return (
      <div className="auth-wrap">
        <p className="muted">Loading studio…</p>
      </div>
    );
  }

  return (
    <div className="shell">
      <header className="topbar">
        <strong>AI Mastery</strong>
        <span className="muted">{me.email}</span>
        <span>
          <button type="button" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
            {theme === "dark" ? "Light" : "Dark"}
          </button>{" "}
          <button type="button" onClick={logout}>
            Sign out
          </button>
        </span>
      </header>
      <div className="panes">
        <aside className="pane">
          <p className="kicker">Where</p>
          <h2>Orientation</h2>
          <p className="muted">Path and unit will appear in Phase 3. You are in the studio shell.</p>
        </aside>
        <main className="pane">
          <p className="kicker">Today</p>
          <h2>Work surface</h2>
          <p>Curriculum, labs, and the tutor ship in later phases. Phase 1 gate: you are authenticated.</p>
          <p className="muted">Press Ctrl+K (or Cmd+K) for the command palette.</p>
          {error ? <p className="error">{error}</p> : null}
        </main>
        <aside className="pane">
          <p className="kicker">Companion</p>
          <h2>Tutor dock</h2>
          <p className="muted">Collapsed-ready. The teaching companion arrives in Phase 7.</p>
        </aside>
      </div>
      <CommandPalette
        onGoToday={() => setError("")}
        onToggleTheme={() => setTheme(theme === "dark" ? "light" : "dark")}
      />
    </div>
  );
}
