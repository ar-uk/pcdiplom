const THEME_KEY = "pcbuilder.theme";

/** @returns {"dark" | "light"} */
export function getStoredTheme() {
  if (typeof window === "undefined" || !window.localStorage) {
    return "dark";
  }
  return window.localStorage.getItem(THEME_KEY) === "light" ? "light" : "dark";
}

/** @param {"dark" | "light"} mode */
export function setStoredTheme(mode) {
  if (typeof window === "undefined" || !window.localStorage) {
    return;
  }
  if (mode === "light") {
    window.localStorage.setItem(THEME_KEY, "light");
  } else {
    window.localStorage.removeItem(THEME_KEY);
  }
}

/** Apply to document root (omit attribute for dark). */
export function applyThemeToDocument(mode) {
  if (typeof document === "undefined") {
    return;
  }
  const root = document.documentElement;
  if (mode === "light") {
    root.dataset.theme = "light";
  } else {
    delete root.dataset.theme;
  }
}

export function initThemeFromStorage() {
  applyThemeToDocument(getStoredTheme());
}
