/**
 * Mock Fallbacks
 * Helper functions for graceful degradation to mock data
 */

import {
  mockUsers,
  mockComponents,
  mockBuilds,
  mockCommunityBuilds,
} from './mockData'
import { ENDPOINTS } from '../endpoints'

/**
 * Create fallback response based on API endpoint
 * Returns appropriate mock data for the requested endpoint
 */
export function createFallbackResponse(endpoint: string): unknown {
  // Auth endpoints
  if (endpoint === ENDPOINTS.AUTH_LOGIN) {
    return {
      success: true,
      data: {
        user: mockUsers[0],
        token: {
          accessToken: 'mock-token-12345',
          refreshToken: 'mock-refresh-token-12345',
          expiresIn: 3600,
        },
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint === ENDPOINTS.AUTH_REGISTER) {
    return {
      success: true,
      data: {
        user: mockUsers[0],
        token: {
          accessToken: 'mock-token-12345',
          refreshToken: 'mock-refresh-token-12345',
          expiresIn: 3600,
        },
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint === ENDPOINTS.AUTH_PROFILE) {
    return {
      success: true,
      data: mockUsers[0],
      timestamp: new Date().toISOString(),
    }
  }

  // Build endpoints
  if (endpoint === ENDPOINTS.BUILDS_LIST) {
    return {
      success: true,
      data: mockBuilds,
      meta: {
        page: 1,
        limit: 20,
        total: mockBuilds.length,
        totalPages: 1,
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint.startsWith(ENDPOINTS.COMPONENTS_LIST)) {
    return {
      success: true,
      data: mockComponents,
      meta: {
        page: 1,
        limit: 100,
        total: mockComponents.length,
        totalPages: 1,
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint === ENDPOINTS.COMMUNITY_BUILDS) {
    return {
      success: true,
      data: mockCommunityBuilds,
      meta: {
        page: 1,
        limit: 20,
        total: mockCommunityBuilds.length,
        totalPages: 1,
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint === ENDPOINTS.COMMUNITY_STATS) {
    return {
      success: true,
      data: {
        totalBuilds: mockCommunityBuilds.length,
        totalUsers: mockUsers.length,
        averageRating: 4.5,
        totalViews: 5270,
      },
      timestamp: new Date().toISOString(),
    }
  }

  if (endpoint === ENDPOINTS.PROFILE_BUILDS) {
    return {
      success: true,
      data: mockBuilds,
      meta: {
        page: 1,
        limit: 20,
        total: mockBuilds.length,
        totalPages: 1,
      },
      timestamp: new Date().toISOString(),
    }
  }

  // Default fallback
  return {
    success: true,
    data: null,
    message: 'Mock fallback data',
    timestamp: new Date().toISOString(),
  }
}

/**
 * Decorator for service methods to add mock fallback
 * Wraps API call and falls back to mock data on error if enabled
 */
export async function wrapWithMockFallback<T>(
  serviceMethod: () => Promise<T>,
  mockData: T,
  enableFallback: boolean = true
): Promise<T> {
  try {
    return await serviceMethod()
  } catch (error) {
    if (enableFallback) {
      console.warn(
        '[Mock Fallback] API call failed, using mock data:',
        error
      )
      return mockData
    }
    throw error
  }
}

/**
 * Get mock data by component category
 */
export function getMockComponentsByCategory(category: string) {
  return mockComponents.filter(
    (c) => c.category.toString().toLowerCase() === category.toLowerCase()
  )
}

/**
 * Get mock build by ID
 */
export function getMockBuildById(id: string) {
  return mockBuilds.find((b) => b.id === id)
}

/**
 * Get mock community build by ID
 */
export function getMockCommunityBuildById(id: string) {
  return mockCommunityBuilds.find((b) => b.id === id)
}

/**
 * Get mock user by ID
 */
export function getMockUserById(id: string) {
  return mockUsers.find((u) => u.id === id)
}

/**
 * Get mock component by ID
 */
export function getMockComponentById(id: string) {
  return mockComponents.find((c) => c.id === id)
}
