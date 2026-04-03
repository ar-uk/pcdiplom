/**
 * Profile Service
 * Handles all user profile-related API calls
 */

import client from '../client'
import { ENDPOINTS } from '../endpoints'
import { User, SavedBuild, ProfileUpdateRequest } from '@/types'
import { NotImplementedError } from '@/utils/errors'
import { mockUsers, mockBuilds } from '../mock/mockData'

export class ProfileService {
  /**
   * Get user profile
   */
  static async getProfile(): Promise<User> {
    try {
      const response = await client.get<User>(ENDPOINTS.PROFILE_INFO)
      return response.data
    } catch (error) {
      // Mock fallback
      return mockUsers[0]
    }
  }

  /**
   * Update user profile
   */
  static async updateProfile(request: ProfileUpdateRequest): Promise<User> {
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

  /**
   * Get user's saved builds
   */
  static async getSavedBuilds(): Promise<SavedBuild[]> {
    try {
      const response = await client.get<SavedBuild[]>(ENDPOINTS.PROFILE_BUILDS)
      return response.data
    } catch (error) {
      // Mock fallback
      return mockBuilds
    }
  }

  /**
   * Change password
   */
  static async changePassword(
    currentPassword: string,
    newPassword: string
  ): Promise<void> {
    try {
      await client.post(ENDPOINTS.PROFILE_CHANGE_PASSWORD, {
        currentPassword,
        newPassword,
      })
    } catch (error) {
      throw new NotImplementedError('Change password not yet implemented')
    }
  }

  /**
   * Get user statistics
   */
  static async getUserStats(): Promise<Record<string, unknown>> {
    try {
      const response = await client.get(`${ENDPOINTS.PROFILE_INFO}/stats`)
      return response.data
    } catch (error) {
      // Mock fallback
      return {
        buildCount: mockBuilds.length,
        publicBuilds: mockBuilds.filter((b) => b.isPublic).length,
        totalViews: mockBuilds.reduce((acc, b) => acc + b.views, 0),
        joinDate: mockUsers[0].createdAt,
      }
    }
  }

  /**
   * Delete user account
   */
  static async deleteAccount(): Promise<void> {
    try {
      await client.delete(ENDPOINTS.PROFILE_INFO)
    } catch (error) {
      throw new NotImplementedError('Delete account not yet implemented')
    }
  }
}
