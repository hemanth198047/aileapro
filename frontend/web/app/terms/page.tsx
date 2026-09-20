import type { Metadata } from "next";
import Link from "next/link";
import { Brand } from "@aileapro/ui";

export const metadata: Metadata = { title: "Terms of Service" };

/**
 * Operational terms for launch. Counsel should review before public traffic.
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
        <p className="muted">Last updated: 2026-09-20 · Operator: AILEAPRO</p>

        <p>
          By creating an AILEAPRO account or using the service you agree to these terms. If you do
          not agree, do not use the service. This is operational product language, not legal advice;
          have counsel review it before a public launch.
        </p>

        <h2>The service</h2>
        <p>
          AILEAPRO provides educational curriculum, assessments, an optional AI tutor, a sandboxed
          coding lab, search, and progress tools. Features may be enabled or disabled by feature
          flags (for example tutor or lab) without ending these terms.
        </p>

        <h2>Accounts</h2>
        <p>
          You must provide a valid email and keep credentials secret. You are responsible for
          activity under your account. We may suspend or terminate accounts that abuse rate limits,
          attack the sandbox, attempt to access other learners&apos; data, scrape content for
          redistribution, or otherwise harm the service.
        </p>
        <p>
          You must be at least 13 (or the higher age required where you live). See the{" "}
          <Link href="/privacy">Privacy Policy</Link> for how we handle personal data.
        </p>

        <h2>Acceptable use</h2>
        <ul>
          <li>Use the product for personal or authorised organisational learning.</li>
          <li>Do not probe, overload, or break out of the lab sandbox or infrastructure.</li>
          <li>Do not attempt to extract secrets, other users&apos; data, or unpublished drafts.</li>
          <li>Do not use the tutor or lab to generate or host illegal content.</li>
          <li>Do not resell or wholesale-copy curriculum for a competing product.</li>
        </ul>

        <h2>Content and IP</h2>
        <p>
          Curriculum, assessments, and product UI we publish remain our intellectual property (or
          that of our licensors). Code, notes, and projects you create remain yours; you grant us a
          limited license to host, process, and run them so the product can teach, assess, and
          operate the lab.
        </p>

        <h2>AI output</h2>
        <p>
          Tutor and other model-generated text can be wrong, incomplete, or outdated. Treat it as a
          study aid, not professional, legal, medical, or safety-critical advice. Always verify
          important claims against authored curriculum and primary sources.
        </p>

        <h2>Assessments and credentials</h2>
        <p>
          Scores and mastery states are evidence of work inside AILEAPRO. They are not accreditation
          by a university, regulator, or employer unless a separate programme says so in writing.
        </p>

        <h2>Availability</h2>
        <p>
          We aim for reliable uptime but do not guarantee uninterrupted service. Planned maintenance,
          incident response, or flag changes may temporarily disable parts of the product.
        </p>

        <h2>Limitation of liability</h2>
        <p>
          The service is provided “as is” for education. To the fullest extent allowed by law we
          disclaim warranties of merchantability, fitness for a particular purpose, and
          non-infringement, and we are not liable for indirect or consequential damages arising from
          use of the service. Some regions do not allow certain limitations; in those places our
          liability is limited to the maximum permitted by law.
        </p>

        <h2>Changes</h2>
        <p>
          We may update these terms when the product changes. Continued use after the “Last updated”
          date means you accept the revised terms. Material changes should be called out by the
          operator of your deployment.
        </p>

        <h2>Contact</h2>
        <p>
          Privacy and deletion: use <Link href="/app/settings">Settings</Link> or contact your
          deployment operator. Security concerns: contact the same operator through the channel they
          publish.
        </p>

        <p className="hint">
          Counsel should replace or amend this operational text before public traffic. See also the{" "}
          <Link href="/privacy">Privacy Policy</Link>.
        </p>
      </article>
    </main>
  );
}
