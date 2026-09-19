import type { Metadata } from "next";
import Link from "next/link";
import { Brand } from "@aileapro/ui";

export const metadata: Metadata = { title: "Terms of Service" };

/**
 * Stub ToS for launch hardening. Replace with counsel-reviewed text before public launch.
 */
export default function TermsPage() {
  return (
    <main className="legal">
      <header className="legal__head">
        <Brand />
        <nav className="legal__nav">
          <Link href="/privacy">Privacy</Link>
          <Link href="/login">Sign in</Link>
        </nav>
      </header>
      <article className="legal__body">
        <h1>Terms of Service</h1>
        <p className="muted">Last updated: 2026-09-18 · Placeholder for counsel review</p>
        <p>
          By creating an AILEAPRO account you agree to use the service for personal learning,
          not to attack the lab sandbox, scrape content for redistribution, or attempt to extract
          other learners&apos; data.
        </p>
        <h2>Content</h2>
        <p>
          Curriculum we publish remains our IP. Code and notes you write remain yours; you grant
          us a license to host and run them so the product can teach and assess.
        </p>
        <h2>Availability</h2>
        <p>
          Feature flags may disable the tutor or lab temporarily. We may suspend accounts that
          abuse rate limits or the sandbox.
        </p>
        <h2>Limitation</h2>
        <p>
          The service is provided as-is for education. Assessment scores are evidence of work
          inside AILEAPRO, not accreditation by a third party.
        </p>
        <p className="hint">
          This page is an operational stub so launch gates have a URL. It is not legal advice.
        </p>
      </article>
    </main>
  );
}
