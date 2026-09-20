"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { api, csrfHeaders, type LearningEventKind, type Mastery } from "@/lib/api";

/**
 * Reports what the learner actually did on a unit page, and holds the server's answer about
 * what it was worth.
 *
 * <p>Three rules keep this honest rather than a time-on-page inflator. Heartbeats stop when
 * the tab is hidden, so a unit left open in a background tab earns nothing. The interval is
 * fixed and the server clamps each beat anyway, so a tampered client buys at most one beat.
 * And the returned mastery is always the server's, never a local guess — the UI cannot show
 * evidence the API did not grant.
 */

const HEARTBEAT_SECONDS = 30;

type Reporter = {
  mastery: Mastery | null;
  report: (kind: LearningEventKind, payload?: { seconds?: number; level?: string }) => void;
};

const UnitProgressContext = createContext<Reporter>({ mastery: null, report: () => {} });

/** Read by anything inside the unit page that observes the learner, like the level switcher. */
export function useUnitProgress() {
  return useContext(UnitProgressContext);
}

export function UnitProgressProvider({
  slug,
  children,
}: {
  slug: string;
  children: React.ReactNode;
}) {
  const [mastery, setMastery] = useState<Mastery | null>(null);
  // Kept in a ref so the heartbeat effect does not restart on every update.
  const slugRef = useRef(slug);
  slugRef.current = slug;

  const send = useCallback(
    async (kind: LearningEventKind, payload?: { seconds?: number; level?: string }) => {
      try {
        const next = await api<Mastery>(`/v1/units/${slugRef.current}/events`, {
          method: "POST",
          headers: await csrfHeaders(),
          body: JSON.stringify({ kind, seconds: payload?.seconds, level: payload?.level }),
        });
        setMastery(next);
      } catch {
        // Losing an event is not worth interrupting the reading. The next beat re-syncs, and
        // the record is a projection of events, so a dropped one costs seconds of dwell.
      }
    },
    [],
  );

  const report = useCallback(
    (kind: LearningEventKind, payload?: { seconds?: number; level?: string }) => {
      void send(kind, payload);
    },
    [send],
  );

  // One open event per unit, and the current standing comes back with it.
  useEffect(() => {
    setMastery(null);
    void send("unit_opened");
  }, [slug, send]);

  // Dwell only accumulates while the page is actually visible.
  useEffect(() => {
    let timer: ReturnType<typeof setInterval> | null = null;

    const start = () => {
      if (timer !== null) return;
      timer = setInterval(() => {
        void send("dwell", { seconds: HEARTBEAT_SECONDS });
      }, HEARTBEAT_SECONDS * 1000);
    };
    const stop = () => {
      if (timer === null) return;
      clearInterval(timer);
      timer = null;
    };

    const onVisibility = () => (document.visibilityState === "visible" ? start() : stop());
    onVisibility();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      stop();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [slug, send]);

  return (
    <UnitProgressContext.Provider value={{ mastery, report }}>
      {children}
    </UnitProgressContext.Provider>
  );
}

/** How much visible reading is still outstanding before Read can go solid. */
export function readingProgress(mastery: Mastery | null) {
  if (!mastery || mastery.readFloorSeconds <= 0) {
    return { percent: 0, remainingMinutes: 0 };
  }
  const percent = Math.min(100, Math.round((mastery.dwellSeconds / mastery.readFloorSeconds) * 100));
  const remaining = Math.max(0, mastery.readFloorSeconds - mastery.dwellSeconds);
  return { percent, remainingMinutes: Math.ceil(remaining / 60) };
}
