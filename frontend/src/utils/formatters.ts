/**
 * Formatting Utilities
 * Helper functions for formatting data for display
 */

import { Component } from '@/types'

/**
 * Format a number as USD currency
 */
export const formatPrice = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

/**
 * Format a date to readable format
 */
export const formatDate = (date: string | Date): string => {
  const d = typeof date === 'string' ? new Date(date) : date
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(d)
}

/**
 * Format a date as relative time (e.g., "2 days ago")
 */
export const formatRelativeTime = (date: string | Date): string => {
  const d = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const seconds = Math.floor((now.getTime() - d.getTime()) / 1000)

  let interval = seconds / 31536000
  if (interval > 1) return Math.floor(interval) + ' years ago'

  interval = seconds / 2592000
  if (interval > 1) return Math.floor(interval) + ' months ago'

  interval = seconds / 86400
  if (interval > 1) return Math.floor(interval) + ' days ago'

  interval = seconds / 3600
  if (interval > 1) return Math.floor(interval) + ' hours ago'

  interval = seconds / 60
  if (interval > 1) return Math.floor(interval) + ' minutes ago'

  return 'just now'
}

/**
 * Format a component's specs as a readable string
 */
export const formatComponentSpecs = (component: Component): string => {
  const specs: string[] = []

  if (component.specs?.cores) {
    specs.push(`${component.specs.cores}-core`)
  }

  if (component.specs?.clockSpeed) {
    specs.push(`${component.specs.clockSpeed}GHz`)
  }

  if (component.specs?.memory) {
    specs.push(`${component.specs.memory}GB`)
  }

  if (component.specs?.memoryType) {
    specs.push(component.specs.memoryType as string)
  }

  if (component.specs?.capacity) {
    specs.push(`${component.specs.capacity}GB`)
  }

  if (component.specs?.power) {
    specs.push(`${component.specs.power}W`)
  }

  return specs.join(', ') || 'No specs available'
}

/**
 * Format a component category name
 */
export const formatComponentCategory = (category: string): string => {
  const categoryMap: Record<string, string> = {
    CPU: 'Processor',
    GPU: 'Graphics Card',
    RAM: 'Memory',
    SSD: 'SSD Storage',
    HDD: 'HDD Storage',
    PSU: 'Power Supply',
    MOTHERBOARD: 'Motherboard',
    CASE: 'Case',
    COOLER: 'CPU Cooler',
    MONITOR: 'Monitor',
  }
  return categoryMap[category] || category
}

/**
 * Format a number with thousands separator
 */
export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('en-US').format(num)
}

/**
 * Format performance tier based on specs
 */
export const formatPerformanceTier = (
  specs?: Record<string, unknown>
): string => {
  if (!specs) return 'Unknown'

  const cores = (specs.cores as number) || 0
  const clockSpeed = (specs.clockSpeed as number) || 0

  if (cores >= 16 || clockSpeed >= 4.5) return 'High-End'
  if (cores >= 12 || clockSpeed >= 4.0) return 'Mid-High'
  if (cores >= 8 || clockSpeed >= 3.5) return 'Mid-Range'
  return 'Entry-Level'
}
