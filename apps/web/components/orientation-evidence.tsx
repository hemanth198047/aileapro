"use client";

import { useEffect, useState } from "react";
import { MasteryChips } from "@aileapro/ui";
import { api, type Mastery } from "@/lib/api";

/**
 * The evidence summary in the orientation column. It reads the server rather than sharing
 * state with the unit page, so the two panes can never disagree about what was earned.
 */
export function OrientationEvidence({ slug }: { slug: string | null }) {
  const [mastery, setMastery] = useState<Mastery | null>(null);

  useEffect(() => {
    let cancelled = false;
    setMastery(null);
    if (!slug) return;
    api<Mastery>(`/v1/units/${slug}/mastery`)
      .then((loaded) => {
        if (!cancelled) setMastery(loaded);
      })
      .catch(() => {
        // A unit that is not published has no evidence to show; the page itself reports the 404.
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  return (
    <>
      <div className="orient__row">
        <span className="kicker">Evidence</span>
        <MasteryChips evidence={mastery?.evidence} />
      </div>
      <p className="hint">
        {slug
          ? mastery
            ? `On this unit you are at ${mastery.state}. Reading fills Read and stops at INTRODUCED; the rest is earned by work you produce.`
            : "Reading the evidence for this unit…"
          : "No unit is open, so there is nothing to report. Chips fill only from real evidence — reading never fills Assess."}
      </p>
    </>
  );
}
