import { Navigate, Route, Routes } from "react-router-dom";
import HomePage from "./HomePage.jsx";
import DiscoveryPage from "./Discovery.jsx";
import ProfilePage from "./ProfilePage.jsx";
import AuthPage from "./AuthPage.jsx";
import BuilderPage from "./Buildpage.jsx";

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/discover" element={<DiscoveryPage />} />
      <Route path="/build" element={<BuilderPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App