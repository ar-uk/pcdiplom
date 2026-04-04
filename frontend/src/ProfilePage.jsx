import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { confirmEnableTwoFactor, disableTwoFactor, enableTwoFactor, logout } from "./lib/authApi.js";
import { clearSession, loadSession, saveSession } from "./lib/session.js";
import "./styles/pages/profile.css";
import profiledflt from "./assets/profiledflt.jpg";
const PLACEHOLDERS = [
  "https://placehold.co/320x220/72c7ff/0b1b24",
  "https://placehold.co/320x220/7a5cff/0b1b24",
  "https://placehold.co/320x220/b06cff/0b1b24",
  "https://placehold.co/320x220/72c7ff/0b1b24",
  "https://placehold.co/320x220/7a5cff/0b1b24",
  "https://placehold.co/320x220/b06cff/0b1b24",
];

function formatKzt(value) {
  const amount = Number(value ?? 0);
  return `${amount.toLocaleString("en-US")} KZT`;
}

export default function ProfilePage() {
  const navigate = useNavigate();
  const [session, setSession] = useState(() => loadSession());
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [builds, setBuilds] = useState([]);
  const [buildsLoading, setBuildsLoading] = useState(false);
  const [buildsError, setBuildsError] = useState("");

  useEffect(() => {
    setSession(loadSession());
  }, []);

  useEffect(() => {
    let alive = true;

    async function loadUserBuilds() {
      if (!session?.email) {
        if (!alive) return;
        setBuilds([]);
        setBuildsError("");
        setBuildsLoading(false);
        return;
      }

      setBuildsLoading(true);
      setBuildsError("");

      try {
        const response = await fetch(`/api/recommendation/manual-builds?userId=${encodeURIComponent(session.email)}`, {
          headers: session?.token
            ? {
              Authorization: `Bearer ${session.token}`,
            }
            : undefined,
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || `Failed to load builds (${response.status})`);
        }

        const rows = await response.json();
        if (!alive) return;
        setBuilds(Array.isArray(rows) ? rows : []);
      } catch (loadError) {
        if (!alive) return;
        setBuilds([]);
        setBuildsError(loadError.message || "Failed to load your builds.");
      } finally {
        if (alive) setBuildsLoading(false);
      }
    }

    loadUserBuilds();
    return () => {
      alive = false;
    };
  }, [session?.email, session?.token]);

  const handleLogout = async () => {
    setBusy(true);
    setError("");
    try {
      if (session?.token) {
        await logout(session.token);
      }
    } catch {
      // Ignore logout transport errors and clear the local session anyway.
    }
    clearSession();
    navigate("/");
    setBusy(false);
  };

  const handleEnableTwoFactor = async () => {
    if (!session?.token) {
      navigate("/auth");
      return;
    }

    setBusy(true);
    setError("");
    setStatus("");
    try {
      const result = await enableTwoFactor(session.token);
      if (result.response.status === 202) {
        setChallenge(result.data);
        setStatus(result.data?.message ?? "Verification code sent.");
      } else {
        const serverMessage = result.data?.message ?? result.data?.error ?? result.response.statusText;
        setError(
          serverMessage
            ? `Unable to request 2FA code (${result.response.status}): ${serverMessage}`
            : `Unable to request 2FA code (${result.response.status}).`,
        );
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const handleConfirmTwoFactor = async (event) => {
    event.preventDefault();
    if (!challenge?.challengeId || !session?.token) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const result = await confirmEnableTwoFactor(
        { challengeId: challenge.challengeId, code },
        session.token,
      );

      if (!result.response.ok) {
        setError(result.data?.message ?? "Unable to enable 2FA.");
        return;
      }

      const nextSession = { ...session, verified: true };
      saveSession(nextSession);
      setSession(nextSession);
      setChallenge(null);
      setCode("");
      setStatus("2FA is now enabled for this account.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const handleDisableTwoFactor = async () => {
    if (!session?.token) {
      navigate("/auth");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const result = await disableTwoFactor(session.token);
      if (!result.response.ok) {
        const serverMessage = result.data?.message ?? result.data?.error ?? result.response.statusText;
        setError(
          serverMessage
            ? `Unable to disable 2FA (${result.response.status}): ${serverMessage}`
            : `Unable to disable 2FA (${result.response.status}).`,
        );
        return;
      }

      const nextSession = { ...session, verified: false };
      saveSession(nextSession);
      setSession(nextSession);
      setStatus("2FA has been turned off.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };


  return (
    <div className="profile-page">
      <header className="topbar">
        <div className="logo">KazPcCraft</div>
        <nav className="menu">
          <span onClick={() => navigate("/discover")}>Discover</span>
          <span>Guides</span>
          <span onClick={() => navigate("/build")}>Builder</span>
        </nav>
        <div className="profile-link" onClick={handleLogout}>
          {session ? "Logout" : "Profile"}
        </div>
      </header>

      <main className="profile-layout">
        <aside className="sidebar">
          <div className="user-card">
            <img className="avatar-box" src={profiledflt} alt="Profile avatar" />

            <div className="user-info">
              <div>{session?.username ?? "Sign in required"}</div>
              <div>{session?.email ?? "No account loaded"}</div>
              <div>2FA {session?.verified ? "On" : "Off"}</div>
              <div>{session?.role ?? "Guest"}</div>
            </div>
          </div>

          <div className="security-card">
            <div className="security-title">Account security</div>
            <p>{session?.verified ? "This account uses email 2FA." : "Turn on email 2FA for extra login protection."}</p>
            {error ? <div className="security-error">{error}</div> : null}
            {status ? <div className="security-status">{status}</div> : null}
            {session ? (
              <div className="security-actions">
                <button type="button" onClick={handleEnableTwoFactor} disabled={busy || session.verified}>
                  Enable 2FA
                </button>
                <button type="button" className="ghost-btn" onClick={handleDisableTwoFactor} disabled={busy || !session.verified}>
                  Disable 2FA
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => navigate("/auth")}>
                Sign in to manage security
              </button>
            )}
          </div>

          {challenge ? (
            <form className="security-card verify-card" onSubmit={handleConfirmTwoFactor}>
              <div className="security-title">Confirm 2FA</div>
              <p>Enter the code sent to {challenge.email}.</p>
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                placeholder="123456"
                value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
              />
              <button type="submit" disabled={busy || code.length !== 6}>
                Confirm 2FA
              </button>
            </form>
          ) : null}

          <div className="side-empty" />
        </aside>

        <section className="build-panel">
          <div className="tabs">
            <span>Favorites</span>
            <span>Your Work</span>
            <span>WishList</span>
            <span>BlackList</span>
            <div className="search-bar" />
            <button
              type="button"
              className="new-build-btn"
              onClick={() => navigate("/build")}
            >
              New Build
            </button>
          </div>

          <div className="build-grid">
            {buildsLoading ? <div className="build-subtitle">Loading your builds...</div> : null}
            {!buildsLoading && buildsError ? <div className="build-subtitle">{buildsError}</div> : null}
            {!buildsLoading && !buildsError && builds.length === 0 ? (
              <div className="build-subtitle">No saved builds yet. Create one in Builder and save it.</div>
            ) : null}
            {builds.map((b, index) => (
              <div className="build-card" key={b.id ?? index}>
                <div className="build-image-wrap">
                  <img
                    className="build-image"
                    src={PLACEHOLDERS[index % PLACEHOLDERS.length]}
                    alt="PC build"
                  />
                  <div className="build-image-overlay">
                    <div className="build-title">{b.title ?? `Build #${b.id}`}</div>
                    <div className="build-price">{formatKzt(b.totalPrice)}</div>
                  </div>
                </div>
                <div className="build-subtitle">
                  {b.updatedAt
                    ? `Updated ${new Date(b.updatedAt).toLocaleString()}`
                    : b.createdAt
                    ? `Created ${new Date(b.createdAt).toLocaleString()}`
                    : `Build ID ${b.id ?? "-"}`}
                </div>
                <button
                  type="button"
                  className="build-edit-btn"
                  onClick={() =>
                    navigate("/build", {
                      state: {
                        editBuild: {
                          id: b.id,
                          title: b.title,
                          selectedParts: b.selectedParts ?? {},
                        },
                      },
                    })
                  }
                >
                  Edit build
                </button>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}