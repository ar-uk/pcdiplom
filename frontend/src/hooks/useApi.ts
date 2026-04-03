/**
 * useApi Hook
 * Generic API fetching hook with mock fallback support
 */

import { useState, useEffect, useCallback } from 'react'
import { ApiError, isRetryableError } from '@/utils/errors'
import { FEATURE_FLAGS } from '@/utils/constants'

export interface UseApiState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}

export interface UseApiOptions<T> {
  mockData?: T
  enableMockFallback?: boolean
  retries?: number
  retryDelay?: number
  onSuccess?: (data: T) => void
  onError?: (error: ApiError) => void
}

/**
 * Generic hook for API calls with automatic retry and mock fallback
 */
export function useApi<T>(
  apiFunction: () => Promise<T>,
  dependencies: unknown[] = [],
  options: UseApiOptions<T> = {}
): UseApiState<T> & { refetch: () => Promise<void> } {
  const {
    mockData = null,
    enableMockFallback = FEATURE_FLAGS.MOCK_MODE,
    retries = 3,
    retryDelay = 1000,
    onSuccess,
    onError,
  } = options

  const [state, setState] = useState<UseApiState<T>>({
    data: mockData || null,
    loading: true,
    error: null,
  })

  const [retryCount, setRetryCount] = useState(0)

  /**
   * Execute API call with retry logic
   */
  const fetchData = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }))

    try {
      const result = await apiFunction()
      setState({
        data: result,
        loading: false,
        error: null,
      })

      if (onSuccess) {
        onSuccess(result)
      }

      // Reset retry count on success
      setRetryCount(0)
    } catch (error) {
      const apiError = error instanceof ApiError ? error : new ApiError(
        error instanceof Error ? error.message : 'Unknown error',
        0,
        error
      )

      // Check if we should retry
      const shouldRetry =
        isRetryableError(apiError) && retryCount < retries

      if (shouldRetry) {
        // Exponential backoff: delay increases with each retry
        const delay = retryDelay * Math.pow(2, retryCount)

        if (FEATURE_FLAGS.DEBUG_API) {
          console.debug(
            `[useApi] Retrying in ${delay}ms (attempt ${retryCount + 1}/${retries})`
          )
        }

        setTimeout(() => {
          setRetryCount((prev) => prev + 1)
          fetchData()
        }, delay)
      } else {
        // Handle final error
        if (enableMockFallback && mockData) {
          if (FEATURE_FLAGS.DEBUG_API) {
            console.debug(
              '[useApi] Using mock data after retries exhausted'
            )
          }

          setState({
            data: mockData,
            loading: false,
            error: null,
          })

          if (onSuccess) {
            onSuccess(mockData)
          }
        } else {
          setState({
            data: null,
            loading: false,
            error: apiError,
          })

          if (onError) {
            onError(apiError)
          }
        }
      }
    }
  }, [apiFunction, mockData, enableMockFallback, retries, retryCount, onSuccess, onError])

  /**
   * Effect to run on mount and when dependencies change
   */
  useEffect(() => {
    fetchData()
  }, dependencies)

  /**
   * Manual refetch function
   */
  const refetch = useCallback(async () => {
    setRetryCount(0)
    await fetchData()
  }, [fetchData])

  return {
    ...state,
    refetch,
  }
}

/**
 * Hook for mutating data (POST, PUT, DELETE)
 */
export interface UseMutationState<T> {
  data: T | null
  loading: boolean
  error: ApiError | null
}

export interface UseMutationOptions<T> {
  onSuccess?: (data: T) => void
  onError?: (error: ApiError) => void
}

export function useMutation<T, P = unknown>(
  mutationFunction: (payload: P) => Promise<T>,
  options: UseMutationOptions<T> = {}
) {
  const { onSuccess, onError } = options

  const [state, setState] = useState<UseMutationState<T>>({
    data: null,
    loading: false,
    error: null,
  })

  const mutate = useCallback(
    async (payload: P) => {
      setState({ data: null, loading: true, error: null })

      try {
        const result = await mutationFunction(payload)
        setState({ data: result, loading: false, error: null })

        if (onSuccess) {
          onSuccess(result)
        }

        return result
      } catch (error) {
        const apiError = error instanceof ApiError ? error : new ApiError(
          error instanceof Error ? error.message : 'Unknown error',
          0,
          error
        )

        setState({ data: null, loading: false, error: apiError })

        if (onError) {
          onError(apiError)
        }

        throw apiError
      }
    },
    [mutationFunction, onSuccess, onError]
  )

  return {
    ...state,
    mutate,
  }
}
