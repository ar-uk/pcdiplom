/**
 * BuildFilters Component
 * Filter panel for community builds (left sidebar)
 */

import React, { useState } from 'react'

export interface BuildFiltersProps {
  onSearchChange?: (search: string) => void
  onPriceRangeChange?: (min: number, max: number) => void
  onCategoryChange?: (category: string | undefined) => void
  onTierChange?: (tier: 'budget' | 'mid-range' | 'high-end' | undefined) => void
  onSortChange?: (sortBy: 'newest' | 'trending' | 'mostUpvoted' | 'cheapest' | 'mostExpensive') => void
  onResetFilters?: () => void
  isCollapsed?: boolean
  onToggleCollapse?: (collapsed: boolean) => void
}

const PRICE_RANGES = [
  { label: 'Under $800', min: 0, max: 800 },
  { label: '$800 - $1,800', min: 800, max: 1800 },
  { label: 'Over $1,800', min: 1800, max: 10000 },
]

const CATEGORIES = ['CPU', 'GPU', 'RAM', 'Motherboard', 'Storage', 'Power Supply']

const TIERS = [
  { label: 'Budget', value: 'budget' },
  { label: 'Mid-Range', value: 'mid-range' },
  { label: 'High-End', value: 'high-end' },
]

const SORT_OPTIONS = [
  { label: 'Newest', value: 'newest' },
  { label: 'Trending', value: 'trending' },
  { label: 'Most Upvoted', value: 'mostUpvoted' },
  { label: 'Cheapest', value: 'cheapest' },
  { label: 'Most Expensive', value: 'mostExpensive' },
]

export const BuildFilters: React.FC<BuildFiltersProps> = ({
  onSearchChange,
  onPriceRangeChange,
  // onCategoryChange,
  onTierChange,
  onSortChange,
  onResetFilters,
  isCollapsed = false,
  onToggleCollapse,
}) => {
  const [search, setSearch] = useState('')
  const [selectedPriceRange, setSelectedPriceRange] = useState<number[] | null>(null)
  const [selectedTier, setSelectedTier] = useState<string | null>(null)
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [selectedSort, setSelectedSort] = useState('newest')

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setSearch(value)
    onSearchChange?.(value)
  }

  const handlePriceRangeChange = (range: { min: number; max: number }) => {
    setSelectedPriceRange([range.min, range.max])
    onPriceRangeChange?.(range.min, range.max)
  }

  const handleTierChange = (tier: string) => {
    const newTier = selectedTier === tier ? null : tier
    setSelectedTier(newTier)
    onTierChange?.(newTier as any)
  }

  const handleCategoryChange = (category: string) => {
    const newCategories = selectedCategories.includes(category)
      ? selectedCategories.filter((c) => c !== category)
      : [...selectedCategories, category]
    setSelectedCategories(newCategories)
  }

  const handleSortChange = (value: string) => {
    setSelectedSort(value)
    onSortChange?.(value as any)
  }

  const handleReset = () => {
    setSearch('')
    setSelectedPriceRange(null)
    setSelectedTier(null)
    setSelectedCategories([])
    setSelectedSort('newest')
    onResetFilters?.()
  }

  const filterContent = (
    <div className="space-y-6">
      {/* Search */}
      <div>
        <label className="block text-sm font-semibold text-gray-900 mb-2">
          Search
        </label>
        <input
          type="text"
          placeholder="Build name, creator..."
          value={search}
          onChange={handleSearchChange}
          className="w-full rounded border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
        />
      </div>

      {/* Sort Options */}
      <div>
        <label className="block text-sm font-semibold text-gray-900 mb-2">
          Sort By
        </label>
        <div className="space-y-2">
          {SORT_OPTIONS.map((option) => (
            <label key={option.value} className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="sort"
                value={option.value}
                checked={selectedSort === option.value}
                onChange={(e) => handleSortChange(e.target.value)}
                className="cursor-pointer"
              />
              <span className="text-sm text-gray-700">{option.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div>
        <label className="block text-sm font-semibold text-gray-900 mb-2">
          Price Range
        </label>
        <div className="space-y-2">
          {PRICE_RANGES.map((range) => (
            <label key={`${range.min}-${range.max}`} className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={
                  selectedPriceRange?.[0] === range.min &&
                  selectedPriceRange?.[1] === range.max
                }
                onChange={() => handlePriceRangeChange(range)}
                className="cursor-pointer"
              />
              <span className="text-sm text-gray-700">{range.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Performance Tier */}
      <div>
        <label className="block text-sm font-semibold text-gray-900 mb-2">
          Performance Tier
        </label>
        <div className="space-y-2">
          {TIERS.map((tier) => (
            <label key={tier.value} className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={selectedTier === tier.value}
                onChange={() => handleTierChange(tier.value)}
                className="cursor-pointer"
              />
              <span className="text-sm text-gray-700">{tier.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Component Categories */}
      <div>
        <label className="block text-sm font-semibold text-gray-900 mb-2">
          Component Types
        </label>
        <div className="space-y-2">
          {CATEGORIES.map((category) => (
            <label key={category} className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={selectedCategories.includes(category)}
                onChange={() => handleCategoryChange(category)}
                className="cursor-pointer"
              />
              <span className="text-sm text-gray-700">{category}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Reset Button */}
      <button
        onClick={handleReset}
        className="w-full rounded bg-gray-200 py-2 text-sm font-medium text-gray-900 transition-colors hover:bg-gray-300"
      >
        Reset Filters
      </button>
    </div>
  )

  return (
    <>
      {/* Mobile Toggle Button */}
      <button
        onClick={() => onToggleCollapse?.(!isCollapsed)}
        className="mb-4 flex items-center gap-2 rounded bg-gray-100 px-4 py-2 text-gray-900 lg:hidden"
      >
        <span>☰</span>
        <span>Filters</span>
      </button>

      {/* Filter Panel */}
      <div
        className={`rounded-lg border border-gray-200 bg-white p-4 lg:block ${
          isCollapsed ? 'hidden' : ''
        }`}
      >
        {filterContent}
      </div>
    </>
  )
}

export default BuildFilters
