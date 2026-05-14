import { useCallback, useEffect, useState } from "react";
import { applyThemeToDocument, getStoredTheme, setStoredTheme } from "../lib/theme.js";
import "./ThemeToggle.css";

export default function ThemeToggle({ className = "" }) {
  const [mode, setMode] = useState(() => getStoredTheme());

  useEffect(() => {
    applyThemeToDocument(mode);
  }, [mode]);

  const toggle = useCallback(() => {
    const next = mode === "dark" ? "light" : "dark";
    setStoredTheme(next);
    setMode(next);
    applyThemeToDocument(next);
  }, [mode]);

  const label = mode === "dark" ? "Switch to light theme" : "Switch to dark theme";

  return (
    <button
      type="button"
      className={`theme-toggle ${className}`.trim()}
      onClick={toggle}
      aria-label={label}
      title={label}
      role="switch"
      aria-checked={mode === "light"}
    >
      <span className="theme-toggle-track" aria-hidden>
        <span className="theme-toggle-thumb" />
      </span>
      <span className="theme-toggle-text">{mode === "dark" ? "Dark" : "Light"}</span>
    </button>
  );
}
