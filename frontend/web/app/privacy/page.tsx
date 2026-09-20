import type { Metadata } from "next";
import Link from "next/link";
import { Brand } from "@aileapro/ui";

export const metadata: Metadata = { title: "Privacy" };

/**
 * Stub privacy policy for launch hardening. Replace with counsel-reviewed text before public launch.
 */
export default function PrivacyPage() {
  return (
    <main className="legal">
      <header className="legal__head">
        <Brand />
        <nav className="legal__nav">
          <Link href="/terms">Terms</Link>
          <Link href="/login">Sign in</Link>
        </nav>
      </header>
      <article className="legal__body">
        <h1>Privacy</h1>
        <p className="muted">Last updated: 2026-09-18 · Placeholder for counsel review</p>
        <p>
          AILEAPRO stores the account email you register with, learning progress (mastery,
          assessments, study plan), notes, tutor transcripts, and lab source you submit. We use
          this data to operate the product — teach, assess, recommend next work — and not to sell
          personal learning graphs.
        </p>
        <h2>Your rights</h2>
        <p>
          Signed-in learners can download a machine-readable copy of their data from Settings
          (<code>GET /v1/me/export</code>) and request deletion. Deletion anonymizes the account
          (email replaced, free text scrubbed, status set to deleted) so the account cannot sign
          in again.
        </p>
        <h2>Subprocessors</h2>
        <p>
          Model providers may process tutor prompts under our contract. Default posture: do not
          use learner data to train provider models; prefer zero-retention where offered.
        </p>
        <h2>Children</h2>
        <p>Accounts are for learners 13+ (or the higher age required in your region).</p>
        <p className="hint">
          This page is an operational stub so launch gates have a URL. It is not legal advice.
        </p>
      </article>
    </main>
  );
}
