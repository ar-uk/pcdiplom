import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { confirmEnableTwoFactor, disableTwoFactor, enableTwoFactor, logout } from "./lib/authApi.js";
import { clearSession, canonicalUserId, displayUsername, loadSession, saveSession, userIdCandidates } from "./lib/session.js";
import { mergeUniqueTagStrings, tagsStringToList } from "./lib/tagUtils.js";
import TagPickerModal from "./components/TagPickerModal.jsx";
import SiteTopbar from "./components/SiteTopbar.jsx";
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

function formatSessionExpiry(epochMillis) {
  const n = Number(epochMillis);
  if (!Number.isFinite(n) || n <= 0) {
    return "—";
  }
  try {
    return new Date(n).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
  } catch {
    return "—";
  }
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
  const [postComposerOpen, setPostComposerOpen] = useState(false);
  const [tagPickerOpen, setTagPickerOpen] = useState(false);

  useEffect(() => {
    setSession(loadSession());
  }, []);

  useEffect(() => {
    if (!postComposerOpen) {
      setTagPickerOpen(false);
    }
  }, [postComposerOpen]);

  useEffect(() => {
    let alive = true;

    async function loadUserBuilds() {
      const ids = userIdCandidates(session);
      if (ids.length === 0) {
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
        const headers = { Authorization: `Bearer ${session.token}` };
        const merged = [];
        for (const userId of ids) {
          const response = await fetch(`/api/recommendation/manual-builds?userId=${encodeURIComponent(userId)}`, {
            headers,
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
            continue;
          }

          const rows = await response.json();
          if (Array.isArray(rows)) {
            merged.push(...rows);
          }
        }

        if (!alive) return;
        const deduped = Array.from(new Map(merged.map((build) => [String(build.id), build])).values());
        deduped.sort((a, b) => {
          const ta = a?.updatedAt != null ? new Date(a.updatedAt).getTime() : 0;
          const tb = b?.updatedAt != null ? new Date(b.updatedAt).getTime() : 0;
          return tb - ta;
        });
        setBuilds(deduped);
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
  }, [session?.username, session?.email, session?.token, navigate]);

  useEffect(() => {
    let alive = true;

    async function loadMyPosts() {
      const ids = userIdCandidates(session);
      if (ids.length === 0) {
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
        const authorIds = new Set(userIdCandidates(session));
        setMyPosts(
          (Array.isArray(rows) ? rows : []).filter((post) => authorIds.has((post.authorUserId || "").toLowerCase())),
        );
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
  }, [session?.username, session?.email]);

  const handleCreatePost = async () => {
    const live = loadSession();
    const token = live?.token;
    const posterId = canonicalUserId(live);
    if (!posterId || !token) {
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
    const selectedParts = selectedBuild?.selectedParts ?? {};
    const partsSum = Object.values(selectedParts).reduce((acc, part) => {
      if (!part || typeof part !== "object") return acc;
      const n = Number(part.price ?? 0);
      return acc + (Number.isFinite(n) ? n : 0);
    }, 0);
    const buildSnapshot =
      selectedBuild
        ? {
            selectedParts,
            totalPrice:
              typeof selectedBuild.totalPrice === "number" && !Number.isNaN(selectedBuild.totalPrice)
                ? selectedBuild.totalPrice
                : partsSum > 0
                  ? partsSum
                  : null,
          }
        : null;

    try {
      const response = await fetch("/community/posts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          authorUserId: posterId,
          title,
          body,
          buildId: selectedBuild ? Number(selectedBuild.id) : null,
          buildSnapshotJson: buildSnapshot ? JSON.stringify(buildSnapshot) : null,
          tags: tagsStringToList(postTags),
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
      setPostComposerOpen(false);
      setError("");
    } catch (createError) {
      setError(createError.message || "Could not create post.");
    }
  };

  const handleDeletePost = async (postId) => {
    const uid = canonicalUserId(session);
    if (!uid) {
      navigate("/auth");
      return;
    }

    try {
      const response = await fetch(`/community/posts/${postId}?userId=${encodeURIComponent(uid)}`, {
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
      <SiteTopbar
        session={session}
        navAction={{
          label: session ? "Sign out" : "Sign in",
          onClick: () => (session ? handleLogout() : navigate("/auth")),
        }}
      />

      <main className="profile-layout">
        <aside className="sidebar">
          <div className="user-card">
            <img className="user-card-avatar" src={profiledflt} alt="Profile avatar" />

            <div className="user-info-panel">
              {session ? (
                <>
                  <div className="user-display-name">{displayUsername(session) ? `@${displayUsername(session)}` : "Account"}</div>
                  <dl className="user-info-dl">
                    <div className="user-info-row">
                      <dt>Username</dt>
                      <dd>{session.username || "—"}</dd>
                    </div>
                    <div className="user-info-row">
                      <dt>Email</dt>
                      <dd className="user-info-email">{session.email || "—"}</dd>
                    </div>
                    <div className="user-info-row">
                      <dt>Role</dt>
                      <dd>{session.role || "—"}</dd>
                    </div>
                    <div className="user-info-row">
                      <dt>2FA</dt>
                      <dd>{session.verified ? "Enabled" : "Off"}</dd>
                    </div>
                    <div className="user-info-row">
                      <dt>Session expires</dt>
                      <dd>{formatSessionExpiry(session.expiresAtEpochMillis)}</dd>
                    </div>
                  </dl>
                </>
              ) : (
                <>
                  <div className="user-display-name">Guest</div>
                  <p className="user-info-guest-hint">Sign in to save builds and post to the community.</p>
                </>
              )}
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
            <div className="community-posts-toolbar">
              <div className="community-posts-head">My posts</div>
              {postComposerOpen ? (
                <button type="button" className="profile-composer-toggle ghost" onClick={() => setPostComposerOpen(false)}>
                  Close editor
                </button>
              ) : (
                <button type="button" className="profile-composer-toggle" onClick={() => setPostComposerOpen(true)}>
                  New post
                </button>
              )}
            </div>

            {postComposerOpen ? (
              <div className="community-post-composer">
                <input value={postTitle} onChange={(event) => setPostTitle(event.target.value)} placeholder="Post title" />
                <textarea value={postBody} onChange={(event) => setPostBody(event.target.value)} placeholder="Write something about your build..." />
                <div className="composer-tags-row">
                  <button type="button" className="tags-mini-btn" onClick={() => setTagPickerOpen(true)}>
                    Tags
                  </button>
                  <span className="composer-tags-preview">
                    {postTags.trim()
                      ? postTags
                          .split(/[,;]+/)
                          .map((t) => t.trim())
                          .filter(Boolean)
                          .join(" · ")
                      : "No tags selected"}
                  </span>
                </div>
                <input
                  className="composer-tags-text"
                  type="text"
                  value={postTags}
                  onChange={(event) => setPostTags(event.target.value)}
                  placeholder="Tags (comma or semicolon) — type your own or merge from Tags"
                />
                <input value={postImageUrl} onChange={(event) => setPostImageUrl(event.target.value)} placeholder="Image URL (optional)" />
                <select value={attachedBuildId} onChange={(event) => setAttachedBuildId(event.target.value)}>
                  <option value="">No attached PC build</option>
                  {builds.map((build) => (
                    <option key={build.id} value={build.id}>{build.title ?? `Build #${build.id}`}</option>
                  ))}
                </select>
                <button type="button" className="new-build-btn" onClick={handleCreatePost}>Create post</button>
              </div>
            ) : null}

            <TagPickerModal
              open={tagPickerOpen}
              onClose={() => setTagPickerOpen(false)}
              selectedTags={postTags
                .split(/[,;]+/)
                .map((t) => t.trim())
                .filter(Boolean)}
              onApply={(tags) => setPostTags((prev) => mergeUniqueTagStrings(prev, tags.join(", ")))}
            />

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