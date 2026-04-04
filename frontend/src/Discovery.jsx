import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { loadSession } from "./lib/session.js";
import "./styles/pages/discovery.css";

const placeholders = [
  "https://placehold.co/520x340/0e2a35/ffffff?text=PC+Build+Preview+1",
  "https://placehold.co/520x340/123444/ffffff?text=PC+Build+Preview+2",
  "https://placehold.co/520x340/17384a/ffffff?text=PC+Build+Preview+3",
  "https://placehold.co/520x340/1b2f56/ffffff?text=PC+Build+Preview+4",
];

const FEATURED_PLACEHOLDERS = [
  "https://placehold.co/900x500/17384a/e8f6ff?text=FEATURED+BUILD+1",
  "https://placehold.co/900x500/1b2f56/e8f6ff?text=FEATURED+BUILD+2",
  "https://placehold.co/900x500/143a31/e8f6ff?text=FEATURED+BUILD+3",
  "https://placehold.co/900x500/4a234f/e8f6ff?text=FEATURED+BUILD+4",
];

function estimatePower(selectedParts) {
  if (!selectedParts || typeof selectedParts !== "object") {
    return 0;
  }
  const cpu = Number(selectedParts.cpu?.powerHint ?? selectedParts.cpu?.wattage ?? 65);
  const gpu = Number(selectedParts.gpu?.powerHint ?? selectedParts.gpu?.wattage ?? 200);
  return cpu + gpu + 90;
}

export default function DiscoveryPage() {
  const navigate = useNavigate();
  const carouselRef = useRef(null);
  const [session] = useState(() => loadSession());
  const [posts, setPosts] = useState([]);
  const [tags, setTags] = useState([]);
  const [sort, setSort] = useState("new");
  const [activeTag, setActiveTag] = useState("All");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [commentsByPost, setCommentsByPost] = useState({});
  const [commentsOpen, setCommentsOpen] = useState({});
  const [commentsLoading, setCommentsLoading] = useState({});
  const [commentDrafts, setCommentDrafts] = useState({});
  const [builds, setBuilds] = useState([]);
  const [composerOpen, setComposerOpen] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [newBody, setNewBody] = useState("");
  const [newTags, setNewTags] = useState("");
  const [newImageUrl, setNewImageUrl] = useState("");
  const [attachBuildId, setAttachBuildId] = useState("");

  const userId = session?.email?.trim().toLowerCase() || session?.username?.trim().toLowerCase() || null;
  const authHeaders = session?.token
    ? {
        Authorization: `Bearer ${session.token}`,
      }
    : undefined;

  useEffect(() => {
    let alive = true;

    async function loadTags() {
      try {
        const response = await fetch("/community/tags", {
          headers: authHeaders,
        });
        if (!response.ok) {
          throw new Error("Failed to load tags");
        }
        const data = await response.json();
        if (!alive) return;
        setTags(Array.isArray(data) ? data : []);
      } catch {
        if (!alive) return;
        setTags([]);
      }
    }

    loadTags();
    return () => {
      alive = false;
    };
  }, [session?.token]);

  useEffect(() => {
    let alive = true;

    async function loadPosts() {
      setLoading(true);
      setError("");
      try {
        const params = new URLSearchParams();
        params.set("sort", sort);
        if (activeTag !== "All") {
          params.set("tag", activeTag);
        }

        const response = await fetch(`/community/posts?${params.toString()}`, {
          headers: authHeaders,
        });
        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || `Failed to load posts (${response.status})`);
        }

        const data = await response.json();
        if (!alive) return;
        setPosts(Array.isArray(data) ? data : []);
      } catch (loadError) {
        if (!alive) return;
        setPosts([]);
        setError(loadError.message || "Failed to load community posts.");
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadPosts();
    return () => {
      alive = false;
    };
  }, [sort, activeTag, session?.token]);

  useEffect(() => {
    let alive = true;

    async function loadUserBuilds() {
      const candidateIds = [session?.email, session?.username]
        .filter(Boolean)
        .map((value) => value.trim().toLowerCase())
        .filter(Boolean);

      if (candidateIds.length === 0) {
        if (alive) setBuilds([]);
        return;
      }

      try {
        const loaded = [];
        for (const candidateId of candidateIds) {
          const response = await fetch(`/api/recommendation/manual-builds?userId=${encodeURIComponent(candidateId)}`, {
            headers: authHeaders,
          });
          if (!response.ok) {
            continue;
          }
          const rows = await response.json();
          if (Array.isArray(rows)) {
            loaded.push(...rows);
          }
        }

        if (!alive) return;
        const deduped = Array.from(new Map(loaded.map((build) => [String(build.id), build])).values());
        setBuilds(deduped);
      } catch {
        if (!alive) return;
        setBuilds([]);
      }
    }

    loadUserBuilds();
    return () => {
      alive = false;
    };
  }, [session?.email, session?.username, session?.token]);

  const featuredBuilds = useMemo(() => {
    return posts.slice(0, 4).map((post, index) => ({
      id: post.id,
      title: post.title,
      author: `by @${post.authorUserId}`,
      img: post.imageUrls?.[0] || FEATURED_PLACEHOLDERS[index % FEATURED_PLACEHOLDERS.length],
      tags: Array.isArray(post.tags) ? post.tags.slice(0, 3) : [],
      score: post.score ?? 0,
      commentCount: post.commentCount ?? 0,
    }));
  }, [posts]);

  const browseTags = useMemo(() => {
    const mapped = tags.map((tag) => tag.slug).filter(Boolean);
    return ["All", ...mapped];
  }, [tags]);

  const castVote = async (targetId, value) => {
    if (!userId) {
      navigate("/auth");
      return;
    }

    try {
      const response = await fetch("/community/votes", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(authHeaders || {}),
        },
        body: JSON.stringify({
          userId,
          targetType: "POST",
          targetId,
          value,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Vote failed (${response.status})`);
      }

      const voteResult = await response.json();
      setPosts((prev) =>
        prev.map((post) =>
          post.id === targetId
            ? {
                ...post,
                score: voteResult.score,
              }
            : post
        )
      );
    } catch (voteError) {
      alert(voteError.message || "Could not vote right now.");
    }
  };

  const loadComments = async (postId) => {
    setCommentsLoading((prev) => ({ ...prev, [postId]: true }));
    try {
      const response = await fetch(`/community/posts/${postId}/comments`, {
        headers: authHeaders,
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to load comments (${response.status})`);
      }
      const data = await response.json();
      setCommentsByPost((prev) => ({
        ...prev,
        [postId]: Array.isArray(data) ? data : [],
      }));
    } catch (commentError) {
      alert(commentError.message || "Could not load comments.");
      setCommentsByPost((prev) => ({ ...prev, [postId]: [] }));
    } finally {
      setCommentsLoading((prev) => ({ ...prev, [postId]: false }));
    }
  };

  const toggleComments = async (postId) => {
    const willOpen = !commentsOpen[postId];
    setCommentsOpen((prev) => ({ ...prev, [postId]: willOpen }));
    if (willOpen && !commentsByPost[postId]) {
      await loadComments(postId);
    }
  };

  const submitComment = async (postId) => {
    if (!userId) {
      navigate("/auth");
      return;
    }

    const body = (commentDrafts[postId] || "").trim();
    if (!body) {
      return;
    }

    try {
      const response = await fetch(`/community/posts/${postId}/comments`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(authHeaders || {}),
        },
        body: JSON.stringify({
          authorUserId: userId,
          body,
          parentCommentId: null,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to post comment (${response.status})`);
      }

      const created = await response.json();
      setCommentsByPost((prev) => ({
        ...prev,
        [postId]: [...(prev[postId] || []), created],
      }));
      setPosts((prev) =>
        prev.map((post) =>
          post.id === postId
            ? { ...post, commentCount: (post.commentCount ?? 0) + 1 }
            : post
        )
      );
      setCommentDrafts((prev) => ({ ...prev, [postId]: "" }));
    } catch (commentError) {
      alert(commentError.message || "Could not post comment.");
    }
  };

  const castCommentVote = async (postId, commentId, value) => {
    if (!userId) {
      navigate("/auth");
      return;
    }

    try {
      const response = await fetch("/community/votes", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...(authHeaders || {}),
        },
        body: JSON.stringify({
          userId,
          targetType: "COMMENT",
          targetId: commentId,
          value,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Vote failed (${response.status})`);
      }

      const voteResult = await response.json();
      setCommentsByPost((prev) => ({
        ...prev,
        [postId]: (prev[postId] || []).map((comment) =>
          comment.id === commentId ? { ...comment, score: voteResult.score } : comment
        ),
      }));
    } catch (voteError) {
      alert(voteError.message || "Could not vote comment.");
    }
  };

  const handleCreatePost = async () => {
    if (!userId) {
      navigate("/auth");
      return;
    }

    const title = newTitle.trim();
    const body = newBody.trim();
    if (!title || !body) {
      alert("Title and content are required.");
      return;
    }

    const selectedBuild = builds.find((build) => String(build.id) === String(attachBuildId));
    const selectedParts = selectedBuild?.selectedParts ?? {};
    const buildSnapshot = selectedBuild
      ? {
          selectedParts,
          totalPrice: selectedBuild.totalPrice ?? null,
          estimatedPower: estimatePower(selectedParts),
        }
      : null;

    try {
      const response = await fetch("/community/posts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(authHeaders || {}),
        },
        body: JSON.stringify({
          authorUserId: userId,
          title,
          body,
          buildId: selectedBuild ? Number(selectedBuild.id) : null,
          buildSnapshotJson: buildSnapshot ? JSON.stringify(buildSnapshot) : null,
          tags: newTags
            .split(",")
            .map((tag) => tag.trim())
            .filter(Boolean),
          imageUrls: newImageUrl.trim() ? [newImageUrl.trim()] : [],
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Failed to create post (${response.status})`);
      }

      const created = await response.json();
      setPosts((prev) => [created, ...prev]);
      setComposerOpen(false);
      setNewTitle("");
      setNewBody("");
      setNewTags("");
      setNewImageUrl("");
      setAttachBuildId("");
    } catch (createError) {
      alert(createError.message || "Could not create post.");
    }
  };

  const moveCarousel = (direction) => {
    const node = carouselRef.current;
    if (!node) return;
    const amount = Math.min(node.clientWidth * 0.85, 760);
    node.scrollBy({ left: direction === "next" ? amount : -amount, behavior: "smooth" });
  };

  return (
    <div className="discovery-page">
      <header className="topbar">
        <div className="logo" onClick={() => navigate("/")}>KZPCCRAFT</div>

        <nav className="topnav">
          <span className="nav-active" onClick={() => navigate("/discover")}>Discovery</span>
          <span onClick={() => navigate("/build")}>Builder</span>
          <span>Guides</span>
        </nav>

        <div className="profile-link" onClick={() => navigate(session ? "/profile" : "/auth")}>
          {session ? "Profile" : "Sign in"}
        </div>
      </header>

      <main className="discovery-layout">
        <section className="featured-block">
          <div className="section-head">
            <div>
              <h1>Featured builds</h1>
              <p>Scroll through live community posts</p>
            </div>

            <div className="carousel-controls">
              <button onClick={() => moveCarousel("prev")} aria-label="Previous featured builds">‹</button>
              <button onClick={() => moveCarousel("next")} aria-label="Next featured builds">›</button>
            </div>
          </div>

          <div className="carousel-shell" ref={carouselRef}>
            {featuredBuilds.map((item, index) => (
              <article className="featured-card" key={item.id} onClick={() => navigate(`/discover/post/${item.id}`)}>
                <img src={item.img} alt={item.title} className="featured-image" />
                <div className="featured-overlay">
                  <div className="featured-kicker">{item.author}</div>
                  <h2>{item.title}</h2>
                  <div className="featured-specs">
                    <span>Score {item.score}</span>
                    <span>{item.commentCount} comments</span>
                  </div>
                  <div className="tag-row">
                    {item.tags.map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                </div>
                <div className="slide-index">{String(index + 1).padStart(2, "0")}</div>
              </article>
            ))}
          </div>
        </section>

        <section className="browse-block">
          <aside className="filters">
            <div className="filters-card">
              <div className="filters-title">Browse</div>
              {browseTags.map((tag) => (
                <button
                  key={tag}
                  className={activeTag === tag ? "filter-btn active" : "filter-btn"}
                  onClick={() => setActiveTag(tag)}
                >
                  {tag}
                </button>
              ))}
            </div>

            <div className="filters-card small-note">
              <div className="filters-title">Quick tips</div>
              <p>Use this side panel for sorting and narrowing results like a classifieds site.</p>
            </div>
          </aside>

          <section className="listings">
            <div className="list-head">
              <div>
                <h2>Other people’s builds</h2>
                <p>{posts.length} listings found</p>
              </div>

              <div className="list-head-actions">
                <button type="button" className="sort-pill create-post-trigger" onClick={() => setComposerOpen(true)}>Create Post</button>
                <select
                  className="sort-pill"
                  value={sort}
                  onChange={(event) => setSort(event.target.value)}
                >
                  <option value="new">Newest</option>
                  <option value="hot">Hot</option>
                </select>
              </div>
            </div>

            {composerOpen ? (
              <div className="post-composer-card">
                <h3>Create post</h3>
                <input value={newTitle} onChange={(event) => setNewTitle(event.target.value)} placeholder="Post title" />
                <textarea value={newBody} onChange={(event) => setNewBody(event.target.value)} placeholder="Write your post..." />
                <input value={newTags} onChange={(event) => setNewTags(event.target.value)} placeholder="Tags (comma separated)" />
                <input value={newImageUrl} onChange={(event) => setNewImageUrl(event.target.value)} placeholder="Image URL (optional)" />
                <select value={attachBuildId} onChange={(event) => setAttachBuildId(event.target.value)}>
                  <option value="">No attached PC build</option>
                  {builds.map((build) => (
                    <option key={build.id} value={build.id}>{build.title ?? `Build #${build.id}`}</option>
                  ))}
                </select>
                <div className="post-composer-actions">
                  <button type="button" className="vote-btn" onClick={handleCreatePost}>Publish</button>
                  <button type="button" className="vote-btn" onClick={() => setComposerOpen(false)}>Cancel</button>
                </div>
              </div>
            ) : null}

            <div className="listing-stack">
              {loading ? <div className="listing-desc">Loading posts...</div> : null}
              {!loading && error ? <div className="listing-desc">{error}</div> : null}
              {!loading && !error && posts.length === 0 ? <div className="listing-desc">No community posts yet.</div> : null}

              {posts.map((post, index) => (
                <article className="listing-card" key={post.id}>
                  <img
                    className="listing-image"
                    src={post.imageUrls?.[0] || placeholders[index % placeholders.length]}
                    alt={post.title}
                    onClick={() => navigate(`/discover/post/${post.id}`)}
                  />

                  <div className="listing-content">
                    <div className="listing-top">
                      <div>
                        <h3 onClick={() => navigate(`/discover/post/${post.id}`)}>{post.title}</h3>
                        <div className="listing-author">{`by @${post.authorUserId}`}</div>
                      </div>
                      <div className="listing-price">{`#${post.id}`}</div>
                    </div>

                    <p className="listing-desc">{post.body}</p>

                    <div className="listing-specs">
                      {(post.tags || []).slice(0, 2).map((tag) => (
                        <span key={`${post.id}-${tag}`}>{tag}</span>
                      ))}
                      <span>{`💬 ${post.commentCount ?? 0}`}</span>
                      <button type="button" className="vote-btn" onClick={() => castVote(post.id, 1)}>⬆ {post.score ?? 0}</button>
                      <button type="button" className="vote-btn" onClick={() => castVote(post.id, -1)}>⬇</button>
                      <button type="button" className="vote-btn" onClick={() => toggleComments(post.id)}>
                        {commentsOpen[post.id] ? "Hide comments" : "Show comments"}
                      </button>
                    </div>

                    {commentsOpen[post.id] ? (
                      <div className="comments-section">
                        {commentsLoading[post.id] ? <div className="listing-desc">Loading comments...</div> : null}
                        {!commentsLoading[post.id] && (commentsByPost[post.id] || []).length === 0 ? (
                          <div className="listing-desc">No comments yet. Start the thread.</div>
                        ) : null}

                        {(commentsByPost[post.id] || []).map((comment) => (
                          <div className="comment-item" key={comment.id}>
                            <div className="comment-head">{`@${comment.authorUserId}`}</div>
                            <div className="comment-body">{comment.body}</div>
                            <div className="comment-actions">
                              <span>{`Score ${comment.score ?? 0}`}</span>
                              <button type="button" className="vote-btn" onClick={() => castCommentVote(post.id, comment.id, 1)}>⬆</button>
                              <button type="button" className="vote-btn" onClick={() => castCommentVote(post.id, comment.id, -1)}>⬇</button>
                            </div>
                          </div>
                        ))}

                        <div className="comment-composer">
                          <input
                            type="text"
                            value={commentDrafts[post.id] || ""}
                            onChange={(event) =>
                              setCommentDrafts((prev) => ({
                                ...prev,
                                [post.id]: event.target.value,
                              }))
                            }
                            placeholder="Write a comment..."
                          />
                          <button type="button" className="vote-btn" onClick={() => submitComment(post.id)}>Post</button>
                        </div>
                      </div>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>
          </section>
        </section>
      </main>
    </div>
  );
}