import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Studio" };

export default function StudioPage() {
  return (
    <SpacePlaceholder id="studio">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Portfolio</span>
          <span className="muted mono">Private by default</span>
        </div>
        <p className="hint">
          Artifacts you produce here are yours; each one can be made public individually. Nothing is
          shared without you choosing it.
        </p>
        <div className="empty" style={{ marginTop: 12 }}>
          <strong>No artifacts.</strong>
          <span>Finishing a project is what puts something here.</span>
        </div>
      </section>
    </SpacePlaceholder>
  );
}
