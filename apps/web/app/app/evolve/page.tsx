import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Evolve" };

export default function EvolvePage() {
  return (
    <SpacePlaceholder id="evolve">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Freshness</span>
          <span className="muted mono">No claims yet</span>
        </div>
        <p className="hint">
          When this feed runs, each item carries the date it was verified and the sources behind it.
          Until then there is no “latest in AI” to show, and inventing one would be worse than an
          empty page.
        </p>
      </section>
    </SpacePlaceholder>
  );
}
