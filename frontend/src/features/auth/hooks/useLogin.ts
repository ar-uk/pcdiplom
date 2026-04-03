/**
 * useLogin Hook
 * Custom hook for login mutation
 */

import { useState } from 'react'
import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { LoginRequest } from '@/types'
import { AuthService } from '@/api/services'

export interface UseLoginReturn {
  isLoading: boolean
  error: string | null
  login: (request: LoginRequest) => Promise<boolean>
}

export const useLogin = (): UseLoginReturn => {
  const store = useContext(AuthContext)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!store) {
    throw new Error('useLogin must be used within AuthProvider')
  }

  const login = async (request: LoginRequest): Promise<boolean> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await AuthService.login(request)
      store.setUser(response.user)
      store.setToken(response.token)
      return true
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Login failed. Please try again.'
      setError(message)
      return false
    } finally {
      setIsLoading(false)
    }
  }

  return {
    isLoading,
    error,
    login,
  }
}
