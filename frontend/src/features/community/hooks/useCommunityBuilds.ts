/**
 * useCommunityBuilds Hook
 * Custom React hook for fetching and managing community builds with filtering
 */

import { useState, useCallback, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { CommunityService } from '@/api/services'
import { CommunityBuild } from '@/types'
import { sortBuilds, filterBuilds } from '../utils'

export interface CommunityBuildsState {
  builds: CommunityBuild[]
  isLoading: boolean
  error: Error | null
  filters: {
    search?: string
    category?: string
    priceRange?: [number, number]
    tier?: 'budget' | 'mid-range' | 'high-end'
  }
  pagination: {
    page: number
    pageSize: number
    total: number
  }
  sortBy: 'newest' | 'trending' | 'mostUpvoted' | 'cheapest' | 'mostExpensive'
}

export function useCommunityBuilds(initialPageSize = 12) {
  const [filters, setLocalFilters] = useState<CommunityBuildsState['filters']>({})
  const [pagination, setPagination] = useState({
    page: 1,
    pageSize: initialPageSize,
    total: 0,
  })
  const [sortBy, setSortBy] = useState<CommunityBuildsState['sortBy']>('newest')

  // Fetch all community builds
  const { data: allBuilds = [], isLoading, error, refetch } = useQuery({
    queryKey: ['communityBuilds'],
    queryFn: () => CommunityService.getBuilds(),
    staleTime: 10 * 60 * 1000, // 10 minutes
  })

  // Apply filters and sorting locally
  const filteredAndSorted = useCallback(() => {
    let result = filterBuilds(allBuilds, filters)
    result = sortBuilds(result, sortBy)
    return result
  }, [allBuilds, filters, sortBy])

  const processedBuilds = useCallback(() => {
    const all = filteredAndSorted()
    const total = all.length
    const start = (pagination.page - 1) * pagination.pageSize
    const end = start + pagination.pageSize
    const paginatedBuilds = all.slice(start, end)

    setPagination((prev) => ({
      ...prev,
      total,
    }))

    return paginatedBuilds
  }, [filteredAndSorted, pagination.page, pagination.pageSize])

  // Reset to page 1 when filters change
  useEffect(() => {
    setPagination((prev) => ({
      ...prev,
      page: 1,
    }))
  }, [filters, sortBy])

  // Methods to update filters
  const setSearch = useCallback((term?: string) => {
    setLocalFilters((prev) => ({
      ...prev,
      search: term || undefined,
    }))
  }, [])

  const setCategory = useCallback((category?: string) => {
    setLocalFilters((prev) => ({
      ...prev,
      category: category || undefined,
    }))
  }, [])

  const setPriceRange = useCallback((min?: number, max?: number) => {
    setLocalFilters((prev) => ({
      ...prev,
      priceRange: min && max ? [min, max] : undefined,
    }))
  }, [])

  const setTier = useCallback(
    (tier?: 'budget' | 'mid-range' | 'high-end') => {
      setLocalFilters((prev) => ({
        ...prev,
        tier: tier || undefined,
      }))
    },
    []
  )

  const handleSortBy = useCallback(
    (
      newSort: 'newest' | 'trending' | 'mostUpvoted' | 'cheapest' | 'mostExpensive'
    ) => {
      setSortBy(newSort)
    },
    []
  )

  const nextPage = useCallback(() => {
    setPagination((prev) => ({
      ...prev,
      page: Math.min(
        prev.page + 1,
        Math.ceil(prev.total / prev.pageSize)
      ),
    }))
  }, [])

  const previousPage = useCallback(() => {
    setPagination((prev) => ({
      ...prev,
      page: Math.max(prev.page - 1, 1),
    }))
  }, [])

  const resetFilters = useCallback(() => {
    setLocalFilters({})
    setSortBy('newest')
    setPagination((prev) => ({
      ...prev,
      page: 1,
    }))
  }, [])

  // Get total count of filtered results (not paginated)
  const totalFiltered = filteredAndSorted().length
  const totalPages = Math.ceil(totalFiltered / pagination.pageSize)

  return {
    // Data
    builds: processedBuilds(),
    allBuilds: filteredAndSorted(),
    isLoading,
    error: error as Error | null,

    // Filter state
    filters,
    setSearch,
    setCategory,
    setPriceRange,
    setTier,
    resetFilters,

    // Sorting
    sortBy,
    setSortBy: handleSortBy,

    // Pagination
    pagination: {
      page: pagination.page,
      pageSize: pagination.pageSize,
      total: pagination.total,
      totalFiltered,
      totalPages,
    },
    nextPage,
    previousPage,
    goToPage: (page: number) => {
      setPagination((prev) => ({
        ...prev,
        page: Math.max(1, Math.min(page, totalPages)),
      }))
    },

    // Refetch
    refetch,
  }
}
