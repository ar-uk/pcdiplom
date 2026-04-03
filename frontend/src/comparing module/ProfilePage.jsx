import { useEffect, useMemo, useState } from "react";
import "./profile.css";
import profiledflt from "./assets/profiledflt.jpg";
const PLACEHOLDERS = [
  "https://placehold.co/320x220/72c7ff/0b1b24?text=PC+Build+1",
  "https://placehold.co/320x220/7a5cff/0b1b24?text=PC+Build+2",
  "https://placehold.co/320x220/b06cff/0b1b24?text=PC+Build+3",
  "https://placehold.co/320x220/72c7ff/0b1b24?text=PC+Build+4",
  "https://placehold.co/320x220/7a5cff/0b1b24?text=PC+Build+5",
  "https://placehold.co/320x220/b06cff/0b1b24?text=PC+Build+6",
];

export default function ProfilePage() {
  const [user, setUser] = useState(null);
  const [builds, setBuilds] = useState([]);

  //const token = localStorage.getItem("token");

  /*useEffect(() => {
    fetch("http://localhost:8082/auth/me", {
      headers: { Authorization: "Bearer " + token },
    })
      .then((res) => res.json())
      .then((data) => setUser(data))
      .catch(() => setUser(null));

    fetch("http://localhost:8083/community/my-builds")
      .then((res) => res.json())
      .then((data) => setBuilds(Array.isArray(data) ? data : []))
      .catch(() => setBuilds([]));
  }, [token]);*/

  useEffect(() => {
  // Mock user
  setUser({
    id: "0917232",
    name: "Demo User",
  });

  // Mock builds
  setBuilds([
    { id: 1, name: "RTX 4060 Build", cpu: "Intel i5 12400F" },
    { id: 2, name: "Gaming Beast", cpu: "Ryzen 7 5800X" },
    { id: 3, name: "Budget Setup", cpu: "Ryzen 5 3600" },
    { id: 4, name: "Streaming Rig", cpu: "Intel i7 12700K" },
    { id: 5, name: "Mini ITX Build", cpu: "Ryzen 5 5600X" },
    { id: 6, name: "Workstation", cpu: "Intel i9 12900K" },
    { id: 7, name: "RGB Monster", cpu: "Ryzen 9 5900X" },
    { id: 8, name: "Silent Build1231", cpu: "Intel i5 13400" },
    { id: 9, name: "Silent Build123", cpu: "Intel i5 13400" },
    { id: 10, name: "Silent Buildfsdf", cpu: "Intel i5 13400" },
    { id: 11, name: "Silent Builddsf", cpu: "Intel i5 13400" },
    { id: 12, name: "Silent Buildasdasd", cpu: "Intel i5 13400" },
  ]);
}, []);

  const currentBuilds = builds;

  const handleLogout = () => {
    localStorage.removeItem("token");
    window.location.href = "/auth";
  };


  return (
    <div className="profile-page">
      <header className="topbar">
        <div className="logo">KazPcCraft</div>
        <nav className="menu">
          <span>Discover</span>
          <span>Guides</span>
          <span>Builder</span>
        </nav>
        <div className="profile-link" onClick={handleLogout}>
          Profile
        </div>
      </header>

      <main className="profile-layout">
        <aside className="sidebar">
          <div className="user-card">
                <img className="avatar-box" src={profiledflt} alt="Company Logo"/>

            <div className="user-info">
              <div>Name</div>
              <div>ID {user?.id ?? "0917232"}</div>
              <div>Password*</div>
              <div>21.05.2025</div>
            </div>
          </div>

          <div className="side-empty" />
        </aside>

        <section className="build-panel">
          <div className="tabs">
            <span>Favorites</span>
            <span>Your Work</span>
            <span>WishList</span>
            <span>BlackList</span>
            <div className="search-bar" />
          </div>

          <div className="build-grid">
            {currentBuilds.map((b, index) => (
              <div className="build-card" key={b.id ?? index}>
                <img
                  className="build-image"
                  src={PLACEHOLDERS[index % PLACEHOLDERS.length]}
                  alt="PC build"
                />
                <div className="build-title">{b.name ?? "RTX 4060"}</div>
                <div className="build-subtitle">
                  {b.cpu ?? "Intel Core i7 12500K"}
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}