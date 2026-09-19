"use client";

import { useEffect, useState } from "react";
import { Callout, ErrorAlert } from "@aileapro/ui";
import { api, type EvolveFeed } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

/**
 * Evolve: published updates with verification timestamps — never "fully up to date."
 */
export default function EvolvePage() {
  const [feed, setFeed] = useState<EvolveFeed | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let live = true;
    api<EvolveFeed>("/v1/evolve/feed")
      .then((next) => live && setFeed(next))
      .catch((err) => live && setError(err instanceof Error ? err.message : "Could not load Evolve"));
    return () => {
      live = false;
    };
  }, []);

  return (
    <>
      <SpaceHeader id="evolve" />

      {error ? <ErrorAlert message={error} /> : null}

      <Callout tone="neutral">
        <strong>Honest currentness.</strong>{" "}
        {feed?.honesty ??
          "Each item states how many sources it was verified against and when — never that the field is fully up to date."}
      </Callout>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Update feed</span>
          <span className="muted mono">{feed ? feed.items.length : "…"}</span>
        </div>
        {!feed ? (
          <p className="hint">Loading…</p>
        ) : feed.items.length === 0 ? (
          <div className="empty">
            <strong>No published updates yet.</strong>
            <span>An empty feed is honest. Inventing “latest in AI” would not be.</span>
          </div>
        ) : (
          <ul className="studio__list" style={{ marginTop: 12 }}>
            {feed.items.map((item) => (
              <li key={item.slug}>
                <div className="studio__pick" style={{ cursor: "default" }}>
                  <strong>{item.title}</strong>
                  <span className="muted mono">
                    {item.lifecycle} · {formatWhen(item.occurredAt)}
                    {item.verifiedAt ? ` · verified ${formatWhen(item.verifiedAt)}` : ""}
                  </span>
                </div>
                <p className="prose" style={{ marginTop: 8 }}>
                  {item.summary}
                </p>
                <p className="hint">{item.currentnessNote}</p>
                {item.sourceSlugs.length > 0 ? (
                  <p className="hint mono">Sources: {item.sourceSlugs.join(", ")}</p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function formatWhen(iso: string): string {
  try {
    return new Date(iso).toISOString().slice(0, 10);
  } catch {
    return iso;
  }
}
