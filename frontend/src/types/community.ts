/**
 * Community Types
 * Defines all community and marketplace-related types
 */

import { SavedBuild } from './builds'
import { User } from './auth'

export interface BuildRating {
  buildId: string
  userId: string
  rating: number // 1-5 stars
  createdAt: string
}

export interface BuildReview extends SavedBuild {
  author: User
  averageRating: number
  reviewCount: number
  ratings: BuildRating[]
  tags: string[]
}

export interface CommunityBuild {
  id: string
  name: string
  description?: string
  author: User
  components: Array<{
    componentId: string
    name: string
    category: string
  }>
  estimatedPrice: number
  averageRating: number
  reviewCount: number
  views: number
  tags: string[]
  likes: number
  createdAt: string
  updatedAt: string
}

export interface CommunityBuildFilter {
  search?: string
  tag?: string
  minPrice?: number
  maxPrice?: number
  minRating?: number
  sortBy?: 'recent' | 'popular' | 'trending' | 'mostRated'
  limit?: number
  offset?: number
}

export interface CommunityStats {
  totalBuilds: number
  totalUsers: number
  averageRating: number
  totalViews: number
}

// AI Assistant & Chat Types
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  suggestions?: string[]
}

export interface ChatResponse {
  message: string
  suggestions?: Array<{
    id: string
    name: string
    category: string
    price: number
  }>
  recommendedBuild?: SavedBuild
}
