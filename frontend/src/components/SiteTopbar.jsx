import { useLocation, useNavigate } from "react-router-dom";
import { loadSession, displayUsername } from "../lib/session.js";
import { useMemo, useState } from "react";

/**
 * Primary site header: brand, route-aware nav highlight, optional @user, action control.
 * Highlighting follows `useLocation()` so it never drifts from the current URL.
 */
export default function SiteTopbar({
  session: sessionProp,
  navAction,
  showAiNav = true,
  role = "banner",
}) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [localSession] = useState(() => loadSession());
  const session = sessionProp !== undefined ? sessionProp : localSession;

  const navClass = useMemo(() => {
    const active = (condition) => (condition ? "active" : undefined);
    return {
      home: active(pathname === "/"),
      discover: active(pathname === "/discover" || pathname.startsWith("/discover/")),
      build: active(pathname === "/build"),
      ai: active(pathname === "/ai-builder"),
      profile: active(pathname === "/profile"),
    };
  }, [pathname]);

  const resolvedAction = useMemo(() => {
    if (navAction) {
      return navAction;
    }
    return {
      label: session ? "Profile" : "Sign in",
      onClick: () => navigate(session ? "/profile" : "/auth"),
    };
  }, [navAction, session, navigate]);

  const user = displayUsername(session);

  return (
    <header className="site-topbar" role={role}>
      <div className="site-brand" onClick={() => navigate("/")}>
        KazPcCraft
      </div>

      <nav className="site-nav" aria-label="Primary">
        <span className={navClass.home} onClick={() => navigate("/")}>
          Home
        </span>
        <span className={navClass.discover} onClick={() => navigate("/discover")}>
          Discover
        </span>
        <span className={navClass.build} onClick={() => navigate("/build")}>
          Builder
        </span>
        {showAiNav ? (
          <span className={navClass.ai} onClick={() => navigate("/ai-builder")}>
            AI Builder
          </span>
        ) : null}
        <span className={navClass.profile} onClick={() => navigate("/profile")}>
          Profile
        </span>
      </nav>

      {user ? (
        <span className="site-nav-user" title={session?.email ?? user}>
          @{user}
        </span>
      ) : null}

      <div
        className="site-nav-action"
        onClick={resolvedAction.onClick}
        role="button"
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            resolvedAction.onClick();
          }
        }}
      >
        {resolvedAction.label}
      </div>
    </header>
  );
}
