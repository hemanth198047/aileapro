"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import {
  Callout,
  ErrorAlert,
  IconAlert,
  IconArrowRight,
  IconBook,
  IconMap,
  MasteryChips,
} from "@aileapro/ui";
import { ApiError, api, type UnitDetail } from "@/lib/api";
import { UnitBody } from "@/components/unit-blocks";
import { PrereqPanel } from "@/components/prereq-panel";

/** Read mode for one unit. No AI is involved on this page — the text is authored. */
export default function UnitPage() {
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [unit, setUnit] = useState<UnitDetail | null>(null);
  const [missing, setMissing] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setUnit(null);
    setMissing(false);
    setError("");
    api<UnitDetail>(`/v1/units/${slug}`)
      .then((loaded) => {
        if (!cancelled) setUnit(loaded);
      })
      .catch((err) => {
        if (cancelled) return;
        // Unpublished units answer 404: the API does not disclose that a draft exists.
        if (err instanceof ApiError && err.status === 404) {
          setMissing(true);
          return;
        }
        setError(err instanceof Error ? err.message : "Could not load this unit");
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  if (missing) {
    return (
      <>
        <header className="space__head">
          <div className="space__titles">
            <p className="kicker">Learn</p>
            <h1>Not published</h1>
            <p className="lede">
              There is no published unit at <code className="mono">{slug}</code>.
            </p>
          </div>
          <div className="space__meta">
            <span className="badge badge--warn">404</span>
          </div>
        </header>
        <section className="card">
          <Callout tone="warn" icon={<IconAlert />}>
            Either the slug is wrong, or the unit exists but is still in draft, review, or
            validation.
          </Callout>
          <p className="hint" style={{ marginTop: 12 }}>
            Drafts are invisible to learners by design: the catalog only ever returns published
            content, so an unfinished unit cannot be reached by guessing its address.
          </p>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--outline" href="/app/learn">
              Back to the catalog
              <IconArrowRight />
            </Link>
          </div>
        </section>
      </>
    );
  }

  if (error) {
    return (
      <>
        <ErrorAlert message={error} />
        <Link className="btn btn--outline" href="/app/learn">
          Back to the catalog
          <IconArrowRight />
        </Link>
      </>
    );
  }

  if (!unit) {
    return (
      <section className="card">
        <p className="muted">Loading the unit…</p>
      </section>
    );
  }

  const isContainer = unit.kind === "domain" || unit.kind === "chapter" || unit.kind === "module";

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <nav className="unit__crumbs" aria-label="Unit location">
            <Link href="/app/learn">Catalog</Link>
            {unit.breadcrumb.map((ancestor) => (
              <span key={ancestor.id}>
                <span aria-hidden> / </span>
                <Link href={`/app/learn/${ancestor.slug}`}>{ancestor.title}</Link>
              </span>
            ))}
          </nav>
          <h1>{unit.title}</h1>
          {unit.description ? <p className="lede">{unit.description}</p> : null}
        </div>
        <div className="space__meta">
          <span className="badge">{unit.kind.replace(/_/g, " ")}</span>
          {unit.estimateMinutes ? <span className="badge">{unit.estimateMinutes} min</span> : null}
        </div>
      </header>

      <section className="card unit__facts">
        <dl className="facts">
          {unit.difficulty ? (
            <div>
              <dt>Difficulty</dt>
              <dd>{unit.difficulty} / 5</dd>
            </div>
          ) : null}
          <div>
            <dt>Volatility</dt>
            <dd>{unit.volatility}</dd>
          </div>
          <div>
            <dt>Knowledge class</dt>
            <dd>{unit.knowledgeClass.replace(/_/g, " ")}</dd>
          </div>
          <div>
            <dt>Version</dt>
            <dd className="mono">{unit.version}</dd>
          </div>
          <div>
            <dt>Last reviewed</dt>
            <dd>{formatDate(unit.lastReviewedAt)}</dd>
          </div>
        </dl>
        <div className="unit__evidence">
          <span className="kicker">Your evidence</span>
          <MasteryChips />
          <p className="hint">
            Nothing is recorded yet. Reading is tracked from Phase 5 and it will only ever fill
            Read — practice, assessment, and application have to be earned separately.
          </p>
        </div>
      </section>

      <PrereqPanel slug={unit.slug} />

      {unit.objectives.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">By the end you should be able to</span>
          </div>
          <ul className="objectives">
            {unit.objectives.map((objective) => (
              <li key={objective}>{objective}</li>
            ))}
          </ul>
        </section>
      ) : null}

      <UnitBody blocks={unit.blocks} />

      {isContainer ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">
              {unit.children.length > 0 ? "Units in here" : "Nothing published here yet"}
            </span>
            {unit.children.length > 0 ? (
              <span className="muted mono">{unit.children.length}</span>
            ) : null}
          </div>
          {unit.children.length > 0 ? (
            <ul className="childlist">
              {unit.children.map((child) => (
                <li key={child.id}>
                  <Link className="childlist__link" href={`/app/learn/${child.slug}`}>
                    <IconBook />
                    <span>
                      <strong>{child.title}</strong>
                      {child.description ? <span>{child.description}</span> : null}
                    </span>
                    <IconArrowRight />
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <>
              <Callout icon={<IconMap />}>
                This {unit.kind} is mapped and described, but no units inside it are published.
              </Callout>
              <p className="hint" style={{ marginTop: 12 }}>
                The taxonomy exists so the shape of the subject is visible from the start.
                Authoring happens domain by domain, and an empty domain says so rather than
                showing a plausible-looking outline nobody wrote.
              </p>
            </>
          )}
        </section>
      ) : null}

      {unit.resources.length > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Sources</span>
            <span className="muted mono">{unit.resources.length}</span>
          </div>
          <ul className="sources">
            {unit.resources.map((resource) => (
              <li className="source" key={resource.url}>
                <a className="source__title" href={resource.url} target="_blank" rel="noreferrer">
                  {resource.title}
                  <IconArrowRight />
                </a>
                <p className="source__meta">
                  <span className="badge">{resource.sourceType.replace(/_/g, " ")}</span>
                  {resource.publisher ? <span className="muted">{resource.publisher}</span> : null}
                  {resource.publishedOn ? (
                    <span className="muted">published {formatDate(resource.publishedOn)}</span>
                  ) : null}
                  {resource.lastVerifiedAt ? (
                    <span className="muted">link checked {formatDate(resource.lastVerifiedAt)}</span>
                  ) : null}
                </p>
                {resource.excerpt ? <p className="source__note">{resource.excerpt}</p> : null}
                {resource.license ? (
                  <p className="source__license">Licence: {resource.license}</p>
                ) : null}
              </li>
            ))}
          </ul>
          <p className="hint" style={{ marginTop: 12 }}>
            Every external link carries its publisher, licence, and the date the URL was last
            checked. Automatic re-checking of dead links is Phase 15.
          </p>
        </section>
      ) : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">What this page does not do yet</span>
        </div>
        <p className="prose">
          Reading this unit records nothing, the exercises are not graded, and the tutor cannot
          discuss it. Evidence and recommendations arrive in Phase 5, graded assessment in Phase 6,
          and the grounded tutor in Phase 7.
        </p>
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Link className="btn btn--outline" href="/app/learn">
            Back to the catalog
            <IconArrowRight />
          </Link>
        </div>
      </section>
    </>
  );
}

function formatDate(value: string | null) {
  if (!value) return "not recorded";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "not recorded";
  return date.toISOString().slice(0, 10);
}
