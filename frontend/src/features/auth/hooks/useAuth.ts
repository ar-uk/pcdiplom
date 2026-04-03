/**
 * useAuth Hook
 * Custom hook to access auth state and actions
 */

import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { User, UserRole } from '@/types'

export interface UseAuthReturn {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  userRole: UserRole | null
  isLoading: boolean
  error: string | null
  logout: () => void
}

export const useAuth = (): UseAuthReturn => {
  const store = useContext(AuthContext)

  if (!store) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return {
    user: store.user,
    token: store.token?.accessToken ?? null,
    isAuthenticated: store.isAuthenticated(),
    userRole: store.getUserRole(),
    isLoading: store.isLoading,
    error: store.error,
    logout: store.logout,
  }
}
