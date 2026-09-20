"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button, ErrorAlert, Field } from "@aileapro/ui";
import { searchCurriculum, type SearchHit, type SearchPage } from "@/lib/api";
import { SpaceHeader } from "@/components/space-view";

type TypeFilter = "all" | "unit" | "block";

const TYPE_CHIPS: { id: TypeFilter; label: string; types?: string[] }[] = [
  { id: "all", label: "All" },
  { id: "unit", label: "Units", types: ["unit"] },
  { id: "block", label: "Blocks", types: ["block"] },
];

/**
 * Curriculum search: published units and blocks only, with honest empty states from the API.
 */
export default function SearchPage() {
  const [draft, setDraft] = useState("");
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("all");
  const [page, setPage] = useState<SearchPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const typesForFilter = useCallback((): string[] | undefined => {
    const chip = TYPE_CHIPS.find((c) => c.id === typeFilter);
    return chip?.types;
  }, [typeFilter]);

  const runSearch = useCallback(
    async (query: string, types?: string[]) => {
      setLoading(true);
      setError("");
      try {
        setPage(await searchCurriculum(query, types));
      } catch (err) {
        setPage(null);
        setError(err instanceof Error ? err.message : "Search could not run");
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      void runSearch(draft.trim(), typesForFilter());
    }, 320);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [draft, typeFilter, runSearch, typesForFilter]);

  const submit = () => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    void runSearch(draft.trim(), typesForFilter());
  };

  const note = page?.note;
  const results = page?.results ?? [];
  const showEmpty = page && results.length === 0;

  return (
    <>
      <SpaceHeader
        id="search"
        action={
          page && page.total > 0 ? (
            <span className="badge badge--ok">{page.total} results</span>
          ) : loading ? (
            <span className="badge">Searching…</span>
          ) : null
        }
      />

      {error ? <ErrorAlert message={error} /> : null}

      <section className="card">
        <div className="card__head">
          <span className="card__title">Query</span>
          {loading ? <span className="muted mono">…</span> : null}
        </div>
        <form
          className="search__form stack"
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
        >
          <Field
            label="Search the published curriculum"
            placeholder="Title, concept, or phrase from a block…"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            hint="At least two characters. Results link straight to the unit or block."
          />
          <div className="search__chips chipset" role="group" aria-label="Result types">
            {TYPE_CHIPS.map((chip) => (
              <button
                key={chip.id}
                type="button"
                className="chipbtn"
                data-checked={typeFilter === chip.id}
                aria-pressed={typeFilter === chip.id}
                onClick={() => setTypeFilter(chip.id)}
              >
                {chip.label}
              </button>
            ))}
          </div>
          <div className="space__actions">
            <Button type="submit" disabled={loading}>
              Search
            </Button>
          </div>
        </form>
      </section>

      {note ? (
        <p className="hint" style={{ marginTop: 0 }}>
          {note}
        </p>
      ) : null}

      {results.length > 0 ? (
        <ul className="search__results">
          {results.map((hit) => (
            <SearchHitRow key={`${hit.type}-${hit.id}`} hit={hit} />
          ))}
        </ul>
      ) : showEmpty ? (
        <section className="empty">
          <strong>No matches.</strong>
          <span>Try a different phrase or widen the type filter.</span>
        </section>
      ) : null}
    </>
  );
}

function SearchHitRow({ hit }: { hit: SearchHit }) {
  return (
    <li className="search__hit card">
      <div className="card__head">
        <Link className="card__title" href={hit.href} style={{ textDecoration: "none" }}>
          {hit.title}
        </Link>
        <span className="badge">{typeLabel(hit.type)}</span>
      </div>
      {hit.snippet ? <p className="prose">{hit.snippet}</p> : null}
      <p className="hint">
        <span className="badge">{hit.knowledgeClass}</span>
        {hit.blockType ? (
          <>
            {" "}
            · <span className="mono">{hit.blockType}</span>
          </>
        ) : null}
        {hit.unitSlug && hit.type === "block" ? (
          <>
            {" "}
            · in <span className="mono">{hit.unitSlug}</span>
          </>
        ) : null}
        {" · "}
        <span className="muted">{formatLocaleDate(hit.updatedAt)}</span>
      </p>
    </li>
  );
}

function typeLabel(type: SearchHit["type"]) {
  if (type === "unit") return "Unit";
  if (type === "block") return "Block";
  return "Path";
}

function formatLocaleDate(value: string | null) {
  if (!value) return "Date not recorded";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Date not recorded";
  return date.toLocaleDateString(undefined, { dateStyle: "medium" });
}
