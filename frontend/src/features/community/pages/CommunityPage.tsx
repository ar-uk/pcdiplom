/**
 * CommunityPage
 * Main community interface with sidebar filters and builds grid
 */

import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  // BuildCard,
  BuildGrid,
  BuildFilters,
  CommunityStats,
} from '../components'
import { useCommunityBuilds } from '../hooks'

export const CommunityPage: React.FC = () => {
  const navigate = useNavigate()
  const [filtersCollapsed, setFiltersCollapsed] = useState(true)

  const {
    builds,
    allBuilds,
    isLoading,
    error,
    // filters,
    setSearch,
    setPriceRange,
    setTier,
    setSortBy,
    sortBy,
    resetFilters,
    pagination,
    nextPage,
    previousPage,
  } = useCommunityBuilds(12)

  const handleBuildClick = useCallback(
    (buildId: string) => {
      // Navigate to builder details page (shared with community builds)
      navigate(`/builder/${buildId}`)
    },
    [navigate]
  )

  const handleCreateBuild = useCallback(() => {
    navigate('/builder')
  }, [navigate])

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Community Builds</h1>
            <p className="mt-1 text-gray-600">
              Browse {pagination.totalFiltered} shared build configurations
            </p>
          </div>
          <button
            onClick={handleCreateBuild}
            className="rounded-lg bg-blue-600 px-6 py-2 font-medium text-white transition-colors hover:bg-blue-700"
          >
            + Share Your Build
          </button>
        </div>

        {/* Stats */}
        <div className="mb-8">
          <CommunityStats builds={allBuilds as any} />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-4">
          {/* Sidebar Filters */}
          <div className="lg:col-span-1">
            <BuildFilters
              onSearchChange={setSearch}
              onPriceRangeChange={setPriceRange}
              onTierChange={setTier}
              onSortChange={setSortBy}
              onResetFilters={resetFilters}
              isCollapsed={filtersCollapsed}
              onToggleCollapse={setFiltersCollapsed}
            />
          </div>

          {/* Main Content */}
          <div className="lg:col-span-3">
            {/* Builds Grid */}
            <BuildGrid
              builds={builds as any}
              isLoading={isLoading}
              error={error}
              onBuildClick={(build) => handleBuildClick(build.id)}
            />

            {/* Pagination */}
            {!isLoading && builds.length > 0 && (
              <div className="mt-8 flex items-center justify-between">
                <button
                  onClick={previousPage}
                  disabled={pagination.page === 1}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 transition-colors disabled:opacity-50 hover:bg-gray-50"
                >
                  ← Previous
                </button>

                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-600">
                    Page {pagination.page} of {pagination.totalPages > 0 ? pagination.totalPages : 1}
                  </span>
                </div>

                <button
                  onClick={nextPage}
                  disabled={pagination.page >= pagination.totalPages}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 transition-colors disabled:opacity-50 hover:bg-gray-50"
                >
                  Next →
                </button>
              </div>
            )}

            {/* Load More / Info */}
            {!isLoading && builds.length > 0 && pagination.page < pagination.totalPages && (
              <div className="mt-6 text-center">
                <p className="text-sm text-gray-600">
                  Showing {builds.length} of {pagination.totalFiltered} builds
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default CommunityPage
