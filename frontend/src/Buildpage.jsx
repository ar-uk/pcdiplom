import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./styles/pages/builder.css";

const CATEGORIES = [
  { key: "cpu", label: "CPU", endpoint: "/api/parsed/cpu" },
  { key: "gpu", label: "GPU", endpoint: "/api/parsed/video-card" },
  { key: "motherboard", label: "Motherboard", endpoint: "/api/parsed/motherboard" },
  { key: "psu", label: "PSU", endpoint: "/api/parsed/power-supply" },
  { key: "storage", label: "Storage", endpoint: "/api/parsed/internal-hard-drive" },
  { key: "ram", label: "RAM", endpoint: "/api/parsed/memory" },
  { key: "case", label: "Case", endpoint: "/api/parsed/pc-case" },
  { key: "cooling", label: "Cooling", endpoint: "/api/parsed/cpu-cooler" },
];

const SOURCE_ENDPOINTS = {
  cpu: "/api/reference/matched-cpu",
  gpu: "/api/reference/matched-gpu",
  motherboard: "/api/reference/matched-motherboard",
};

const STORAGE_SLOTS = ["storage_1", "storage_2", "storage_3"];

const SLOT_ORDER = [
  { key: "cpu", label: "CPU" },
  { key: "motherboard", label: "Motherboard" },
  { key: "gpu", label: "GPU" },
  { key: "ram", label: "RAM" },
  { key: "storage_1", label: "Storage 1" },
  { key: "storage_2", label: "Storage 2" },
  { key: "storage_3", label: "Storage 3" },
  { key: "psu", label: "PSU" },
  { key: "case", label: "Case" },
  { key: "cooling", label: "Cooling" },
];

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "No price";
  }
  return `${Number(value).toLocaleString("en-US")} KZT`;
}

function extractDdrFromText(text) {
  const normalized = String(text ?? "").toUpperCase().replace(/[\s_-]+/g, "");

  if (/\bDDR5\b/.test(normalized) || normalized.includes("DDR5") || normalized.includes("D5")) {
    return "DDR5";
  }

  if (/\bDDR4\b/.test(normalized) || normalized.includes("DDR4") || normalized.includes("D4")) {
    return "DDR4";
  }

  if (/\bDDR3\b/.test(normalized) || normalized.includes("DDR3") || normalized.includes("D3")) {
    return "DDR3";
  }

  if (/\bDDR2\b/.test(normalized) || normalized.includes("DDR2") || normalized.includes("D2")) {
    return "DDR2";
  }

  return null;
}

async function fetchParsedRows(endpoint, pageSize = 200, maxPages = 10, headers = {}) {
  const rows = [];

  for (let page = 0; page < maxPages; page += 1) {
    const response = await fetch(`${endpoint}?page=${page}&size=${pageSize}`, {
      headers,
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch ${endpoint}`);
    }

    const data = await response.json();
    const pageRows = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : [];
    if (pageRows.length === 0) {
      break;
    }

    rows.push(...pageRows);
    if (pageRows.length < pageSize) {
      break;
    }
  }

  return rows;
}

function normalizePart(row, category, sourceType = "parsed") {
  const price = Number(row?.price_kzt ?? row?.priceKzt ?? row?.price ?? row?.priceKZT ?? 0);
  const socket = row?.socket ?? row?.cpuSocket ?? row?.socket_type ?? null;
  const memoryType = row?.memoryType ?? row?.memoryRamType ?? row?.memory_type ?? row?.memory_ram_type ?? row?.ddr ?? row?.ddrType ?? null;
  const wattage = row?.wattage ?? row?.tdp ?? row?.tdpWatts ?? null;
  const name = row?.name ?? "";
  const upperName = name.toUpperCase();
  const compactName = upperName.replace(/\s+/g, "");
  const inferredRamType = extractDdrFromText(name);
  const inferredSocket =
    compactName.includes("AM5")
      ? "AM5"
      : compactName.includes("AM4")
        ? "AM4"
        : compactName.includes("LGA1851")
          ? "LGA1851"
          : compactName.includes("LGA1700")
            ? "LGA1700"
            : compactName.includes("LGA1200")
              ? "LGA1200"
              : compactName.includes("LGA1151")
                ? "LGA1151"
                : null;
  const formFactor =
    upperName.includes("MINI-ITX") || upperName.includes("MINI ITX") || upperName.includes("ITX")
      ? "ITX"
      : upperName.includes("M-ATX") || upperName.includes("MATX") || upperName.includes("MICRO-ATX") || upperName.includes("MICRO ATX")
        ? "mATX"
        : upperName.includes("ATX")
          ? "ATX"
          : null;
  const supportedSockets = ["AM4", "AM5", "LGA1200", "LGA1700", "LGA1851", "LGA1151"].filter((candidate) => compactName.includes(candidate));

  return {
    id: row?.id ?? `${category}-${Math.random().toString(36).slice(2)}`,
    category,
    name,
    price,
    retailer: row?.retailer ?? "",
    currency: row?.currency ?? "KZT",
    url: row?.url ?? row?.sourceLink ?? "#",
    socket: socket ?? (category === "cpu" || category === "motherboard" || category === "cooling" ? inferredSocket : null),
    ramType: category === "ram"
      ? inferredRamType ?? (memoryType ? (String(memoryType).toUpperCase().startsWith("DDR") ? String(memoryType).toUpperCase() : `DDR${memoryType}`) : null)
      : memoryType
        ? String(memoryType).toUpperCase().startsWith("DDR")
          ? String(memoryType).toUpperCase()
          : `DDR${memoryType}`
        : null,
    formFactor,
    supportedSockets,
    wattage: wattage != null ? Number(wattage) : null,
    sourceType,
    referenceMatchedId: sourceType === "reference-matched" ? row?.id ?? null : null,
    parsedPartId: row?.parsedPartId ?? null,
    opendbId: row?.opendbId ?? null,
    sourceLink: row?.sourceLink ?? null,
    raw: row,
  };
}

function buildEstimatedPower(selectedParts) {
  let total = 0;

  if (selectedParts.cpu) total += selectedParts.cpu.wattage ?? 65;
  if (selectedParts.gpu) total += selectedParts.gpu.wattage ?? 200;
  if (selectedParts.motherboard) total += 50;
  if (selectedParts.ram) total += 5;
  total += STORAGE_SLOTS.filter((key) => selectedParts[key]).length * 10;
  if (selectedParts.cooling) total += 10;

  return total;
}

function getPowerBudget(selectedParts) {
  const cpuPower = selectedParts.cpu?.wattage ?? 65;
  const gpuPower = selectedParts.gpu?.wattage ?? 200;
  const overhead = 150;

  return cpuPower + gpuPower + overhead;
}

function isPartCompatible(part, selectedParts) {
  const cpu = selectedParts.cpu || null;
  const motherboard = selectedParts.motherboard || null;
  const ram = selectedParts.ram || null;
  const casePart = selectedParts.case || null;
  const powerBudget = getPowerBudget(selectedParts);

  if (part.category === "cpu") {
    return !motherboard?.socket || !part.socket || motherboard.socket === part.socket;
  }

  if (part.category === "motherboard") {
    if (cpu?.socket && part.socket && cpu.socket !== part.socket) {
      return false;
    }

    if (ram?.ramType && part.ramType && ram.ramType !== part.ramType) {
      return false;
    }

    return true;
  }

  if (part.category === "ram") {
    return !motherboard?.ramType || !part.ramType || motherboard.ramType === part.ramType;
  }

  if (part.category === "psu") {
    return !part.wattage || part.wattage >= powerBudget;
  }

  if (part.category === "case") {
    if (!motherboard?.formFactor || !part.formFactor) {
      return true;
    }

    const allowed = {
      ATX: ["ATX", "mATX", "ITX"],
      mATX: ["mATX", "ITX"],
      ITX: ["ITX"],
    };

    return allowed[part.formFactor]?.includes(motherboard.formFactor) ?? true;
  }

  if (part.category === "cooling") {
    if (!cpu?.socket) {
      return true;
    }

    if (Array.isArray(part.supportedSockets) && part.supportedSockets.length > 0) {
      return part.supportedSockets.includes(cpu.socket);
    }

    return !part.socket || part.socket === cpu.socket || String(part.name ?? "").toUpperCase().includes(cpu.socket);
  }

  if (part.category === "gpu") {
    if (casePart?.formFactor === "ITX" && (part.wattage ?? 0) >= 320) {
      return false;
    }
  }

  return true;
}

function formatSocketLabel(socket) {
  if (!socket) {
    return null;
  }

  return socket.replace(/\s+/g, "").toUpperCase();
}

function buildFilterTags(selectedParts) {
  const tags = [];
  const cpuSocket = formatSocketLabel(selectedParts.cpu?.socket);
  const motherboardSocket = formatSocketLabel(selectedParts.motherboard?.socket);
  const ramType = selectedParts.motherboard?.ramType ?? selectedParts.ram?.ramType;
  const boardFormFactor = selectedParts.motherboard?.formFactor;
  const powerBudget = getPowerBudget(selectedParts);
  const cpuTdp = selectedParts.cpu?.wattage ?? selectedParts.cpu?.powerHint ?? null;
  const gpuTdp = selectedParts.gpu?.wattage ?? selectedParts.gpu?.powerHint ?? null;

  if (cpuSocket) {
    tags.push(`Showing ${cpuSocket} Board`);
  }

  if (motherboardSocket) {
    tags.push(`Showing ${motherboardSocket} CPUs`);
  }

  if (ramType) {
    tags.push(`Showing ${ramType} RAM`);
  }

  if (boardFormFactor) {
    tags.push(`Case filter: ${boardFormFactor} fit`);
  }

  if (cpuTdp || gpuTdp) {
    tags.push(`Power filter: PSU at least ${powerBudget}W`);
  }

  if (cpuTdp) {
    tags.push(`CPU TDP ${cpuTdp}W`);
  }

  if (gpuTdp) {
    tags.push(`GPU TDP ${gpuTdp}W`);
  }

  return tags;
}

export default function BuilderPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [session] = useState(() => loadSession());
  const [partsByCategory, setPartsByCategory] = useState({
    cpu: [],
    gpu: [],
    motherboard: [],
    psu: [],
    storage: [],
    ram: [],
    case: [],
    cooling: [],
  });
  const [selectedParts, setSelectedParts] = useState({});
  const [activeCategory, setActiveCategory] = useState("cpu");
  const [activeStorageSlot, setActiveStorageSlot] = useState("storage_1");
  const [searchText, setSearchText] = useState("");
  const [priceOrder, setPriceOrder] = useState("asc");
  const [buildTitle, setBuildTitle] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const userId = session?.email?.trim().toLowerCase() ?? null;
  const editingBuildId = Number.isFinite(Number(location.state?.editBuild?.id)) ? Number(location.state.editBuild.id) : null;

  useEffect(() => {
    let alive = true;

    async function loadParts() {
      setLoading(true);
      setLoadError("");

      try {
        const loaded = await Promise.all(
          CATEGORIES.map(async (category) => {
            try {
              const endpoint = SOURCE_ENDPOINTS[category.key] ?? category.endpoint;
              const headers = SOURCE_ENDPOINTS[category.key] && session?.token ? { Authorization: `Bearer ${session.token}` } : {};
              const rows = await fetchParsedRows(endpoint, 200, 10, headers);
              const sourceType = SOURCE_ENDPOINTS[category.key] ? "reference-matched" : "parsed";
              return [category.key, rows.map((row) => normalizePart(row, category.key, sourceType))];
            } catch {
              if (SOURCE_ENDPOINTS[category.key]) {
                const fallbackRows = await fetchParsedRows(category.endpoint, 200, 10);
                return [category.key, fallbackRows.map((row) => normalizePart(row, category.key, "parsed"))];
              }

              return [category.key, []];
            }
          })
        );

        if (!alive) {
          return;
        }

        setPartsByCategory(Object.fromEntries(loaded));
      } catch {
        if (!alive) {
          return;
        }
        setLoadError("Could not load parts.");
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    loadParts();

    return () => {
      alive = false;
    };
  }, [session?.token]);

  useEffect(() => {
    const editBuild = location.state?.editBuild;
    if (!editBuild) {
      return;
    }

    if (editBuild.selectedParts && typeof editBuild.selectedParts === "object") {
      setSelectedParts(editBuild.selectedParts);
    }

    if (typeof editBuild.title === "string" && editBuild.title.trim()) {
      setBuildTitle(editBuild.title.trim());
    }
  }, [location.state]);

  const activeList = useMemo(() => partsByCategory[activeCategory] || [], [partsByCategory, activeCategory]);

  const visibleParts = useMemo(() => {
    const query = searchText.toLowerCase().trim();

    const sortedList = [...activeList].sort((left, right) => {
      const leftPrice = Number(left?.price ?? 0);
      const rightPrice = Number(right?.price ?? 0);

      if (leftPrice === rightPrice) {
        return String(left?.name ?? "").localeCompare(String(right?.name ?? ""));
      }

      return priceOrder === "asc" ? leftPrice - rightPrice : rightPrice - leftPrice;
    });

    return sortedList.filter((part) => {
      if (Number(part.price) <= 0) {
        return false;
      }

      const isPicked =
        activeCategory === "storage"
          ? STORAGE_SLOTS.some((key) => selectedParts[key]?.id === part.id)
          : selectedParts[activeCategory]?.id === part.id;

      if (!isPicked && !isPartCompatible(part, selectedParts)) {
        return false;
      }

      if (!query) {
        return true;
      }

      return part.name.toLowerCase().includes(query) || (part.retailer || "").toLowerCase().includes(query);
    });
  }, [activeList, searchText, selectedParts, activeCategory, priceOrder]);

  const completion = useMemo(() => {
    const hasStorage = STORAGE_SLOTS.some((key) => selectedParts[key]) || Boolean(selectedParts.storage);
    const hasRam = Boolean(selectedParts.ram);
    const selectedCount = CATEGORIES.filter((category) => {
      if (category.key === "storage") return hasStorage;
      if (category.key === "ram") return hasRam;
      return Boolean(selectedParts[category.key]);
    }).length;

    const missing = CATEGORIES.filter((category) => {
      if (category.key === "storage") return !hasStorage;
      if (category.key === "ram") return !hasRam;
      return !selectedParts[category.key];
    }).map((category) => category.label);

    return {
      selectedCount,
      total: CATEGORIES.length,
      done: selectedCount === CATEGORIES.length,
      missing,
    };
  }, [selectedParts]);

  const estimatedPower = useMemo(() => buildEstimatedPower(selectedParts), [selectedParts]);

  const totalCost = useMemo(
    () => Object.values(selectedParts).reduce((sum, part) => sum + Number(part?.price ?? 0), 0),
    [selectedParts]
  );

  const filterTags = useMemo(() => buildFilterTags(selectedParts), [selectedParts]);
  const compatibilityIssues = [];

  const savePartPayload = (part) => ({
    id: part?.id ?? null,
    name: part?.name ?? null,
    price: part?.price ?? null,
    socket: part?.socket ?? null,
    ramType: part?.ramType ?? null,
    wattage: part?.wattage ?? null,
    formFactor: part?.formFactor ?? null,
    sourceType: part?.sourceType ?? null,
    referenceMatchedId: part?.referenceMatchedId ?? null,
    parsedPartId: part?.parsedPartId ?? null,
    opendbId: part?.opendbId ?? null,
    sourceLink: part?.sourceLink ?? null,
    raw: part?.raw ?? null,
  });

  const handlePick = (categoryKey, part) => {
    const slotKey = categoryKey === "storage" ? activeStorageSlot : categoryKey;
    setSelectedParts((prev) => ({ ...prev, [slotKey]: part }));
  };

  const handleClear = (slotKey) => {
    setSelectedParts((prev) => {
      const next = { ...prev };
      delete next[slotKey];
      return next;
    });
  };

  const handleShare = async () => {
    if (!completion.done) {
      return;
    }

    const normalizedTitle = buildTitle.trim();
    if (!normalizedTitle) {
      alert("Please enter a build name before saving.");
      return;
    }

    if (!session?.token || !userId) {
      alert("Please sign in first to save your build.");
      navigate("/auth");
      return;
    }

    try {
      const createDraftResponse = await fetch("/api/recommendation/manual-builds/drafts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({ userId, title: normalizedTitle }),
      });

      if (createDraftResponse.status === 401) {
        alert("Session expired. Please sign in again.");
        navigate("/auth");
        return;
      }

      if (!createDraftResponse.ok) {
        const text = await createDraftResponse.text();
        throw new Error(text || `Failed to create build draft (${createDraftResponse.status})`);
      }

      const draft = await createDraftResponse.json();
      const draftId = draft?.id;
      if (!draftId) {
        throw new Error("Draft ID is missing in create response.");
      }

      for (const [category, part] of Object.entries(selectedParts).filter(([, part]) => Boolean(part))) {
        const patchResponse = await fetch(`/api/recommendation/manual-builds/drafts/${draftId}/parts`, {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${session.token}`,
          },
          body: JSON.stringify({
            userId,
            category,
            part: savePartPayload(part),
            estimatedPower,
            compatibilityIssues,
          }),
        });

        if (patchResponse.status === 401) {
          alert("Session expired. Please sign in again.");
          navigate("/auth");
          return;
        }

        if (!patchResponse.ok) {
          const text = await patchResponse.text();
          throw new Error(text || `Failed to save draft part (${patchResponse.status})`);
        }
      }

      const finalizeResponse = await fetch(`/api/recommendation/manual-builds/drafts/${draftId}/finalize`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({
          userId,
          targetBuildId: editingBuildId,
          title: normalizedTitle,
          description: "Saved from builder page",
          publicBuild: false,
        }),
      });

      if (finalizeResponse.status === 401) {
        alert("Session expired. Please sign in again.");
        navigate("/auth");
        return;
      }

      if (!finalizeResponse.ok) {
        const text = await finalizeResponse.text();
        throw new Error(text || `Failed to finalize draft (${finalizeResponse.status})`);
      }

      const saved = await finalizeResponse.json();
      alert(`Build saved successfully. Build ID: ${saved.id}`);
    } catch (error) {
      alert(`Could not save build: ${error.message}`);
    }
  };

  return (
    <div className="builder-page">
      <header className="topbar">
        <div className="logo" onClick={() => navigate("/")}>KZPCCRAFT</div>

        <nav className="topnav">
          <span onClick={() => navigate("/")}>Home</span>
          <span onClick={() => navigate("/discover")}>Discover</span>
          <span className="nav-active">Builder</span>
        </nav>

        <div className="profile-link" onClick={() => navigate(session ? "/profile" : "/auth")}>
          {session ? "Profile" : "Sign in"}
        </div>
      </header>

      <main className="builder-layout">
        <aside className="catalog-panel">
          <div className="panel-title">
            <h2>Component catalogue</h2>
            <p>Select one part for each category.</p>
          </div>

          <div className="category-list">
            {CATEGORIES.map((cat) => {
              const picked =
                cat.key === "storage"
                  ? STORAGE_SLOTS.some((key) => selectedParts[key])
                  : Boolean(selectedParts[cat.key]);
              const isActive = activeCategory === cat.key;

              return (
                <button
                  key={cat.key}
                  className={`category-btn ${isActive ? "active" : ""} ${picked ? "picked" : ""}`}
                  onClick={() => {
                    setActiveCategory(cat.key);
                    setSearchText("");
                  }}
                >
                  <span>{cat.label}</span>
                  {picked && <span className="mini-check">✓</span>}
                </button>
              );
            })}
          </div>

          {filterTags.length > 0 ? (
            <div className="catalog-tags" aria-label="Current compatibility filters">
              {filterTags.map((tag) => (
                <span key={tag} className="catalog-tag">
                  {tag}
                </span>
              ))}
            </div>
          ) : null}

          <div className="summary-box">
            <div className="summary-title">Build summary</div>
            <div className="summary-text">
              {completion.done
                ? "All parts selected. Ready to share."
                : `Selected ${completion.selectedCount}/${completion.total}. Missing: ${completion.missing.join(", ")}.`}
            </div>
          </div>
        </aside>

        <section className="workspace">
          <div className="workspace-head">
            <div>
              <h1>Build your PC</h1>
            </div>

            <div className="progress-pill">
              {completion.selectedCount}/{completion.total} selected
            </div>
          </div>

          <div className="case-stage">
            <div className="parts-box workspace-parts-box">
              <div className="parts-head">
                <h3>{CATEGORIES.find((c) => c.key === activeCategory)?.label}</h3>
                <div className="parts-head-right">
                  <span>{visibleParts.length} options</span>
                  <div className="parts-head-leds">
                    <span className="case-led" />
                    <span className="case-led" />
                    <span className="case-led" />
                  </div>
                </div>
              </div>

              {activeCategory === "storage" ? (
                <div className="storage-slot-picker" role="group" aria-label="Storage slot picker">
                  {STORAGE_SLOTS.map((slotKey, index) => {
                    const filled = Boolean(selectedParts[slotKey]);
                    const active = activeStorageSlot === slotKey;
                    return (
                      <button
                        key={slotKey}
                        type="button"
                        className={`storage-slot-btn ${active ? "active" : ""} ${filled ? "filled" : ""}`}
                        onClick={() => setActiveStorageSlot(slotKey)}
                      >
                        {`Slot ${index + 1}`}
                      </button>
                    );
                  })}
                </div>
              ) : null}
              <div className="search-wrap">
                <input
                  className="search-input"
                  type="text"
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  placeholder="Search parts by name or shop..."
                />

                <button
                  type="button"
                  className={`price-toggle-btn ${priceOrder === "desc" ? "descending" : "ascending"}`}
                  onClick={() => setPriceOrder((prev) => (prev === "asc" ? "desc" : "asc"))}
                  title="Toggle price order"
                >
                  <span className="price-toggle-label">Price</span>
                  <span className="price-toggle-value">{priceOrder === "asc" ? "Low → High" : "High → Low"}</span>
                </button>
              </div>

              <div className="parts-list" key={activeCategory}>
                {loading && <div className="parts-state">Loading parts...</div>}
                {!loading && loadError && <div className="parts-state error">{loadError}</div>}
                {!loading &&
                  !loadError &&
                  visibleParts.map((part) => {
                    const isPicked =
                      activeCategory === "storage"
                        ? STORAGE_SLOTS.some((key) => selectedParts[key]?.id === part.id)
                        : selectedParts[activeCategory]?.id === part.id;
                    const tooltip = `${part.name}\n${part.compatibility?.message ?? "Compatible with the current build."}`;

                    return (
                      <button
                        key={`${activeCategory}-${part.id}`}
                        className={`part-card ${isPicked ? "picked" : ""}`}
                        onClick={() => handlePick(activeCategory, part)}
                        title={tooltip}
                      >
                        <div className="part-card-top">
                          <strong>{part.name}</strong>
                          {isPicked && <span className="picked-badge">Selected</span>}
                        </div>

                        <div className="part-note">
                          {part.retailer ? `${part.retailer} · ` : ""}
                          {part.price ? `${part.price.toLocaleString("en-US")} ${part.currency}` : "No price"}
                        </div>
                      </button>
                    );
                  })}

                {!loading && !loadError && visibleParts.length === 0 && (
                  <div className="parts-state">No parts match your search.</div>
                )}
              </div>
            </div>

            <div className="preview-side">
              <div className="preview-card">
                <div className="preview-title">Selected parts</div>

                <div className="selected-list">
                  {SLOT_ORDER.map((slot) => {
                    const part = selectedParts[slot.key];
                    const partUrl = part?.url && part.url !== "#" ? part.url : null;
                    const partCost = part ? formatMoney(part.price) : "Empty";
                    const wattageValue = part?.wattage ?? part?.powerHint ?? null;

                    return (
                      <div className={`selected-item ${part ? "active" : ""}`} key={slot.key}>
                        <div className="selected-item-header">
                          <span>{slot.label}</span>
                          {part ? (
                            <button
                              className="deselect-btn"
                              onClick={() => handleClear(slot.key)}
                              title="Remove this part"
                            >
                              ✕
                            </button>
                          ) : null}
                        </div>
                        <strong>{part ? part.name : "Empty"}</strong>
                        {part && wattageValue ? <div className="selected-item-wattage">{`${wattageValue}W`}</div> : null}
                        {part ? <div className="selected-item-price">{partCost}</div> : null}
                        {partUrl ? (
                          <a href={partUrl} target="_blank" rel="noreferrer" className="selected-item-link" title={partUrl}>
                            {partUrl}
                          </a>
                        ) : null}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>

          <div className="preview-card hint-card">
            <div className="build-info-box">
              <div className="preview-title">Build info</div>

              <div className="build-info-grid">
                <div>CPU: {selectedParts.cpu?.name || "Not selected"}</div>
                <div>GPU: {selectedParts.gpu?.name || "Not selected"}</div>
                <div>Motherboard: {selectedParts.motherboard?.name || "Not selected"}</div>
                <div>RAM: {selectedParts.ram?.name || "Not selected"}</div>
                <div>Storage 1: {selectedParts.storage_1?.name || "Not selected"}</div>
                <div>Storage 2: {selectedParts.storage_2?.name || "Not selected"}</div>
                <div>Storage 3: {selectedParts.storage_3?.name || "Not selected"}</div>
                <div>PSU: {selectedParts.psu?.name || "Not selected"}</div>
                <div>Case: {selectedParts.case?.name || "Not selected"}</div>
                <div>Cooling: {selectedParts.cooling?.name || "Not selected"}</div>
              </div>

              <div className="build-info-row">
                <div className="build-power">Estimated power: {estimatedPower}W</div>
                <div className="build-status">{completion.done ? "Ready to share" : `Missing ${completion.missing.length} parts`}</div>
              </div>

              <div className="build-name-field">
                <label htmlFor="build-title-input">Build name</label>
                <input
                  id="build-title-input"
                  type="text"
                  maxLength={80}
                  value={buildTitle}
                  onChange={(event) => setBuildTitle(event.target.value)}
                  placeholder="Example: White Gaming Rig"
                />
              </div>

              <div className="build-total-box">
                <span>Total cost</span>
                <strong>{formatMoney(totalCost)}</strong>
              </div>

              {compatibilityIssues.length > 0 ? (
                <div className="build-errors">
                  {compatibilityIssues.map((issue, i) => (
                    <div key={i}>⚠ {issue}</div>
                  ))}
                </div>
              ) : (
                <div className="build-ok">✅ Build looks compatible</div>
              )}
            </div>

            <button
              className={`share-btn ${completion.done ? "ready" : "locked"}`}
              onClick={handleShare}
              disabled={!completion.done || !buildTitle.trim()}
            >
              Save build
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
