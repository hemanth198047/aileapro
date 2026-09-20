"use client";

import Link from "next/link";
import { IconArrowRight } from "@aileapro/ui";
import type { StudyPlan } from "@/lib/api";

/**
 * A generated plan, with its priors and its reasoning visible.
 *
 * <p>Every line shows why it is on the list, and every prior shows how much evidence sits
 * behind it. A plan the learner cannot argue with is a plan they have to take on trust, and
 * the whole point of showing the confidence is that a two-question guess should be arguable.
 */
export function PlanView({ plan }: { plan: StudyPlan }) {
  return (
    <>
      <section className="card">
        <div className="card__head">
          <span className="card__title">Plan v{plan.version}</span>
          <span className="muted mono">{plan.horizon}</span>
        </div>
        <p className="prose">{plan.reason}</p>
        <p className="hint" style={{ marginTop: 12 }}>
          Generated {new Date(plan.generatedAt).toLocaleString()} from your{" "}
          {plan.source.replace(/_/g, " ")}. Plans are versioned rather than overwritten, so this
          one stays readable after the next is made.
        </p>
      </section>

      {plan.priors.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Where it placed you</span>
            <span className="muted mono">{plan.priors.length} domains</span>
          </div>
          <ul className="priors">
            {plan.priors.map((prior) => (
              <li className="prior" key={prior.domain.id}>
                <div className="prior__head">
                  <Link className="prior__title" href={`/app/learn/${prior.domain.slug}`}>
                    {prior.domain.title}
                  </Link>
                  <span className="quiz__tags">
                    <span className="badge" data-confidence={prior.confidence}>
                      {prior.confidence} confidence
                    </span>
                    <span className="muted mono">
                      {prior.itemsCorrect}/{prior.itemsServed}
                    </span>
                  </span>
                </div>
                <div className="progress">
                  <span
                    className="progress__fill"
                    style={{ width: `${Math.round(prior.estimate * 100)}%` }}
                  />
                </div>
                <p className="prior__reason">{prior.reason}</p>
              </li>
            ))}
          </ul>
          <p className="hint">
            These are starting points, not scores. Confidence is shown because a domain probed
            twice cannot say anything trustworthy about a single page inside it.
          </p>
        </section>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">What to do, in order</span>
          <span className="muted mono">{plan.items.length}</span>
        </div>
        {plan.items.length === 0 ? (
          <p className="muted">The plan is empty, which should not happen. Retake the diagnostic.</p>
        ) : (
          <ol className="planlist">
            {plan.items.map((line, index) => (
              <li className="planline" key={`${line.kind}-${index}`}>
                <span className="planline__kind badge">{line.kind}</span>
                <span className="planline__body">
                  <strong>
                    {line.href && line.unit ? (
                      <Link href={line.href}>{line.unit.title}</Link>
                    ) : (
                      (line.unit?.title ?? line.kind)
                    )}
                  </strong>
                  <span className="planline__reason">{line.reason}</span>
                </span>
                {line.due ? <span className="muted mono">{line.due}</span> : null}
              </li>
            ))}
          </ol>
        )}
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Link className="btn btn--primary" href="/app/today">
            Start with the first line
            <IconArrowRight />
          </Link>
        </div>
      </section>
    </>
  );
}
