/**
 * API Endpoints
 * Centralized API route constants
 */

export const ENDPOINTS = {
  // Auth endpoints
  AUTH_LOGIN: '/auth/login',
  AUTH_REGISTER: '/auth/register',
  AUTH_LOGOUT: '/auth/logout',
  AUTH_REFRESH: '/auth/refresh',
  AUTH_PROFILE: '/auth/profile',

  // Build endpoints
  BUILDS_LIST: '/api/builds',
  BUILD_DETAIL: (id: string) => `/api/builds/${id}`,
  BUILD_CREATE: '/api/builds',
  BUILD_UPDATE: (id: string) => `/api/builds/${id}`,
  BUILD_DELETE: (id: string) => `/api/builds/${id}`,

  // Component endpoints
  COMPONENTS_LIST: '/api/parts',
  COMPONENT_DETAIL: (id: string) => `/api/parts/${id}`,
  COMPONENTS_BY_CATEGORY: (category: string) =>
    `/api/parts?category=${category}`,

  // Coverage check endpoint
  BUILD_COVERAGE: (id: string) => `/api/builds/${id}/coverage`,
  COMPATIBILITY_CHECK: '/api/builds/compatibility',

  // Community endpoints
  COMMUNITY_BUILDS: '/community/builds',
  COMMUNITY_BUILD_DETAIL: (id: string) => `/community/builds/${id}`,
  COMMUNITY_BUILD_RATE: (id: string) => `/community/builds/${id}/rate`,
  COMMUNITY_BUILD_REVIEW: (id: string) => `/community/builds/${id}/review`,
  COMMUNITY_STATS: '/community/stats',

  // AI/Recommendation endpoints
  AI_CHAT: '/api/recommendation/chat',
  AI_BUILD_RECOMMENDATION: '/api/recommendation/build',

  // Profile endpoints
  PROFILE_INFO: '/auth/profile',
  PROFILE_UPDATE: '/auth/profile',
  PROFILE_BUILDS: '/auth/builds',
  PROFILE_CHANGE_PASSWORD: '/auth/change-password',

  // Admin endpoints (if needed)
  ADMIN_USERS: '/admin/users',
  ADMIN_USER_DETAIL: (id: string) => `/admin/users/${id}`,
  ADMIN_COMPONENTS: '/admin/parts',
} as const

/**
 * Helper function to construct URLs with parameters
 */
export function buildUrl(
  endpoint: string,
  params?: Record<string, string | number>
): string {
  const url = new URL(endpoint, import.meta.env.VITE_API_URL)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      url.searchParams.append(key, String(value))
    })
  }
  return url.toString()
}
