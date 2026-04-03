/**
 * Community Service
 * Handles all community and marketplace-related API calls
 */

import client from '../client'
import { ENDPOINTS } from '../endpoints'
import {
  CommunityBuild,
  CommunityBuildFilter,
  CommunityStats,
  BuildRating,
} from '@/types'
import { NotImplementedError } from '@/utils/errors'
import { mockCommunityBuilds } from '../mock/mockData'

export class CommunityService {
  /**
   * Get all community builds with optional filtering
   */
  static async getBuilds(
    filter?: CommunityBuildFilter
  ): Promise<CommunityBuild[]> {
    try {
      const params = new URLSearchParams()
      if (filter) {
        if (filter.search) params.append('search', filter.search)
        if (filter.tag) params.append('tag', filter.tag)
        if (filter.minPrice) params.append('minPrice', String(filter.minPrice))
        if (filter.maxPrice) params.append('maxPrice', String(filter.maxPrice))
        if (filter.minRating)
          params.append('minRating', String(filter.minRating))
        if (filter.sortBy) params.append('sortBy', filter.sortBy)
        if (filter.limit) params.append('limit', String(filter.limit))
        if (filter.offset) params.append('offset', String(filter.offset))
      }
      const url =
        ENDPOINTS.COMMUNITY_BUILDS +
        (params.toString() ? `?${params.toString()}` : '')
      const response = await client.get<CommunityBuild[]>(url)
      return response.data
    } catch (error) {
      // Mock fallback
      return mockCommunityBuilds
    }
  }

  /**
   * Get community build by ID
   */
  static async getBuildById(id: string): Promise<CommunityBuild> {
    try {
      const response = await client.get<CommunityBuild>(
        ENDPOINTS.COMMUNITY_BUILD_DETAIL(id)
      )
      return response.data
    } catch (error) {
      // Mock fallback
      const build = mockCommunityBuilds.find((b) => b.id === id)
      if (!build) {
        throw new Error(`Community build ${id} not found`)
      }
      return build
    }
  }

  /**
   * Rate a community build
   */
  static async rateBuild(
    buildId: string,
    rating: number
  ): Promise<BuildRating> {
    try {
      const response = await client.post<BuildRating>(
        ENDPOINTS.COMMUNITY_BUILD_RATE(buildId),
        { rating }
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError('Rate build not yet implemented')
    }
  }

  /**
   * Add review to community build
   */
  static async addReview(
    buildId: string,
    review: Record<string, unknown>
  ): Promise<Record<string, unknown>> {
    try {
      const response = await client.post(
        ENDPOINTS.COMMUNITY_BUILD_REVIEW(buildId),
        review
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError('Add review not yet implemented')
    }
  }

  /**
   * Get community statistics
   */
  static async getStats(): Promise<CommunityStats> {
    try {
      const response = await client.get<CommunityStats>(
        ENDPOINTS.COMMUNITY_STATS
      )
      return response.data
    } catch (error) {
      // Mock fallback
      return {
        totalBuilds: mockCommunityBuilds.length,
        totalUsers: 100,
        averageRating: 4.5,
        totalViews: mockCommunityBuilds.reduce((acc, b) => acc + b.views, 0),
      }
    }
  }

  /**
   * Search community builds
   */
  static async searchBuilds(query: string): Promise<CommunityBuild[]> {
    try {
      const response = await client.get<CommunityBuild[]>(
        `${ENDPOINTS.COMMUNITY_BUILDS}?search=${encodeURIComponent(query)}`
      )
      return response.data
    } catch (error) {
      // Mock fallback - simple search
      return mockCommunityBuilds.filter(
        (b) =>
          b.name.toLowerCase().includes(query.toLowerCase()) ||
          b.description?.toLowerCase().includes(query.toLowerCase())
      )
    }
  }

  /**
   * Get trending builds
   */
  static async getTrendingBuilds(): Promise<CommunityBuild[]> {
    try {
      const response = await client.get<CommunityBuild[]>(
        `${ENDPOINTS.COMMUNITY_BUILDS}?sortBy=trending`
      )
      return response.data
    } catch (error) {
      // Mock fallback
      return mockCommunityBuilds.sort((a, b) => b.views - a.views)
    }
  }
}
