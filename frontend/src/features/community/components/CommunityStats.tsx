/**
 * CommunityStats Component
 * Displays community statistics in info cards
 */

import React, { useMemo } from 'react'
import { CommunityBuild } from '@/types'
import { calculateBuildTier } from '../utils'

export interface CommunityStatsProps {
  builds: CommunityBuild[]
}

export const CommunityStats: React.FC<CommunityStatsProps> = ({ builds }) => {
  const stats = useMemo(() => {
    if (builds.length === 0) {
      return {
        totalBuilds: 0,
        totalUpvotes: 0,
        avgCost: 0,
        mostPopularTier: 'N/A',
      }
    }

    const totalBuilds = builds.length
    const totalUpvotes = builds.reduce((sum, b) => sum + (b.likes || 0), 0)
    const avgCost = Math.round(
      builds.reduce((sum, b) => sum + b.estimatedPrice, 0) / totalBuilds
    )

    // Calculate most popular tier
    const tiers = {
      budget: 0,
      'mid-range': 0,
      'high-end': 0,
    }
    builds.forEach((b) => {
      const tier = calculateBuildTier(b.estimatedPrice)
      tiers[tier]++
    })
    const mostPopularTier = Object.entries(tiers).sort(
      ([, a], [, b]) => b - a
    )[0][0]

    return {
      totalBuilds,
      totalUpvotes,
      avgCost,
      mostPopularTier,
    }
  }, [builds])

  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
      <div className="rounded-lg bg-gray-50 p-4">
        <div className="text-2xl font-bold text-gray-900">
          {stats.totalBuilds}
        </div>
        <div className="text-sm text-gray-600">Total Builds</div>
      </div>

      <div className="rounded-lg bg-gray-50 p-4">
        <div className="text-2xl font-bold text-gray-900">
          {stats.totalUpvotes}
        </div>
        <div className="text-sm text-gray-600">Total Upvotes</div>
      </div>

      <div className="rounded-lg bg-gray-50 p-4">
        <div className="text-2xl font-bold text-gray-900">
          ${stats.avgCost.toLocaleString()}
        </div>
        <div className="text-sm text-gray-600">Average Cost</div>
      </div>

      <div className="rounded-lg bg-gray-50 p-4">
        <div className="text-2xl font-bold text-gray-900">
          {stats.mostPopularTier}
        </div>
        <div className="text-sm text-gray-600">Most Popular</div>
      </div>
    </div>
  )
}

export default CommunityStats
