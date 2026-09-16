import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Map" };

export default function MapPage() {
  return (
    <SpacePlaceholder id="map">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Nodes</span>
          <span className="muted mono">List fallback</span>
        </div>
        <p className="hint">
          The graph canvas always has this list beside it, so the map is usable with a keyboard and
          a screen reader.
        </p>
        <div className="empty" style={{ marginTop: 12 }}>
          <strong>No nodes.</strong>
          <span>Units become nodes in Phase 3; prerequisite edges follow in Phase 4.</span>
        </div>
      </section>
    </SpacePlaceholder>
  );
}
