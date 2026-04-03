/**
 * Authentication Context
 * Wraps Zustand store and provides initialization logic
 */

import React, { createContext, useEffect } from 'react'
import { useAuthStore, AuthStoreState } from '../store/authStore'

export const AuthContext = createContext<AuthStoreState | undefined>(undefined)

interface AuthProviderProps {
  children: React.ReactNode
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const authStore = useAuthStore()

  // Hydrate store from localStorage on mount
  useEffect(() => {
    const savedUser = localStorage.getItem('auth_user')
    const savedToken = localStorage.getItem('auth_token')

    if (savedUser && savedToken) {
      try {
        authStore.hydrate(JSON.parse(savedUser), JSON.parse(savedToken))
      } catch (error) {
        console.error('Failed to restore auth state:', error)
        authStore.logout()
      }
    }
  }, [authStore])

  return (
    <AuthContext.Provider value={authStore}>
      {children}
    </AuthContext.Provider>
  )
}
