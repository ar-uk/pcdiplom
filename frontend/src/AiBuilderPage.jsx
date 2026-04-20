import { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./styles/pages/ai-builder.css";

function formatPrice(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "-";
  }
  return new Intl.NumberFormat("en-US").format(Math.round(amount)) + " KZT";
}

function normalizeParts(parts) {
  if (!parts || typeof parts !== "object") {
    return [];
  }

  const order = ["cpu", "gpu", "motherboard", "memory", "storage", "powerSupply", "pcCase"];
  const labelByKey = {
    cpu: "CPU",
    gpu: "GPU",
    motherboard: "Motherboard",
    memory: "Memory",
    storage: "Storage",
    powerSupply: "Power Supply",
    pcCase: "Case",
  };

  return order
    .filter((key) => parts[key])
    .map((key) => ({ key, label: labelByKey[key] ?? key, part: parts[key] }));
}

export default function AiBuilderPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const initialPrompt = (location.state && location.state.prompt) || "";
  const [session] = useState(() => loadSession());

  const [prompt, setPrompt] = useState(initialPrompt);
  const [chatMessage, setChatMessage] = useState("");
  const [response, setResponse] = useState(null);
  const [sessionId, setSessionId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [chatLog, setChatLog] = useState([]);

  const topBuild = useMemo(() => {
    if (!response || !Array.isArray(response.top3Builds) || response.top3Builds.length === 0) {
      return null;
    }
    return response.top3Builds[0];
  }, [response]);

  const topParts = useMemo(() => normalizeParts(topBuild && topBuild.parts), [topBuild]);

  const authHeaders = session?.token
    ? {
        Authorization: `Bearer ${session.token}`,
      }
    : {};

  async function readErrorMessage(res, fallback) {
    let message = fallback;
    try {
      const text = await res.text();
      if (text) {
        try {
          const parsed = JSON.parse(text);
          message = parsed?.message || parsed?.error || text;
        } catch {
          message = text;
        }
      }
    } catch {
      // Ignore body parse failure and keep fallback message.
    }
    return `${fallback} (${res.status})${message ? `: ${message}` : ""}`;
  }

  const runGenerate = async () => {
    const text = prompt.trim();
    if (!text) {
      setError("Write a prompt first.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const res = await fetch("/api/recommendation/build", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders },
        body: JSON.stringify({
          prompt: text,
          currency: "KZT",
          region: "KZ",
          strictBudget: false,
          userId: null,
        }),
      });

      if (!res.ok) {
        if (res.status === 401) {
          throw new Error("Generate failed (401): please sign in first.");
        }
        const details = await readErrorMessage(res, "Generate request failed");
        throw new Error(details);
      }

      const data = await res.json();
      setResponse(data);
      setSessionId(data.sessionId || "");
      setChatLog([{ role: "user", content: text }]);
    } catch (err) {
      setError(err.message || "Failed to generate build.");
    } finally {
      setLoading(false);
    }
  };

  const runChatEdit = async () => {
    const text = chatMessage.trim();
    if (!text) {
      return;
    }
    if (!sessionId) {
      setError("Generate a build first.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const res = await fetch(`/api/recommendation/chat/${sessionId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders },
        body: JSON.stringify({ message: text }),
      });

      if (!res.ok) {
        if (res.status === 401) {
          throw new Error("Chat edit failed (401): please sign in first.");
        }
        const details = await readErrorMessage(res, "Chat edit request failed");
        throw new Error(details);
      }

      const data = await res.json();
      setResponse(data);
      setChatLog((prev) => [...prev, { role: "user", content: text }]);
      setChatMessage("");
    } catch (err) {
      setError(err.message || "Failed to edit build.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-builder-page">
      <header className="site-topbar">
        <div className="site-brand" onClick={() => navigate("/")}>KazPcCraft</div>
        <nav className="site-nav">
          <span onClick={() => navigate("/")}>Home</span>
          <span className="active" onClick={() => navigate("/ai-builder")}>AI Builder</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span onClick={() => navigate("/discover")}>Discover</span>
        </nav>
        <div className="site-nav-action" onClick={() => navigate(session ? "/profile" : "/auth")}>{session ? "Profile" : "Sign in"}</div>
      </header>

      <div className="ai-builder-wrap">
        <div className="ai-builder-head">
          <h1>AI Builder</h1>
          <button type="button" className="ai-btn ghost" onClick={() => navigate("/")}>Back Home</button>
        </div>

        <div className="ai-card ai-input-card">
          <textarea
            rows={3}
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            placeholder="Example: 450000 KZT gaming build, 1440p, upgrade-friendly"
            className="ai-textarea"
          />
          <div>
            <button type="button" className="ai-btn" onClick={runGenerate} disabled={loading}>
              {loading ? "Generating..." : "Generate Build"}
            </button>
          </div>
        </div>

        {error ? (
          <div className="ai-alert error">
            {error}
          </div>
        ) : null}

        {topBuild ? (
          <div className="ai-card">
            <h2>Current Build: {topBuild.label}</h2>
            <div className="ai-parts-grid">
              {topParts.map(({ key, label, part }) => (
                <div key={key} className="ai-part-row">
                  <strong>{label}</strong>
                  <span>{part && part.name ? part.name : "-"}</span>
                  <span>{formatPrice(part && part.priceKzt)}</span>
                </div>
              ))}
            </div>
            <div className="ai-total">
              Total: {formatPrice(topBuild.totals && topBuild.totals.partsTotalKzt)}
            </div>
          </div>
        ) : null}

        <div className="ai-card">
          <h3>Edit With Chat</h3>
          <div className="ai-chat-input-row">
            <input
              type="text"
              value={chatMessage}
              onChange={(event) => setChatMessage(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  runChatEdit();
                }
              }}
              placeholder="Example: make it cheaper by 50000 KZT"
              className="ai-chat-input"
            />
            <button type="button" className="ai-btn" onClick={runChatEdit} disabled={loading || !sessionId}>Send</button>
          </div>

          <div className="ai-chat-log">
            {chatLog.length === 0 ? <p className="ai-muted">No messages yet.</p> : null}
            {chatLog.map((item, index) => (
              <div key={index} className="ai-chat-item">
                <strong>{item.role}:</strong> {item.content}
              </div>
            ))}
          </div>
        </div>

        {Array.isArray(response && response.warnings) && response.warnings.length > 0 ? (
          <div className="ai-alert warn">
            <h4>Warnings</h4>
            <ul>
              {response.warnings.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
    </div>
  );
}
