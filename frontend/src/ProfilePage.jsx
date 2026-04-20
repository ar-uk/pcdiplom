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
  const [myPosts, setMyPosts] = useState([]);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postTitle, setPostTitle] = useState("");
  const [postBody, setPostBody] = useState("");
  const [postTags, setPostTags] = useState("");
  const [postImageUrl, setPostImageUrl] = useState("");
  const [attachedBuildId, setAttachedBuildId] = useState("");

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

      if (!session?.token) {
        if (!alive) return;
        setBuilds([]);
        setBuildsError("Session expired. Please sign in again.");
        navigate("/auth");
        return;
      }

      setBuildsLoading(true);
      setBuildsError("");

      try {
        const response = await fetch(`/api/recommendation/manual-builds?userId=${encodeURIComponent(session.email)}`, {
          headers: {
            Authorization: `Bearer ${session.token}`,
          },
        });

        if (response.status === 401) {
          if (!alive) return;
          clearSession();
          setSession(null);
          setBuilds([]);
          setBuildsError("Session expired. Please sign in again.");
          navigate("/auth");
          return;
        }

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
  }, [session?.email, session?.token, navigate]);

  useEffect(() => {
    let alive = true;

    async function loadMyPosts() {
      if (!session?.email) {
        if (alive) {
          setMyPosts([]);
          setPostsLoading(false);
        }
        return;
      }

      setPostsLoading(true);
      try {
        const response = await fetch("/community/posts?sort=new");
        if (!response.ok) {
          throw new Error("Failed to load community posts");
        }

        const rows = await response.json();
        if (!alive) return;
        const normalizedUser = session.email.trim().toLowerCase();
        setMyPosts((Array.isArray(rows) ? rows : []).filter((post) => (post.authorUserId || "").toLowerCase() === normalizedUser));
      } catch {
        if (!alive) return;
        setMyPosts([]);
      } finally {
        if (alive) setPostsLoading(false);
      }
    }

    loadMyPosts();
    return () => {
      alive = false;
    };
  }, [session?.email]);

  const handleCreatePost = async () => {
    if (!session?.email) {
      navigate("/auth");
      return;
    }

    const title = postTitle.trim();
    const body = postBody.trim();
    if (!title || !body) {
      setError("Post title and text are required.");
      return;
    }

    const selectedBuild = builds.find((build) => String(build.id) === String(attachedBuildId));

    try {
      const response = await fetch("/community/posts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          authorUserId: session.email.trim().toLowerCase(),
          title,
          body,
          buildId: selectedBuild ? Number(selectedBuild.id) : null,
          buildSnapshotJson: selectedBuild ? JSON.stringify(selectedBuild.selectedParts ?? {}) : null,
          tags: postTags
            .split(",")
            .map((tag) => tag.trim())
            .filter(Boolean),
          imageUrls: postImageUrl.trim() ? [postImageUrl.trim()] : [],
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to create post (${response.status})`);
      }

      const created = await response.json();
      setMyPosts((prev) => [created, ...prev]);
      setPostTitle("");
      setPostBody("");
      setPostTags("");
      setPostImageUrl("");
      setAttachedBuildId("");
      setError("");
    } catch (createError) {
      setError(createError.message || "Could not create post.");
    }
  };

  const handleDeletePost = async (postId) => {
    if (!session?.email) {
      navigate("/auth");
      return;
    }

    try {
      const response = await fetch(`/community/posts/${postId}?userId=${encodeURIComponent(session.email.trim().toLowerCase())}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to delete post (${response.status})`);
      }

      setMyPosts((prev) => prev.filter((post) => post.id !== postId));
    } catch (deleteError) {
      setError(deleteError.message || "Could not delete post.");
    }
  };

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
      <header className="site-topbar">
        <div className="site-brand" onClick={() => navigate("/")}>KazPcCraft</div>
        <nav className="site-nav">
          <span onClick={() => navigate("/")}>Home</span>
          <span onClick={() => navigate("/discover")}>Discover</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span className="active" onClick={() => navigate("/profile")}>Profile</span>
        </nav>
        <div className="site-nav-action" onClick={handleLogout}>{session ? "Logout" : "Profile"}</div>
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

        </aside>

        <section className="build-panel">
          <div className="panel-head">
            <div className="community-posts-head">Your builds</div>
            <button type="button" className="new-build-btn" onClick={() => navigate("/build")}>
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

          <div className="community-posts-section">
            <div className="community-posts-head">My posts</div>
            <div className="community-post-composer">
              <input value={postTitle} onChange={(event) => setPostTitle(event.target.value)} placeholder="Post title" />
              <textarea value={postBody} onChange={(event) => setPostBody(event.target.value)} placeholder="Write something about your build..." />
              <input value={postTags} onChange={(event) => setPostTags(event.target.value)} placeholder="Tags (comma separated)" />
              <input value={postImageUrl} onChange={(event) => setPostImageUrl(event.target.value)} placeholder="Image URL (optional)" />
              <select value={attachedBuildId} onChange={(event) => setAttachedBuildId(event.target.value)}>
                <option value="">No attached PC build</option>
                {builds.map((build) => (
                  <option key={build.id} value={build.id}>{build.title ?? `Build #${build.id}`}</option>
                ))}
              </select>
              <button type="button" className="new-build-btn" onClick={handleCreatePost}>Create post</button>
            </div>

            {postsLoading ? <div className="build-subtitle">Loading your posts...</div> : null}
            {!postsLoading && myPosts.length === 0 ? <div className="build-subtitle">No posts yet.</div> : null}

            <div className="my-posts-grid">
              {myPosts.map((post, index) => (
                <div className="build-card" key={post.id ?? index}>
                  <div className="build-image-wrap">
                    <img className="build-image" src={post.imageUrls?.[0] || PLACEHOLDERS[index % PLACEHOLDERS.length]} alt={post.title} />
                    <div className="build-image-overlay">
                      <div className="build-title">{post.title}</div>
                      <div className="build-price">{`Score ${post.score ?? 0}`}</div>
                    </div>
                  </div>
                  <div className="build-subtitle">{post.body}</div>
                  <div className="build-subtitle">{post.buildId ? `Attached PC build #${post.buildId}` : "No attached PC build"}</div>
                  <button type="button" className="build-edit-btn" onClick={() => handleDeletePost(post.id)}>Delete post</button>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}