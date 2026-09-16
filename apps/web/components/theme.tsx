"use client";

import { useCallback, useEffect, useState } from "react";
import { IconMoon, IconSun } from "./icons";

export type Theme = "dark" | "light";

const STORAGE_KEY = "ailepro-theme";

/** Mirrors the inline script in the root layout, which sets the attribute before paint. */
export function useTheme() {
  const [theme, setTheme] = useState<Theme>("dark");

  useEffect(() => {
    const current = document.documentElement.dataset.theme;
    if (current === "light" || current === "dark") setTheme(current);
  }, []);

  const toggle = useCallback(() => {
    setTheme((prev) => {
      const next: Theme = prev === "dark" ? "light" : "dark";
      document.documentElement.dataset.theme = next;
      try {
        localStorage.setItem(STORAGE_KEY, next);
      } catch {
        // Private mode or blocked storage: the theme still applies for this session.
      }
      return next;
    });
  }, []);

  return { theme, toggle };
}

export function ThemeToggle({ theme, onToggle }: { theme: Theme; onToggle: () => void }) {
  return (
    <button
      type="button"
      className="btn btn--ghost btn--icon"
      onClick={onToggle}
      aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} theme`}
      title={`Switch to ${theme === "dark" ? "light" : "dark"} theme`}
    >
      {theme === "dark" ? <IconSun /> : <IconMoon />}
    </button>
  );
}
