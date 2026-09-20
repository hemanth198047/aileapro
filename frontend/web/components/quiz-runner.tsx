"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button, ErrorAlert, IconArrowRight, IconCheck, IconClose } from "@aileapro/ui";
import { Markdown } from "@aileapro/ui/markdown";
import {
  api,
  csrfHeaders,
  type AttemptState,
  type Marked,
  type Report,
  type ServedItem,
} from "@/lib/api";

/**
 * Runs one attempt: serve, answer, teach, advance, submit.
 *
 * <p>The order matters more than it looks. An answer is sent to the server and the mark comes
 * back with the explanation, which is then shown whether the answer was right or wrong — so
 * feedback lands while the learner still remembers their reasoning. Nothing is marked in the
 * browser, and the item never arrives carrying its own key, so there is no version of this
 * page where the answer can be read out of the response before it is committed.
 */
export function QuizRunner({
  attempt: initial,
  /** Where submitting goes. The diagnostic keeps the report in place and adds its plan. */
  onSubmitted,
}: {
  attempt: AttemptState;
  onSubmitted?: (report: Report) => void;
}) {
  const router = useRouter();
  const [attempt, setAttempt] = useState(initial);
  const [marked, setMarked] = useState<Marked | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const item = attempt.nextItem;
  const complete = item === null;

  const advance = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      setAttempt(await api<AttemptState>(`/v1/attempts/${attempt.id}`));
      setMarked(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load the next question");
    } finally {
      setBusy(false);
    }
  }, [attempt.id]);

  const submit = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const report = await api<Report>(`/v1/attempts/${attempt.id}/submit`, {
        method: "POST",
        headers: await csrfHeaders(),
      });
      if (onSubmitted) onSubmitted(report);
      else router.push(`/app/report/${attempt.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit this attempt");
      setBusy(false);
    }
  }, [attempt.id, onSubmitted, router]);

  return (
    <>
      <AttemptProgress attempt={attempt} onExpired={submit} />

      {error ? <ErrorAlert message={error} /> : null}

      {complete ? (
        <section className="card card--hero">
          <p className="kicker">Every question answered</p>
          <h2>Submit for your report.</h2>
          <p className="lede">
            The report names what held up, what did not, and which page to reread for each
            weakness. It is written once, from this attempt, and kept.
          </p>
          <div className="space__actions">
            <Button variant="primary" onClick={submit} disabled={busy}>
              {busy ? "Writing your report…" : "Submit and read the report"}
              <IconArrowRight />
            </Button>
          </div>
        </section>
      ) : (
        <ItemCard
          key={item.id}
          item={item}
          attemptId={attempt.id}
          marked={marked}
          busy={busy}
          onMarked={(result) => {
            setMarked(result);
            setAttempt((current) => ({
              ...current,
              answeredCount: result.answeredCount,
              servedCount: result.servedCount,
            }));
          }}
          onBusy={setBusy}
          onError={setError}
          onNext={advance}
          onSubmit={submit}
        />
      )}
    </>
  );
}

/** How far through, and how long is left when the assessment is timed. */
function AttemptProgress({
  attempt,
  onExpired,
}: {
  attempt: AttemptState;
  onExpired: () => void;
}) {
  const [seconds, setSeconds] = useState(attempt.secondsRemaining);
  const expired = useRef(false);

  useEffect(() => {
    setSeconds(attempt.secondsRemaining);
  }, [attempt.secondsRemaining]);

  useEffect(() => {
    if (attempt.secondsRemaining === null) return;
    const timer = setInterval(() => setSeconds((left) => (left === null ? null : left - 1)), 1000);
    return () => clearInterval(timer);
  }, [attempt.secondsRemaining]);

  // Submitting on expiry rather than locking the page: the work already done is still worth a
  // report, and a frozen screen with no way out is not a kinder outcome.
  useEffect(() => {
    if (seconds !== null && seconds <= 0 && !expired.current) {
      expired.current = true;
      onExpired();
    }
  }, [seconds, onExpired]);

  // servedCount is the whole set on a fixed assessment; remaining is what is left of it, so
  // adding them counts the unanswered items twice. A null remaining means adaptive, where
  // there is honestly no total to show.
  const total = attempt.remaining === null ? null : attempt.servedCount;
  const percent = total ? Math.round((attempt.answeredCount / total) * 100) : 0;

  return (
    <section className="card quiz__bar">
      <div className="card__head">
        <span className="card__title">{attempt.assessmentTitle}</span>
        <span className="muted mono">
          {attempt.answeredCount} answered
          {total ? ` of ${total}` : ""}
        </span>
      </div>
      {total ? (
        <div className="progress">
          <span className="progress__fill" style={{ width: `${percent}%` }} />
        </div>
      ) : (
        <p className="hint">
          This one adapts as you go, so there is no question count to tick down. It stops when it
          has enough to place you.
        </p>
      )}
      {seconds !== null ? (
        <p className="quiz__clock mono" data-low={seconds < 120}>
          {formatClock(Math.max(0, seconds))} left
        </p>
      ) : null}
    </section>
  );
}

/** One question, then its mark and its teaching in the same card. */
function ItemCard({
  item,
  attemptId,
  marked,
  busy,
  onMarked,
  onBusy,
  onError,
  onNext,
  onSubmit,
}: {
  item: ServedItem;
  attemptId: string;
  marked: Marked | null;
  busy: boolean;
  onMarked: (result: Marked) => void;
  onBusy: (busy: boolean) => void;
  onError: (message: string) => void;
  onNext: () => void;
  onSubmit: () => void;
}) {
  const [selected, setSelected] = useState<string[]>([]);
  const [text, setText] = useState("");
  // The card is keyed by item id, so this is the moment this question appeared.
  const startedAt = useRef(Date.now());

  const choiceBased =
    item.type === "mcq" || item.type === "true_false" || item.type === "multi_select";
  const multi = item.type === "multi_select";
  const answerable = choiceBased ? selected.length > 0 : text.trim().length > 0;

  const send = async () => {
    onBusy(true);
    onError("");
    try {
      onMarked(
        await api<Marked>(`/v1/attempts/${attemptId}/items/${item.id}`, {
          method: "POST",
          headers: await csrfHeaders(),
          body: JSON.stringify({
            selectedIds: choiceBased ? selected : [],
            text: choiceBased ? null : text,
            elapsedSeconds: Math.round((Date.now() - startedAt.current) / 1000),
          }),
        }),
      );
    } catch (err) {
      onError(err instanceof Error ? err.message : "Could not record that answer");
    } finally {
      onBusy(false);
    }
  };

  return (
    <section className="card quiz__item">
      <div className="card__head">
        <span className="card__title">Question {item.sort + 1}</span>
        <span className="quiz__tags">
          <span className="badge">{item.bloom.toLowerCase()}</span>
          <span className="badge">difficulty {item.difficulty}/5</span>
          {item.unit ? (
            <Link className="badge" href={`/app/learn/${item.unit.slug}`}>
              {item.unit.title}
            </Link>
          ) : null}
        </span>
      </div>

      <div className="quiz__stem">
        <Markdown>{item.stem}</Markdown>
      </div>

      {choiceBased ? (
        <ul className="choices">
          {item.choices.map((choice) => {
            const chosen = selected.includes(choice.id);
            const key = marked?.correctIds.includes(choice.id) ?? false;
            return (
              <li key={choice.id}>
                <label
                  className="choice"
                  data-chosen={chosen}
                  // After marking, the key is shown on every choice, not only the one picked:
                  // knowing which answer was right is the point of asking.
                  data-verdict={marked ? (key ? "right" : chosen ? "wrong" : "") : ""}
                >
                  <input
                    type={multi ? "checkbox" : "radio"}
                    name={`item-${item.id}`}
                    checked={chosen}
                    disabled={marked !== null || busy}
                    onChange={() =>
                      setSelected((current) =>
                        multi
                          ? current.includes(choice.id)
                            ? current.filter((id) => id !== choice.id)
                            : [...current, choice.id]
                          : [choice.id],
                      )
                    }
                  />
                  <span className="choice__text">{choice.text}</span>
                  {marked && key ? <IconCheck /> : null}
                  {marked && chosen && !key ? <IconClose /> : null}
                </label>
              </li>
            );
          })}
        </ul>
      ) : item.type === "coding_stub" ? (
        <textarea
          className="input quiz__code mono"
          value={text}
          disabled={marked !== null || busy}
          onChange={(event) => setText(event.target.value)}
          placeholder="Write your answer"
          rows={8}
          spellCheck={false}
        />
      ) : (
        <input
          className="input"
          value={text}
          disabled={marked !== null || busy}
          onChange={(event) => setText(event.target.value)}
          placeholder="Your answer"
        />
      )}

      {multi && !marked ? (
        <p className="hint">
          More than one answer is correct. Partial credit applies, and a wrong pick costs you,
          so guessing everything scores worse than choosing carefully.
        </p>
      ) : null}

      {marked ? (
        <Feedback
          marked={marked}
          onNext={onNext}
          onSubmit={onSubmit}
          busy={busy}
        />
      ) : (
        <div className="space__actions" style={{ marginTop: 16 }}>
          <Button variant="primary" onClick={send} disabled={!answerable || busy}>
            {busy ? "Marking…" : "Answer"}
            <IconArrowRight />
          </Button>
          <span className="hint">
            You cannot change an answer once it is in. That is what makes it evidence.
          </span>
        </div>
      )}
    </section>
  );
}

/** The verdict, the teaching, and anything this answer revealed about a held belief. */
function Feedback({
  marked,
  onNext,
  onSubmit,
  busy,
}: {
  marked: Marked;
  onNext: () => void;
  onSubmit: () => void;
  busy: boolean;
}) {
  const verdict = marked.unverified
    ? { tone: "warn", label: "Not marked" }
    : marked.correct
      ? { tone: "ok", label: "Correct" }
      : marked.fraction > 0
        ? { tone: "warn", label: `Partly right · ${Math.round(marked.fraction * 100)}%` }
        : { tone: "bad", label: "Not right" };

  return (
    <div className="quiz__feedback" data-tone={verdict.tone}>
      <p className="quiz__verdict">
        <span className={`badge badge--${verdict.tone === "bad" ? "warn" : verdict.tone}`}>
          {verdict.label}
        </span>
        {marked.unverified ? (
          <span className="muted">
            Nobody can mark free-form code by comparing text, so this is recorded and left
            unmarked rather than guessed at. It counts towards neither.
          </span>
        ) : null}
      </p>

      <div className="quiz__explain">
        <Markdown>{marked.explanation}</Markdown>
      </div>

      {marked.raised.length > 0 ? (
        <div className="quiz__misconceptions">
          <p className="kicker">This answer suggests a specific belief</p>
          <ul className="misconceptions">
            {marked.raised.map((raised) => (
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
            It stays flagged until you answer something that depends on the opposite. Until then
            it holds your state on this unit down, because a held false belief is not a detail.
          </p>
        </div>
      ) : null}

      <div className="space__actions" style={{ marginTop: 16 }}>
        {marked.complete ? (
          <Button variant="primary" onClick={onSubmit} disabled={busy}>
            {busy ? "Writing your report…" : "Submit and read the report"}
            <IconArrowRight />
          </Button>
        ) : (
          <Button variant="primary" onClick={onNext} disabled={busy}>
            {busy ? "Loading…" : "Next question"}
            <IconArrowRight />
          </Button>
        )}
        <span className="muted mono">
          {marked.answeredCount} of {marked.servedCount} answered
        </span>
      </div>
    </div>
  );
}

function formatClock(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, "0")}`;
}
