/**
 * Authentication Service
 * Handles all authentication-related API calls
 */

import client from '../client'
import { ENDPOINTS } from '../endpoints'
import {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  User,
  ProfileUpdateRequest,
  UserRole,
} from '@/types'
import { NotImplementedError } from '@/utils/errors'
import { mockUsers } from '../mock/mockData'

export class AuthService {
  /**
   * Login user with email and password
   */
  static async login(request: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await client.post<AuthResponse>(
        ENDPOINTS.AUTH_LOGIN,
        request
      )
      return response.data
    } catch (error) {
      // Mock fallback
      return {
        user: mockUsers[0],
        token: {
          accessToken: 'mock-token-' + Date.now(),
          refreshToken: 'mock-refresh-token-' + Date.now(),
          expiresIn: 3600,
        },
      }
    }
  }

  /**
   * Register new user
   */
  static async register(request: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await client.post<AuthResponse>(
        ENDPOINTS.AUTH_REGISTER,
        request
      )
      return response.data
    } catch (error) {
      // Mock fallback
      return {
        user: {
          id: 'new-user-' + Date.now(),
          email: request.email,
          username: request.username,
          role: UserRole.USER,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
        token: {
          accessToken: 'mock-token-' + Date.now(),
          refreshToken: 'mock-refresh-token-' + Date.now(),
          expiresIn: 3600,
        },
      }
    }
  }

  /**
   * Logout user
   */
  static async logout(): Promise<void> {
    try {
      await client.post(ENDPOINTS.AUTH_LOGOUT)
    } catch (error) {
      // Mock: just log out locally
      console.log('Logged out')
    }
  }

  /**
   * Get current user profile
   */
  static async getProfile(): Promise<User> {
    try {
      const response = await client.get<User>(ENDPOINTS.AUTH_PROFILE)
      return response.data
    } catch (error) {
      // Mock fallback
      return mockUsers[0]
    }
  }

  /**
   * Refresh authentication token
   */
  static async refreshToken(): Promise<string> {
    try {
      const response = await client.post<{ accessToken: string }>(
        ENDPOINTS.AUTH_REFRESH
      )
      return response.data.accessToken
    } catch (error) {
      throw new NotImplementedError('Token refresh not yet implemented')
    }
  }

  /**
   * Update user profile
   */
  static async updateProfile(
    request: ProfileUpdateRequest
  ): Promise<User> {
    try {
      const response = await client.put<User>(
        ENDPOINTS.PROFILE_UPDATE,
        request
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError('Profile update not yet implemented')
    }
  }
}
