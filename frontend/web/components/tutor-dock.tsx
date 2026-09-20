"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Callout, ErrorAlert } from "@aileapro/ui";
import { Markdown } from "@aileapro/ui/markdown";
import {
  ApiError,
  api,
  csrfHeaders,
  tutorTurn,
  type TutorContext,
  type TutorConversation,
  type TutorMessage,
  type TutorSources,
  type TutorStatus,
} from "@/lib/api";

/**
 * The tutor, docked beside the work rather than owning a page.
 *
 * <p>Three things on screen here are not decoration and were the hardest part of the phase to
 * get right. The banner says when a fixture is answering, because a canned reply presented as
 * teaching is the worst failure this feature could have. The sources drawer says retrieval is
 * off instead of showing an empty list, because an empty list reads as a bug. And the reply
 * streams, because a learner watching a blank panel decides the thing is broken.
 */
export function TutorDock({ slug, title }: { slug: string | null; title?: string }) {
  const [status, setStatus] = useState<TutorStatus | null>(null);
  const [context, setContext] = useState<TutorContext | null>(null);
  const [conversation, setConversation] = useState<TutorConversation | null>(null);
  const [draft, setDraft] = useState("");
  const [streaming, setStreaming] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [sources, setSources] = useState<TutorSources | null>(null);
  const [showSources, setShowSources] = useState(false);
  const [showContext, setShowContext] = useState(false);
  const scroller = useRef<HTMLDivElement | null>(null);
  const showSourcesRef = useRef(showSources);
  showSourcesRef.current = showSources;

  useEffect(() => {
    let live = true;
    void (async () => {
      try {
        const [next, ctx] = await Promise.all([
          api<TutorStatus>("/v1/tutor/status"),
          api<TutorContext>(`/v1/tutor/context${slug ? `?unit=${encodeURIComponent(slug)}` : ""}`),
        ]);
        if (!live) return;
        setStatus(next);
        setContext(ctx);
      } catch (err) {
        if (live) setError(err instanceof Error ? err.message : "Could not reach the tutor");
      }
    })();
    return () => {
      live = false;
    };
  }, [slug]);

  // A new unit is a new conversation. Carrying the old one across would have the tutor
  // answering about the last page using this page's context, which is worse than starting over.
  useEffect(() => {
    setConversation(null);
    setStreaming("");
    setSources(null);
    setShowSources(false);
  }, [slug]);

  useEffect(() => {
    const node = scroller.current;
    if (node) node.scrollTop = node.scrollHeight;
  }, [conversation?.messages.length, streaming]);

  const ensureConversation = useCallback(async (): Promise<TutorConversation> => {
    if (conversation) return conversation;
    const started = await api<TutorConversation>("/v1/tutor/conversations", {
      method: "POST",
      headers: await csrfHeaders(),
      body: JSON.stringify({ unitSlug: slug, persona: status?.personas[0]?.slug ?? "patient_teacher" }),
    });
    setConversation(started);
    return started;
  }, [conversation, slug, status]);

  const send = async (body: { text?: string; action?: string }) => {
    if (busy) return;
    setBusy(true);
    setError("");
    setStreaming("");

    const asked: TutorMessage = {
      id: `pending-${Date.now()}`,
      role: "user",
      content: body.text ?? actionLabel(status, body.action),
      action: body.action ?? null,
      development: false,
      refusalCode: null,
      sources: [],
      model: null,
      promptVersion: null,
      createdAt: new Date().toISOString(),
    };

    try {
      const active = await ensureConversation();
      setConversation({ ...active, messages: [...active.messages, asked] });
      setDraft("");

      let text = "";
      let failed = "";
      await tutorTurn(active.id, body, (event) => {
        if (event.type === "token") {
          text += event.text;
          setStreaming(text);
        }
        if (event.type === "error") failed = event.message;
      });

      if (failed) {
        // The question stays on screen with the failure under it. Clearing it would leave the
        // learner retyping something they can see they already asked.
        setError(failed);
        setStreaming("");
        return;
      }

      // Reloaded rather than assembled locally, so what is shown is what was stored — including
      // the development flag, the refusal code, and which model wrote it.
      setConversation(await api<TutorConversation>(`/v1/tutor/conversations/${active.id}`));
      setStreaming("");
      setStatus(await api<TutorStatus>("/v1/tutor/status"));
      setSources(null);
      if (showSourcesRef.current) {
        try {
          setSources(await api<TutorSources>(`/v1/tutor/conversations/${active.id}/sources`));
        } catch {
          // Same as openSources: a failed refresh should not cover the answer.
        }
      }
    } catch (err) {
      const message =
        err instanceof ApiError && err.code === "budget_exceeded"
          ? err.message
          : err instanceof Error
            ? err.message
            : "The tutor could not answer";
      setError(message);
      setStreaming("");
    } finally {
      setBusy(false);
    }
  };

  const openSources = async () => {
    setShowSources((open) => !open);
    if (sources || !conversation) return;
    try {
      setSources(await api<TutorSources>(`/v1/tutor/conversations/${conversation.id}/sources`));
    } catch {
      // The drawer is ambient. Failing to load it should not put an error over the answer.
    }
  };

  const messages = conversation?.messages ?? [];

  return (
    <>
      <div className="card__head">
        <span className="card__title">Tutor</span>
        {status?.development ? (
          <span className="badge badge--warn">Fixtures</span>
        ) : status?.modelSlug ? (
          <span className="badge badge--brand">{status.modelSlug}</span>
        ) : null}
      </div>

      {status && !status.available ? (
        <Callout>
          No model is configured, so the tutor cannot answer. The unit, the practice set, and
          your reviews do not need one.
        </Callout>
      ) : null}

      {status?.development ? (
        <Callout>
          <strong>Development implementation.</strong> No model is configured, so answers come
          from fixtures. They are written to exercise the teaching rules — hints before answers,
          refusals, streaming — and they are not teaching. Every reply below is marked.
        </Callout>
      ) : null}

      {context?.unitSlug ? (
        <div className="tutor__context">
          <button
            type="button"
            className="tutor__contextToggle"
            onClick={() => setShowContext((open) => !open)}
            aria-expanded={showContext}
          >
            What it knows about you here
          </button>
          {showContext ? (
            <div className="tutor__contextBody">
              <p className="hint">
                On <strong>{context.unitTitle}</strong> your recorded state is{" "}
                <strong>{context.masteryState}</strong>
                {context.readSolid ? ", and the page is marked read." : ", and the page is not marked read yet."}
              </p>
              {context.openMisconceptions.length > 0 ? (
                <p className="hint">
                  Open misconceptions it will work on first:{" "}
                  {context.openMisconceptions.join("; ")}
                </p>
              ) : null}
              {context.recentMistakes.length > 0 ? (
                <ul className="tutor__mistakes">
                  {context.recentMistakes.map((mistake) => (
                    <li key={mistake}>{mistake}</li>
                  ))}
                </ul>
              ) : null}
              <p className="hint">
                {status?.retrieval
                  ? "It reads this page, your record on it, and can cite published curriculum blocks it retrieves — nothing from anyone else."
                  : "It reads this page and your record on it. Nothing from anyone else, and no search — so it has no sources to cite."}
              </p>
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="tutor__thread" ref={scroller} aria-live="polite">
        {messages.length === 0 && !streaming ? (
          <div className="empty" style={{ padding: 16 }}>
            <strong>{slug ? `Ask about ${title ?? slug}.` : "Open a unit to give it context."}</strong>
            <span>
              It answers with a hint first and a full explanation once you have had a go. Asking
              for the answers to something you are being graded on gets you a refusal and an
              offer to practise instead.
            </span>
          </div>
        ) : null}

        {messages.map((message) => (
          <div
            key={message.id}
            className="tutor__turn"
            data-role={message.role}
            data-refusal={message.refusalCode ? "true" : undefined}
          >
            <div className="tutor__who">
              <span>{message.role === "user" ? "You" : "Tutor"}</span>
              {message.development ? <span className="badge badge--warn">Fixture</span> : null}
              {message.refusalCode ? <span className="badge">Declined</span> : null}
              {message.model && !message.development ? (
                <span className="badge">{message.model}</span>
              ) : null}
            </div>
            <div className="tutor__body">
              <Markdown>{message.content}</Markdown>
            </div>
          </div>
        ))}

        {streaming ? (
          <div className="tutor__turn" data-role="assistant" data-streaming="true">
            <div className="tutor__who">
              <span>Tutor</span>
              <span className="badge">Writing…</span>
            </div>
            <div className="tutor__body">
              <Markdown>{streaming}</Markdown>
            </div>
          </div>
        ) : null}
      </div>

      <ErrorAlert message={error} />

      <div className="tutor__actions chipset">
        {(status?.actions ?? []).map((action) => (
          <button
            key={action.slug}
            type="button"
            className="chipbtn"
            title={action.hint}
            disabled={busy || !status?.available}
            onClick={() => void send({ action: action.slug })}
          >
            {action.label}
          </button>
        ))}
      </div>

      <form
        className="tutor__composer"
        onSubmit={(event) => {
          event.preventDefault();
          const text = draft.trim();
          if (text) void send({ text });
        }}
      >
        <label className="sr-only" htmlFor="tutor-draft">
          Ask the tutor
        </label>
        <textarea
          id="tutor-draft"
          className="input tutor__input"
          rows={3}
          value={draft}
          placeholder={slug ? "Ask about this page, or paste what you tried…" : "Ask a question…"}
          disabled={busy || !status?.available}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            // Enter sends, shift+enter breaks the line. A tutor question is usually one line,
            // and the one case that needs several is pasted code, which shift+enter covers.
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              const text = draft.trim();
              if (text) void send({ text });
            }
          }}
        />
        <div className="tutor__composerRow">
          <Button type="submit" disabled={busy || !draft.trim() || !status?.available}>
            {busy ? "Thinking…" : "Ask"}
          </Button>
          {conversation ? (
            <Button variant="ghost" type="button" onClick={() => void openSources()}>
              Sources
            </Button>
          ) : null}
          {status ? (
            <span className="hint tutor__budget">
              {status.turnsUsed}/{status.turnsCap} turns today
              {status.usdCap > 0 ? ` · $${status.usdSpent.toFixed(4)} of $${status.usdCap.toFixed(2)}` : ""}
            </span>
          ) : null}
        </div>
      </form>

      {showSources ? (
        <div className="tutor__sources">
          <div className="card__head">
            <span className="card__title">Sources</span>
            <span className="badge">{sources?.retrieval ? "Retrieval on" : "Retrieval off"}</span>
          </div>
          <p className="hint">{sources?.note ?? "Loading what this conversation was grounded in…"}</p>
          {sources && sources.blocks.length === 0 ? (
            <p className="hint">
              Nothing is cited, and that is the honest answer rather than a missing list. The
              tutor is told not to invent a source it cannot point to.
            </p>
          ) : null}
          {sources && sources.blocks.length > 0 ? (
            <ul className="tutor__sourceBlocks">
              {sources.blocks.map((block) => (
                <li key={block.blockId} className="tutor__sourceBlock">
                  <Link className="inline-link" href={block.href}>
                    {block.unitTitle}
                  </Link>
                  <p className="hint">
                    <span className="mono">{block.blockType}</span>
                    {" · "}
                    <span className="badge">{block.knowledgeClass}</span>
                    {" · "}
                    Updated {formatLocaleDate(block.updatedAt)}
                  </p>
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}
    </>
  );
}

function actionLabel(status: TutorStatus | null, slug?: string): string {
  if (!slug) return "";
  return status?.actions.find((action) => action.slug === slug)?.label ?? slug;
}

function formatLocaleDate(value: string | null) {
  if (!value) return "date not recorded";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "date not recorded";
  return date.toLocaleDateString(undefined, { dateStyle: "medium" });
}
