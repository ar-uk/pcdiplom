import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./Build.css";

const CATEGORIES = [
  { key: "cpu", label: "CPU", endpoint: "/api/cpus" },
  { key: "gpu", label: "GPU", endpoint: "/api/gpus" },
  { key: "motherboard", label: "Motherboard", endpoint: "/api/motherboards" },
  { key: "psu", label: "PSU", endpoint: "/api/psus" },
  { key: "storage", label: "Storage", endpoint: "/api/storages" },
  { key: "ram", label: "RAM", endpoint: "/api/rams" },
  { key: "case", label: "Case", endpoint: "/api/cases" },
  { key: "cooling", label: "Cooling", endpoint: "/api/coolings" },
];

const SLOT_ORDER = [
  { key: "cpu", label: "CPU" },
  { key: "motherboard", label: "Motherboard" },
  { key: "gpu", label: "GPU" },
  { key: "ram", label: "RAM" },
  { key: "storage", label: "Storage" },
  { key: "psu", label: "PSU" },
  { key: "case", label: "Case" },
  { key: "cooling", label: "Cooling" },
];

const MOCK_ROWS = {
  cpu: [
    { id: 1, name: "Процессор AMD Ryzen 5 7500F, AM5, OEM", price_kzt: 80990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "Процессор Intel Core i5-12400F, LGA1700", price_kzt: 69990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "Процессор AMD Ryzen 7 7800X3D, AM5", price_kzt: 219990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "Процессор Intel Core i7-13700K, LGA1700", price_kzt: 189990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  gpu: [
    { id: 1, name: "Видеокарта Gigabyte RX 9070 XT Gaming, 16 GB, Radeon RX 9070 XT", price_kzt: 499990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "NVIDIA GeForce RTX 4060 8 GB", price_kzt: 169990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "NVIDIA GeForce RTX 4070 Super 12 GB", price_kzt: 299990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "NVIDIA GeForce RTX 4080 Super 16 GB", price_kzt: 629990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  motherboard: [
    { id: 1, name: "Материнская плата ASRock B550 Extreme4, AM4", price_kzt: 99990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "Материнская плата MSI B650 Tomahawk, AM5", price_kzt: 149990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "Материнская плата ASUS TUF Gaming B760M, LGA1700", price_kzt: 129990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "Материнская плата Gigabyte X670 AORUS Elite, AM5", price_kzt: 189990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  psu: [
    { id: 1, name: "Блок питания ATX 500 W PCCooler GI-BR500", price_kzt: 24990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "Блок питания 650 W 80+ Gold", price_kzt: 39990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "Блок питания 750 W 80+ Gold", price_kzt: 54990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "Блок питания 850 W 80+ Platinum", price_kzt: 79990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  storage: [
    { id: 1, name: 'SSD накопитель 500 GB Transcend SSD225S, 2.5", SATA III', price_kzt: 62990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "SSD NVMe 1 TB M.2 PCIe 4.0", price_kzt: 55990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "SSD NVMe 2 TB M.2 PCIe 4.0", price_kzt: 99990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "HDD 1 TB 7200 RPM", price_kzt: 29990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  ram: [
    { id: 1, name: "DDR-4 DIMM 8 GB 2666 MHz Geil Pristine Series, BOX", price_kzt: 51990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "DDR4 16 GB 3200 MHz", price_kzt: 39990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "DDR5 32 GB 6000 MHz", price_kzt: 89990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "DDR5 64 GB 6400 MHz", price_kzt: 169990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  case: [
    { id: 1, name: "Корпус Zalman Z10, Mid Tower, Black", price_kzt: 43600, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "Корпус Micro-ATX Compact Case", price_kzt: 28990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "Корпус ATX Glass Showcase Case", price_kzt: 59990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "Корпус Mini-ITX Small Form Factor", price_kzt: 74990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
  cooling: [
    { id: 1, name: "Stock Air Cooler", price_kzt: 0, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 2, name: "Tower Air Cooler AM4 / AM5 / LGA1700", price_kzt: 19990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 3, name: "240mm AIO Liquid Cooler AM5 / LGA1700", price_kzt: 49990, retailer: "shop.kz", currency: "KZT", url: "#" },
    { id: 4, name: "360mm AIO Liquid Cooler AM5 / LGA1700", price_kzt: 69990, retailer: "shop.kz", currency: "KZT", url: "#" },
  ],
};

function safeText(value) {
  return String(value ?? "").trim();
}

function extractSocket(name) {
  const upper = safeText(name).toUpperCase();
  const sockets = ["AM4", "AM5", "LGA1200", "LGA1700", "LGA1851"];
  return sockets.find((socket) => upper.includes(socket)) || null;
}

function extractRamType(name) {
  if (/DDR5/i.test(name)) return "DDR5";
  if (/DDR4/i.test(name)) return "DDR4";
  return null;
}

function extractWattage(name) {
  const match = safeText(name).match(/(\d{3,4})\s*W/i);
  return match ? Number(match[1]) : null;
}

function extractFormFactor(name) {
  const text = safeText(name).toUpperCase();
  if (text.includes("MINI-ITX") || text.includes("MINI ITX") || text.includes("ITX")) return "ITX";
  if (text.includes("M-ATX") || text.includes("MATX") || text.includes("MICRO-ATX") || text.includes("MICRO ATX")) return "mATX";
  if (text.includes("ATX")) return "ATX";
  return null;
}

function extractStorageType(name) {
  const text = safeText(name).toUpperCase();
  if (text.includes("NVME") || text.includes("M.2") || text.includes("M2")) return "NVMe";
  if (text.includes("SATA")) return "SATA";
  if (text.includes("HDD")) return "HDD";
  return null;
}

function estimateCpuPower(name) {
  const text = safeText(name).toUpperCase();
  if (text.includes("I9") || text.includes("RYZEN 9")) return 170;
  if (text.includes("I7") || text.includes("RYZEN 7")) return 125;
  if (text.includes("I5") || text.includes("RYZEN 5")) return 65;
  if (text.includes("I3") || text.includes("RYZEN 3")) return 45;
  return 65;
}

function estimateGpuPower(name) {
  const text = safeText(name).toUpperCase();
  if (text.includes("4090")) return 450;
  if (text.includes("4080")) return 320;
  if (text.includes("4070")) return 220;
  if (text.includes("4060")) return 115;
  if (text.includes("9070 XT")) return 300;
  if (text.includes("9070")) return 260;
  if (text.includes("7600")) return 165;
  if (text.includes("6600")) return 130;
  return 200;
}

function normalizePart(row, category) {
  const name = row?.name ?? "";
  const normalized = {
    id: row?.id ?? `${category}-${Math.random().toString(36).slice(2)}`,
    category,
    name,
    price: Number(row?.price_kzt ?? row?.price ?? 0),
    retailer: row?.retailer ?? "",
    currency: row?.currency ?? "KZT",
    url: row?.url ?? "#",
    lastScraped: row?.last_scraped ?? null,
    socket: extractSocket(name),
    ramType: extractRamType(name),
    wattage: extractWattage(name),
    formFactor: extractFormFactor(name),
    storageType: extractStorageType(name),
    powerHint:
      category === "cpu"
        ? estimateCpuPower(name)
        : category === "gpu"
        ? estimateGpuPower(name)
        : null,
    raw: row,
  };

  return normalized;
}

function getEstimatedPower(selectedParts) {
  let total = 0;

  if (selectedParts.cpu) total += selectedParts.cpu.powerHint || 65;
  if (selectedParts.gpu) total += selectedParts.gpu.powerHint || 200;
  if (selectedParts.motherboard) total += 50;
  if (selectedParts.ram) total += 15;
  if (selectedParts.storage) total += 10;
  if (selectedParts.cooling) total += 10;

  return total;
}

function isCompatible(part, selectedParts) {
  const cpu = selectedParts.cpu || null;
  const motherboard = selectedParts.motherboard || null;
  const ram = selectedParts.ram || null;
  const psu = selectedParts.psu || null;
  const casePart = selectedParts.case || null;

  if (part.category === "cpu" && motherboard?.socket && part.socket && part.socket !== motherboard.socket) {
    return false;
  }

  if (part.category === "motherboard" && cpu?.socket && part.socket && part.socket !== cpu.socket) {
    return false;
  }

  if (part.category === "ram") {
    const expectedRamType = motherboard?.ramType || null;
    if (expectedRamType && part.ramType && part.ramType !== expectedRamType) {
      return false;
    }
  }

  if (part.category === "case" && motherboard?.formFactor) {
    const caseForm = part.formFactor;

    if (caseForm) {
      const allowed = {
        ATX: ["ATX", "mATX", "ITX"],
        mATX: ["mATX", "ITX"],
        ITX: ["ITX"],
      };

      if (!allowed[caseForm]?.includes(motherboard.formFactor)) {
        return false;
      }
    }
  }

  if (part.category === "cooling" && cpu?.socket && part.socket) {
    const text = safeText(part.name).toUpperCase();
    const supportsSocket = text.includes(cpu.socket);
    if (!supportsSocket && part.socket !== cpu.socket) {
      return false;
    }
  }

  if (part.category === "psu") {
    const estimatedPower = getEstimatedPower(selectedParts);
    if (part.wattage && estimatedPower && part.wattage < estimatedPower + 150) {
      return false;
    }
  }

  if (part.category === "storage") {
    return true;
  }

  if (part.category === "gpu") {
    if (casePart?.formFactor === "ITX" && part.powerHint >= 320) {
      return false;
    }
  }

  return true;
}

export default function BuilderPage() {
  const navigate = useNavigate();
  const [session] = useState(() => loadSession());
  const [activeCategory, setActiveCategory] = useState("cpu");
  const [selectedParts, setSelectedParts] = useState({});
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
  const [searchText, setSearchText] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    let alive = true;

    async function loadParts() {
      setLoading(true);
      setLoadError("");

      try {
        const loaded = await Promise.all(
          CATEGORIES.map(async (cat) => {
            try {
              const response = await fetch(cat.endpoint);

              if (!response.ok) {
                throw new Error(`Failed to fetch ${cat.key}`);
              }

              const data = await response.json();
              const rows = Array.isArray(data) ? data : [];
              return [cat.key, rows.map((row) => normalizePart(row, cat.key))];
            } catch {
              return [cat.key, MOCK_ROWS[cat.key].map((row) => normalizePart(row, cat.key))];
            }
          })
        );

        if (!alive) return;

        setPartsByCategory(Object.fromEntries(loaded));
      } catch (error) {
        if (!alive) return;
        setLoadError("Could not load parts.");
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadParts();

    return () => {
      alive = false;
    };
  }, []);

  const activeList = partsByCategory[activeCategory] || [];

  const visibleParts = useMemo(() => {
    const query = searchText.toLowerCase().trim();

    return activeList.filter((part) => {
      const matchesSearch =
        !query ||
        part.name.toLowerCase().includes(query) ||
        part.retailer.toLowerCase().includes(query);

      const compatible = isCompatible(part, selectedParts);
      const isSelected = selectedParts[activeCategory]?.id === part.id;

      return matchesSearch && (compatible || isSelected);
    });
  }, [activeList, activeCategory, searchText, selectedParts]);

  const completion = useMemo(() => {
    const selectedCount = CATEGORIES.filter((c) => selectedParts[c.key]).length;
    const missing = CATEGORIES.filter((c) => !selectedParts[c.key]).map((c) => c.label);

    return {
      selectedCount,
      total: CATEGORIES.length,
      done: selectedCount === CATEGORIES.length,
      missing,
    };
  }, [selectedParts]);

  const compatibilityIssues = useMemo(() => {
    const issues = [];

    if (selectedParts.cpu && selectedParts.motherboard) {
      if (selectedParts.cpu.socket && selectedParts.motherboard.socket && selectedParts.cpu.socket !== selectedParts.motherboard.socket) {
        issues.push("CPU socket and motherboard socket do not match.");
      }
    }

    if (selectedParts.ram && selectedParts.motherboard) {
      if (selectedParts.ram.ramType && selectedParts.motherboard.ramType && selectedParts.ram.ramType !== selectedParts.motherboard.ramType) {
        issues.push("RAM type is not supported by the motherboard.");
      }
    }

    if (selectedParts.cooling && selectedParts.cpu) {
      const coolingText = safeText(selectedParts.cooling.name).toUpperCase();
      const cpuSocket = selectedParts.cpu.socket;
      if (cpuSocket && !coolingText.includes(cpuSocket) && selectedParts.cooling.socket && selectedParts.cooling.socket !== cpuSocket) {
        issues.push("Cooling does not support the selected CPU socket.");
      }
    }

    if (selectedParts.psu) {
      const estimatedPower = getEstimatedPower(selectedParts);
      if (selectedParts.psu.wattage && selectedParts.psu.wattage < estimatedPower + 150) {
        issues.push("PSU wattage may be too low for this build.");
      }
    }

    if (selectedParts.case && selectedParts.motherboard && selectedParts.case.formFactor) {
      const allowed = {
        ATX: ["ATX", "mATX", "ITX"],
        mATX: ["mATX", "ITX"],
        ITX: ["ITX"],
      };

      if (!allowed[selectedParts.case.formFactor]?.includes(selectedParts.motherboard.formFactor)) {
        issues.push("Case form factor does not fit the selected motherboard.");
      }
    }

    return issues;
  }, [selectedParts]);

  const estimatedPower = useMemo(() => getEstimatedPower(selectedParts), [selectedParts]);

  const handlePick = (categoryKey, part) => {
    setSelectedParts((prev) => ({
      ...prev,
      [categoryKey]: part,
    }));
  };

  const handleShare = async () => {
    if (!completion.done) return;

    const payload = {
      createdAt: new Date().toISOString(),
      selectedParts: Object.fromEntries(
        Object.entries(selectedParts).map(([key, part]) => [
          key,
          part
            ? {
                id: part.id,
                name: part.name,
                price: part.price,
                retailer: part.retailer,
                currency: part.currency,
                url: part.url,
                socket: part.socket,
                ramType: part.ramType,
                wattage: part.wattage,
                formFactor: part.formFactor,
                storageType: part.storageType,
              }
            : null,
        ])
      ),
      estimatedPower,
      compatibilityIssues,
    };

    localStorage.setItem("kzpc:last-build-draft", JSON.stringify(payload));

    /*
      Future backend save:
      await fetch("/api/builds", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
    */

    alert("Build draft saved. Backend share endpoint can be connected later.");
    console.log("Build payload ready for backend:", payload);
  };

  return (
    <div className="builder-page">
      <header className="topbar">
        <div className="logo" onClick={() => navigate("/")}>KZPCCRAFT</div>

        <nav className="topnav">
          <span onClick={() => navigate("/discover")}>Discovery</span>
          <span className="nav-active">Builder</span>
          <span>Guides</span>
          <span>Community</span>
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
              const picked = selectedParts[cat.key];
              const isActive = activeCategory === cat.key;

              return (
                <button
                  key={cat.key}
                  className={`category-btn ${isActive ? "active" : ""} ${picked ? "picked" : ""}`}
                  onClick={() => setActiveCategory(cat.key)}
                >
                  <span>{cat.label}</span>
                  {picked && <span className="mini-check">✓</span>}
                </button>
              );
            })}
          </div>

          <div className="parts-box">
            <div className="parts-head">
              <h3>{CATEGORIES.find((c) => c.key === activeCategory)?.label}</h3>
              <span>{visibleParts.length} options</span>
            </div>

            <div className="search-wrap">
              <input
                className="search-input"
                type="text"
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                placeholder="Filter by word..."
              />
            </div>

            <div className="parts-list">
              {loading && <div className="parts-state">Loading parts...</div>}
              {!loading && loadError && <div className="parts-state error">{loadError}</div>}
              {!loading &&
                !loadError &&
                visibleParts.map((part) => {
                  const isPicked = selectedParts[activeCategory]?.id === part.id;

                  return (
                    <button
                      key={part.id}
                      className={`part-card ${isPicked ? "picked" : ""}`}
                      onClick={() => handlePick(activeCategory, part)}
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
                <div className="parts-state">No compatible parts found.</div>
              )}
            </div>
          </div>

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
            <div className="case-frame">
              <div className="case-header">
                <span className="case-led" />
                <span className="case-led" />
                <span className="case-led" />
              </div>

              <div className="case-window">
                {SLOT_ORDER.map((slot, index) => {
                  const part = selectedParts[slot.key];

                  return (
                    <div className={`slot-row ${part ? "filled" : ""}`} key={slot.key}>
                      <div className="slot-label">
                        <span className="slot-dot" />
                        {slot.label}
                      </div>

                      <div className="slot-value">
                        {part ? part.name : "Not selected"}
                      </div>

                      <div className="slot-index">{index + 1}</div>
                    </div>
                  );
                })}
              </div>

              <div className="case-footer">
                <div className="case-spec">
                  <span>Airflow</span>
                  <strong>Optimized</strong>
                </div>
                <div className="case-spec">
                  <span>Status</span>
                  <strong>{completion.done ? "Complete" : "In progress"}</strong>
                </div>
              </div>
            </div>

            <div className="preview-side">
              <div className="preview-card">
                <div className="preview-title">Selected parts</div>

                <div className="selected-list">
                  {SLOT_ORDER.map((slot) => {
                    const part = selectedParts[slot.key];

                    return (
                      <div className={`selected-item ${part ? "active" : ""}`} key={slot.key}>
                        <span>{slot.label}</span>
                        <strong>{part ? part.name : "Empty"}</strong>
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
              <div>Storage: {selectedParts.storage?.name || "Not selected"}</div>
              <div>PSU: {selectedParts.psu?.name || "Not selected"}</div>
              <div>Case: {selectedParts.case?.name || "Not selected"}</div>
              <div>Cooling: {selectedParts.cooling?.name || "Not selected"}</div>
            </div>

            <div className="build-info-row">
              <div className="build-power">Estimated power: {estimatedPower}W</div>
              <div className="build-status">{completion.done ? "Ready to share" : `Missing ${completion.missing.length} parts`}</div>
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
          </div>

          <button
            className={`share-btn ${completion.done ? "ready" : "locked"}`}
            onClick={handleShare}
            disabled={!completion.done}
          >
            Share build
          </button>
        </section>
      </main>
    </div>
  );
}