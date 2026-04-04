import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./Build.css";

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

const STORAGE_SLOT_KEYS = ["storage_1", "storage_2", "storage_3"];

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
  const sockets = ["AM4", "AM5", "LGA1151", "LGA1200", "LGA1700", "LGA1851"];
  return sockets.find((socket) => upper.includes(socket)) || null;
}

function inferCpuSocket(name) {
  const explicitSocket = extractSocket(name);
  if (explicitSocket) {
    return explicitSocket;
  }

  const upper = safeText(name).toUpperCase();

  if (upper.includes("RYZEN")) {
    if (/(9600|9700|9800|9950|9000|8500|8700|8600|8400|8300|7800|7700|7600|7500|7400|7300)/.test(upper)) {
      return "AM5";
    }

    if (/(5950|5900|5800|5700|5600|5500|5300|530|5200|5100|5000|4700|4600|4500|4300|4100|3900|3800|3700|3600|3500|3400|3300|3200|3100|3000|2600|2500|2400|2300|2200|1600|1500)/.test(upper)) {
      return "AM4";
    }
  }

  if (upper.includes("INTEL")) {
    if (/(I[3579]-?[6789]\d{3}|I[3579]-?9\d{3}|6100|6400|6500|6600|6700|6800|6900|7100|7400|7500|7600|7700|7800|7900|8100|8400|8500|8600|8700|8900|9100|9400|9500|9600|9700|9900|6TH|7TH|8TH|9TH)/.test(upper)) {
      return "LGA1151";
    }

    if (/(I[3579]-?10\d{3}|I[3579]-?11\d{3}|10100|10400|10600|10700|10900|11400|11600|11700|11900|10TH|11TH)/.test(upper)) {
      return "LGA1200";
    }

    if (/(I[3579]-?1[234]\d{3}|12TH|13TH|14TH|12400|12600|13400|13600|14600|14700|14900)/.test(upper)) {
      return "LGA1700";
    }

    if (/(200S|CORE ULTRA|ULTRA\s*[579]|285K|265K|245K)/.test(upper)) {
      return "LGA1851";
    }
  }

  return null;
}

function inferMotherboardSocket(name) {
  const explicitSocket = extractSocket(name);
  if (explicitSocket) {
    return explicitSocket;
  }

  const upper = safeText(name).toUpperCase();

  if (/(B850|X870E?|B840|X870|B650E?|X670E?|A620|B650|X670)/.test(upper)) {
    return "AM5";
  }

  if (/(B550|X570|B450|A520|A320|B350|X470)/.test(upper)) {
    return "AM4";
  }

  if (/(H610|B660|B760|Z690|Z790)/.test(upper)) {
    return "LGA1700";
  }

  if (/(H110|B150|H170|Z170|B250|H270|Z270|H310|B360|H370|Z370|Z390)/.test(upper)) {
    return "LGA1151";
  }

  if (/(H810|B860|Z890)/.test(upper)) {
    return "LGA1851";
  }

  return null;
}

function inferMotherboardMemoryType(name, socket) {
  const upper = safeText(name).toUpperCase();

  if (upper.includes("DDR5")) {
    return "DDR5";
  }

  if (upper.includes("DDR4")) {
    return "DDR4";
  }

  if (/(H810|B860|Z890)/.test(upper)) {
    return "DDR5";
  }

  if (/(H610|B660|B760|Z690|Z790)/.test(upper)) {
    return "DDR5";
  }

  if (/(H110|B150|H170|Z170|B250|H270|Z270|H310|B360|H370|Z370|Z390)/.test(upper)) {
    return "DDR4";
  }

  if (socket === "AM5") {
    return "DDR5";
  }

  if (socket === "AM4") {
    return "DDR4";
  }

  if (socket === "LGA1851") {
    return "DDR5";
  }

  if (socket === "LGA1151" || socket === "LGA1200") {
    return "DDR4";
  }

  return null;
}

function inferMotherboardFormFactor(name) {
  const text = safeText(name).toUpperCase();

  if (text.includes("MINI-ITX") || text.includes("MINI ITX") || text.includes("ITX")) {
    return "ITX";
  }

  if (
    text.includes("M-ATX") ||
    text.includes("MATX") ||
    text.includes("MICRO-ATX") ||
    text.includes("MICRO ATX") ||
    /\b[A-Z0-9]+M\b/.test(text)
  ) {
    return "mATX";
  }

  return "ATX";
}

function inferCoolingSockets(name) {
  const upper = safeText(name).toUpperCase();
  const sockets = ["AM4", "AM5", "LGA1200", "LGA1700", "LGA1851"];
  return sockets.filter((socket) => upper.includes(socket));
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

function extractCpuWattage(name) {
  const text = safeText(name);
  const upper = text.toUpperCase();

  // AMD Ryzen 5
  if (upper.includes("RYZEN 5 1600") || upper.includes("RYZEN 5 2600") || upper.includes("RYZEN 5 3600") || upper.includes("RYZEN 5 5600X") || upper.includes("RYZEN 5 5600") || upper.includes("RYZEN 5 7500F")) return 65;
  if (upper.includes("RYZEN 5 7600X") || upper.includes("RYZEN 5 9600X")) return 105;

  // AMD Ryzen 7
  if (upper.includes("RYZEN 7 1700") || upper.includes("RYZEN 7 2700") || upper.includes("RYZEN 7 3700X") || upper.includes("RYZEN 7 5700X")) return 65;
  if (upper.includes("RYZEN 7 5800X") || upper.includes("RYZEN 7 7700X") || upper.includes("RYZEN 7 9700X")) return 105;

  // Intel modern K / KF (more power hungry)
  if (/i[3579]-1[2-9]\d{3}k[f]?/i.test(text)) return 125;
  // Intel modern non-K
  if (/i[3579]-1[2-9]\d{3}(?![a-z0-9])/i.test(text)) return 65;

  // Intel older K
  if (/i[3579]-[7-9]\d{3}k/i.test(text)) return 91;
  // Intel older non-K
  if (/i[3579]-[7-9]\d{3}(?![a-z0-9])/i.test(text)) return 65;

  // Fallback by series
  if (upper.includes("RYZEN 9")) return 125;
  if (upper.includes("RYZEN 7")) return 105;
  if (upper.includes("RYZEN 5")) return 65;
  if (upper.includes("I9")) return 125;
  if (upper.includes("I7")) return 100;
  if (upper.includes("I5")) return 65;
  if (upper.includes("I3")) return 50;

  return 65;
}

function extractGpuWattage(name) {
  const text = safeText(name);
  const upper = text.toUpperCase();

  // NVIDIA GTX 10 series
  if (upper.includes("GT 1030")) return 30;
  if (upper.includes("GTX 1050 TI")) return 75;
  if (upper.includes("GTX 1050")) return 75;
  if (upper.includes("GTX 1060")) return 120;
  if (upper.includes("GTX 1070 TI")) return 180;
  if (upper.includes("GTX 1070")) return 150;
  if (upper.includes("GTX 1080 TI")) return 250;
  if (upper.includes("GTX 1080")) return 180;

  // NVIDIA GTX 16 series
  if (upper.includes("GTX 1665 SUPER")) return 125;
  if (upper.includes("GTX 1660 TI")) return 120;
  if (upper.includes("GTX 1660")) return 120;
  if (upper.includes("GTX 1650")) return 75;

  // NVIDIA RTX 20 series
  if (upper.includes("RTX 2060 SUPER")) return 175;
  if (upper.includes("RTX 2060")) return 160;
  if (upper.includes("RTX 2070 SUPER")) return 215;
  if (upper.includes("RTX 2070")) return 175;
  if (upper.includes("RTX 2080 TI")) return 250;
  if (upper.includes("RTX 2080 SUPER")) return 250;
  if (upper.includes("RTX 2080")) return 215;

  // NVIDIA RTX 30 series
  if (upper.includes("RTX 3090 TI")) return 450;
  if (upper.includes("RTX 3090")) return 350;
  if (upper.includes("RTX 3080 TI")) return 350;
  if (upper.includes("RTX 3080")) return 320;
  if (upper.includes("RTX 3070 TI")) return 290;
  if (upper.includes("RTX 3070")) return 220;
  if (upper.includes("RTX 3060 TI")) return 200;
  if (upper.includes("RTX 3060")) return 170;
  if (upper.includes("RTX 3050")) return 130;

  // NVIDIA RTX 40 series
  if (upper.includes("RTX 4090")) return 450;
  if (upper.includes("RTX 4080 SUPER")) return 320;
  if (upper.includes("RTX 4080")) return 320;
  if (upper.includes("RTX 4070 TI SUPER")) return 285;
  if (upper.includes("RTX 4070 TI")) return 285;
  if (upper.includes("RTX 4070 SUPER")) return 220;
  if (upper.includes("RTX 4070")) return 200;
  if (upper.includes("RTX 4060 TI")) return 160;
  if (upper.includes("RTX 4060")) return 115;

  // AMD RX 400/500
  if (upper.includes("RX 590")) return 225;
  if (upper.includes("RX 580")) return 185;
  if (upper.includes("RX 570")) return 150;
  if (upper.includes("RX 560")) return 80;
  if (upper.includes("RX 550")) return 50;
  if (upper.includes("RX 480")) return 150;
  if (upper.includes("RX 470")) return 120;
  if (upper.includes("RX 460")) return 75;

  // AMD RX 5000
  if (upper.includes("RX 5700 XT")) return 225;
  if (upper.includes("RX 5700")) return 180;
  if (upper.includes("RX 5600 XT")) return 150;
  if (upper.includes("RX 5500 XT")) return 130;

  // AMD RX 6000
  if (upper.includes("RX 6950 XT")) return 335;
  if (upper.includes("RX 6900 XT")) return 300;
  if (upper.includes("RX 6800 XT")) return 300;
  if (upper.includes("RX 6800")) return 250;
  if (upper.includes("RX 6750 XT")) return 250;
  if (upper.includes("RX 6700 XT")) return 230;
  if (upper.includes("RX 6650 XT")) return 176;
  if (upper.includes("RX 6600 XT")) return 160;
  if (upper.includes("RX 6600")) return 132;
  if (upper.includes("RX 6500 XT")) return 107;
  if (upper.includes("RX 6400")) return 53;

  // AMD RX 7000
  if (upper.includes("RX 7900 XTX")) return 355;
  if (upper.includes("RX 7900 XT")) return 315;
  if (upper.includes("RX 7900 GRE")) return 260;
  if (upper.includes("RX 7800 XT")) return 263;
  if (upper.includes("RX 7700 XT")) return 245;
  if (upper.includes("RX 7600 XT")) return 190;
  if (upper.includes("RX 7600")) return 165;

  return 200;
}

function normalizePart(row, category) {
  const name = row?.name ?? "";
  const socket =
    category === "cpu"
      ? inferCpuSocket(name)
      : category === "motherboard"
      ? inferMotherboardSocket(name)
      : category === "cooling"
      ? inferCoolingSockets(name)[0] ?? null
      : extractSocket(name);

  const supportedSockets = category === "cooling" ? inferCoolingSockets(name) : [];
  const memoryGeneration = category === "ram" ? extractRamType(name) : category === "motherboard" ? inferMotherboardMemoryType(name, socket) : null;
  const normalized = {
    id: row?.id ?? `${category}-${Math.random().toString(36).slice(2)}`,
    category,
    name,
    price: Number(row?.price_kzt ?? row?.priceKzt ?? row?.price ?? 0),
    retailer: row?.retailer ?? "",
    currency: row?.currency ?? "KZT",
    url: row?.url ?? "#",
    lastScraped: row?.last_scraped ?? null,
    socket,
    supportedSockets,
    chipset: category === "motherboard" ? safeText(name).match(/\b(B\d{3}|X\d{3}|H\d{3}|A\d{3})\b/i)?.[1]?.toUpperCase() ?? null : null,
    ramType: memoryGeneration,
    memoryGeneration,
    wattage: extractWattage(name),
    formFactor: category === "motherboard" ? inferMotherboardFormFactor(name) : extractFormFactor(name),
    storageType: extractStorageType(name),
    powerHint:
      category === "cpu"
        ? extractCpuWattage(name)
        : category === "gpu"
        ? extractGpuWattage(name)
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
  if (selectedParts.ram) total += 5;
  const storageCount = STORAGE_SLOT_KEYS.filter((key) => selectedParts[key]).length + (selectedParts.storage ? 1 : 0);
  total += storageCount * 10;
  if (selectedParts.cooling) total += 10;

  return total;
}

function collectCompatibilityIssues(selectedParts) {
  const issues = [];

  if (selectedParts.cpu && selectedParts.motherboard) {
    if (selectedParts.cpu.socket && selectedParts.motherboard.socket && selectedParts.cpu.socket !== selectedParts.motherboard.socket) {
      issues.push("CPU socket and motherboard socket do not match.");
    }
  }

  const anyRam = selectedParts.ram;
  if (anyRam && selectedParts.motherboard) {
    if (anyRam.ramType && selectedParts.motherboard.ramType && anyRam.ramType !== selectedParts.motherboard.ramType) {
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
      mATX: ["mATX"],
      ITX: ["ITX"],
    };

    if (!allowed[selectedParts.case.formFactor]?.includes(selectedParts.motherboard.formFactor)) {
      issues.push("Case form factor does not fit the selected motherboard.");
    }
  }

  return issues;
}

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "No price";
  }

  return `${Number(value).toLocaleString("en-US")} KZT`;
}

async function fetchParsedRows(endpoint, pageSize = 200, maxPages = 10) {
  const rows = [];

  for (let page = 0; page < maxPages; page += 1) {
    const response = await fetch(`${endpoint}?page=${page}&size=${pageSize}`);

    if (!response.ok) {
      throw new Error(`Failed to fetch parsed page ${page}`);
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

function getCompatibilityResult(part, selectedParts) {
  const cpu = selectedParts.cpu || null;
  const motherboard = selectedParts.motherboard || null;
  const casePart = selectedParts.case || null;
  const reasons = [];

  if (part.category === "cpu" && motherboard?.socket && part.socket && part.socket !== motherboard.socket) {
    reasons.push(`${part.name} only works with ${part.socket} motherboards.`);
  }

  if (part.category === "motherboard" && cpu?.socket && part.socket && part.socket !== cpu.socket) {
    reasons.push(`${part.name} only works with ${part.socket} CPUs.`);
  }

  if (part.category === "motherboard") {
    const anyRam = selectedParts.ram;
    if (anyRam?.ramType && part.ramType && anyRam.ramType !== part.ramType) {
      reasons.push(`${part.name} supports ${part.ramType}, but selected RAM is ${anyRam.ramType}.`);
    }
  }

  if (part.category === "ram") {
    const expectedRamType = motherboard?.ramType || null;
    if (expectedRamType && part.ramType && part.ramType !== expectedRamType) {
      reasons.push(`${part.name} is ${part.ramType}, but the selected motherboard requires ${expectedRamType}.`);
    }
  }

  if (part.category === "case" && motherboard?.formFactor) {
    const caseForm = part.formFactor;

    if (caseForm) {
      const allowed = {
        ATX: ["ATX", "mATX", "ITX"],
        mATX: ["mATX"],
        ITX: ["ITX"],
      };

      if (!allowed[caseForm]?.includes(motherboard.formFactor)) {
        reasons.push(`${part.name} does not fit a ${motherboard.formFactor} motherboard.`);
      }
    }
  }

  if (part.category === "cooling" && cpu?.socket) {
    const supportsSocket = Array.isArray(part.supportedSockets) && part.supportedSockets.length > 0
      ? part.supportedSockets.includes(cpu.socket)
      : safeText(part.name).toUpperCase().includes(cpu.socket);

    if (!supportsSocket && part.socket && part.socket !== cpu.socket) {
      reasons.push(`${part.name} only supports ${part.supportedSockets.join(" / ") || part.socket}, but the selected CPU is ${cpu.socket}.`);
    }
  }

  if (part.category === "psu") {
    const estimatedPower = getEstimatedPower(selectedParts);
    const requiredPower = estimatedPower + 150;
    if (!part.wattage) {
      reasons.push(`${part.name} does not have wattage information. Cannot validate compatibility.`);
    } else if (part.wattage < requiredPower) {
      reasons.push(`${part.name} (${part.wattage}W) is too weak for this build. Need at least ${requiredPower}W.`);
    }
  }

  if (part.category === "storage") {
    // Keep storage open for now.
  }

  if (part.category === "gpu") {
    if (casePart?.formFactor === "ITX" && part.powerHint >= 320) {
      reasons.push(`${part.name} is a high-power GPU and may be too large or hot for this ITX build.`);
    }
  }

  return {
    compatible: reasons.length === 0,
    reasons,
    message: reasons[0] || `Compatible with the current build.`,
  };
}

export default function BuilderPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [session] = useState(() => loadSession());
  const [activeCategory, setActiveCategory] = useState("cpu");
  const [activeStorageSlot, setActiveStorageSlot] = useState("storage_1");
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
  const [buildTitle, setBuildTitle] = useState("");
  const [editSeedApplied, setEditSeedApplied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const userId = session?.email?.trim().toLowerCase() ?? null;
  const editingBuildId = Number.isFinite(Number(location.state?.editBuild?.id))
    ? Number(location.state.editBuild.id)
    : null;

  useEffect(() => {
    if (editSeedApplied) {
      return;
    }

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

    setEditSeedApplied(true);
  }, [location.state, editSeedApplied]);

  useEffect(() => {
    let alive = true;

    async function loadParts() {
      setLoading(true);
      setLoadError("");
      const failedCategories = [];

      try {
        const loaded = await Promise.all(
          CATEGORIES.map(async (cat) => {
            try {
              if (!cat.endpoint) {
                return [cat.key, MOCK_ROWS[cat.key].map((row) => normalizePart(row, cat.key))];
              }

              const rows = await fetchParsedRows(cat.endpoint, 200, 10);
              return [cat.key, rows.map((row) => normalizePart(row, cat.key))];
            } catch {
              failedCategories.push(cat.key);
              return [cat.key, MOCK_ROWS[cat.key].map((row) => normalizePart(row, cat.key))];
            }
          })
        );

        if (!alive) return;

        setPartsByCategory(Object.fromEntries(loaded));
        if (failedCategories.length > 0) {
          setLoadError(`Live catalog unavailable for: ${failedCategories.join(", ")}. Showing fallback data.`);
        }
      } catch {
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

  const activeList = useMemo(() => partsByCategory[activeCategory] || [], [partsByCategory, activeCategory]);

  const visibleParts = useMemo(() => {
    const query = searchText.toLowerCase().trim();

    return activeList
      .filter((part) => {
        const hasPrice = Number(part.price) > 0;
        const matchesSearch =
          !query ||
          part.name.toLowerCase().includes(query) ||
          part.retailer.toLowerCase().includes(query);

        if (!hasPrice || !matchesSearch) return false;

        const compatibility = getCompatibilityResult(part, selectedParts);
        // Only hide incompatible parts if they're not currently picked
        const isPicked =
          activeCategory === "storage"
            ? STORAGE_SLOT_KEYS.some((key) => selectedParts[key]?.id === part.id)
            : selectedParts[activeCategory]?.id === part.id;

        if (!compatibility.compatible && !isPicked) {
          return false;
        }

        return true;
      })
      .map((part) => ({
        ...part,
        compatibility: getCompatibilityResult(part, selectedParts),
      }));
  }, [activeList, searchText, selectedParts, activeCategory]);

  const completion = useMemo(() => {
    const hasStorage = STORAGE_SLOT_KEYS.some((key) => selectedParts[key]) || Boolean(selectedParts.storage);
    const hasRam = Boolean(selectedParts.ram);
    const selectedCount = CATEGORIES.filter((c) => {
      if (c.key === "storage") return hasStorage;
      if (c.key === "ram") return hasRam;
      return Boolean(selectedParts[c.key]);
    }).length;
    const missing = CATEGORIES.filter((c) => {
      if (c.key === "storage") return !hasStorage;
      if (c.key === "ram") return !hasRam;
      return !selectedParts[c.key];
    }).map((c) => c.label);

    return {
      selectedCount,
      total: CATEGORIES.length,
      done: selectedCount === CATEGORIES.length,
      missing,
    };
  }, [selectedParts]);

  const compatibilityIssues = useMemo(() => collectCompatibilityIssues(selectedParts), [selectedParts]);

  const estimatedPower = useMemo(() => getEstimatedPower(selectedParts), [selectedParts]);

  const totalCost = useMemo(() => {
    return Object.values(selectedParts).reduce((sum, part) => sum + Number(part?.price ?? 0), 0);
  }, [selectedParts]);

  const handlePick = (categoryKey, part) => {
    let targetCategory = categoryKey;
    if (categoryKey === "storage") targetCategory = activeStorageSlot;

    setSelectedParts((prev) => {
      const next = {
        ...prev,
        [targetCategory]: part,
      };
      return next;
    });
  };

  const handleClear = (slotKey) => {
    setSelectedParts((prev) => {
      const next = { ...prev };
      delete next[slotKey];
      return next;
    });
  };

  const handleShare = async () => {
    if (!completion.done) return;
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

    const payload = {
      userId,
      title: normalizedTitle,
      description: "Saved from builder page",
      publicBuild: false,
      sourceSessionId: `builder:${Date.now()}`,
      selectedParts,
      estimatedPower,
      compatibilityIssues,
    };

    localStorage.setItem("kzpc:last-build-draft", JSON.stringify({
      ...payload,
    }));

    try {
      const createDraftResponse = await fetch("/api/recommendation/manual-builds/drafts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({
          userId,
          title: normalizedTitle,
        }),
      });

      if (!createDraftResponse.ok) {
        const text = await createDraftResponse.text();
        throw new Error(text || `Failed to create build draft (${createDraftResponse.status})`);
      }

      const draft = await createDraftResponse.json();
      const draftId = draft?.id;

      if (!draftId) {
        throw new Error("Draft ID is missing in create response.");
      }

      const selectedEntries = Object.entries(selectedParts).filter(([, part]) => Boolean(part));
      for (const [category, part] of selectedEntries) {
        const patchResponse = await fetch(`/api/recommendation/manual-builds/drafts/${draftId}/parts`, {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${session.token}`,
          },
          body: JSON.stringify({
            userId,
            category,
            part,
            estimatedPower,
            compatibilityIssues,
          }),
        });

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
          description: payload.description,
          publicBuild: payload.publicBuild,
        }),
      });

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
          <span onClick={() => navigate("/discover")}>Discovery</span>
          <span className="nav-active">Builder</span>
          <span>Guides</span>
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
                  ? STORAGE_SLOT_KEYS.some((key) => selectedParts[key])
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
                  {STORAGE_SLOT_KEYS.map((slotKey, index) => {
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
                  placeholder="Filter by word..."
                />
              </div>

              <div className="parts-list" key={activeCategory}>
                {loading && <div className="parts-state">Loading parts...</div>}
                {!loading && loadError && <div className="parts-state error">{loadError}</div>}
                {!loading &&
                  !loadError &&
                  visibleParts.map((part) => {
                    const isPicked =
                      activeCategory === "storage"
                        ? STORAGE_SLOT_KEYS.some((key) => selectedParts[key]?.id === part.id)
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
                          <a
                            href={partUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="selected-item-link"
                            title={partUrl}
                          >
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
          </div>

          <button
            className={`share-btn ${completion.done ? "ready" : "locked"}`}
            onClick={handleShare}
            disabled={!completion.done || !buildTitle.trim()}
          >
            Save build
          </button>
        </section>
      </main>
    </div>
  );
}