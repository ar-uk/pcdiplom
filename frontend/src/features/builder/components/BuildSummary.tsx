/**
 * BuildSummary Component
 * Right panel showing selected components and build overview
 */

import React from 'react'
import { Component, ComponentCategory } from '@/types'
import { CompatibilityIssue } from '@/types/builds'
import { formatCurrency, formatWattage } from '@/utils/validation'
import CompatibilityChecker from './CompatibilityChecker'

interface BuildSummaryProps {
  components: Map<ComponentCategory, Component>
  totalCost: number
  totalPower: number
  compatibility: CompatibilityIssue[]
  onRemoveComponent: (category: ComponentCategory) => void
  onClearBuild: () => void
  onSaveBuild: () => void
  isLoading?: boolean
}

const CATEGORY_ORDER = [
  ComponentCategory.CPU,
  ComponentCategory.MOTHERBOARD,
  ComponentCategory.RAM,
  ComponentCategory.GPU,
  ComponentCategory.SSD,
  ComponentCategory.PSU,
  ComponentCategory.CASE,
  ComponentCategory.COOLER,
]

const getCategoryLabel = (category: ComponentCategory): string => {
  const labels: Record<ComponentCategory, string> = {
    [ComponentCategory.CPU]: 'Processor',
    [ComponentCategory.GPU]: 'Graphics Card',
    [ComponentCategory.RAM]: 'Memory',
    [ComponentCategory.SSD]: 'SSD Storage',
    [ComponentCategory.HDD]: 'HDD Storage',
    [ComponentCategory.PSU]: 'Power Supply',
    [ComponentCategory.MOTHERBOARD]: 'Motherboard',
    [ComponentCategory.CASE]: 'Case',
    [ComponentCategory.COOLER]: 'CPU Cooler',
    [ComponentCategory.MONITOR]: 'Monitor',
  }
  return labels[category] || category
}

export const BuildSummary: React.FC<BuildSummaryProps> = ({
  components,
  totalCost,
  totalPower,
  compatibility,
  onRemoveComponent,
  onClearBuild,
  onSaveBuild,
  isLoading = false,
}) => {
  const errors = compatibility.filter((i) => i.type === 'error')
  const warnings = compatibility.filter((i) => i.type === 'warning')

  const orderedComponents = Array.from(components.entries())
    .sort(
      ([catA], [catB]) =>
        CATEGORY_ORDER.indexOf(catA) - CATEGORY_ORDER.indexOf(catB)
    )

  return (
    <div className="flex flex-col h-full bg-gray-50">
      {/* Header */}
      <div className="p-4 border-b border-gray-200 bg-white">
        <h2 className="text-lg font-bold text-gray-900 mb-2">Build Summary</h2>
        <p className="text-xs text-gray-600">
          {components.size} component{components.size !== 1 ? 's' : ''} selected
        </p>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Components List */}
        {orderedComponents.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <p className="text-sm">No components selected yet</p>
            <p className="text-xs mt-2">Start by selecting a category on the left</p>
          </div>
        ) : (
          <div className="space-y-2">
            {orderedComponents.map(([category, component]) => (
              <div
                key={category}
                className="p-3 bg-white rounded border border-gray-200 flex justify-between items-start hover:shadow-sm transition-shadow"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-semibold text-gray-600">
                    {getCategoryLabel(category)}
                  </p>
                  <p className="text-sm font-medium text-gray-900 truncate mt-1">
                    {component.name}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatCurrency(component.price)}
                  </p>
                </div>
                <button
                  onClick={() => onRemoveComponent(category)}
                  className="ml-2 px-2 py-1 text-red-600 hover:bg-red-50 rounded text-xs font-medium transition-colors flex-shrink-0"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Compatibility Checker */}
        {components.size > 0 && (
          <div className="pt-2">
            <CompatibilityChecker errors={errors} warnings={warnings} />
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="border-t border-gray-200 bg-white p-4 space-y-3">
        {/* Stats */}
        {components.size > 0 && (
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">Total Power:</span>
              <span className="font-semibold text-gray-900">{formatWattage(totalPower)}</span>
            </div>
            <div className="flex justify-between text-lg font-bold border-t border-gray-200 pt-2">
              <span className="text-gray-900">Total Cost:</span>
              <span className="text-blue-600">{formatCurrency(totalCost)}</span>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="space-y-2">
          <button
            onClick={onSaveBuild}
            disabled={components.size === 0 || isLoading || errors.length > 0}
            className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium rounded transition-colors text-sm"
          >
            {isLoading ? 'Saving...' : 'Save Build'}
          </button>

          <button
            onClick={onClearBuild}
            disabled={components.size === 0}
            className="w-full px-4 py-2 bg-gray-200 hover:bg-gray-300 disabled:opacity-50 disabled:cursor-not-allowed text-gray-900 font-medium rounded transition-colors text-sm"
          >
            Clear All
          </button>
        </div>
      </div>
    </div>
  )
}

export default BuildSummary
