/**
 * BuildCard Component
 * Individual build card for grid display
 */

import React from 'react'
import { CommunityBuild } from '@/types'
import { formatBuildSpecsSummary, formatPrice, formatRelativeTime } from '../utils'
import RatingDisplay from './RatingDisplay'

export interface BuildCardProps {
  build: CommunityBuild
  onClick?: (build: CommunityBuild) => void
}

export const BuildCard: React.FC<BuildCardProps> = ({ build, onClick }) => {
  const handleClick = () => {
    onClick?.(build)
  }

  const specsSummary = formatBuildSpecsSummary(build)
  const postedAgo = formatRelativeTime(build.createdAt)

  return (
    <div
      onClick={handleClick}
      className="cursor-pointer rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition-all duration-200 hover:shadow-md hover:border-gray-300"
    >
      {/* Header with creator */}
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900 line-clamp-2">
            {build.name}
          </h3>
          <p className="text-sm text-gray-600">
            by {build.author?.username || 'Unknown'}
          </p>
        </div>
      </div>

      {/* PC Icon/Placeholder */}
      <div className="mb-3 flex h-24 items-center justify-center rounded bg-gray-100">
        <div className="text-4xl">💻</div>
      </div>

      {/* Specs Summary */}
      <p className="mb-3 text-sm text-gray-700 line-clamp-2">{specsSummary}</p>

      {/* Price and Stats Row */}
      <div className="mb-3 flex items-center justify-between border-t border-gray-200 pt-2">
        <div>
          <div className="text-lg font-bold text-gray-900">
            {formatPrice(build.estimatedPrice)}
          </div>
        </div>
        <div className="flex gap-3 text-sm text-gray-600">
          <span>👁️ {build.views}</span>
          <span>💬 {build.reviewCount}</span>
        </div>
      </div>

      {/* Rating and Engagement */}
      <div className="mb-3">
        <RatingDisplay
          rating={build.averageRating}
          count={build.reviewCount}
          size="sm"
        />
      </div>

      {/* Likes and Posted */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>👍 {build.likes} upvotes</span>
        <span>{postedAgo}</span>
      </div>

      {/* Tags */}
      {build.tags && build.tags.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1">
          {build.tags.slice(0, 2).map((tag) => (
            <span
              key={tag}
              className="inline-block rounded-full bg-blue-50 px-2 py-1 text-xs text-blue-700"
            >
              #{tag}
            </span>
          ))}
          {build.tags.length > 2 && (
            <span className="inline-block text-xs text-gray-500">
              +{build.tags.length - 2} more
            </span>
          )}
        </div>
      )}
    </div>
  )
}

export default React.memo(BuildCard)
