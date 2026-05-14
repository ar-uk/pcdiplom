/** Merge comma/semicolon-separated tag strings; dedupe case-insensitively; preserve first-seen casing. */
export function mergeUniqueTagStrings(...chunks) {
  const seen = new Set();
  const out = [];
  for (const chunk of chunks) {
    for (const raw of String(chunk ?? "").split(/[,;]+/)) {
      const t = raw.trim();
      if (!t) continue;
      const k = t.toLowerCase();
      if (seen.has(k)) continue;
      seen.add(k);
      out.push(t);
    }
  }
  return out.join(", ");
}

export function tagsStringToList(value) {
  const seen = new Set();
  const out = [];
  for (const raw of String(value ?? "").split(/[,;]+/)) {
    const t = raw.trim();
    if (!t) continue;
    const k = t.toLowerCase();
    if (seen.has(k)) continue;
    seen.add(k);
    out.push(t);
  }
  return out;
}
