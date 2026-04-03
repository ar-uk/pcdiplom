/**
 * Authentication Types
 * Defines all auth-related types and interfaces
 */

export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
}

export interface User {
  id: string
  email: string
  username: string
  role: UserRole
  createdAt: string
  updatedAt: string
}

export interface AuthToken {
  accessToken: string
  refreshToken?: string
  expiresIn: number
}

export interface AuthState {
  user: User | null
  token: AuthToken | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
  confirmPassword: string
}

export interface AuthResponse {
  user: User
  token: AuthToken
}

export interface ProfileUpdateRequest {
  username?: string
  email?: string
  currentPassword?: string
  newPassword?: string
}
