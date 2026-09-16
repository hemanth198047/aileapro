"use client";

import { useCallback, useSyncExternalStore } from "react";
import { IconMoon, IconSun } from "./icons";
import { IconButton } from "./button";

export type Theme = "dark" | "light";

const STORAGE_KEY = "aileapro-theme";

/**
 * The attribute on <html> is the single source of truth, set by the inline
 * script in the root layout before first paint. Reading it through an external
 * store keeps every toggle on the page in agreement.
 */
function subscribe(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["data-theme"],
  });
  return () => observer.disconnect();
}

function readTheme(): Theme {
  return document.documentElement.dataset.theme === "light" ? "light" : "dark";
}

export function useTheme() {
  const theme = useSyncExternalStore(subscribe, readTheme, () => "dark" as Theme);

  const toggle = useCallback(() => {
    const next: Theme = readTheme() === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Private mode or blocked storage: the theme still applies for this session.
    }
  }, []);

  return { theme, toggle };
}

export function ThemeToggle({ theme, onToggle }: { theme: Theme; onToggle: () => void }) {
  const label = `Switch to ${theme === "dark" ? "light" : "dark"} theme`;
  return (
    <IconButton label={label} onClick={onToggle} icon={theme === "dark" ? <IconSun /> : <IconMoon />} />
  );
}
