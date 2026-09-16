import type { Metadata } from "next";
import { SplitPane } from "@aileapro/ui";
import { SpacePlaceholder } from "@/components/space-view";

export const metadata: Metadata = { title: "Learn" };

export default function LearnPage() {
  return (
    <SpacePlaceholder id="learn">
      {/* The split is the shape Learn will keep: explanation left, practice right. */}
      <SplitPane
        start={
          <section className="card">
            <div className="card__head">
              <span className="card__title">Explanation</span>
              <span className="muted mono">Levels 1–6</span>
            </div>
            <div className="empty">
              <strong>No unit open.</strong>
              <span>Authored units, blocks, and paths land in Phase 3.</span>
            </div>
          </section>
        }
        end={
          <section className="card">
            <div className="card__head">
              <span className="card__title">Practice dock</span>
              <span className="muted mono">Phase 5</span>
            </div>
            <div className="empty">
              <strong>No exercises.</strong>
              <span>Practice attaches to a unit, so it follows the curriculum.</span>
            </div>
          </section>
        }
      />
    </SpacePlaceholder>
  );
}
