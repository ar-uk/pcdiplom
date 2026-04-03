/**
 * API Client
 * Axios instance with interceptors for authentication, error handling, and logging
 */

import axios, {
  AxiosInstance,
  InternalAxiosRequestConfig,
  AxiosResponse,
} from 'axios'
import { handleApiError } from '@/utils/errors'
import { getToken } from '@/utils/storage'
import { FEATURE_FLAGS } from '@/utils/constants'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const API_TIMEOUT = 30000

/**
 * Create axios instance with default configuration
 */
const client: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * Request interceptor: Add auth token and logging
 */
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (FEATURE_FLAGS.DEBUG_API) {
      console.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`)
    }

    return config
  },
  (error) => {
    console.error('[API] Request error:', error)
    return Promise.reject(error)
  }
)

/**
 * Response interceptor: Handle errors and logging
 */
client.interceptors.response.use(
  (response: AxiosResponse) => {
    if (FEATURE_FLAGS.DEBUG_API) {
      console.debug(`[API] Response OK: ${response.config.url}`, response.data)
    }
    return response
  },
  (error: AxiosError) => {
    const apiError = handleApiError(error)

    if (FEATURE_FLAGS.DEBUG_API) {
      console.error('[API] Response error:', {
        status: apiError.statusCode,
        message: apiError.message,
        errors: apiError.errors,
      })
    }

    // Handle 401 Unauthorized (token expired)
    if (error.response?.status === 401) {
      console.warn('[API] Token expired or unauthorized')
      // Trigger logout from store (will be implemented in Phase 2)
      // dispatch(logout())
    }

    return Promise.reject(apiError)
  }
)

export default client
