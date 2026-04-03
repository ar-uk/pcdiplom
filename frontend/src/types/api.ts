/**
 * API Response Types
 * Defines all generic API-related types and interfaces
 */

export interface PaginationMeta {
  page: number
  limit: number
  total: number
  totalPages: number
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  timestamp: string
}

export interface PaginatedApiResponse<T> extends ApiResponse<T[]> {
  meta: PaginationMeta
}

export interface ApiError {
  statusCode: number
  message: string
  errors?: Record<string, string[]>
  timestamp: string
  path?: string
}

export interface ApiErrorResponse {
  success: false
  error: ApiError
}

export class ApiException extends Error {
  constructor(
    public statusCode: number,
    message: string,
    public originalError?: unknown,
    public errors?: Record<string, string[]>
  ) {
    super(message)
    this.name = 'ApiException'
  }
}

export interface RequestConfig {
  timeout?: number
  retries?: number
  mockFallback?: boolean
}

export interface InterceptorConfig {
  onRequest?: (config: any) => any
  onError?: (error: any) => any
  onSuccess?: (response: any) => any
}
