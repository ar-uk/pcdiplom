import { useEffect, useMemo, useState } from "react";
import { mergeUniqueTagStrings } from "../lib/tagUtils.js";
import "./TagPickerModal.css";

/** Curated chips aligned with Stitch discover hardware / intent tags */
export const TAG_SUGGESTIONS = [
  "RTX 4090",
  "RTX 4080",
  "Ryzen 9",
  "Ryzen 7",
  "Intel Core i9",
  "RGB Sync",
  "Liquid Cooling",
  "4K Gaming",
  "1440p",
  "Budget",
  "SFF",
  "Workstation",
  "Gaming",
  "Mini ITX",
  "White build",
  "DDR5",
  "Air cooled",
];

function normalizeTagList(tags) {
  return (tags || []).map((t) => String(t).trim()).filter(Boolean);
}

export default function TagPickerModal({ open, onClose, selectedTags, onApply, extraTags = [] }) {
  const [draft, setDraft] = useState(() => new Set(normalizeTagList(selectedTags)));
  const [typedTag, setTypedTag] = useState("");

  const allOptions = useMemo(() => {
    const merged = [...TAG_SUGGESTIONS, ...normalizeTagList(extraTags)];
    return Array.from(new Map(merged.map((t) => [t.toLowerCase(), t])).values()).sort((a, b) =>
      a.localeCompare(b),
    );
  }, [extraTags]);

  const chipList = useMemo(() => {
    const extrasFromDraft = [...draft].filter((t) => !allOptions.some((o) => o.toLowerCase() === t.toLowerCase()));
    return [...extrasFromDraft, ...allOptions];
  }, [draft, allOptions]);

  useEffect(() => {
    if (open) {
      setDraft(new Set(normalizeTagList(selectedTags)));
      setTypedTag("");
    }
  }, [open, selectedTags]);

  if (!open) {
    return null;
  }

  const toggle = (tag) => {
    setDraft((prev) => {
      const next = new Set(prev);
      if (next.has(tag)) {
        next.delete(tag);
      } else {
        next.add(tag);
      }
      return next;
    });
  };

  const handleApply = () => {
    onApply([...draft]);
    onClose();
  };

  const addTypedTags = () => {
    const merged = mergeUniqueTagStrings([...draft].join(", "), typedTag);
    setDraft(new Set(normalizeTagList(merged.split(","))));
    setTypedTag("");
  };

  return (
    <div className="tag-picker-backdrop" role="presentation" onClick={onClose}>
      <div
        className="tag-picker-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tag-picker-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="tag-picker-head">
          <h2 id="tag-picker-title">Tags</h2>
          <button type="button" className="tag-picker-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <p className="tag-picker-hint">Tap suggested tags, add your own below, then Apply. You can also type tags in the post form.</p>
        <div className="tag-picker-chips">
          {chipList.map((tag) => (
            <button
              key={tag}
              type="button"
              className={draft.has(tag) ? "tag-chip tag-chip-active" : "tag-chip"}
              onClick={() => toggle(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
        <div className="tag-picker-custom">
          <label className="tag-picker-custom-label" htmlFor="tag-picker-custom-input">
            Custom tags
          </label>
          <div className="tag-picker-custom-row">
            <input
              id="tag-picker-custom-input"
              type="text"
              value={typedTag}
              onChange={(event) => setTypedTag(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  addTypedTags();
                }
              }}
              placeholder="Type a tag (comma-separated for several)"
              autoComplete="off"
            />
            <button type="button" className="tag-picker-add-custom" onClick={addTypedTags}>
              Add
            </button>
          </div>
        </div>
        <div className="tag-picker-actions">
          <button type="button" className="tag-picker-ghost" onClick={() => setDraft(new Set())}>
            Clear all
          </button>
          <button type="button" className="tag-picker-primary" onClick={handleApply}>
            Apply
          </button>
        </div>
      </div>
    </div>
  );
}
