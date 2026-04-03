/**
 * ProtectedRoute Component
 * Wraps routes and checks authentication/authorization
 */

import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks'
import { UserRole } from '@/types'

interface ProtectedRouteProps {
  requiredRole?: UserRole
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ requiredRole }) => {
  const { isAuthenticated, userRole } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && userRole !== requiredRole) {
    return (
      <div className="unauthorized-container">
        <h1>Access Denied</h1>
        <p>You don't have permission to access this page.</p>
        <a href="/builder">Go to Builder</a>
      </div>
    )
  }

  return <Outlet />
}
