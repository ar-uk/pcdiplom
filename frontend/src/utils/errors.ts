/**
 * Error Utilities
 * Custom error classes and error handling utilities
 */

export class ApiError extends Error {
  constructor(
    message: string,
    public statusCode: number = 0,
    public originalError?: unknown,
    public errors?: Record<string, string[]>
  ) {
    super(message)
    this.name = 'ApiError'
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

export class ValidationError extends Error {
  constructor(
    message: string,
    public field?: string,
    public value?: unknown
  ) {
    super(message)
    this.name = 'ValidationError'
    Object.setPrototypeOf(this, ValidationError.prototype)
  }
}

export class NotImplementedError extends Error {
  constructor(
    message: string = 'This feature is not yet implemented'
  ) {
    super(message)
    this.name = 'NotImplementedError'
    Object.setPrototypeOf(this, NotImplementedError.prototype)
  }
}

export class NetworkError extends Error {
  constructor(
    message: string = 'Network error occurred',
    public retryable: boolean = true
  ) {
    super(message)
    this.name = 'NetworkError'
    Object.setPrototypeOf(this, NetworkError.prototype)
  }
}

/**
 * Normalize and handle API errors
 */
export function handleApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }

  if (error instanceof ValidationError) {
    return new ApiError(
      error.message,
      400,
      error,
      error.field ? { [error.field]: [error.message] } : undefined
    )
  }

  if (error instanceof NetworkError) {
    return new ApiError(error.message, 0, error)
  }

  if (error instanceof Error) {
    // Check for axios error structure
    const axiosError = error as any
    if (axiosError.response) {
      return new ApiError(
        axiosError.response.data?.message || axiosError.message,
        axiosError.response.status,
        error,
        axiosError.response.data?.errors
      )
    }

    if (axiosError.request) {
      return new NetworkError(
        'No response from server',
        true
      ) as any
    }

    return new ApiError(
      error.message,
      0,
      error
    )
  }

  return new ApiError(
    'An unknown error occurred',
    0,
    error
  )
}

/**
 * Check if error is retryable
 */
export function isRetryableError(error: ApiError): boolean {
  const retryableStatuses = [408, 429, 500, 502, 503, 504]
  if (error instanceof NetworkError) {
    return error.retryable
  }
  return retryableStatuses.includes(error.statusCode)
}

/**
 * Format error message for display
 */
export function formatErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'An unexpected error occurred'
}
