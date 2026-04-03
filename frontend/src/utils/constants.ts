/**
 * Application Constants
 * App-wide constants and configuration values
 */

import { UserRole, ComponentCategory } from '@/types'

export const USER_ROLES = {
  USER: UserRole.USER,
  ADMIN: UserRole.ADMIN,
} as const

export const COMPONENT_CATEGORIES = {
  CPU: ComponentCategory.CPU,
  GPU: ComponentCategory.GPU,
  RAM: ComponentCategory.RAM,
  SSD: ComponentCategory.SSD,
  HDD: ComponentCategory.HDD,
  PSU: ComponentCategory.PSU,
  MOTHERBOARD: ComponentCategory.MOTHERBOARD,
  CASE: ComponentCategory.CASE,
  COOLER: ComponentCategory.COOLER,
  MONITOR: ComponentCategory.MONITOR,
} as const

export const COMPONENT_CATEGORY_LABELS: Record<ComponentCategory, string> = {
  [ComponentCategory.CPU]: 'Processor (CPU)',
  [ComponentCategory.GPU]: 'Graphics Card (GPU)',
  [ComponentCategory.RAM]: 'Memory (RAM)',
  [ComponentCategory.SSD]: 'SSD Storage',
  [ComponentCategory.HDD]: 'HDD Storage',
  [ComponentCategory.PSU]: 'Power Supply (PSU)',
  [ComponentCategory.MOTHERBOARD]: 'Motherboard',
  [ComponentCategory.CASE]: 'Case',
  [ComponentCategory.COOLER]: 'CPU Cooler',
  [ComponentCategory.MONITOR]: 'Monitor',
}

export const API_TIMEOUT = 30000 // 30 seconds

export const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  REFRESH_TOKEN: 'auth_refresh_token',
  USER: 'auth_user',
  BUILD_DRAFT: 'build_draft',
  RECENT_BUILDS: 'recent_builds',
  USER_PREFERENCES: 'user_preferences',
} as const

export const FEATURE_FLAGS = {
  MOCK_MODE: import.meta.env.VITE_MOCK_ENABLED === 'true',
  DEBUG_API: import.meta.env.VITE_DEBUG_API === 'true',
  ENABLE_AI_ASSISTANT: true,
  ENABLE_COMMUNITY: true,
} as const

export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
} as const

export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  TIMEOUT: 'Request timed out. Please try again.',
  UNAUTHORIZED: 'Unauthorized. Please log in.',
  FORBIDDEN: 'You do not have permission to access this resource.',
  NOT_FOUND: 'Resource not found.',
  SERVER_ERROR: 'Server error. Please try again later.',
  VALIDATION_ERROR: 'Validation error. Please check your input.',
  GENERIC_ERROR: 'An error occurred. Please try again.',
} as const

export const PAGINATION_DEFAULTS = {
  PAGE: 1,
  LIMIT: 20,
  MAX_LIMIT: 100,
} as const

export const CACHE_DURATIONS = {
  SHORT: 5 * 60 * 1000, // 5 minutes
  MEDIUM: 30 * 60 * 1000, // 30 minutes
  LONG: 60 * 60 * 1000, // 1 hour
} as const

export const BUILD_STATUS_LABELS = {
  DRAFT: 'Draft',
  SAVED: 'Saved',
  PUBLISHED: 'Published',
  ARCHIVED: 'Archived',
} as const

export const VALIDATION_RULES = {
  PASSWORD_MIN_LENGTH: 8,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 30,
  EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  BUILD_NAME_MAX_LENGTH: 100,
  BUILD_DESC_MAX_LENGTH: 500,
} as const

// UI Component Defaults
export const UI_DEFAULTS = {
  TOAST_DURATION: 4000, // ms
  MODAL_ANIMATION_DURATION: 200, // ms
  INPUT_DEBOUNCE_DELAY: 300, // ms
  AUTO_SCROLL_DELAY: 100, // ms
} as const

// Form Field Limits
export const FORM_LIMITS = {
  CHAT_MESSAGE_MAX: 2000,
  PROFILE_BIO_MAX: 500,
  BUILD_NOTES_MAX: 1000,
  COMPONENT_COMMENT_MAX: 500,
} as const

// AI Assistant Constants
export const AI_ASSISTANT_CONFIG = {
  MESSAGE_PAGE_SIZE: 20,
  SUGGESTION_COUNT: 3,
  TYPING_INDICATOR_DELAY: 500,
} as const
