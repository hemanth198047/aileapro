"use client";

import Link from "next/link";
import { IconArrowRight } from "@aileapro/ui";
import type { Report, ReportLine } from "@/lib/api";

/**
 * A finished attempt, rendered in full.
 *
 * <p>The score is here, but it is deliberately not the headline and it is never alone: the
 * sections below it are what the learner can act on, and the API guarantees each one is
 * populated. A report that is only a percentage tells you that you failed without telling you
 * what to do about it, which is the one outcome this screen is built to prevent.
 */
export function ReportView({ report }: { report: Report }) {
  const percent = Math.round(report.fraction * 100);
  const passMark = Math.round(report.passMark * 100);
  const placement = report.kind === "diagnostic";

  return (
    <>
      <section className="card card--hero">
        <p className="kicker">{placement ? "Where you should start" : report.assessmentTitle}</p>
        <h2>{report.summary}</h2>
        <p className="lede">{report.readinessReason}</p>

        <div className="report__score">
          {/* A placement has no score to report, and inventing one would invite the learner to
              treat a guess as a grade. */}
          {placement ? (
            <span className="badge badge--brand">Not scored</span>
          ) : (
            <>
              <span className="report__figure mono">{percent}%</span>
              <span className="muted">
                {report.correct} of {report.answered} right · pass mark {passMark}%
              </span>
              {report.passed === null ? null : (
                <span className={`badge badge--${report.passed ? "ok" : "warn"}`}>
                  {report.passed ? "Passed" : "Below the pass mark"}
                </span>
              )}
            </>
          )}
          {report.unmarked > 0 ? (
            <span className="badge badge--warn">{report.unmarked} left unmarked</span>
          ) : null}
        </div>

        {/* A placement is not about one unit, so it has no state to quote. Printing the field
            anyway would produce a sentence with a hole in it. */}
        <p className="hint" style={{ marginTop: 16 }}>
          Readiness: <strong>{report.readiness.replace(/_/g, " ")}</strong>.
          {report.masteryState ? (
            <>
              {" "}
              Your state on this material is <strong>{report.masteryState}</strong> —{" "}
              {report.masteryNote}
            </>
          ) : null}
        </p>
      </section>

      <div className="grid-2">
        <Lines
          title="What held up"
          lines={report.strengths}
          empty="Nothing in this attempt was strong enough to call a strength yet."
        />
        <Lines
          title="What did not"
          lines={report.weaknesses}
          empty="Nothing came out clearly weak, which is worth knowing too."
        />
      </div>

      {report.misconceptions.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Specific beliefs to correct</span>
            <span className="muted mono">{report.misconceptions.length}</span>
          </div>
          <ul className="misconceptions">
            {report.misconceptions.map((raised) => (
              <li key={raised.code}>
                <strong>{raised.claim}</strong>
                {raised.href ? (
                  <Link className="inline-link" href={raised.href}>
                    Read the page that argues against it
                    <IconArrowRight />
                  </Link>
                ) : null}
              </li>
            ))}
          </ul>
          <p className="hint">
            These are named rather than counted as generic mistakes, because &ldquo;you got three
            wrong&rdquo; and &ldquo;you think shape is preserved under multiplication&rdquo; call
            for completely different reading.
          </p>
        </section>
      ) : null}

      {report.missingPrereqs.length > 0 ? (
        <Lines
          title="Prerequisites that are not in place"
          lines={report.missingPrereqs}
          empty=""
          note="These come first. Practising on top of a gap is how the gap becomes permanent."
        />
      ) : null}

      <Lines
        title="Reread these"
        lines={report.revision}
        empty="Nothing needs rereading from this attempt."
      />

      <Lines
        title="Next steps"
        lines={report.nextSteps}
        empty="No further steps from this attempt."
        note="Ordered. The first line is the one to do now."
      />

      <p className="hint">
        Submitted {new Date(report.submittedAt).toLocaleString()}. This report is written once
        from this attempt and kept as it was — a later attempt gets its own, so you can see what
        changed rather than having the record rewritten.
      </p>
    </>
  );
}

function Lines({
  title,
  lines,
  empty,
  note,
}: {
  title: string;
  lines: ReportLine[];
  empty: string;
  note?: string;
}) {
  return (
    <section className="card">
      <div className="card__head">
        <span className="card__title">{title}</span>
        {lines.length > 0 ? <span className="muted mono">{lines.length}</span> : null}
      </div>
      {lines.length === 0 ? (
        <p className="muted">{empty}</p>
      ) : (
        <ul className="reportlines">
          {lines.map((line, index) => (
            <li key={`${line.label}-${index}`}>
              <span className="reportlines__label">
                {line.href ? <Link href={line.href}>{line.label}</Link> : line.label}
              </span>
              <span className="reportlines__detail">{line.detail}</span>
            </li>
          ))}
        </ul>
      )}
      {note ? (
        <p className="hint" style={{ marginTop: 12 }}>
          {note}
        </p>
      ) : null}
    </section>
  );
}
