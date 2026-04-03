/**
 * Community Utilities
 * Helper functions for community features
 */

import { SavedBuild, CommunityBuild } from '@/types'

/**
 * Format build specs summary (e.g., "Intel i7 / RTX 4070 / 32GB DDR5")
 */
export function formatBuildSpecsSummary(
  build: SavedBuild | CommunityBuild
): string {
  if (!build.components || build.components.length === 0) {
    return 'No components'
  }

  const components = build.components
  
  // All BuildComponent objects: { componentId, component }
  const cpu = components.find((c: any) => c.component?.category === 'CPU')
  const gpu = components.find((c: any) => c.component?.category === 'GPU')
  const ram = components.find((c: any) => c.component?.category === 'RAM')

  const specs = []
  if (cpu?.component?.name) specs.push(cpu.component.name.split(' ').slice(0, 3).join(' '))
  if (gpu?.component?.name) specs.push(gpu.component.name.split(' ').slice(0, 2).join(' '))
  if (ram?.component?.name) specs.push(ram.component.name.split(' ').slice(0, 2).join(' '))

  return specs.join(' / ') || 'Custom Build'
}

/**
 * Calculate build tier based on cost
 */
export function calculateBuildTier(
  cost: number
): 'budget' | 'mid-range' | 'high-end' {
  if (cost < 800) return 'budget'
  if (cost <= 1800) return 'mid-range'
  return 'high-end'
}

/**
 * Format price as currency
 */
export function formatPrice(price: number): string {
  return `$${price.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  })}`
}

/**
 * Format date as relative time (e.g., "2 days ago")
 */
export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000)

  const intervals: { [key: string]: number } = {
    year: 31536000,
    month: 2592000,
    week: 604800,
    day: 86400,
    hour: 3600,
    minute: 60,
  }

  for (const [key, value] of Object.entries(intervals)) {
    const interval = Math.floor(seconds / value)
    if (interval >= 1) {
      return interval === 1
        ? `${interval} ${key} ago`
        : `${interval} ${key}s ago`
    }
  }

  return 'just now'
}

/**
 * Sort builds by various criteria
 */
export function sortBuilds(
  builds: (SavedBuild | CommunityBuild)[],
  sortBy: 'newest' | 'trending' | 'mostUpvoted' | 'cheapest' | 'mostExpensive'
): (SavedBuild | CommunityBuild)[] {
  const sorted = [...builds]

  switch (sortBy) {
    case 'newest':
      return sorted.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )
    case 'mostUpvoted':
      return sorted.sort((a, b) => {
        const aLikes = 'likes' in a ? a.likes : 0
        const bLikes = 'likes' in b ? b.likes : 0
        return bLikes - aLikes
      })
    case 'trending':
      // Trending = recent + popular weighted score
      return sorted.sort((a, b) => {
        const aLikes = 'likes' in a ? a.likes : 0
        const bLikes = 'likes' in b ? b.likes : 0
        const aDays = Math.max(
          1,
          Math.floor(
            (Date.now() - new Date(a.createdAt).getTime()) / (1000 * 60 * 60 * 24)
          )
        )
        const bDays = Math.max(
          1,
          Math.floor(
            (Date.now() - new Date(b.createdAt).getTime()) / (1000 * 60 * 60 * 24)
          )
        )
        const aScore = aLikes / aDays
        const bScore = bLikes / bDays
        return bScore - aScore
      })
    case 'cheapest':
      return sorted.sort(
        (a, b) => a.estimatedPrice - b.estimatedPrice
      )
    case 'mostExpensive':
      return sorted.sort(
        (a, b) => b.estimatedPrice - a.estimatedPrice
      )
    default:
      return sorted
  }
}

/**
 * Filter builds by search term and category
 */
export function filterBuilds(
  builds: (SavedBuild | CommunityBuild)[],
  filters: {
    search?: string
    category?: string
    priceRange?: [number, number]
    tier?: 'budget' | 'mid-range' | 'high-end'
  }
): (SavedBuild | CommunityBuild)[] {
  return builds.filter((build) => {
    // Search filter (title, description, creator name)
    if (filters.search) {
      const search = filters.search.toLowerCase()
      const nameMatch = build.name.toLowerCase().includes(search)
      const descMatch = build.description?.toLowerCase().includes(search)
      const creatorMatch =
        'author' in build &&
        build.author.username.toLowerCase().includes(search)
      if (!nameMatch && !descMatch && !creatorMatch) return false
    }

    // Price range filter
    if (filters.priceRange) {
      const [min, max] = filters.priceRange
      if (build.estimatedPrice < min || build.estimatedPrice > max) {
        return false
      }
    }

    // Tier filter
    if (filters.tier) {
      const tier = calculateBuildTier(build.estimatedPrice)
      if (tier !== filters.tier) return false
    }

    return true
  })
}
