import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { canonicalUserId, loadSession, userIdCandidates } from "./lib/session.js";
import { mergeUniqueTagStrings, tagsStringToList } from "./lib/tagUtils.js";
import TagPickerModal from "./components/TagPickerModal.jsx";
import SiteTopbar from "./components/SiteTopbar.jsx";
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

const PAGE_SIZE = 4;
const BUDGET_MAX_KZT = 3_000_000;

function estimatePower(selectedParts) {
  if (!selectedParts || typeof selectedParts !== "object") {
    return 0;
  }
  const cpu = Number(selectedParts.cpu?.powerHint ?? selectedParts.cpu?.wattage ?? 65);
  const gpu = Number(selectedParts.gpu?.powerHint ?? selectedParts.gpu?.wattage ?? 200);
  return cpu + gpu + 90;
}

function sumSelectedPartsPrice(selectedParts) {
  if (!selectedParts || typeof selectedParts !== "object") {
    return 0;
  }
  return Object.values(selectedParts).reduce((acc, part) => {
    if (!part || typeof part !== "object") {
      return acc;
    }
    const n = Number(part.price ?? 0);
    return acc + (Number.isFinite(n) ? n : 0);
  }, 0);
}

/** Total KZT from snapshot: explicit totalPrice, else sum of part prices (Profile posts store parts-only JSON). */
function getPostBuildTotalKzt(post) {
  if (!post?.buildSnapshotJson) {
    return null;
  }
  try {
    const parsed = JSON.parse(post.buildSnapshotJson);
    if (typeof parsed.totalPrice === "number" && !Number.isNaN(parsed.totalPrice)) {
      return parsed.totalPrice;
    }
    let parts = null;
    if (parsed.selectedParts && typeof parsed.selectedParts === "object") {
      parts = parsed.selectedParts;
    } else if (parsed && typeof parsed === "object") {
      parts = { ...parsed };
      delete parts.totalPrice;
      delete parts.estimatedPower;
      delete parts.selectedParts;
    }
    if (!parts || typeof parts !== "object") {
      return null;
    }
    const sum = sumSelectedPartsPrice(parts);
    return sum > 0 ? sum : null;
  } catch {
    return null;
  }
}

function formatKzt(value) {
  const amount = Number(value ?? 0);
  return `${amount.toLocaleString("en-US")} KZT`;
}

function tierBadge(score) {
  const s = Number(score ?? 0);
  if (s >= 30) {
    return { text: "ELITE TIER", className: "tier-badge tier-badge-elite" };
  }
  if (s >= 12) {
    return { text: "PRO BUILD", className: "tier-badge tier-badge-pro" };
  }
  if (s >= 4) {
    return { text: "RISING", className: "tier-badge tier-badge-rise" };
  }
  return { text: "COMMUNITY", className: "tier-badge tier-badge-community" };
}

function rankLabel(globalIndex) {
  return `RANK ${String(globalIndex + 1).padStart(2, "0")}`;
}

function getSelectedPartsFromSnapshotJson(raw) {
  if (!raw || typeof raw !== "string") {
    return null;
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed.selectedParts && typeof parsed.selectedParts === "object") {
      return parsed.selectedParts;
    }
    if (parsed && typeof parsed === "object") {
      const parts = { ...parsed };
      delete parts.totalPrice;
      delete parts.estimatedPower;
      delete parts.selectedParts;
      return Object.keys(parts).length ? parts : null;
    }
  } catch {
    return null;
  }
  return null;
}

/** Text from saved build parts for tag / keyword search */
function collectBuildPartSearchText(parts) {
  if (!parts || typeof parts !== "object") {
    return "";
  }
  const chunks = [];
  for (const [slot, part] of Object.entries(parts)) {
    if (!part || typeof part !== "object") {
      continue;
    }
    chunks.push(String(slot).replace(/_/g, " "));
    for (const key of ["name", "socket", "ramType", "memoryType", "formFactor", "sourceType", "sourceLink", "wattage"]) {
      const v = part[key];
      if (v != null && typeof v !== "object") {
        chunks.push(String(v));
      }
    }
    if (typeof part.raw === "string") {
      chunks.push(part.raw);
    }
  }
  return chunks.join(" ");
}

/** Lowercase haystack: post tags, title, body, and attached build component text */
function getDiscoveryTagHaystack(post) {
  const tagStr = (post.tags || []).map((t) => String(t)).join(" ");
  const titleBody = `${post.title ?? ""} ${post.body ?? ""}`;
  const parts = getSelectedPartsFromSnapshotJson(post.buildSnapshotJson);
  const buildText = collectBuildPartSearchText(parts);
  return `${tagStr} ${titleBody} ${buildText}`.toLowerCase();
}

/** True if any selected tag matches post tags, text, or saved build parts */
function matchesTagFilter(post, selectedTags) {
  if (!selectedTags.size) {
    return true;
  }
  const haystack = getDiscoveryTagHaystack(post);
  for (const tag of selectedTags) {
    const low = String(tag).toLowerCase().trim();
    if (!low) {
      continue;
    }
    if (haystack.includes(low)) {
      return true;
    }
  }
  return false;
}

function matchesBudget(post, maxKzt) {
  const price = getPostBuildTotalKzt(post);
  if (price == null) {
    return true;
  }
  return price <= maxKzt;
}

export default function DiscoveryPage() {
  const navigate = useNavigate();
  const carouselRef = useRef(null);
  const [session] = useState(() => loadSession());
  const [posts, setPosts] = useState([]);
  const [apiTags, setApiTags] = useState([]);
  const [sort, setSort] = useState("new");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [commentsByPost, setCommentsByPost] = useState({});
  const [commentsOpen, setCommentsOpen] = useState({});
  const [commentsLoading, setCommentsLoading] = useState({});
  const [commentDrafts, setCommentDrafts] = useState({});
  const [builds, setBuilds] = useState([]);
  const [composerOpen, setComposerOpen] = useState(false);
  const [tagPickerOpen, setTagPickerOpen] = useState(false);
  const [filterTagPickerOpen, setFilterTagPickerOpen] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [newBody, setNewBody] = useState("");
  const [newTags, setNewTags] = useState("");
  const [newImageUrl, setNewImageUrl] = useState("");
  const [attachBuildId, setAttachBuildId] = useState("");
  const [bookmarks, setBookmarks] = useState({});

  const [draftHardware, setDraftHardware] = useState(() => new Set());
  const [draftBudgetMax, setDraftBudgetMax] = useState(BUDGET_MAX_KZT);

  const [appliedHardware, setAppliedHardware] = useState(() => new Set());
  const [appliedBudgetMax, setAppliedBudgetMax] = useState(BUDGET_MAX_KZT);

  const [page, setPage] = useState(1);
  const [filterEpoch, setFilterEpoch] = useState(0);

  const userId = canonicalUserId(session);
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
        setApiTags(Array.isArray(data) ? data : []);
      } catch {
        if (!alive) return;
        setApiTags([]);
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
  }, [sort, session?.token]);

  useEffect(() => {
    let alive = true;

    async function loadUserBuilds() {
      const candidateIds = userIdCandidates(session);

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

  const extraTagNames = useMemo(() => apiTags.map((t) => t.displayName || t.slug).filter(Boolean), [apiTags]);

  const featuredBuilds = useMemo(() => {
    const sorted = [...posts].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
    return sorted.slice(0, 4).map((post, index) => ({
      id: post.id,
      title: post.title,
      author: `by @${post.authorUserId}`,
      img: post.imageUrls?.[0] || FEATURED_PLACEHOLDERS[index % FEATURED_PLACEHOLDERS.length],
      tags: Array.isArray(post.tags) ? post.tags.slice(0, 3) : [],
      score: post.score ?? 0,
      commentCount: post.commentCount ?? 0,
      tier: tierBadge(post.score ?? 0),
      totalKzt: getPostBuildTotalKzt(post),
    }));
  }, [posts]);

  const filteredPosts = useMemo(() => {
    return posts.filter((post) => matchesTagFilter(post, appliedHardware) && matchesBudget(post, appliedBudgetMax));
  }, [posts, appliedHardware, appliedBudgetMax]);

  useEffect(() => {
    setPage(1);
  }, [sort, filterEpoch]);

  const totalPages = Math.max(1, Math.ceil(filteredPosts.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pagedPosts = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return filteredPosts.slice(start, start + PAGE_SIZE);
  }, [filteredPosts, safePage]);

  const toggleBookmark = (postId) => {
    setBookmarks((prev) => ({ ...prev, [postId]: !prev[postId] }));
  };

  const applyFilterDraft = () => {
    setAppliedHardware(new Set(draftHardware));
    setAppliedBudgetMax(draftBudgetMax);
    setFilterEpoch((n) => n + 1);
  };

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
            : post,
        ),
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
          post.id === postId ? { ...post, commentCount: (post.commentCount ?? 0) + 1 } : post,
        ),
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
          comment.id === commentId ? { ...comment, score: voteResult.score } : comment,
        ),
      }));
    } catch (voteError) {
      alert(voteError.message || "Could not vote comment.");
    }
  };

  const handleCreatePost = async () => {
    const live = loadSession();
    const token = live?.token;
    const posterId = live?.email?.trim().toLowerCase() || live?.username?.trim().toLowerCase() || null;
    if (!token || !posterId) {
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
    const partsSum = sumSelectedPartsPrice(selectedParts);
    const buildSnapshot = selectedBuild
      ? {
          selectedParts,
          totalPrice:
            typeof selectedBuild.totalPrice === "number" && !Number.isNaN(selectedBuild.totalPrice)
              ? selectedBuild.totalPrice
              : partsSum > 0
                ? partsSum
                : null,
          estimatedPower: estimatePower(selectedParts),
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
          tags: tagsStringToList(newTags),
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

  const selectedTagsForPicker = tagsStringToList(newTags);

  return (
    <div className="discovery-page">
      <SiteTopbar session={session} />

      <main className="discovery-layout">
        <section className="featured-block">
          <div className="section-head">
            <div>
              <p className="section-eyebrow">Spotlight</p>
              <h1>Top performing rigs this week</h1>
              <p className="section-sub">Curated from community scores and engagement</p>
            </div>

            <div className="carousel-controls">
              <button type="button" onClick={() => moveCarousel("prev")} aria-label="Previous featured builds">
                ‹
              </button>
              <button type="button" onClick={() => moveCarousel("next")} aria-label="Next featured builds">
                ›
              </button>
            </div>
          </div>

          <div className="carousel-shell" ref={carouselRef}>
            {featuredBuilds.map((item, index) => (
              <article
                className="featured-card"
                key={item.id ?? `feat-${index}`}
                onClick={() => navigate(`/discover/post/${item.id}`)}
              >
                <img src={item.img} alt="" className="featured-image" />
                <span className={item.tier.className}>{item.tier.text}</span>
                <div className="featured-overlay">
                  <div className="featured-kicker">{item.author}</div>
                  <h2>{item.title}</h2>
                  <div className="featured-specs">
                    <span>Score {item.score}</span>
                    <span>{item.commentCount} comments</span>
                    {item.totalKzt != null ? <span className="featured-total">{formatKzt(item.totalKzt)}</span> : null}
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
          <aside className="filters" aria-label="Discovery filters">
            <div className="filters-card filters-card-main">
              <div className="filters-header">
                <span className="filters-icon" aria-hidden="true">
                  ≡
                </span>
                <div className="filters-title">Filters</div>
              </div>

              <div className="filter-group">
                <div className="filter-group-label">Budget ceiling (KZT)</div>
                <p className="filter-range-hint">Hides listings above this price when a build price exists.</p>
                <div className="budget-slider-wrap">
                  <input
                    type="range"
                    min={100_000}
                    max={BUDGET_MAX_KZT}
                    step={50_000}
                    value={draftBudgetMax}
                    onChange={(event) => setDraftBudgetMax(Number(event.target.value))}
                  />
                  <div className="budget-slider-labels">
                    <span>100k</span>
                    <span className="budget-current">{formatKzt(draftBudgetMax)}</span>
                    <span>3M+</span>
                  </div>
                </div>
              </div>

              <div className="filter-group">
                <div className="filter-group-label">Tags</div>
                <p className="filter-range-hint">Matches post tags, title, description, and attached build parts (names, sockets, links). Any tag can match.</p>
                <div className="composer-tags-row filter-tags-toolbar">
                  <button type="button" className="tags-mini-btn" onClick={() => setFilterTagPickerOpen(true)}>
                    Tags
                  </button>
                  <span className="composer-tags-preview">
                    {draftHardware.size ? [...draftHardware].join(" · ") : "None selected"}
                  </span>
                </div>
              </div>

              <TagPickerModal
                open={filterTagPickerOpen}
                onClose={() => setFilterTagPickerOpen(false)}
                selectedTags={[...draftHardware]}
                extraTags={extraTagNames}
                onApply={(tags) => setDraftHardware(new Set(tags))}
              />

              <button type="button" className="apply-filters-btn" onClick={applyFilterDraft}>
                Apply changes
              </button>
            </div>

            <div className="filters-card small-note">
              <div className="filter-group-label">Quick tips</div>
              <p>Open Tags to pick filters, then Apply changes. Tag search includes titles, descriptions, and saved build components.</p>
            </div>
          </aside>

          <section className="listings">
            <div className="list-head">
              <div>
                <p className="list-eyebrow">Discovery grid</p>
                <h2>
                  Showing <strong>{filteredPosts.length}</strong> builds available
                </h2>
                <p className="list-sub">Filtered from {posts.length} total listings</p>
              </div>

              <label className="sort-control">
                <span className="sort-label">Sort by</span>
                <select className="sort-select" value={sort} onChange={(event) => setSort(event.target.value)}>
                  <option value="new">Most recent</option>
                  <option value="hot">Hot</option>
                </select>
              </label>
            </div>

            {composerOpen ? (
              <div className="post-composer-card">
                <div className="post-composer-head">
                  <h3>Create post</h3>
                  <button type="button" className="composer-dismiss" onClick={() => setComposerOpen(false)} aria-label="Close composer">
                    ×
                  </button>
                </div>
                <input value={newTitle} onChange={(event) => setNewTitle(event.target.value)} placeholder="Post title" />
                <textarea value={newBody} onChange={(event) => setNewBody(event.target.value)} placeholder="Write your post..." />
                <div className="composer-tags-row">
                  <button type="button" className="tags-mini-btn" onClick={() => setTagPickerOpen(true)}>
                    Tags
                  </button>
                  <span className="composer-tags-preview">
                    {selectedTagsForPicker.length ? selectedTagsForPicker.join(" · ") : "No tags selected"}
                  </span>
                </div>
                <input
                  className="composer-tags-text"
                  type="text"
                  value={newTags}
                  onChange={(event) => setNewTags(event.target.value)}
                  placeholder="Tags (comma or semicolon) — type your own or merge from Tags"
                />
                <input value={newImageUrl} onChange={(event) => setNewImageUrl(event.target.value)} placeholder="Image URL (optional)" />
                <select value={attachBuildId} onChange={(event) => setAttachBuildId(event.target.value)}>
                  <option value="">No attached PC build</option>
                  {builds.map((build) => (
                    <option key={build.id} value={build.id}>
                      {build.title ?? `Build #${build.id}`}
                    </option>
                  ))}
                </select>
                <div className="post-composer-actions">
                  <button type="button" className="vote-btn vote-btn-primary" onClick={handleCreatePost}>
                    Publish
                  </button>
                  <button type="button" className="vote-btn" onClick={() => setComposerOpen(false)}>
                    Cancel
                  </button>
                </div>
              </div>
            ) : null}

            <TagPickerModal
              open={tagPickerOpen}
              onClose={() => setTagPickerOpen(false)}
              selectedTags={selectedTagsForPicker}
              extraTags={extraTagNames}
              onApply={(tags) => setNewTags((prev) => mergeUniqueTagStrings(prev, tags.join(", ")))}
            />

            <div className="listing-grid">
              {loading ? <div className="listing-desc grid-span">Loading posts...</div> : null}
              {!loading && error ? <div className="listing-desc grid-span">{error}</div> : null}
              {!loading && !error && filteredPosts.length === 0 ? (
                <div className="listing-desc grid-span">No builds match these filters.</div>
              ) : null}

              {!loading &&
                !error &&
                pagedPosts.map((post, index) => {
                  const globalIndex = (safePage - 1) * PAGE_SIZE + index;
                  const totalKzt = getPostBuildTotalKzt(post);
                  const specTags = (post.tags || []).slice(0, 2);
                  return (
                    <article className="discover-card" key={post.id}>
                      <div
                        className="discover-card-media"
                        onClick={() => navigate(`/discover/post/${post.id}`)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter") {
                            navigate(`/discover/post/${post.id}`);
                          }
                        }}
                        role="button"
                        tabIndex={0}
                      >
                        <img
                          className="discover-card-image"
                          src={post.imageUrls?.[0] || placeholders[globalIndex % placeholders.length]}
                          alt=""
                        />
                        <span className={`discover-price-pill ${totalKzt == null ? "is-muted" : ""}`}>
                          {totalKzt != null ? formatKzt(totalKzt) : "No total"}
                        </span>
                        <span className={`discover-rank-pill ${(post.score ?? 0) >= 30 ? "rank-elite" : ""}`}>
                          {rankLabel(globalIndex)}
                        </span>
                      </div>
                      <div className="discover-card-body">
                        <h3 onClick={() => navigate(`/discover/post/${post.id}`)}>{post.title}</h3>
                        <div className="discover-author">{`@${post.authorUserId}`}</div>
                        <div className="discover-total-line" aria-label="Total build price">
                          <span className="discover-total-label">Total</span>
                          <span className="discover-total-value">{totalKzt != null ? formatKzt(totalKzt) : "—"}</span>
                        </div>
                        <p className="discover-snippet">{post.body}</p>
                        <div className="discover-spec-tags">
                          {specTags.map((tag) => (
                            <span key={`${post.id}-${tag}`}>{tag}</span>
                          ))}
                          {specTags.length === 0 ? <span className="discover-spec-muted">No spec tags</span> : null}
                        </div>
                        <div className="discover-card-actions">
                          <button type="button" className="discover-stat" onClick={() => castVote(post.id, 1)}>
                            ♥ {post.score ?? 0}
                          </button>
                          <button type="button" className="discover-stat" onClick={() => toggleComments(post.id)}>
                            💬 {post.commentCount ?? 0}
                          </button>
                          <button
                            type="button"
                            className={`discover-bookmark ${bookmarks[post.id] ? "on" : ""}`}
                            onClick={() => toggleBookmark(post.id)}
                            aria-pressed={Boolean(bookmarks[post.id])}
                            aria-label="Bookmark"
                          >
                            ★
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
                                  <button type="button" className="vote-btn" onClick={() => castCommentVote(post.id, comment.id, 1)}>
                                    ⬆
                                  </button>
                                  <button type="button" className="vote-btn" onClick={() => castCommentVote(post.id, comment.id, -1)}>
                                    ⬇
                                  </button>
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
                              <button type="button" className="vote-btn" onClick={() => submitComment(post.id)}>
                                Post
                              </button>
                            </div>
                          </div>
                        ) : null}
                      </div>
                    </article>
                  );
                })}
            </div>

            {!loading && !error && totalPages > 1 ? (
              <nav className="discovery-pagination" aria-label="Pagination">
                <button type="button" className="page-btn" disabled={safePage <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                  Prev
                </button>
                <span className="page-indicator">
                  {safePage} <span className="page-indicator-sep">/</span> {totalPages}
                </span>
                <button
                  type="button"
                  className="page-btn"
                  disabled={safePage >= totalPages}
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                >
                  Next
                </button>
              </nav>
            ) : null}
          </section>
        </section>
      </main>

      <footer className="discovery-footer">
        <div className="discovery-footer-brand">KazPcCraft</div>
        <div className="discovery-footer-links">
          <span>Community</span>
          <span>Support</span>
          <span>Privacy</span>
          <span>Terms</span>
        </div>
      </footer>

      <button
        type="button"
        className="discovery-fab"
        aria-label="Create post"
        onClick={() => {
          if (!session) {
            navigate("/auth");
            return;
          }
          setComposerOpen((open) => !open);
        }}
      >
        +
      </button>
    </div>
  );
}
