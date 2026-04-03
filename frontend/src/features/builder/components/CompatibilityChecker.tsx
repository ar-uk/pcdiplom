/**
 * CompatibilityChecker Component
 * Display warnings and errors for build compatibility
 */

import React, { useState } from 'react'
import { CompatibilityIssue } from '@/types'

interface CompatibilityCheckerProps {
  errors: CompatibilityIssue[]
  warnings: CompatibilityIssue[]
  isLoading?: boolean
}

export const CompatibilityChecker: React.FC<CompatibilityCheckerProps> = ({
  errors,
  warnings,
  isLoading = false,
}) => {
  const [isExpanded, setIsExpanded] = useState(true)

  if (isLoading) {
    return (
      <div className="p-4 bg-gray-50 rounded border border-gray-200">
        <p className="text-sm text-gray-600">Checking compatibility...</p>
      </div>
    )
  }

  if (errors.length === 0 && warnings.length === 0) {
    return (
      <div className="p-4 bg-green-50 rounded border border-green-200">
        <p className="text-sm font-medium text-green-800">✓ Build is compatible</p>
      </div>
    )
  }

  return (
    <div className="rounded border border-gray-200 overflow-hidden">
      {/* Header */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={`w-full p-4 flex items-center justify-between font-medium text-sm transition-colors ${
          errors.length > 0
            ? 'bg-red-50 hover:bg-red-100 text-red-900'
            : 'bg-yellow-50 hover:bg-yellow-100 text-yellow-900'
        }`}
      >
        <div className="flex items-center gap-2">
          <span>{errors.length > 0 ? '✗ Build Issues' : '⚠ Compatibility Warnings'}</span>
          <span className="text-xs bg-opacity-80 px-2 py-1 rounded">
            {errors.length + warnings.length} issue{errors.length + warnings.length !== 1 ? 's' : ''}
          </span>
        </div>
        <span className="text-xs">{isExpanded ? '−' : '+'}</span>
      </button>

      {/* Content */}
      {isExpanded && (
        <div className="p-4 space-y-3 max-h-48 overflow-y-auto">
          {/* Errors */}
          {errors.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-xs font-semibold text-red-900 uppercase">Errors</h3>
              {errors.map((error, idx) => (
                <div
                  key={idx}
                  className="p-3 bg-red-100 border-l-4 border-red-500 rounded text-sm text-red-900"
                >
                  <p className="font-medium">{error.message}</p>
                  <p className="text-xs mt-1 opacity-80">
                    Affects: {error.components.join(', ')}
                  </p>
                </div>
              ))}
            </div>
          )}

          {/* Warnings */}
          {warnings.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-xs font-semibold text-yellow-900 uppercase">Warnings</h3>
              {warnings.map((warning, idx) => (
                <div
                  key={idx}
                  className="p-3 bg-yellow-100 border-l-4 border-yellow-500 rounded text-sm text-yellow-900"
                >
                  <p className="font-medium">{warning.message}</p>
                  <p className="text-xs mt-1 opacity-80">
                    Affects: {warning.components.join(', ')}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default CompatibilityChecker
