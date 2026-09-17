"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button, ErrorAlert, IconArrowRight, IconRepeat } from "@aileapro/ui";
import { Markdown } from "@aileapro/ui/markdown";
import {
  api,
  csrfHeaders,
  type DueCard,
  type ReviewQueue,
  type ReviewResult,
} from "@/lib/api";

/**
 * Spaced review, one card at a time.
 *
 * <p>The back of the card is not in the queue response. It arrives only when a grade is
 * posted, so the learner has to commit to &ldquo;I knew this&rdquo; before finding out whether
 * they did. Showing both at once measures recognition, which feels like recall and is not.
 */
export default function ReviewPage() {
  const [queue, setQueue] = useState<ReviewQueue | null>(null);
  const [result, setResult] = useState<ReviewResult | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setQueue(await api<ReviewQueue>("/v1/reviews/due"));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load your review queue");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const card = queue?.cards[0] ?? null;

  const grade = async (value: number) => {
    if (!card) return;
    setBusy(true);
    setError("");
    try {
      setResult(
        await api<ReviewResult>(`/v1/reviews/${card.scheduleId}`, {
          method: "POST",
          headers: await csrfHeaders(),
          body: JSON.stringify({ grade: value }),
        }),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not record that grade");
    } finally {
      setBusy(false);
    }
  };

  const next = async () => {
    setResult(null);
    setRevealed(false);
    await load();
  };

  return (
    <>
      <header className="space__head">
        <div className="space__titles">
          <p className="kicker">Study</p>
          <h1>Review</h1>
          <p className="lede">
            Cards you have read, returning at the point you are most likely to have started
            forgetting them.
          </p>
        </div>
        <div className="space__meta">
          {queue ? <span className="badge">{queue.dueCount} due</span> : null}
          {queue && queue.overdueCount > 0 ? (
            <span className="badge badge--warn">{queue.overdueCount} overdue</span>
          ) : null}
        </div>
      </header>

      {error ? <ErrorAlert message={error} /> : null}

      {!queue ? (
        <section className="card">
          <p className="muted">Checking what is due…</p>
        </section>
      ) : result ? (
        <Outcome result={result} onNext={next} />
      ) : card ? (
        <CardFront
          card={card}
          revealed={revealed}
          busy={busy}
          onReveal={() => setRevealed(true)}
          onGrade={grade}
        />
      ) : (
        <section className="card">
          <div className="empty">
            <strong>Nothing is due.</strong>
            <span>{queue.note}</span>
            <Link className="inline-link" href="/app/today">
              Back to Today
              <IconArrowRight />
            </Link>
          </div>
        </section>
      )}

      {queue && queue.leechCount > 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Held back from the queue</span>
            <span className="muted mono">{queue.leechCount}</span>
          </div>
          <p className="prose">
            {queue.leechCount === 1 ? "One card has" : `${queue.leechCount} cards have`} failed
            often enough that showing them again is not the answer. They are parked until the
            page behind them is reread, because repetition cannot install something that was
            never understood.
          </p>
        </section>
      ) : null}
    </>
  );
}

/** The prompt, then the grades. Grading is the only thing that reveals the answer. */
function CardFront({
  card,
  revealed,
  busy,
  onReveal,
  onGrade,
}: {
  card: DueCard;
  revealed: boolean;
  busy: boolean;
  onReveal: () => void;
  onGrade: (grade: number) => void;
}) {
  return (
    <section className="card review">
      <div className="card__head">
        <span className="card__title">
          {card.unit ? (
            <Link href={`/app/learn/${card.unit.slug}`}>{card.unit.title}</Link>
          ) : (
            "Review"
          )}
        </span>
        <span className="quiz__tags">
          {card.daysOverdue > 0 ? (
            <span className="badge badge--warn">{card.daysOverdue}d overdue</span>
          ) : (
            <span className="badge">due today</span>
          )}
          <span className="badge">
            seen {card.reps}×{card.lapses > 0 ? ` · ${card.lapses} lapses` : ""}
          </span>
        </span>
      </div>

      <div className="review__front">
        <Markdown>{card.front}</Markdown>
      </div>

      {revealed ? (
        <div className="review__grades">
          <p className="kicker">How did that go?</p>
          <div className="chipset">
            {GRADES.map((option) => (
              <button
                key={option.value}
                type="button"
                className="chipbtn"
                disabled={busy}
                onClick={() => onGrade(option.value)}
                title={option.hint}
              >
                {option.label}
              </button>
            ))}
          </div>
          <p className="hint">
            Answer honestly. A card you grade generously comes back too late to catch, and the
            only person the inflated interval fools is you.
          </p>
        </div>
      ) : (
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Button variant="primary" onClick={onReveal}>
            I have an answer in mind
            <IconArrowRight />
          </Button>
          <span className="hint">
            Recall it before you grade. The back of the card is not loaded until you commit.
          </span>
        </div>
      )}
    </section>
  );
}

/** What the grade did to the schedule, spelled out so the interval is never a mystery. */
function Outcome({ result, onNext }: { result: ReviewResult; onNext: () => void }) {
  return (
    <section className="card review">
      <div className="card__head">
        <span className="card__title">The answer</span>
        <span className={`badge badge--${result.passed ? "ok" : "warn"}`}>
          {result.passed ? "Recalled" : "Missed"}
        </span>
      </div>

      <div className="review__back">
        <Markdown>{result.back}</Markdown>
      </div>

      <dl className="facts">
        <div>
          <dt>Interval</dt>
          <dd>
            {result.intervalBefore}d → {result.intervalAfter}d
          </dd>
        </div>
        <div>
          <dt>Next due</dt>
          <dd>{result.nextDue}</dd>
        </div>
        <div>
          <dt>Ease</dt>
          <dd className="mono">{result.ease.toFixed(2)}</dd>
        </div>
        <div>
          <dt>Counted for recall</dt>
          <dd>{result.wasDue ? "yes" : "no — it was not due"}</dd>
        </div>
      </dl>

      <p className="prose">{result.note}</p>

      {result.leech ? (
        <p className="hint">
          This card is now parked. It has failed too often for another repetition to help, so it
          leaves the queue until the page behind it is reread.
        </p>
      ) : null}

      <div className="space__actions" style={{ marginTop: 16 }}>
        <Button variant="primary" onClick={onNext}>
          {result.remaining > 0 ? `Next card · ${result.remaining} left` : "Finish"}
          <IconRepeat size={15} />
        </Button>
      </div>
    </section>
  );
}

/**
 * SM-2 grades, labelled in words. The numbers are the algorithm's; the learner should be
 * choosing between experiences, not guessing what a 4 is supposed to mean.
 */
const GRADES = [
  { value: 0, label: "No idea", hint: "Complete blank" },
  { value: 2, label: "Wrong", hint: "I answered and it was wrong" },
  { value: 3, label: "Hard", hint: "Right, but it took real effort" },
  { value: 4, label: "Good", hint: "Right, after a moment" },
  { value: 5, label: "Easy", hint: "Immediate" },
];
