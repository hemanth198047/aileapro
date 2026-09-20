"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import {
  Callout,
  ErrorAlert,
  IconAlert,
  IconArrowRight,
  IconBook,
  IconMap,
  MasteryChips,
} from "@aileapro/ui";
import { Markdown } from "@aileapro/ui/markdown";
import {
  ApiError,
  api,
  type UnitAssessments,
  type UnitBlock,
  type UnitDetail,
} from "@/lib/api";
import { UnitBody } from "@/components/unit-blocks";
import { PrereqPanel } from "@/components/prereq-panel";
import { UnitProgressProvider, readingProgress, useUnitProgress } from "@/components/unit-progress";

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
    // Everything below is inside the reporter, so opening, level changes, and visible dwell
    // are observed once, in one place, for the unit actually on screen.
    <UnitProgressProvider slug={unit.slug}>
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
        <UnitEvidence />
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

      <PracticeDock slug={unit.slug} blocks={unit.blocks} />

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
          Your reading fills the Read chip, and the graded practice and quiz above can fill
          Practice and Assess. Apply fills when you finish a linked project in{" "}
          <Link href="/app/studio">Studio</Link> — quizzes alone never count as application.
        </p>
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Link className="btn btn--outline" href="/app/learn">
            Back to the catalog
            <IconArrowRight />
          </Link>
        </div>
      </section>
    </UnitProgressProvider>
  );
}

/** The learner's evidence on this unit, and what the Read chip is still waiting for. */
function UnitEvidence() {
  const { mastery } = useUnitProgress();
  const { percent, remainingMinutes } = readingProgress(mastery);

  return (
    <div className="unit__evidence">
      <span className="kicker">Your evidence</span>
      <MasteryChips evidence={mastery?.evidence} />
      {mastery ? (
        <>
          <div className="readmeter" role="group" aria-label="Reading evidence">
            <div className="progress">
              <span className="progress__fill" style={{ width: `${percent}%` }} />
            </div>
            <span className="readmeter__label mono">
              {mastery.evidence.read.state === "solid"
                ? "Read: solid"
                : `${remainingMinutes} min of reading left for a solid Read`}
            </span>
          </div>
          <p className="hint">
            State: <strong>{mastery.state}</strong>. Reading counts only while this tab is in
            front of you, and it stops at INTRODUCED however long you stay. Everything above it
            is earned in the graded practice and quiz below.
          </p>
        </>
      ) : (
        <p className="hint">Checking what you have shown on this unit…</p>
      )}
    </div>
  );
}

/**
 * The practice dock: the graded work authored for this unit, plus the ungraded exercises.
 *
 * <p>Graded and ungraded are kept visibly separate. The practice set and the quiz move the
 * chips because a server marked them; the written exercises are worth doing and move nothing,
 * and offering a "mark as done" button beside them would turn self-report into evidence.
 */
function PracticeDock({ slug, blocks }: { slug: string; blocks: UnitBlock[] }) {
  const { report } = useUnitProgress();
  const [offered, setOffered] = useState<UnitAssessments | null>(null);
  const exercises = blocks.filter((block) => block.type === "exercise_set");
  const opened = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setOffered(null);
    api<UnitAssessments>(`/v1/units/${slug}/assessments`)
      .then((loaded) => {
        if (!cancelled) setOffered(loaded);
      })
      .catch(() => {
        // The dock degrades to the authored exercises rather than blocking the reading.
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  const graded = offered ? [offered.practice, offered.quiz].filter(Boolean) : [];

  return (
    <section className="card dock" id="practice" aria-label="Practice">
      <div className="card__head">
        <span className="card__title">Practice and assess</span>
        {offered ? (
          <span className="badge" data-mastery={offered.ceiling}>
            Ceiling: {offered.ceiling}
          </span>
        ) : null}
      </div>

      {graded.length > 0 ? (
        <>
          <ul className="offers">
            {graded.map((offer) => (
              <li className="offer" key={offer!.id}>
                <span className="offer__body">
                  <strong>{offer!.title}</strong>
                  {offer!.description ? <span>{offer!.description}</span> : null}
                  <span className="offer__meta">
                    <span className="badge">{offer!.kind}</span>
                    <span className="muted mono">{offer!.itemCount} items</span>
                    {offer!.kind === "quiz" ? (
                      <span className="muted mono">
                        pass {Math.round(offer!.passMark * 100)}%
                      </span>
                    ) : null}
                    {offer!.openAttemptId ? (
                      <span className="badge badge--warn">In progress</span>
                    ) : null}
                  </span>
                </span>
                <Link
                  className={`btn ${offer!.kind === "quiz" ? "btn--outline" : "btn--primary"}`}
                  href={`/app/quiz/${offer!.slug}`}
                >
                  {offer!.openAttemptId ? "Resume" : "Start"}
                  <IconArrowRight />
                </Link>
              </li>
            ))}
          </ul>
          {offered ? <p className="hint">{offered.stateNote}</p> : null}
        </>
      ) : (
        <div className="empty">
          <strong>
            {offered ? "No graded items are authored for this unit." : "Checking what is graded…"}
          </strong>
          <span>
            {offered
              ? "Until there are, the Practice and Assess chips on this unit cannot move for anyone, which is why they are empty rather than optimistic."
              : "The practice set and quiz for this unit load in a moment."}
          </span>
        </div>
      )}

      {exercises.length > 0 ? (
        <details
          className="dock__body"
          onToggle={(event) => {
            // Recorded as history, credited to nothing: looking at exercises is not practice.
            if (event.currentTarget.open && !opened.current) {
              opened.current = true;
              report("exercise_viewed");
            }
          }}
        >
          <summary>
            Open the written exercises
            <span className="muted mono">
              {exercises.length} set{exercises.length > 1 ? "s" : ""} · not marked
            </span>
          </summary>
          {exercises.map((block) => (
            <div className="dock__set" key={block.id}>
              {block.payload.markdown ? <Markdown>{block.payload.markdown}</Markdown> : null}
            </div>
          ))}
        </details>
      ) : null}
    </section>
  );
}

function formatDate(value: string | null) {
  if (!value) return "not recorded";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "not recorded";
  return date.toISOString().slice(0, 10);
}
