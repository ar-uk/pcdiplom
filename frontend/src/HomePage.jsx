import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";

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
      <header className="site-topbar">
        <div className="site-brand" onClick={() => navigate("/")}>KazPcCraft</div>
        <nav className="site-nav">
          <span className="active" onClick={() => navigate("/")}>Home</span>
          <span onClick={() => navigate("/discover")}>Discover</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span onClick={() => navigate("/profile")}>Profile</span>
        </nav>
        <div className="site-nav-action" onClick={() => navigate(session ? "/profile" : "/auth")}>{session ? "Profile" : "Sign in"}</div>
      </header>

      <main className="content">
        <div className="textcontent">
          <h1>Ask AI to generate the best build for YOU</h1>
          <h2>Or build one yourself</h2>
        </div>
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
            placeholder="Write your prompt here. Keep it simple!(Ex. under 200000 KZT, Full White, )"
          />
          <button type="button" onClick={handleGenerate}><span>Generate</span></button>
        </div>
      </main>
    </div>
  );
}