/**
 * BuildGrid Component
 * Grid layout container for displaying builds
 */

import React from 'react'
import { CommunityBuild } from '@/types'
import BuildCard from './BuildCard'

export interface BuildGridProps {
  builds: CommunityBuild[]
  isLoading?: boolean
  error?: Error | null
  onBuildClick?: (build: CommunityBuild) => void
  pageSize?: number
}

const SkeletonCard: React.FC = () => (
  <div className="animate-pulse rounded-lg border border-gray-200 bg-white p-4">
    <div className="mb-3 h-6 bg-gray-200 rounded w-3/4" />
    <div className="mb-1 h-4 bg-gray-200 rounded w-1/2" />
    <div className="mb-3 h-24 bg-gray-100 rounded" />
    <div className="mb-2 h-4 bg-gray-200 rounded" />
    <div className="mb-3 h-8 bg-gray-200 rounded w-1/3" />
    <div className="h-4 bg-gray-200 rounded w-1/4" />
  </div>
)

export const BuildGrid: React.FC<BuildGridProps> = ({
  builds,
  isLoading = false,
  error = null,
  onBuildClick,
  pageSize: _pageSize = 12,
}) => {
  const skeletonCards = Array.from({ length: 6 }).map((_, i) => (
    <SkeletonCard key={`skeleton-${i}`} />
  ))

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-8 text-center">
        <p className="text-red-800">
          Failed to load builds: {error.message}
        </p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        {skeletonCards}
      </div>
    )
  }

  if (builds.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-12 text-center">
        <p className="text-2xl">🔍</p>
        <p className="mt-2 text-lg font-semibold text-gray-700">
          No builds found
        </p>
        <p className="text-gray-600">
          Try adjusting your filters or search terms
        </p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      {builds.map((build) => (
        <BuildCard
          key={build.id}
          build={build}
          onClick={onBuildClick}
        />
      ))}
    </div>
  )
}

export default BuildGrid
