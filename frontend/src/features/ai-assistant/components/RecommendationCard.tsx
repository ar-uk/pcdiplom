/**
 * RecommendationCard Component
 * Display AI component suggestion
 */

import { Component } from '@/types'
import { Button } from '@/components/common'

interface RecommendationCardProps {
  component: Component
  onAccept?: () => void
  onReject?: () => void
}

export const RecommendationCard = ({
  component,
  onAccept,
  onReject,
}: RecommendationCardProps) => {
  return (
    <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border border-blue-200 p-4 my-2">
      <div className="mb-3">
        <h4 className="font-semibold text-gray-900 mb-1">{component.name}</h4>
        <p className="text-sm text-gray-600">{component.category}</p>
      </div>

      <div className="space-y-2 mb-3">
        {component.specs && (
          <div className="text-sm text-gray-700">
            <p>
              {Object.entries(component.specs)
                .map(([key, value]) => `${key}: ${value}`)
                .join(', ')}
            </p>
          </div>
        )}
        <p className="text-lg font-bold text-blue-600">
          ${component.price?.toLocaleString()}
        </p>
      </div>

      <div className="flex gap-2">
        <Button
          variant="primary"
          size="small"
          onClick={onAccept}
        >
          Add to Build
        </Button>
        <Button
          variant="secondary"
          size="small"
          onClick={onReject}
        >
          Skip
        </Button>
      </div>
    </div>
  )
}
