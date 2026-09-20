import type { Metadata } from "next";
import Link from "next/link";
import { Brand } from "@aileapro/ui";

export const metadata: Metadata = { title: "Privacy" };

/**
 * Operational privacy policy for launch. Counsel should review before public traffic;
 * this text mirrors product behaviour in Settings export/delete and auth.
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
        <h1>Privacy Policy</h1>
        <p className="muted">Last updated: 2026-09-20 · Operator: AILEAPRO</p>

        <p>
          This policy explains what AILEAPRO collects, why, and what you can do about it. It is
          written to match how the product works today (export, delete, feature flags, tutor, and
          lab). It is not legal advice; have counsel review it before a public launch in your
          jurisdiction.
        </p>

        <h2>Who we are</h2>
        <p>
          AILEAPRO is an AI learning product: curriculum, assessments, tutor chat, coding lab, and
          progress tracking. The service is operated from the deployment you sign into (for example
          your organisation&apos;s staging or production host).
        </p>

        <h2>What we collect</h2>
        <p>Depending on how you use the product, we may store:</p>
        <ul>
          <li>
            <strong>Account</strong> — email address and a password hash (we do not store your
            password in plain text).
          </li>
          <li>
            <strong>Learning</strong> — path choice, mastery and evidence, assessment attempts,
            study plan and review cards, notes, and project/portfolio artefacts.
          </li>
          <li>
            <strong>Tutor</strong> — chat transcripts when you use the AI tutor.
          </li>
          <li>
            <strong>Lab</strong> — source code you submit to run in the sandbox, plus run metadata
            (status, timing, exit codes — not unrestricted network access).
          </li>
          <li>
            <strong>Usage / ops</strong> — request IDs, audit events (for example login), feature-flag
            state, and aggregate rate limits. We do not sell personal learning graphs.
          </li>
        </ul>

        <h2>Why we process it</h2>
        <p>
          We process this data to operate the product: authenticate you, teach and assess, recommend
          next work, run sandboxed code, improve reliability and security, and honour your export or
          deletion requests. We do not use learner data for advertising profiles.
        </p>

        <h2>AI model providers</h2>
        <p>
          When the tutor (or related AI features) is enabled, prompts and necessary context may be
          sent to a contracted model provider. Default posture: do not use learner data to train
          provider models; prefer zero-retention or equivalent contractual terms where offered.
          Feature flags may disable the tutor without deleting your past transcripts until you
          delete your account.
        </p>

        <h2>Cookies and sessions</h2>
        <p>
          We use a first-party session cookie to keep you signed in, plus a CSRF token cookie for
          form safety. In production the session cookie is marked Secure. We do not rely on
          third-party advertising cookies in the P0 product.
        </p>

        <h2>Your rights</h2>
        <p>
          While signed in, open <Link href="/app/settings">Settings</Link> to:
        </p>
        <ul>
          <li>
            <strong>Export</strong> — download a machine-readable copy of your account and learning
            data (<code>GET /v1/me/export</code>).
          </li>
          <li>
            <strong>Delete</strong> — request deletion. Deletion anonymizes the account (email
            replaced, free text scrubbed, status set to deleted) so you cannot sign in again.
          </li>
        </ul>
        <p>
          Depending on your region you may also have rights to access, correct, restrict, or object
          to certain processing. Contact the operator of your deployment for those requests.
        </p>

        <h2>Children</h2>
        <p>
          Accounts are for learners 13 or older (or the higher age required where you live). We do
          not knowingly create accounts for children under that age.
        </p>

        <h2>Retention</h2>
        <p>
          We keep account and learning data while your account is active. After deletion, residual
          anonymized operational logs may remain for security and integrity. Lab and tutor content
          follows the same account lifecycle unless a shorter retention window is configured by the
          operator.
        </p>

        <h2>Security</h2>
        <p>
          Passwords are hashed. Sessions are server-side. The coding lab is isolated from the main
          API process. No product is perfectly secure; report suspected issues to the deployment
          operator.
        </p>

        <h2>Changes</h2>
        <p>
          We may update this page when product behaviour changes. The “Last updated” date at the top
          will change when we do.
        </p>

        <p className="hint">
          Counsel should replace or amend this operational text before public traffic in regulated
          markets. See also <Link href="/terms">Terms of Service</Link>.
        </p>
      </article>
    </main>
  );
}
