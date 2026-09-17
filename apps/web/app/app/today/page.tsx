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
import { api, type NextAction, type Today } from "@/lib/api";
import { useSession } from "@/components/session";
import { VerifyEmailForm } from "@/components/verify-email";
import { SpaceHeader } from "@/components/space-view";

/** Phases already delivered, so the learner can see where the product actually is. */
const delivered = [
  { label: "Account, session, and roles", state: "done", note: "Phase 1" },
  { label: "Studio shell and every space routed", state: "done", note: "Phase 2" },
  { label: "Authored curriculum, paths, and sources", state: "done", note: "Phase 3" },
  { label: "Knowledge map with typed prerequisites", state: "done", note: "Phase 4" },
  { label: "Recorded evidence and a real recommendation", state: "active", note: "Phase 5" },
  { label: "Graded assessment, reports, and spaced review", state: "todo", note: "Phase 6" },
];

/**
 * Today is one recommendation with its reasoning, then what is due, then what needs
 * attention. The recommendation comes from the API so the sentence the learner reads is the
 * same one the policy actually applied.
 */
export default function TodayPage() {
  const { me } = useSession();
  const [verifying, setVerifying] = useState(false);
  const [today, setToday] = useState<Today | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api<Today>("/v1/me/today")
      .then((loaded) => {
        if (!cancelled) setToday(loaded);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load Today");
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

      {/* Verification outranks the recommendation: it is the one thing blocking the account. */}
      {me.emailVerified ? (
        <NextActionCard today={today} />
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
        <section className="card">
          <div className="card__head">
            <span className="card__title">Reviews due</span>
            <span className="muted mono">{today?.reviews.length ?? 0}</span>
          </div>
          <div className="empty">
            <strong>No reviews scheduled.</strong>
            <span>
              {today?.reviewsNote ??
                "Spaced review is generated from graded attempts, which arrive in Phase 6."}
            </span>
          </div>
        </section>

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

      {today?.pathSlug ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">{today.pathTitle}</span>
            <span className="muted mono">
              {today.waypointsIntroduced} of {today.waypointCount} introduced
            </span>
          </div>
          <p className="prose">
            {today.waypointsStarted === 0
              ? "You have not opened a waypoint on this path yet."
              : `You have opened ${today.waypointsStarted} of ${today.waypointCount} waypoints, and ${today.waypointsIntroduced} have enough reading behind them to count as introduced.`}
          </p>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--outline" href="/app/learn">
              See the whole path with your evidence
              <IconArrowRight />
            </Link>
          </div>
          <p className="hint" style={{ marginTop: 12 }}>
            Introduced is the ceiling that reading can reach. Everything above it is earned by
            work you produce, which starts with graded practice in Phase 6.
          </p>
        </section>
      ) : null}

      {/* Product progress, clearly labelled as such — never presented as your mastery. */}
      <section className="card">
        <div className="card__head">
          <span className="card__title">What is live in AILEAPRO</span>
          <span className="muted mono">Build progress, not yours</span>
        </div>
        <ul className="timeline">
          {delivered.map((item) => (
            <li className="timeline__item" key={item.label}>
              <span className="timeline__dot" data-state={item.state} />
              <span>{item.label}</span>
              <span className="muted mono">{item.note}</span>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}

/** Verb, object, minutes, and the sentence explaining why this and not something else. */
function NextActionCard({ today }: { today: Today | null }) {
  if (!today) {
    return (
      <section className="card card--hero">
        <p className="kicker">Your next action</p>
        <p className="muted">Working out what to recommend…</p>
      </section>
    );
  }

  const next: NextAction = today.next;
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
        {today.onboarded ? (
          <Link className="btn btn--outline" href="/app/onboarding">
            Change your goal
          </Link>
        ) : null}
      </div>

      <p className="hint" style={{ marginTop: 16 }}>
        Chosen by rule <code className="mono">{next.rule}</code>. Overdue spaced review would
        outrank this, but there is no review schedule until graded items exist.
      </p>
    </section>
  );
}
