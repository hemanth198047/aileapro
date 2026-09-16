import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Career" };

export default function CareerPage() {
  return (
    <SpacePlaceholder id="career">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Role readiness</span>
          <span className="muted mono">Not calculable</span>
        </div>
        <p className="hint">
          Readiness is derived from assessed evidence against a target role. With no assessments on
          record there is no percentage to give you, so none is shown.
        </p>
      </section>
    </SpacePlaceholder>
  );
}
