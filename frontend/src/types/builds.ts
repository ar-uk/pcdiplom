/**
 * Build Types
 * Defines all PC build-related types and interfaces
 */

import { Component, ComponentCategory } from './components'

export enum BuildStatus {
  DRAFT = 'DRAFT',
  SAVED = 'SAVED',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

export interface BuildComponent {
  componentId: string
  component?: Component
  quantity: number
  addedAt: string
}

export interface CompatibilityIssue {
  type: 'error' | 'warning'
  message: string
  components: ComponentCategory[]
}

export interface BuildCompatibility {
  isCompatible: boolean
  issues: CompatibilityIssue[]
  warnings: CompatibilityIssue[]
}

export interface Build {
  id: string
  name: string
  description?: string
  components: BuildComponent[]
  estimatedPrice: number
  status: BuildStatus
  compatibility: BuildCompatibility
  createdAt: string
  updatedAt: string
}

export interface SavedBuild extends Build {
  userId: string
  isPublic: boolean
  views: number
}

export interface BuildCreateRequest {
  name: string
  description?: string
  isPublic?: boolean
}

export interface BuildUpdateRequest {
  name?: string
  description?: string
  isPublic?: boolean
  status?: BuildStatus
}

export interface AddComponentRequest {
  componentId: string
  quantity?: number
}

export interface RemoveComponentRequest {
  componentId: string
}

export interface BuildFilter {
  category?: ComponentCategory
  maxPrice?: number
  search?: string
  inStockOnly?: boolean
}
