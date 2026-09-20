"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Callout, ErrorAlert, IconArrowRight, IconClose, IconNotes } from "@aileapro/ui";
import { api, csrfHeaders, type Note } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";
import { formatStamp } from "@/components/notes-panel";

/**
 * Everything the learner has written, newest first, grouped by the unit that prompted it.
 *
 * <p>Writing happens where the reading happens — in the companion pane on a unit — so this
 * page is for finding things again rather than composing. Notes are the learner's own words
 * and are never treated as evidence of mastery.
 */
export default function NotesPage() {
  const [notes, setNotes] = useState<Note[] | null>(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setNotes(await api<Note[]>("/v1/me/notes"));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load your notes");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const groups = useMemo(() => {
    if (!notes) return [];
    const byUnit = new Map<string, { title: string; slug: string | null; notes: Note[] }>();
    for (const note of notes) {
      // A note whose unit was unpublished keeps its text and loses its link, rather than
      // disappearing because the curriculum moved.
      const key = note.unitId ?? "unattached";
      const existing = byUnit.get(key);
      if (existing) {
        existing.notes.push(note);
        continue;
      }
      byUnit.set(key, {
        title: note.unitTitle ?? "No longer published",
        slug: note.unitSlug,
        notes: [note],
      });
    }
    return [...byUnit.values()];
  }, [notes]);

  const remove = async (note: Note) => {
    if (!note.unitSlug) return;
    setError("");
    try {
      await api(`/v1/units/${note.unitSlug}/notes/${note.id}`, {
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
      <SpaceHeader
        id="notes"
        action={notes ? <span className="badge badge--ok">{notes.length} notes</span> : null}
      />

      {error ? <ErrorAlert message={error} /> : null}

      {notes === null ? (
        <section className="card">
          <p className="muted">Loading your notes…</p>
        </section>
      ) : notes.length === 0 ? (
        <section className="card">
          <div className="card__head">
            <span className="card__title">Nothing written yet</span>
          </div>
          <Callout icon={<IconNotes />}>
            Notes are written next to the reading: open a unit and use the Notes tab in the
            companion pane on the right.
          </Callout>
          <div className="space__actions" style={{ marginTop: 16 }}>
            <Link className="btn btn--primary" href="/app/learn">
              Open the catalog
              <IconArrowRight />
            </Link>
          </div>
          <p className="hint" style={{ marginTop: 12 }}>
            Linking notes to each other and to blocks, and searching them alongside the
            curriculum, comes with the search engine in Phase 8.
          </p>
        </section>
      ) : (
        groups.map((group) => (
          <section className="card" key={group.slug ?? group.title}>
            <div className="card__head">
              {group.slug ? (
                <Link className="card__title" href={`/app/learn/${group.slug}`}>
                  {group.title}
                </Link>
              ) : (
                <span className="card__title">{group.title}</span>
              )}
              <span className="muted mono">{group.notes.length}</span>
            </div>
            <ul className="notelist">
              {group.notes.map((note) => (
                <li className="note" key={note.id}>
                  <p className="note__body">{note.body}</p>
                  <div className="note__foot">
                    <span className="muted mono">{formatStamp(note.createdAt)}</span>
                    {note.unitSlug ? (
                      <Button variant="ghost" onClick={() => remove(note)}>
                        <IconClose size={14} />
                        <span className="sr-only">Delete note</span>
                      </Button>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          </section>
        ))
      )}
    </>
  );
}
