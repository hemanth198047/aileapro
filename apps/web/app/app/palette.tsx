"use client";

import { useEffect, useState } from "react";

export function CommandPalette({
  onGoToday,
  onToggleTheme,
}: {
  onGoToday: () => void;
  onToggleTheme: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setOpen((v) => !v);
      }
      if (e.key === "Escape") setOpen(false);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  if (!open) return null;

  const items = [
    { id: "today", label: "Go to Today", run: onGoToday },
    { id: "theme", label: "Toggle theme", run: onToggleTheme },
  ].filter((i) => i.label.toLowerCase().includes(q.toLowerCase()));

  return (
    <div className="palette" role="dialog" aria-label="Command palette">
      <input
        autoFocus
        placeholder="Search commands…"
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          onClick={() => {
            item.run();
            setOpen(false);
          }}
        >
          {item.label}
        </button>
      ))}
    </div>
  );
}
