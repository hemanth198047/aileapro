import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Assess" };

export default function AssessPage() {
  return (
    <SpacePlaceholder id="assess">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Due reviews</span>
          <span className="muted mono">0 scheduled</span>
        </div>
        <p className="hint">
          Spaced review schedules itself from real attempts. Nothing is due because nothing has been
          assessed.
        </p>
      </section>
    </SpacePlaceholder>
  );
}
