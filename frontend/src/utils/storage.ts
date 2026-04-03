/**
 * Storage Utilities
 * localStorage helpers with type safety and JSON serialization
 */
import { STORAGE_KEYS } from './constants'

/**
 * Generic localStorage getter with JSON parsing
 */
function getItem<T>(key: string): T | null {
  try {
    const item = localStorage.getItem(key)
    return item ? JSON.parse(item) : null
  } catch (error) {
    console.error(`Error parsing localStorage item "${key}":`, error)
    return null
  }
}

/**
 * Generic localStorage setter with JSON serialization
 */
function setItem<T>(key: string, value: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.error(`Error saving localStorage item "${key}":`, error)
  }
}

/**
 * Remove item from localStorage
 */
function removeItem(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.error(`Error removing localStorage item "${key}":`, error)
  }
}

/**
 * Clear all application data from localStorage
 */
export function clearStorage(): void {
  try {
    Object.values(STORAGE_KEYS).forEach((key) => {
      localStorage.removeItem(key as string)
    })
  } catch (error) {
    console.error('Error clearing localStorage:', error)
  }
}

// Auth token helpers

export function getToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.TOKEN)
}

export function setToken(token: string): void {
  setItem(STORAGE_KEYS.TOKEN, token)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
}

export function setRefreshToken(token: string): void {
  setItem(STORAGE_KEYS.REFRESH_TOKEN, token)
}

export function clearToken(): void {
  removeItem(STORAGE_KEYS.TOKEN)
  removeItem(STORAGE_KEYS.REFRESH_TOKEN)
}

// User helpers

export function getUser(): User | null {
  return getItem<User>(STORAGE_KEYS.USER)
}

export function setUser(user: User): void {
  setItem(STORAGE_KEYS.USER, user)
}

export function clearUser(): void {
  removeItem(STORAGE_KEYS.USER)
}

// Build draft helpers

export function getBuildDraft(): Record<string, unknown> | null {
  return getItem(STORAGE_KEYS.BUILD_DRAFT)
}

export function setBuildDraft(draft: Record<string, unknown>): void {
  setItem(STORAGE_KEYS.BUILD_DRAFT, draft)
}

export function clearBuildDraft(): void {
  removeItem(STORAGE_KEYS.BUILD_DRAFT)
}

// Recent builds helpers

export function getRecentBuilds(): Array<Record<string, unknown>> | null {
  return getItem(STORAGE_KEYS.RECENT_BUILDS)
}

export function addRecentBuild(build: Record<string, unknown>): void {
  const recent = getRecentBuilds() || []
  recent.unshift(build)
  // Keep only last 10 builds
  setItem(STORAGE_KEYS.RECENT_BUILDS, recent.slice(0, 10))
}

export function clearRecentBuilds(): void {
  removeItem(STORAGE_KEYS.RECENT_BUILDS)
}

// User preferences helpers

export function getUserPreferences(): Record<string, unknown> | null {
  return getItem(STORAGE_KEYS.USER_PREFERENCES)
}

export function setUserPreferences(
  preferences: Record<string, unknown>
): void {
  setItem(STORAGE_KEYS.USER_PREFERENCES, preferences)
}

export function updateUserPreference(
  key: string,
  value: unknown
): void {
  const prefs = getUserPreferences() || {}
  prefs[key] = value
  setUserPreferences(prefs)
}

export function clearUserPreferences(): void {
  removeItem(STORAGE_KEYS.USER_PREFERENCES)
}

// Composite clearers

export function clearAuthData(): void {
  clearToken()
  clearUser()
}

export function clearBuildData(): void {
  clearBuildDraft()
  clearRecentBuilds()
}

export function clearAllData(): void {
  clearStorage()
}
