import type { Metadata } from "next";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Lab" };

export default function LabPage() {
  return (
    <SpacePlaceholder id="lab">
      <section className="card">
        <div className="card__head">
          <span className="card__title">Sessions</span>
          <span className="muted mono">Python first</span>
        </div>
        <p className="hint">
          Runs happen in a disposable container with the network denied by default. Your last
          session will be one keystroke away — never buried behind a wizard.
        </p>
        <div className="empty" style={{ marginTop: 12 }}>
          <strong>No sessions.</strong>
          <span>The executor service is deployed but accepts no work until Phase 9.</span>
        </div>
      </section>
    </SpacePlaceholder>
  );
}
