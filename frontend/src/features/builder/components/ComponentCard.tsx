/**
 * ComponentCard Component
 * Single component card displaying component info and selection state
 */

import React from 'react'
import { Component } from '@/types'
import { formatCurrency } from '@/utils/validation'

interface ComponentCardProps {
  component: Component
  isSelected: boolean
  onSelect: (component: Component) => void
  compatibilityIndicator?: 'green' | 'yellow' | 'red'
}

export const ComponentCard: React.FC<ComponentCardProps> = ({
  component,
  isSelected,
  onSelect,
  compatibilityIndicator = 'green',
}) => {
  const getSpecsDisplay = () => {
    switch (component.category.toLowerCase()) {
      case 'cpu':
        return `${component.specs.cores} Cores • ${component.specs.baseClockGHz}GHz`
      case 'gpu':
        return `${component.specs.memory} • ${component.specs.cudaCores || component.specs.streamProcessors} Cores`
      case 'ram':
        return `${component.specs.capacity} • ${component.specs.speed}`
      case 'ssd':
      case 'hdd':
        return `${component.specs.capacity} • ${component.specs.interface}`
      case 'psu':
        return `${component.specs.wattage}W • ${component.specs.efficiency}`
      case 'motherboard':
        return `${component.specs.socket} • ${component.specs.chipset}`
      case 'case':
        return `${component.specs.supportedFormFactors || 'ATX'}`
      case 'cooler':
        return `${component.specs.coolerType || 'Air'} • Supports ${component.specs.socket || 'AM5'}`
      default:
        return Object.entries(component.specs)
          .slice(0, 2)
          .map(([_k, v]) => `${v}`)
          .join(' • ')
    }
  }

  const compatibilityBgColor = {
    green: 'border-l-4 border-l-green-500',
    yellow: 'border-l-4 border-l-yellow-500',
    red: 'border-l-4 border-l-red-500',
  }[compatibilityIndicator]

  return (
    <div
      onClick={() => onSelect(component)}
      className={`p-4 border rounded cursor-pointer transition-all ${compatibilityBgColor} ${
        isSelected
          ? 'bg-blue-50 border-2 border-blue-400'
          : 'bg-white border border-gray-200 hover:border-gray-300 hover:shadow-sm'
      }`}
    >
      <div className="flex justify-between items-start mb-2">
        <div className="flex-1">
          <h3 className="font-semibold text-sm text-gray-900 truncate">
            {component.name}
          </h3>
          <p className="text-xs text-gray-600 mt-1">{component.manufacturer}</p>
        </div>
        {isSelected && (
          <span className="ml-2 text-blue-600 font-bold">✓</span>
        )}
      </div>

      <p className="text-xs text-gray-600 mb-3">{getSpecsDisplay()}</p>

      <div className="flex justify-between items-center">
        <span className="text-sm font-semibold text-gray-900">
          {formatCurrency(component.price)}
        </span>
        {component.inStock ? (
          <span className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded">
            In Stock
          </span>
        ) : (
          <span className="text-xs px-2 py-1 bg-red-100 text-red-700 rounded">
            Out of Stock
          </span>
        )}
      </div>
    </div>
  )
}

export default React.memo(ComponentCard)
