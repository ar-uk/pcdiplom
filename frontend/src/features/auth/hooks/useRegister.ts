/**
 * useRegister Hook
 * Custom hook for registration mutation
 */

import { useState, useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { RegisterRequest, UserRole } from '@/types'
import { AuthService } from '@/api/services'

export interface UseRegisterPayload extends Omit<RegisterRequest, 'confirmPassword'> {
  role?: UserRole
}

export interface UseRegisterReturn {
  isLoading: boolean
  error: string | null
  register: (payload: UseRegisterPayload) => Promise<boolean>
}

export const useRegister = (): UseRegisterReturn => {
  const store = useContext(AuthContext)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!store) {
    throw new Error('useRegister must be used within AuthProvider')
  }

  const register = async (payload: UseRegisterPayload): Promise<boolean> => {
    setIsLoading(true)
    setError(null)

    try {
      // Build request
      const request: RegisterRequest = {
        email: payload.email,
        username: payload.username,
        password: payload.password,
        confirmPassword: payload.password, // Front-end already validated match
      }

      const response = await AuthService.register(request)
      store.setUser(response.user)
      store.setToken(response.token)
      return true
    } catch (err) {
      const message =
        err instanceof Error
          ? err.message
          : 'Registration failed. Please try again.'
      setError(message)
      return false
    } finally {
      setIsLoading(false)
    }
  }

  return {
    isLoading,
    error,
    register,
  }
}
