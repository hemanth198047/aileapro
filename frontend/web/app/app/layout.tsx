"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Brand,
  Button,
  type Command,
  CommandPalette,
  ErrorAlert,
  IconArrowRight,
  IconClose,
  IconLogout,
  IconPanel,
  IconSearch,
  IconSpark,
  Tabs,
  ThemeToggle,
  useTheme,
} from "@aileapro/ui";
import { ApiError, api, csrfHeaders, type Me } from "@/lib/api";
import { NotesPanel } from "@/components/notes-panel";
import { OrientationEvidence } from "@/components/orientation-evidence";
import { TutorDock } from "@/components/tutor-dock";
import { SessionProvider } from "@/components/session";
import { SPACES, SPACE_GROUPS, spaceForPath } from "@/lib/spaces";

export default function StudioLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { theme, toggle } = useTheme();
  const [me, setMe] = useState<Me | null>(null);
  const [loadError, setLoadError] = useState("");
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [navOpen, setNavOpen] = useState(false);
  const [companionOpen, setCompanionOpen] = useState(false);
  const [companionTab, setCompanionTab] = useState("tutor");

  const loadMe = useCallback(async () => {
    try {
      setMe(await api<Me>("/v1/me"));
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/login");
        return;
      }
      setLoadError(err instanceof Error ? err.message : "Could not load your session");
    }
  }, [router]);

  useEffect(() => {
    void loadMe();
  }, [loadMe]);

  // Route changes close the mobile overlays, otherwise they cover the new page.
  useEffect(() => {
    setNavOpen(false);
    setCompanionOpen(false);
  }, [pathname]);

  const signOut = useCallback(async () => {
    try {
      await api("/v1/auth/logout", { method: "POST", headers: await csrfHeaders() });
    } finally {
      router.replace("/login");
    }
  }, [router]);

  const isAdmin = me?.roles.includes("admin") ?? false;
  const space = spaceForPath(pathname);
  // The companion needs to know what is being read to attach notes to it. The route is the
  // only thing that knows, so it is read from there rather than threaded through the tree.
  const unitSlug = pathname.startsWith("/app/learn/") ? pathname.slice("/app/learn/".length) : null;

  const visibleSpaces = useMemo(
    () => SPACES.filter((s) => !s.adminOnly || isAdmin),
    [isAdmin],
  );

  const commands = useMemo<Command[]>(() => {
    const navigation: Command[] = visibleSpaces.map((s) => ({
      id: `go-${s.id}`,
      label: `Go to ${s.label}`,
      group: "Navigate",
      icon: s.icon,
      keywords: s.purpose,
      hint: s.arrivesIn,
      run: () => router.push(s.href),
    }));
    return [
      ...navigation,
      {
        id: "ask-tutor",
        label: "Ask the tutor",
        group: "Intents",
        icon: <IconSpark size={15} />,
        hint: "Phase 7",
      },
      {
        id: "what-next",
        label: "What should I learn today?",
        group: "Intents",
        icon: <IconArrowRight />,
        hint: "Today",
        run: () => router.push("/app/today"),
      },
      {
        id: "theme",
        label: `Switch to ${theme === "dark" ? "light" : "dark"} theme`,
        group: "Preferences",
        icon: <IconPanel size={15} />,
        run: toggle,
      },
      {
        id: "signout",
        label: "Sign out",
        group: "Account",
        icon: <IconLogout size={15} />,
        run: signOut,
      },
    ];
  }, [visibleSpaces, router, theme, toggle, signOut]);

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

  const role = isAdmin ? "admin" : (me.roles[0] ?? "learner");

  return (
    <SessionProvider value={{ me, isAdmin, signOut, refresh: loadMe }}>
      <a className="skip-link" href="#work">
        Skip to main content
      </a>

      <div className="shell">
        <header className="topbar">
          <Button
            variant="ghost"
            className="topbar__menu"
            onClick={() => setNavOpen(true)}
            aria-expanded={navOpen}
            aria-controls="orientation"
          >
            <IconPanel />
            <span className="sr-only">Open navigation</span>
          </Button>

          <Link href="/app/today" className="brand-link">
            <Brand />
          </Link>

          <span className="topbar__spacer" />

          <button type="button" className="searchbtn" onClick={() => setPaletteOpen(true)}>
            <IconSearch />
            <span className="searchbtn__label">Search or jump to…</span>
            <span className="searchbtn__spacer" />
            <span className="kbd">Ctrl K</span>
          </button>

          <div className="topbar__actions">
            <ThemeToggle theme={theme} onToggle={toggle} />
            <Button
              variant="ghost"
              className="topbar__companion"
              onClick={() => setCompanionOpen((v) => !v)}
              aria-expanded={companionOpen}
              aria-controls="companion"
            >
              <IconSpark />
              <span className="sr-only">Toggle companion</span>
            </Button>
            <Button variant="ghost" onClick={signOut}>
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
          <div
            className="pane-scrim"
            data-open={navOpen || companionOpen}
            onClick={() => {
              setNavOpen(false);
              setCompanionOpen(false);
            }}
            aria-hidden
          />

          {/* Orientation: where you are, where you can go, and what state it is in. */}
          <div className="pane pane--orientation" id="orientation" data-open={navOpen}>
            <div className="pane__close">
              <Button variant="ghost" onClick={() => setNavOpen(false)}>
                <IconClose />
                <span className="sr-only">Close navigation</span>
              </Button>
            </div>

            <nav className="crumbs" aria-label="Breadcrumb">
              <ol>
                <li>
                  <Link href="/app/today">Studio</Link>
                </li>
                {space ? (
                  <>
                    <li aria-hidden>/</li>
                    <li>{space.group}</li>
                    <li aria-hidden>/</li>
                    <li aria-current="page">{space.label}</li>
                  </>
                ) : null}
              </ol>
            </nav>

            {SPACE_GROUPS.map((group) => {
              const items = visibleSpaces.filter((s) => s.group === group);
              if (items.length === 0) return null;
              return (
                <div className="navgroup" key={group}>
                  <p className="kicker navgroup__label">{group}</p>
                  {items.map((item) => (
                    <Link
                      key={item.id}
                      href={item.href}
                      className="navitem"
                      aria-current={space?.id === item.id ? "page" : undefined}
                      title={item.arrivesIn ? `Real content arrives in ${item.arrivesIn}` : undefined}
                    >
                      {item.icon}
                      {item.label}
                      {item.arrivesIn ? (
                        <span className="badge navitem__tail">{item.arrivesIn}</span>
                      ) : null}
                    </Link>
                  ))}
                </div>
              );
            })}

            <div className="navgroup">
              <p className="kicker navgroup__label">Where you are</p>
              <p className="orient__why">{space?.purpose ?? "Pick a space to begin."}</p>
              <OrientationEvidence slug={unitSlug} />
              <div className="orient__row">
                <span className="kicker">Prerequisites</span>
                <span className="badge">Authored, read-checked</span>
              </div>
              <p className="hint">
                The graph knows what each unit needs, and the unit page shows that chain against
                your evidence on it. A prerequisite you have only read is marked as read, not as
                understood — that distinction is the whole point of tracking both.
              </p>
            </div>
          </div>

          <main className="pane pane--work" id="work" tabIndex={-1}>
            {children}
          </main>

          {/* Companion: tutor and notes, docked next to the work — never a separate page. */}
          <aside
            className="pane pane--companion"
            id="companion"
            data-open={companionOpen}
            aria-label="Companion"
          >
            <div className="pane__close">
              <Button variant="ghost" onClick={() => setCompanionOpen(false)}>
                <IconClose />
                <span className="sr-only">Close companion</span>
              </Button>
            </div>

            <Tabs
              label="Companion"
              value={companionTab}
              onChange={setCompanionTab}
              items={[
                { id: "tutor", label: "Tutor" },
                { id: "notes", label: "Notes" },
              ]}
            />

            {companionTab === "tutor" ? (
              <TutorDock slug={unitSlug} />
            ) : (
              <NotesPanel slug={unitSlug} />
            )}
          </aside>
        </div>
      </div>

      <CommandPalette
        open={paletteOpen}
        onOpenChange={setPaletteOpen}
        commands={commands}
        placeholder="Jump to a space, ask, or run a command…"
      />
    </SessionProvider>
  );
}
