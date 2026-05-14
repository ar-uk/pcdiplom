const SESSION_KEY = "pcbuilder.session";

function hasWindow() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function loadSession() {
  if (!hasWindow()) {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  if (!hasWindow()) {
    return;
  }

  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  if (!hasWindow()) {
    return;
  }

  window.localStorage.removeItem(SESSION_KEY);
}

/** Display handle: account username only (not email). */
export function displayUsername(session) {
  if (!session || typeof session !== "object") {
    return null;
  }
  const u = typeof session.username === "string" ? session.username.trim() : "";
  return u || null;
}

/** Primary id for APIs and builds: normalized username, else email. */
export function canonicalUserId(session) {
  if (!session || typeof session !== "object") {
    return null;
  }
  const u = typeof session.username === "string" ? session.username.trim().toLowerCase() : "";
  if (u) {
    return u;
  }
  const e = typeof session.email === "string" ? session.email.trim().toLowerCase() : "";
  return e || null;
}

/** Distinct ids to query for legacy rows (e.g. builds saved under email before username migration). */
export function userIdCandidates(session) {
  if (!session || typeof session !== "object") {
    return [];
  }
  const seen = new Set();
  const out = [];
  const u = typeof session.username === "string" ? session.username.trim().toLowerCase() : "";
  const e = typeof session.email === "string" ? session.email.trim().toLowerCase() : "";
  if (u) {
    seen.add(u);
    out.push(u);
  }
  if (e && !seen.has(e)) {
    out.push(e);
  }
  return out;
}