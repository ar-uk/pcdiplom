import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { login, register, verifyTwoFactor } from "./lib/authApi.js";
import { loadSession, saveSession } from "./lib/session.js";
import "./styles/pages/auth.css";

const emptyRegisterForm = {
  username: "",
  email: "",
  password: "",
};

const emptyLoginForm = {
  email: "",
  password: "",
};

function toSession(data) {
  return {
    token: data.token,
    username: data.username,
    email: data.email,
    role: data.role,
    verified: data.verified,
    expiresAtEpochMillis: data.expiresAtEpochMillis,
  };
}

export default function AuthPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState("login");
  const [registerForm, setRegisterForm] = useState(emptyRegisterForm);
  const [loginForm, setLoginForm] = useState(emptyLoginForm);
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loggedInSession = useMemo(() => loadSession(), []);

  useEffect(() => {
    if (loggedInSession?.token) {
      navigate("/profile", { replace: true });
    }
  }, [loggedInSession, navigate]);

  const submitRegister = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const result = await register(registerForm);
      if (!result.response.ok) {
        setError(result.data?.message ?? "Registration failed.");
        return;
      }

      saveSession(toSession(result.data));
      navigate("/profile");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  const submitLogin = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const result = await login(loginForm);
      if (result.response.status === 202 && result.data?.challengeId) {
        setChallenge(result.data);
        setMessage(result.data.message ?? "Verification code sent.");
        setCode("");
        return;
      }

      if (!result.response.ok) {
        setError(result.data?.message ?? "Login failed.");
        return;
      }

      saveSession(toSession(result.data));
      navigate("/profile");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  const submitVerify = async (event) => {
    event.preventDefault();
    if (!challenge?.challengeId) {
      return;
    }

    setLoading(true);
    setError("");

    try {
      const result = await verifyTwoFactor({ challengeId: challenge.challengeId, code });
      if (!result.response.ok) {
        setError(result.data?.message ?? "Verification failed.");
        return;
      }

      saveSession(toSession(result.data));
      navigate("/profile");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <header className="auth-topbar">
        <div className="auth-brand" onClick={() => navigate("/")}>KazPcCraft</div>
        <nav className="auth-nav">
          <span onClick={() => navigate("/")}>Home</span>
          <span onClick={() => navigate("/discover")}>Discover</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span onClick={() => navigate("/profile")}>Profile</span>
        </nav>
      </header>

      <main className="auth-layout">
        <section className="auth-card">
          <div className="auth-tabs">
            <button className={tab === "login" ? "active" : ""} type="button" onClick={() => { setTab("login"); setChallenge(null); setError(""); setMessage(""); }}>
              Login
            </button>
            <button className={tab === "register" ? "active" : ""} type="button" onClick={() => { setTab("register"); setChallenge(null); setError(""); setMessage(""); }}>
              Register
            </button>
          </div>

          {error ? <div className="auth-alert error">{error}</div> : null}
          {message ? <div className="auth-alert success">{message}</div> : null}

          {tab === "register" ? (
            <form className="auth-form" onSubmit={submitRegister}>
              <label>
                Username
                <input
                  type="text"
                  value={registerForm.username}
                  onChange={(event) => setRegisterForm({ ...registerForm, username: event.target.value })}
                  placeholder="johnsmith"
                  required
                />
              </label>
              <label>
                Email
                <input
                  type="email"
                  value={registerForm.email}
                  onChange={(event) => setRegisterForm({ ...registerForm, email: event.target.value })}
                  placeholder="john@example.com"
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={registerForm.password}
                  onChange={(event) => setRegisterForm({ ...registerForm, password: event.target.value })}
                  placeholder="Strong password"
                  required
                />
              </label>
              <button type="submit" disabled={loading}>
                Create account
              </button>
            </form>
          ) : challenge ? (
            <form className="auth-form" onSubmit={submitVerify}>
              <label>
                Challenge
                <input type="text" value={challenge.challengeId} readOnly />
              </label>
              <label>
                Verification code
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
                  placeholder="123456"
                  required
                />
              </label>
              <button type="submit" disabled={loading || code.length !== 6}>
                Verify code
              </button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={submitLogin}>
              <label>
                Email
                <input
                  type="email"
                  value={loginForm.email}
                  onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
                  placeholder="john@example.com"
                  required
                />
              </label>
              <label>
                Password
                <input
                  type="password"
                  value={loginForm.password}
                  onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
                  placeholder="Your password"
                  required
                />
              </label>
              <button type="submit" disabled={loading}>
                Sign in
              </button>
            </form>
          )}

          <div className="auth-footer">
            <button type="button" className="text-button" onClick={() => navigate("/")}>Back to home</button>
            <button type="button" className="text-button" onClick={() => navigate("/discover")}>Browse builds</button>
          </div>
        </section>
      </main>
    </div>
  );
}