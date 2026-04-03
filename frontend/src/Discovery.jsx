import { useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./styles/pages/discovery.css";

const featuredBuilds = [
  {
    id: 1,
    title: "RTX 4070 White Build",
    author: "by @neo_builds",
    cpu: "Ryzen 7 7800X3D",
    gpu: "RTX 4070",
    img: "https://placehold.co/900x500/17384a/e8f6ff?text=FEATURED+BUILD+1",
    tags: ["Gaming", "White", "AM5"],
  },
  {
    id: 2,
    title: "Budget Beast 1440p",
    author: "by @nightrigs",
    cpu: "Intel i5-12400F",
    gpu: "RTX 4060",
    img: "https://placehold.co/900x500/1b2f56/e8f6ff?text=FEATURED+BUILD+2",
    tags: ["Budget", "1440p", "RGB"],
  },
  {
    id: 3,
    title: "Silent Workstation",
    author: "by @cleanframe",
    cpu: "Ryzen 9 7900",
    gpu: "RTX 4070 Ti",
    img: "https://placehold.co/900x500/143a31/e8f6ff?text=FEATURED+BUILD+3",
    tags: ["Silent", "Creator", "ATX"],
  },
  {
    id: 4,
    title: "Mini ITX Monster",
    author: "by @smallformlab",
    cpu: "Intel i7-14700K",
    gpu: "RTX 4080 Super",
    img: "https://placehold.co/900x500/4a234f/e8f6ff?text=FEATURED+BUILD+4",
    tags: ["Mini-ITX", "SFF", "Premium"],
  },
];

const discoverPosts = [
  {
    id: 11,
    title: "Dark Gaming Setup",
    author: "by @arcticfox",
    desc: "Custom loop, clean cable management, and a compact dual-monitor desk.",
    price: "$2,450",
    cpu: "Ryzen 7 5800X3D",
    gpu: "RTX 4070",
    likes: "128",
  },
  {
    id: 12,
    title: "All-Black Creator Rig",
    author: "by @monochrome",
    desc: "Built for Blender, video editing, and quiet daily use.",
    price: "$3,120",
    cpu: "Intel i9-14900K",
    gpu: "RTX 4080",
    likes: "96",
  },
  {
    id: 13,
    title: "Budget Starter PC",
    author: "by @starterbuilds",
    desc: "Best value build for esports and school work.",
    price: "$780",
    cpu: "Ryzen 5 5600",
    gpu: "RX 6600",
    likes: "211",
  },
  {
    id: 14,
    title: "RGB Showcase Case",
    author: "by @lightsync",
    desc: "Pure visual build with tempered glass, custom fans, and synced lighting.",
    price: "$1,940",
    cpu: "Ryzen 7 7700",
    gpu: "RTX 4070 Ti",
    likes: "174",
  },
  {
    id: 15,
    title: "Compact Office Build",
    author: "by @workmode",
    desc: "Small, quiet, and practical with strong multi-tasking performance.",
    price: "$640",
    cpu: "Intel i3-13100",
    gpu: "Integrated",
    likes: "54",
  },
  {
    id: 16,
    title: "High-End Racing Sim",
    author: "by @simgarage",
    desc: "Powerful system paired with triple-screen racing setup.",
    price: "$4,300",
    cpu: "Ryzen 9 7950X3D",
    gpu: "RTX 4090",
    likes: "89",
  },
];

const placeholders = [
  "https://placehold.co/520x340/0e2a35/ffffff?text=PC+Build+Preview+1",
  "https://placehold.co/520x340/123444/ffffff?text=PC+Build+Preview+2",
  "https://placehold.co/520x340/17384a/ffffff?text=PC+Build+Preview+3",
  "https://placehold.co/520x340/1b2f56/ffffff?text=PC+Build+Preview+4",
];

export default function DiscoveryPage() {
  const navigate = useNavigate();
  const carouselRef = useRef(null);
  const [filter, setFilter] = useState("All");

  const filteredPosts = useMemo(() => {
    if (filter === "All") return discoverPosts;
    if (filter === "Budget") return discoverPosts.filter((p) => Number(p.price.replace(/[^0-9]/g, "")) < 1000);
    if (filter === "High End") return discoverPosts.filter((p) => Number(p.price.replace(/[^0-9]/g, "")) >= 2000);
    if (filter === "Silent") return discoverPosts.filter((p) => p.title.toLowerCase().includes("silent") || p.title.toLowerCase().includes("office"));
    return discoverPosts;
  }, [filter]);

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
          <span>Builder</span>
          <span>Guides</span>
          <span>Community</span>
        </nav>

        <div className="profile-link" onClick={() => navigate("/profile")}>Profile</div>
      </header>

      <main className="discovery-layout">
        <section className="featured-block">
          <div className="section-head">
            <div>
              <h1>Featured builds</h1>
              <p>Scroll through the best community builds!</p>
            </div>

            <div className="carousel-controls">
              <button onClick={() => moveCarousel("prev")} aria-label="Previous featured builds">‹</button>
              <button onClick={() => moveCarousel("next")} aria-label="Next featured builds">›</button>
            </div>
          </div>

          <div className="carousel-shell" ref={carouselRef}>
            {featuredBuilds.map((item, index) => (
              <article className="featured-card" key={item.id}>
                <img src={item.img} alt={item.title} className="featured-image" />
                <div className="featured-overlay">
                  <div className="featured-kicker">{item.author}</div>
                  <h2>{item.title}</h2>
                  <div className="featured-specs">
                    <span>{item.cpu}</span>
                    <span>{item.gpu}</span>
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
              <button className={filter === "All" ? "filter-btn active" : "filter-btn"} onClick={() => setFilter("All")}>All</button>
              <button className={filter === "Budget" ? "filter-btn active" : "filter-btn"} onClick={() => setFilter("Budget")}>Budget</button>
              <button className={filter === "High End" ? "filter-btn active" : "filter-btn"} onClick={() => setFilter("High End")}>High End</button>
              <button className={filter === "Silent" ? "filter-btn active" : "filter-btn"} onClick={() => setFilter("Silent")}>Silent</button>
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
                <p>{filteredPosts.length} listings found</p>
              </div>

              <div className="sort-pill">Recommended ▼</div>
            </div>

            <div className="listing-stack">
              {filteredPosts.map((post, index) => (
                <article className="listing-card" key={post.id}>
                  <img
                    className="listing-image"
                    src={placeholders[index % placeholders.length]}
                    alt={post.title}
                  />

                  <div className="listing-content">
                    <div className="listing-top">
                      <div>
                        <h3>{post.title}</h3>
                        <div className="listing-author">{post.author}</div>
                      </div>
                      <div className="listing-price">{post.price}</div>
                    </div>

                    <p className="listing-desc">{post.desc}</p>

                    <div className="listing-specs">
                      <span>{post.cpu}</span>
                      <span>{post.gpu}</span>
                      <span>❤️ {post.likes}</span>
                    </div>
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