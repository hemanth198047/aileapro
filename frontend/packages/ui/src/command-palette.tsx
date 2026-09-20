"use client";

import { KeyboardEvent, ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { IconSearch } from "./icons";

export type Command = {
  id: string;
  label: string;
  group: string;
  icon?: ReactNode;
  /** Keywords widen fuzzy matching beyond the visible label. */
  keywords?: string;
  /** Shown as a badge; use it to say when an intent becomes real. */
  hint?: string;
  run?: () => void;
};

export function CommandPalette({
  open,
  onOpenChange,
  commands,
  placeholder = "Search commands…",
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  commands: Command[];
  placeholder?: string;
}) {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const activeRef = useRef<HTMLButtonElement | null>(null);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return commands;
    return commands.filter((c) =>
      `${c.label} ${c.group} ${c.keywords ?? ""}`.toLowerCase().includes(q),
    );
  }, [commands, query]);

  useEffect(() => {
    function onKey(e: globalThis.KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        onOpenChange(!open);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onOpenChange]);

  useEffect(() => {
    if (open) {
      setQuery("");
      setActive(0);
    }
  }, [open]);

  useEffect(() => {
    setActive(0);
  }, [query]);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: "nearest" });
  }, [active]);

  if (!open) return null;

  function close() {
    onOpenChange(false);
  }

  function runAt(index: number) {
    const command = results[index];
    if (!command?.run) return;
    command.run();
    close();
  }

  function onInputKey(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Escape") {
      e.preventDefault();
      close();
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setActive((i) => (results.length ? (i + 1) % results.length : 0));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActive((i) => (results.length ? (i - 1 + results.length) % results.length : 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      runAt(active);
    }
  }

  let lastGroup = "";

  return (
    <div
      className="palette-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) close();
      }}
    >
      <div className="palette" role="dialog" aria-modal="true" aria-label="Command palette">
        <div className="palette__search">
          <IconSearch size={16} />
          <input
            autoFocus
            className="palette__input"
            placeholder={placeholder}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={onInputKey}
            aria-label="Search commands"
          />
          <span className="kbd">Esc</span>
        </div>

        <div className="palette__list">
          {results.length === 0 ? (
            <p className="palette__empty">No commands match “{query}”.</p>
          ) : (
            results.map((command, index) => {
              const showGroup = command.group !== lastGroup;
              lastGroup = command.group;
              return (
                <div key={command.id}>
                  {showGroup ? <p className="kicker palette__group">{command.group}</p> : null}
                  <button
                    ref={index === active ? activeRef : null}
                    type="button"
                    className="palette__item"
                    data-active={index === active}
                    onMouseEnter={() => setActive(index)}
                    onClick={() => runAt(index)}
                    disabled={!command.run}
                  >
                    {command.icon}
                    {command.label}
                    {command.hint ? (
                      <span className="badge palette__item-tail">{command.hint}</span>
                    ) : null}
                  </button>
                </div>
              );
            })
          )}
        </div>

        <div className="palette__foot">
          <span>
            <span className="kbd">↑</span> <span className="kbd">↓</span> to navigate
          </span>
          <span>
            <span className="kbd">Enter</span> to run
          </span>
          <span>
            <span className="kbd">Ctrl K</span> to toggle
          </span>
        </div>
      </div>
    </div>
  );
}
