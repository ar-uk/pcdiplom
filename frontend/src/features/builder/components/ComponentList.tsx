/**
 * ComponentList Component
 * Paginated list of components with search and sorting
 */

import React, { useState, useMemo } from 'react'
import { Component, ComponentCategory } from '@/types'
import { useComponentSearch } from '../hooks/useComponentSearch'
import ComponentCard from './ComponentCard'

interface ComponentListProps {
  category: ComponentCategory | null
  selectedComponent: Component | undefined
  onSelectComponent: (component: Component) => void
}

export const ComponentList: React.FC<ComponentListProps> = ({
  category,
  selectedComponent,
  onSelectComponent,
}) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState<'price' | 'name'>('price')
  const [maxPrice, setMaxPrice] = useState<number | undefined>()

  const { results, isLoading, totalResults, currentPage, totalPages, goToPage } =
    useComponentSearch({
      category: category || undefined,
      searchTerm,
      maxPrice,
      pageSize: 10,
    })

  const sortedResults = useMemo(() => {
    const sorted = [...results]
    if (sortBy === 'price') {
      sorted.sort((a, b) => a.price - b.price)
    } else {
      sorted.sort((a, b) => a.name.localeCompare(b.name))
    }
    return sorted
  }, [results, sortBy])

  if (!category) {
    return (
      <div className="p-8 text-center text-gray-500">
        <p>Select a component category to begin</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* Search and Filter */}
      <div className="p-4 border-b border-gray-200 space-y-3">
        <input
          type="text"
          placeholder="Search components by name or manufacturer..."
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value)
            goToPage(1)
          }}
          className="w-full px-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />

        <div className="flex gap-3 flex-wrap">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'price' | 'name')}
            className="px-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="price">Sort by Price</option>
            <option value="name">Sort by Name</option>
          </select>

          <input
            type="number"
            placeholder="Max price"
            value={maxPrice || ''}
            onChange={(e) => {
              setMaxPrice(e.target.value ? parseInt(e.target.value) : undefined)
              goToPage(1)
            }}
            className="px-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 w-32"
          />

          {(searchTerm || maxPrice) && (
            <button
              onClick={() => {
                setSearchTerm('')
                setMaxPrice(undefined)
                goToPage(1)
              }}
              className="px-3 py-2 bg-gray-200 hover:bg-gray-300 rounded text-sm font-medium transition-colors"
            >
              Clear Filters
            </button>
          )}
        </div>

        <p className="text-xs text-gray-600">
          Found {totalResults} component{totalResults !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Component List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {isLoading ? (
          <div className="p-8 text-center text-gray-500">
            <p>Loading components...</p>
          </div>
        ) : sortedResults.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <p>No components match your search</p>
          </div>
        ) : (
          sortedResults.map((component) => (
            <ComponentCard
              key={component.id}
              component={component}
              isSelected={selectedComponent?.id === component.id}
              onSelect={onSelectComponent}
            />
          ))
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="p-4 border-t border-gray-200 flex items-center justify-between">
          <button
            onClick={() => goToPage(currentPage - 1)}
            disabled={currentPage === 1}
            className="px-3 py-2 bg-gray-200 hover:bg-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>

          <span className="text-xs text-gray-600">
            Page {currentPage} of {totalPages}
          </span>

          <button
            onClick={() => goToPage(currentPage + 1)}
            disabled={currentPage === totalPages}
            className="px-3 py-2 bg-gray-200 hover:bg-gray-300 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}

export default ComponentList
