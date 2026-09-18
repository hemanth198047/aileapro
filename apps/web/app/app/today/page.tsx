"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  Button,
  Callout,
  ErrorAlert,
  IconAlert,
  IconArrowRight,
  MasteryChips,
} from "@aileapro/ui";
import { api, type LearnerDashboard, type NextAction } from "@/lib/api";
import { useSession } from "@/components/session";
import { VerifyEmailForm } from "@/components/verify-email";
import { SpaceHeader } from "@/components/space-view";

/**
 * Today: one recommendation, due reviews, then evidence-backed dashboard numbers from tables.
 */
export default function TodayPage() {
  const { me } = useSession();
  const [verifying, setVerifying] = useState(false);
  const [dash, setDash] = useState<LearnerDashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api<LearnerDashboard>("/v1/me/dashboard")
      .then((loaded) => {
        if (!cancelled) setDash(loaded);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load dashboard");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <SpaceHeader
        id="today"
        action={
          me.emailVerified ? (
            <span className="badge badge--ok">Email verified</span>
          ) : (
            <span className="badge badge--warn">Email not verified</span>
          )
        }
      />

      {error ? <ErrorAlert message={error} /> : null}

      {me.emailVerified ? (
        <NextActionCard dash={dash} />
      ) : (
        <section className="card card--hero">
          <p className="kicker">Your next action</p>
          <h2>Verify your email address.</h2>
          <p className="lede">
            It is the one action on your account that is still open, and it takes under a minute.
          </p>
          {verifying ? (
            <VerifyEmailForm />
          ) : (
            <div className="space__actions">
              <Button variant="primary" onClick={() => setVerifying(true)}>
                Verify now
                <IconArrowRight />
              </Button>
            </div>
          )}
        </section>
      )}

      <div className="grid-2">
        <ReviewsCard dash={dash} />

        <section className="card">
          <div className="card__head">
            <span className="card__title">Alerts</span>
            <span className="muted mono">{me.emailVerified ? "0" : "1"}</span>
          </div>
          {me.emailVerified ? (
            <div className="empty">
              <strong>Nothing needs your attention.</strong>
              <span>Account, session, and roles are all in order.</span>
            </div>
          ) : (
            <Callout tone="warn" icon={<IconAlert />}>
              Your email is unverified. Password reset and notifications depend on it.
            </Callout>
          )}
        </section>
      </div>

      <section className="card">
        <div className="card__head">
          <span className="card__title">Your progress</span>
          <span className="muted mono">From tables</span>
        </div>
        {dash ? (
          <>
            <dl className="facts">
              <div>
                <dt>Time on task</dt>
                <dd>{formatSeconds(dash.timeOnTaskSeconds)}</dd>
              </div>
              <div>
                <dt>Velocity ({dash.velocityWindowDays}d)</dt>
                <dd>{dash.practicedOrHigherInWindow} to PRACTICED+</dd>
              </div>
              <div>
                <dt>Reviews due</dt>
                <dd>{dash.reviewsDue}</dd>
              </div>
            </dl>
            <p className="hint" style={{ marginTop: 12 }}>
              {dash.velocityNote}
            </p>
            <p className="hint">{dash.note}</p>
          </>
        ) : (
          <p className="hint">Loading evidence…</p>
        )}
      </section>

      {dash && dash.weak.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Weak list</span>
            <span className="muted mono">{dash.weak.length}</span>
          </div>
          <ul className="studio__list">
            {dash.weak.map((w) => (
              <li key={w.unitSlug}>
                <Link className="studio__pick" href={w.href}>
                  <strong>{w.unitTitle}</strong>
                  <span className="muted mono">
                    {w.state} · {w.reason}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {dash && dash.domains.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">By domain</span>
            <span className="muted mono">{dash.domains.length}</span>
          </div>
          <ul className="studio__list">
            {dash.domains.map((d) => (
              <li key={d.domainSlug} className="hint">
                <strong>{d.domainTitle}</strong>
                <span className="muted mono">
                  {" "}
                  {d.introducedOrHigher}/{d.trackedUnits} introduced · {d.practicedOrHigher} practiced+
                </span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {dash?.pathSlug ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">{dash.pathTitle}</span>
            <span className="muted mono">
              {dash.waypointsIntroduced} of {dash.waypointCount} introduced
            </span>
          </div>
          <p className="prose">
            {dash.waypointsStarted === 0
              ? "You have not opened a waypoint on this path yet."
              : `You have opened ${dash.waypointsStarted} of ${dash.waypointCount} waypoints, and ${dash.waypointsIntroduced} have enough reading behind them to count as introduced.`}
          </p>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--outline" href="/app/learn">
              See the whole path with your evidence
              <IconArrowRight />
            </Link>
          </div>
        </section>
      ) : null}
    </>
  );
}

function formatSeconds(total: number): string {
  if (total < 60) return `${total}s`;
  const m = Math.floor(total / 60);
  const s = total % 60;
  if (m < 60) return s === 0 ? `${m}m` : `${m}m ${s}s`;
  const h = Math.floor(m / 60);
  return `${h}h ${m % 60}m`;
}

function ReviewsCard({ dash }: { dash: LearnerDashboard | null }) {
  const due = dash?.reviewsDue ?? 0;

  return (
    <section className="card">
      <div className="card__head">
        <span className="card__title">Reviews due</span>
        <span className="muted mono">{due}</span>
      </div>
      {due === 0 ? (
        <div className="empty">
          <strong>Nothing is due.</strong>
          <span>{dash?.reviewsNote ?? "Checking your schedule…"}</span>
        </div>
      ) : (
        <>
          <p className="prose">{dash?.reviewsNote}</p>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--primary" href="/app/review">
              Open review queue
              <IconArrowRight />
            </Link>
          </div>
        </>
      )}
    </section>
  );
}

function NextActionCard({ dash }: { dash: LearnerDashboard | null }) {
  if (!dash) {
    return (
      <section className="card card--hero">
        <p className="kicker">Your next action</p>
        <p className="muted">Working out what to recommend…</p>
      </section>
    );
  }

  const next: NextAction = dash.next;
  const actionable = next.kind !== "blocked";

  return (
    <section className="card card--hero">
      <p className="kicker">Your next action</p>
      <h2>
        {next.verb} {next.object}
        {next.minutes ? <span className="hero__minutes"> · {next.minutes} min</span> : null}
      </h2>
      <p className="lede">{next.reason}</p>

      {next.mastery ? (
        <div className="hero__evidence">
          <span className="kicker">Your evidence on it</span>
          <MasteryChips evidence={next.mastery.evidence} />
        </div>
      ) : null}

      <div className="space__actions">
        {actionable ? (
          <Link className="btn btn--primary" href={next.href}>
            {next.verb} {next.unit ? next.unit.title : next.object}
            <IconArrowRight />
          </Link>
        ) : (
          <Link className="btn btn--outline" href={next.href}>
            See where that stands
            <IconArrowRight />
          </Link>
        )}
        {dash.onboarded ? (
          <Link className="btn btn--outline" href="/app/onboarding">
            Change your goal
          </Link>
        ) : null}
      </div>

      <p className="hint" style={{ marginTop: 16 }}>
        Chosen by rule <code className="mono">{next.rule}</code>.
      </p>
    </section>
  );
}
