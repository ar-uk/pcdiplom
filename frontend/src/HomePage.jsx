import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";

export default function HomePage() {
  const navigate = useNavigate();
  const [session] = useState(() => loadSession());

  return (
    <div className="home-shell">
      <header>
        <div className="logo" onClick={() => navigate("/")}><p>PCPartPicker</p></div>
        <nav className="menu">
          <p onClick={() => navigate("/discover")}>Discover</p>
          <p>Guides</p>
          <p onClick={() => navigate("/build")}>Builder</p>
        </nav>
        <div className="profile" onClick={() => navigate(session ? "/profile" : "/auth")}><p>{session ? "Profile" : "Sign In"}</p></div>
      </header>

      <main className="content">
        <div className="textcontent">
          <h1>Ask AI to generate the best build for YOU</h1>
          <h2>Or build one yourself</h2>
        </div>
        <div className="AI">
          <input
            type="text"
            placeholder="Write your prompt here. Keep it simple!(Ex. under 200000 KZT, Full White, )"
          />
          <button type="button"><span>Generate</span></button>
        </div>
      </main>
    </div>
  );
}