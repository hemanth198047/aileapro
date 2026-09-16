import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Research" };

export default function ResearchPage() {
  return (
    <SpacePlaceholder id="research">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Reading list</span>
          <span className="muted mono">Citations enforced</span>
        </div>
        <p className="hint">
          Every explainer here must cite the paper it came from. An explainer that cannot cite is
          not shown.
        </p>
        <div className="empty" style={{ marginTop: 12 }}>
          <strong>No papers.</strong>
          <span>Paper metadata is seeded in Phase 11.</span>
        </div>
      </section>
    </SpacePlaceholder>
  );
}
