/**
 * ComponentSelector Component
 * Category selector for the builder
 */

import React from 'react'
import { ComponentCategory } from '@/types'

const CATEGORIES = [
  { value: ComponentCategory.CPU, label: 'CPU' },
  { value: ComponentCategory.GPU, label: 'GPU' },
  { value: ComponentCategory.RAM, label: 'RAM' },
  { value: ComponentCategory.MOTHERBOARD, label: 'Motherboard' },
  { value: ComponentCategory.PSU, label: 'PSU' },
  { value: ComponentCategory.CASE, label: 'Case' },
  { value: ComponentCategory.COOLER, label: 'Cooler' },
  { value: ComponentCategory.SSD, label: 'SSD' },
]

interface ComponentSelectorProps {
  selectedCategory: ComponentCategory | null
  onCategoryChange: (category: ComponentCategory) => void
}

export const ComponentSelector: React.FC<ComponentSelectorProps> = ({
  selectedCategory,
  onCategoryChange,
}) => {
  return (
    <div className="p-4 bg-white border-b border-gray-200">
      <h2 className="text-sm font-semibold text-gray-700 mb-3">Select Category</h2>
      <div className="grid grid-cols-2 gap-2">
        {CATEGORIES.map((category) => (
          <button
            key={category.value}
            onClick={() => onCategoryChange(category.value)}
            className={`p-3 rounded text-sm font-medium transition-all ${
              selectedCategory === category.value
                ? 'bg-blue-500 text-white shadow-md'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {category.label}
          </button>
        ))}
      </div>
    </div>
  )
}

export default ComponentSelector
