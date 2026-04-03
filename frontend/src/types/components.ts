/**
 * PC Component Types
 * Defines all hardware component-related types and interfaces
 */

export enum ComponentCategory {
  CPU = 'CPU',
  GPU = 'GPU',
  RAM = 'RAM',
  SSD = 'SSD',
  HDD = 'HDD',
  PSU = 'PSU',
  MOTHERBOARD = 'MOTHERBOARD',
  CASE = 'CASE',
  COOLER = 'COOLER',
  MONITOR = 'MONITOR',
}

export interface ComponentSpec {
  [key: string]: string | number | boolean | string[]
}

export interface Component {
  id: string
  name: string
  category: ComponentCategory
  manufacturer: string
  specs: ComponentSpec
  price: number
  inStock: boolean
  imageUrl?: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface CompatibilityRule {
  id: string
  category1: ComponentCategory
  category2: ComponentCategory
  compatible: boolean
  reason?: string
}

export interface ComponentFilter {
  category: ComponentCategory
  minPrice?: number
  maxPrice?: number
  manufacturer?: string
  search?: string
  inStockOnly?: boolean
}
