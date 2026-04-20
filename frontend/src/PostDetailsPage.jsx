import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./styles/pages/post-details.css";

const PART_ORDER = ["cpu", "motherboard", "gpu", "ram", "storage", "storage_1", "storage_2", "storage_3", "psu", "case", "cooling"];

function formatKzt(value) {
  const amount = Number(value ?? 0);
  return `${amount.toLocaleString("en-US")} KZT`;
}

function estimatePower(selectedParts) {
  if (!selectedParts || typeof selectedParts !== "object") {
    return 0;
  }

  const cpu = Number(selectedParts.cpu?.powerHint ?? selectedParts.cpu?.wattage ?? 65);
  const gpu = Number(selectedParts.gpu?.powerHint ?? selectedParts.gpu?.wattage ?? 200);
  const base = 90;
  return cpu + gpu + base;
}

function parseBuildSnapshot(raw) {
  if (!raw) return null;
  try {
    let parsed = typeof raw === "string" ? JSON.parse(raw) : raw;
    if (typeof parsed === "string") {
      parsed = JSON.parse(parsed);
    }
    const selectedParts = parsed?.selectedParts && typeof parsed.selectedParts === "object"
      ? parsed.selectedParts
      : (parsed && typeof parsed === "object" ? parsed : {});
    const totalPrice = parsed?.totalPrice ?? null;
    const estimatedPower = parsed?.estimatedPower ?? estimatePower(selectedParts);
    return { selectedParts, totalPrice, estimatedPower };
  } catch {
    return null;
  }
}

export default function PostDetailsPage() {
  const navigate = useNavigate();
  const { postId } = useParams();
  const [session] = useState(() => loadSession());
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [attachedBuild, setAttachedBuild] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [commentDraft, setCommentDraft] = useState("");

  const userId = session?.email?.trim().toLowerCase() || session?.username?.trim().toLowerCase() || null;
  const authHeaders = session?.token ? { Authorization: `Bearer ${session.token}` } : undefined;

  useEffect(() => {
    let alive = true;

    async function loadPostAndComments() {
      if (!postId) return;
      setLoading(true);
      setError("");

      try {
        const [postRes, commentsRes] = await Promise.all([
          fetch(`/community/posts/${postId}`, { headers: authHeaders }),
          fetch(`/community/posts/${postId}/comments`, { headers: authHeaders }),
        ]);

        if (!postRes.ok) {
          const text = await postRes.text();
          throw new Error(text || `Failed to load post (${postRes.status})`);
        }
        if (!commentsRes.ok) {
          const text = await commentsRes.text();
          throw new Error(text || `Failed to load comments (${commentsRes.status})`);
        }

        const postData = await postRes.json();
        const commentsData = await commentsRes.json();

        if (!alive) return;
        setPost(postData);
        setComments(Array.isArray(commentsData) ? commentsData : []);
      } catch (loadError) {
        if (!alive) return;
        setError(loadError.message || "Failed to load post details.");
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadPostAndComments();
    return () => {
      alive = false;
    };
  }, [postId, session?.token]);

  useEffect(() => {
    let alive = true;

    async function loadAttachedBuildFallback() {
      if (!post?.buildId || !post?.authorUserId) {
        if (alive) setAttachedBuild(null);
        return;
      }

      try {
        const response = await fetch(`/api/recommendation/manual-builds?userId=${encodeURIComponent(post.authorUserId)}`, {
          headers: authHeaders,
        });
        if (!response.ok) {
          if (alive) setAttachedBuild(null);
          return;
        }

        const rows = await response.json();
        if (!alive) return;
        const found = (Array.isArray(rows) ? rows : []).find((row) => String(row.id) === String(post.buildId));
        setAttachedBuild(found || null);
      } catch {
        if (!alive) return;
        setAttachedBuild(null);
      }
    }

    loadAttachedBuildFallback();
    return () => {
      alive = false;
    };
  }, [post?.buildId, post?.authorUserId, session?.token]);

  const buildSnapshot = useMemo(() => parseBuildSnapshot(post?.buildSnapshotJson), [post?.buildSnapshotJson]);

  const effectiveBuild = useMemo(() => {
    const snapshotParts = buildSnapshot?.selectedParts && Object.keys(buildSnapshot.selectedParts).length > 0
      ? buildSnapshot.selectedParts
      : null;

    if (snapshotParts) {
      return {
        selectedParts: snapshotParts,
        totalPrice: buildSnapshot?.totalPrice,
        estimatedPower: buildSnapshot?.estimatedPower ?? estimatePower(snapshotParts),
      };
    }

    if (attachedBuild?.selectedParts && typeof attachedBuild.selectedParts === "object") {
      return {
        selectedParts: attachedBuild.selectedParts,
        totalPrice: attachedBuild.totalPrice ?? null,
        estimatedPower: estimatePower(attachedBuild.selectedParts),
      };
    }

    return null;
  }, [buildSnapshot, attachedBuild]);

  const orderedParts = useMemo(() => {
    const source = effectiveBuild?.selectedParts;
    if (!source || typeof source !== "object") {
      return [];
    }

    const entries = Object.entries(source).filter(([, part]) => part && typeof part === "object");
    const rank = new Map(PART_ORDER.map((key, index) => [key, index]));
    return entries.sort((a, b) => (rank.get(a[0]) ?? 999) - (rank.get(b[0]) ?? 999));
  }, [effectiveBuild]);

  const submitComment = async () => {
    if (!userId) {
      navigate("/auth");
      return;
    }

    const body = commentDraft.trim();
    if (!body || !postId) return;

    try {
      const response = await fetch(`/community/posts/${postId}/comments`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(authHeaders || {}),
        },
        body: JSON.stringify({
          authorUserId: userId,
          parentCommentId: null,
          body,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to post comment (${response.status})`);
      }

      const created = await response.json();
      setComments((prev) => [...prev, created]);
      setCommentDraft("");
    } catch (commentError) {
      alert(commentError.message || "Could not post comment.");
    }
  };

  const handleCopyBuildToEditor = () => {
    if (!effectiveBuild?.selectedParts) {
      return;
    }

    navigate("/build", {
      state: {
        editBuild: {
          title: `Copy of ${post?.title ?? "Community build"}`,
          selectedParts: effectiveBuild.selectedParts,
        },
      },
    });
  };

  return (
    <div className="post-details-page">
      <header className="site-topbar">
        <div className="site-brand" onClick={() => navigate("/")}>KazPcCraft</div>
        <nav className="site-nav">
          <span onClick={() => navigate("/")}>Home</span>
          <span className="active" onClick={() => navigate("/discover")}>Discover</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span onClick={() => navigate("/profile")}>Profile</span>
        </nav>
        <div className="site-nav-action" onClick={() => navigate(session ? "/profile" : "/auth")}>{session ? "Profile" : "Sign in"}</div>
      </header>

      <main className="post-details-layout">
        {loading ? <div className="panel">Loading post...</div> : null}
        {!loading && error ? <div className="panel">{error}</div> : null}

        {!loading && !error && post ? (
          <>
            <section className="panel">
              <h1>{post.title}</h1>
              <div className="meta">{`by @${post.authorUserId}`}</div>
              <p className="body">{post.body}</p>
            </section>

            <section className="panel">
              <h2>Attached Build</h2>
              {effectiveBuild ? (
                <>
                  <div className="build-summary">
                    <span>{`Build ID: ${post.buildId ?? "-"}`}</span>
                    <span>{`Total price: ${formatKzt(effectiveBuild.totalPrice)}`}</span>
                    <span>{`Estimated wattage: ${effectiveBuild.estimatedPower ?? 0}W`}</span>
                  </div>
                  <button type="button" className="copy-build-btn" onClick={handleCopyBuildToEditor}>
                    Copy Build To Edit
                  </button>
                  <div className="parts-grid">
                    {orderedParts.map(([slot, part]) => (
                      <div key={slot} className="part-card">
                        <div className="part-slot">{slot}</div>
                        <div className="part-name">{part.name || "Unknown part"}</div>
                        <div className="part-meta">{part.price ? formatKzt(part.price) : "Price unavailable"}</div>
                        <div className="part-meta">{part.wattage || part.powerHint ? `${part.wattage ?? part.powerHint}W` : "Wattage unavailable"}</div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <div className="meta">No build was attached to this post.</div>
              )}
            </section>

            <section className="panel">
              <h2>Comments</h2>
              <div className="comment-list">
                {comments.length === 0 ? <div className="meta">No comments yet.</div> : null}
                {comments.map((comment) => (
                  <div key={comment.id} className="comment-item">
                    <div className="comment-author">{`@${comment.authorUserId}`}</div>
                    <div>{comment.body}</div>
                  </div>
                ))}
              </div>

              <div className="comment-compose">
                <input
                  value={commentDraft}
                  onChange={(event) => setCommentDraft(event.target.value)}
                  placeholder="Write a comment..."
                />
                <button type="button" onClick={submitComment}>Post</button>
              </div>
            </section>
          </>
        ) : null}
      </main>
    </div>
  );
}