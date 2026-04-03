/**
 * useComponentSearch Hook
 * Custom hook for searching and filtering components
 */

import { useState, useCallback, useMemo } from 'react'
import { ComponentCategory } from '@/types'
import { useQuery } from '@tanstack/react-query'
import { BuildService } from '@/api/services/buildService'

interface UseComponentSearchOptions {
  category?: ComponentCategory
  searchTerm?: string
  maxPrice?: number
  pageSize?: number
}

export function useComponentSearch(options: UseComponentSearchOptions = {}) {
  const { category, searchTerm = '', maxPrice, pageSize = 10 } = options

  const [currentPage, setCurrentPage] = useState(1)

  // Fetch components
  const { data: allComponents = [], isLoading, error } = useQuery({
    queryKey: ['components', category],
    queryFn: () => BuildService.getComponents(category),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })

  // Filter components
  const filteredComponents = useMemo(() => {
    let result = [...allComponents]

    // Text search
    if (searchTerm) {
      const lower = searchTerm.toLowerCase()
      result = result.filter(
        (c) =>
          c.name.toLowerCase().includes(lower) ||
          c.manufacturer.toLowerCase().includes(lower) ||
          c.description?.toLowerCase().includes(lower)
      )
    }

    // Price filter
    if (maxPrice) {
      result = result.filter((c) => c.price <= maxPrice)
    }

    return result
  }, [allComponents, searchTerm, maxPrice])

  // Pagination
  const totalPages = Math.ceil(filteredComponents.length / pageSize)
  const paginatedComponents = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    return filteredComponents.slice(start, start + pageSize)
  }, [filteredComponents, currentPage, pageSize])

  const goToPage = useCallback((page: number) => {
    const validPage = Math.max(1, Math.min(page, totalPages))
    setCurrentPage(validPage)
  }, [totalPages])

  return {
    results: paginatedComponents,
    allResults: filteredComponents,
    isLoading,
    error: error instanceof Error ? error.message : null,
    
    // Pagination
    currentPage,
    totalPages,
    pageSize,
    goToPage,
    hasNextPage: currentPage < totalPages,
    hasPrevPage: currentPage > 1,

    // Helpers
    totalResults: filteredComponents.length,
    displayStart: (currentPage - 1) * pageSize + 1,
    displayEnd: Math.min(currentPage * pageSize, filteredComponents.length),
  }
}
