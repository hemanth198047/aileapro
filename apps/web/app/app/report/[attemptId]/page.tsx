"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ErrorAlert, IconArrowRight } from "@aileapro/ui";
import { api, type Report } from "@/lib/api";
import { ReportView } from "@/components/report-view";

/** A submitted attempt, read back. Reports are addressable so they can be returned to. */
export default function ReportPage() {
  const params = useParams<{ attemptId: string }>();
  const attemptId = params.attemptId;
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    api<Report>(`/v1/attempts/${attemptId}/report`)
      .then((loaded) => {
        if (!cancelled) setReport(loaded);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load this report");
      });
    return () => {
      cancelled = true;
    };
  }, [attemptId]);

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <p className="kicker">Assess</p>
          <h1>{report ? report.assessmentTitle : "Your report"}</h1>
          {report?.unit ? (
            <p className="lede">
              On <Link href={`/app/learn/${report.unit.slug}`}>{report.unit.title}</Link>.
            </p>
          ) : null}
        </div>
        <div className="space__meta">
          <Link className="btn btn--outline" href="/app/today">
            Back to Today
            <IconArrowRight />
          </Link>
        </div>
      </header>

      {error ? <ErrorAlert message={error} /> : null}

      {report ? (
        <ReportView report={report} />
      ) : (
        <section className="card">
          <p className="muted">Loading your report…</p>
        </section>
      )}
    </>
  );
}
