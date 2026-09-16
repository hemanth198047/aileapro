"use client";

import Link from "next/link";
import { useState } from "react";
import { Button, Callout, IconAlert, IconArrowRight } from "@aileapro/ui";
import { useSession } from "@/components/session";
import { VerifyEmailForm } from "@/components/verify-email";
import { SpaceHeader } from "@/components/space-view";

/** Phases already delivered, so the learner can see where the product actually is. */
const delivered = [
  { label: "Account, session, and roles", state: "done", note: "Phase 1" },
  { label: "Studio shell and every space routed", state: "done", note: "Phase 2" },
  { label: "Authored curriculum, paths, and sources", state: "active", note: "Phase 3" },
  { label: "Knowledge map with prerequisites", state: "todo", note: "Phase 4" },
];

export default function TodayPage() {
  const { me } = useSession();
  const [verifying, setVerifying] = useState(false);

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

      {/* One primary action, and it has to be something that genuinely works today. */}
      <section className="card card--hero">
        <p className="kicker">Your next action</p>
        {me.emailVerified ? (
          <>
            <h2>Start at the beginning of the path.</h2>
            <p className="lede">
              Ten authored units are published, so there is real reading available. This is the
              first one, not a recommendation: nothing about your knowledge is recorded yet, so
              Today cannot yet claim to know where you should be. Ranking starts in Phase 5.
            </p>
            <div className="space__actions">
              <Link className="btn btn--primary" href="/app/learn/instructions-and-state">
                Read: Instructions and State
                <IconArrowRight />
              </Link>
              <Link className="btn btn--outline" href="/app/learn">
                See the whole path
              </Link>
            </div>
          </>
        ) : (
          <>
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
          </>
        )}
      </section>

      <div className="grid-2">
        <section className="card">
          <div className="card__head">
            <span className="card__title">Reviews due</span>
            <span className="muted mono">0</span>
          </div>
          <div className="empty">
            <strong>No reviews scheduled.</strong>
            <span>
              Spaced review is generated from assessment attempts, which arrive in Phase 6.
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
