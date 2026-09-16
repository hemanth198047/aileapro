import Link from "next/link";
import { ReactNode } from "react";
import { IconArrowRight } from "@aileapro/ui";
import { getSpace, type SpaceId } from "@/lib/spaces";

export function SpaceHeader({
  id,
  action,
}: {
  id: SpaceId;
  action?: ReactNode;
}) {
  const space = getSpace(id);
  return (
    <header className="space__head">
      <div className="space__titles">
        <p className="kicker">{space.group}</p>
        <h1>{space.label}</h1>
        <p className="lede">{space.purpose}</p>
      </div>
      <div className="space__meta">
        {space.arrivesIn ? <span className="badge badge--warn">{space.arrivesIn}</span> : null}
        {action}
      </div>
    </header>
  );
}

/**
 * The standard view for a space whose content is not built yet. It states the
 * phase, the honest next action, and what the space is waiting on — never a
 * fabricated stat, streak, or mastery figure.
 */
export function SpacePlaceholder({ id, children }: { id: SpaceId; children?: ReactNode }) {
  const space = getSpace(id);
  return (
    <>
      <SpaceHeader id={id} />

      <section className="card">
        <div className="card__head">
          <span className="card__title">Next action</span>
          {space.arrivesIn ? <span className="muted mono">{space.arrivesIn}</span> : null}
        </div>
        <p className="prose">{space.nextAction}</p>
        {space.waitingOn ? (
          <p className="hint" style={{ marginTop: 12 }}>
            {space.waitingOn}
          </p>
        ) : null}
      </section>

      {children}

      <section className="empty">
        <strong>This space is routed and empty on purpose.</strong>
        <span>
          It will show your own work or nothing at all. Placeholder numbers would teach you the
          wrong thing about your progress.
        </span>
        <Link className="inline-link" href="/app/today">
          Back to Today
          <IconArrowRight />
        </Link>
      </section>
    </>
  );
}
