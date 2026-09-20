"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { Button, ErrorAlert, IconClose } from "@aileapro/ui";
import { api, csrfHeaders, type Note } from "@/lib/api";

/**
 * Notes for one unit, docked in the companion pane.
 *
 * <p>Notes v0 is plain text stored verbatim: write, list, delete. No linking and no
 * formatting toolbar, because a half-built rich editor is worse than a textarea. The panel
 * only appears against a unit — a note with nothing to attach to is how note piles start.
 */
export function NotesPanel({ slug, title }: { slug: string | null; title?: string }) {
  const [notes, setNotes] = useState<Note[] | null>(null);
  const [body, setBody] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!slug) {
      setNotes(null);
      return;
    }
    try {
      setNotes(await api<Note[]>(`/v1/units/${slug}/notes`));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load your notes");
    }
  }, [slug]);

  useEffect(() => {
    setError("");
    void load();
  }, [load]);

  if (!slug) {
    return (
      <>
        <div className="card__head">
          <span className="card__title">Notes</span>
          <span className="badge">Per unit</span>
        </div>
        <p className="hint">
          Notes attach to the unit you are reading, so they stay findable later. Open a unit and
          this pane becomes a notepad for it.
        </p>
        <div className="empty" style={{ padding: 16 }}>
          <strong>No unit open.</strong>
          <span>
            Everything you have written is listed in <Link href="/app/notes">Notes</Link>.
          </span>
        </div>
      </>
    );
  }

  const save = async () => {
    const trimmed = body.trim();
    if (!trimmed) return;
    setBusy(true);
    setError("");
    try {
      await api<Note>(`/v1/units/${slug}/notes`, {
        method: "POST",
        headers: await csrfHeaders(),
        body: JSON.stringify({ body: trimmed }),
      });
      setBody("");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save the note");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: string) => {
    setError("");
    try {
      await api(`/v1/units/${slug}/notes/${id}`, {
        method: "DELETE",
        headers: await csrfHeaders(),
      });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete the note");
    }
  };

  return (
    <>
      <div className="card__head">
        <span className="card__title">Notes</span>
        <span className="muted mono">{notes?.length ?? 0} on this unit</span>
      </div>

      {error ? <ErrorAlert message={error} /> : null}

      <textarea
        className="input notes__input"
        rows={4}
        value={body}
        placeholder={title ? `A note on ${title}…` : "A note on this unit…"}
        onChange={(event) => setBody(event.target.value)}
        onKeyDown={(event) => {
          // Ctrl/Cmd+Enter saves, so a thought can be captured without leaving the keyboard.
          if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
            event.preventDefault();
            void save();
          }
        }}
      />
      <div className="notes__actions">
        <Button variant="primary" onClick={save} disabled={busy || body.trim() === ""}>
          {busy ? "Saving…" : "Save note"}
        </Button>
        <span className="hint mono">Ctrl + Enter</span>
      </div>

      {notes === null ? (
        <p className="muted">Loading…</p>
      ) : notes.length === 0 ? (
        <div className="empty" style={{ padding: 16 }}>
          <strong>Nothing written here yet.</strong>
          <span>Notes are yours alone and are never used as evidence of mastery.</span>
        </div>
      ) : (
        <ul className="notelist">
          {notes.map((note) => (
            <li className="note" key={note.id}>
              <p className="note__body">{note.body}</p>
              <div className="note__foot">
                <span className="muted mono">{formatStamp(note.createdAt)}</span>
                <Button variant="ghost" onClick={() => remove(note.id)}>
                  <IconClose size={14} />
                  <span className="sr-only">Delete note</span>
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export function formatStamp(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toISOString().slice(0, 16).replace("T", " ");
}
