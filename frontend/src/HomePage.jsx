import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import SiteTopbar from "./components/SiteTopbar.jsx";

export default function HomePage() {
  const navigate = useNavigate();
  const [session] = useState(() => loadSession());
  const [prompt, setPrompt] = useState("");

  const handleGenerate = () => {
    const trimmed = prompt.trim();
    if (!trimmed) {
      navigate("/ai-builder");
      return;
    }
    navigate("/ai-builder", { state: { prompt: trimmed } });
  };

  return (
    <div className="home-shell">
      <SiteTopbar session={session} />

      <main className="home-main">
        <section className="home-hero" aria-labelledby="home-hero-title">
          <p className="home-hero-eyebrow">KazPcCraft · Command center</p>
          <h1 id="home-hero-title" className="home-hero-title">
            Ask AI to generate the best build for you
          </h1>
          <p className="home-hero-lead">
            Or open the manual builder — same precision, your hands on every slot.
          </p>
          <div className="home-hero-actions">
            <button type="button" className="home-hero-secondary" onClick={() => navigate("/build")}>
              Manual builder
            </button>
            <button type="button" className="home-hero-secondary" onClick={() => navigate("/discover")}>
              Community
            </button>
          </div>
        </section>

        <section className="home-prompt-card" aria-label="AI prompt">
          <p className="home-prompt-label">Neural build request</p>
          <div className="AI">
            <input
              type="text"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  handleGenerate();
                }
              }}
              placeholder="Budget, color theme, use case… (e.g. under 200000 KZT, full white, 1440p gaming)"
            />
            <button type="button" onClick={handleGenerate}><span>Generate</span></button>
          </div>
        </section>
      </main>
    </div>
  );
}
