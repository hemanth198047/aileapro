"use client";

import { useEffect, useMemo, useState } from "react";
import {
  IconChart,
  IconCompass,
  IconLab,
  IconLogout,
  IconPath,
  IconSearch,
  IconSpark,
  IconSun,
} from "@/components/icons";

type Command = {
  id: string;
  label: string;
  group: string;
  icon: React.ReactNode;
  hint?: string;
  run?: () => void;
};

export function CommandPalette({
  open,
  onOpenChange,
  onToggleTheme,
  onSignOut,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onToggleTheme: () => void;
  onSignOut: () => void;
}) {
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);

  const commands = useMemo<Command[]>(
    () => [
      { id: "today", label: "Go to Today", group: "Navigate", icon: <IconCompass size={15} />, run: () => {} },
      { id: "path", label: "Open Path", group: "Navigate", icon: <IconPath size={15} />, hint: "Phase 3" },
      { id: "lab", label: "Open Lab", group: "Navigate", icon: <IconLab size={15} />, hint: "Phase 5" },
      { id: "tutor", label: "Ask the tutor", group: "Navigate", icon: <IconSpark size={15} />, hint: "Phase 7" },
      { id: "mastery", label: "View mastery map", group: "Navigate", icon: <IconChart size={15} />, hint: "Phase 4" },
      { id: "theme", label: "Toggle theme", group: "Preferences", icon: <IconSun size={15} />, run: onToggleTheme },
      { id: "signout", label: "Sign out", group: "Account", icon: <IconLogout size={15} />, run: onSignOut },
    ],
    [onToggleTheme, onSignOut],
  );

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? commands.filter((c) => c.label.toLowerCase().includes(q)) : commands;
  }, [commands, query]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
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

  function onInputKey(e: React.KeyboardEvent<HTMLInputElement>) {
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
            placeholder="Search commands…"
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
