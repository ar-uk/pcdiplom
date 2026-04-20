import { Suspense, lazy } from "react";
import { Navigate, Route, Routes } from "react-router-dom";

const HomePage = lazy(() => import("./HomePage.jsx"));
const DiscoveryPage = lazy(() => import("./Discovery.jsx"));
const PostDetailsPage = lazy(() => import("./PostDetailsPage.jsx"));
const ProfilePage = lazy(() => import("./ProfilePage.jsx"));
const AuthPage = lazy(() => import("./AuthPage.jsx"));
const BuilderPage = lazy(() => import("./Buildpage.jsx"));
const AiBuilderPage = lazy(() => import("./AiBuilderPage.jsx"));

function withFallback(element) {
  return <Suspense fallback={<div style={{ padding: "20px", color: "#e8eef2" }}>Loading...</div>}>{element}</Suspense>;
}

function App() {
  return (
    <Routes>
      <Route path="/" element={withFallback(<HomePage />)} />
      <Route path="/auth" element={withFallback(<AuthPage />)} />
      <Route path="/discover" element={withFallback(<DiscoveryPage />)} />
      <Route path="/discover/post/:postId" element={withFallback(<PostDetailsPage />)} />
      <Route path="/build" element={withFallback(<BuilderPage />)} />
      <Route path="/ai-builder" element={withFallback(<AiBuilderPage />)} />
      <Route path="/profile" element={withFallback(<ProfilePage />)} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App