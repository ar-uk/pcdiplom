/**
 * Authentication Store (Zustand)
 * Global state management for authentication
 */

import { create } from 'zustand'
import { User, AuthToken, UserRole } from '@/types'

export interface AuthStoreState {
  // State
  user: User | null
  token: AuthToken | null
  isLoading: boolean
  error: string | null

  // Actions
  setUser: (user: User | null) => void
  setToken: (token: AuthToken | null) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  logout: () => void
  hydrate: (user: User | null, token: AuthToken | null) => void

  // Selectors
  isAuthenticated: () => boolean
  getUserRole: () => UserRole | null
}

const STORAGE_KEY_USER = 'auth_user'
const STORAGE_KEY_TOKEN = 'auth_token'

export const useAuthStore = create<AuthStoreState>((set, get) => ({
  user: null,
  token: null,
  isLoading: false,
  error: null,

  setUser: (user) => {
    set({ user })
    if (user) {
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user))
    } else {
      localStorage.removeItem(STORAGE_KEY_USER)
    }
  },

  setToken: (token) => {
    set({ token })
    if (token) {
      localStorage.setItem(STORAGE_KEY_TOKEN, JSON.stringify(token))
    } else {
      localStorage.removeItem(STORAGE_KEY_TOKEN)
    }
  },

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (error) => set({ error }),

  logout: () => {
    set({
      user: null,
      token: null,
      error: null,
      isLoading: false,
    })
    localStorage.removeItem(STORAGE_KEY_USER)
    localStorage.removeItem(STORAGE_KEY_TOKEN)
  },

  hydrate: (user, token) => {
    set({ user, token })
  },

  isAuthenticated: () => {
    const state = get()
    return state.token != null && state.user != null
  },

  getUserRole: () => {
    const state = get()
    return state.user?.role ?? null
  },
}))
