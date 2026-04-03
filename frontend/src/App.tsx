/**
 * Main App Component
 * Root component with routing, providers, and layout
 */


import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/api/queryClient'
import { AuthProvider, LoginPage, RegisterPage, ProtectedRoute } from '@/features/auth'
import { MainLayout } from '@/components/layout'
import {
  HomePage,
  NotFoundPage,
  BuilderPage,
  BuildDetailsPage,
  CommunityPage,
  AIAssistantPage,
  ProfilePage,
  AdminPage,
} from '@/pages'
import { UserRole } from '@/types'
import './App.css'

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected Routes (authenticated users) */}
            <Route element={<ProtectedRoute />}>
              <Route element={<MainLayout />}>
                <Route path="/builder" element={<BuilderPage />} />
                <Route path="/builder/:buildId" element={<BuildDetailsPage />} />
                <Route path="/community" element={<CommunityPage />} />
                <Route path="/ai-assistant" element={<AIAssistantPage />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/settings" element={<ProfilePage />} />
              </Route>
            </Route>

            {/* Protected Admin Routes */}
            <Route element={<ProtectedRoute requiredRole={UserRole.ADMIN} />}>
              <Route element={<MainLayout />}>
                <Route path="/admin" element={<AdminPage />} />
              </Route>
            </Route>

            {/* Fallback Routes */}
            <Route path="/404" element={<NotFoundPage />} />
            <Route path="*" element={<Navigate to="/404" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
