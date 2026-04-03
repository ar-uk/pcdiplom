/**
 * BuildSuggestion Component
 * Display AI-generated build template
 */

import { Build } from '@/types'
import { Card, Button } from '@/components/common'
import { formatPrice } from '@/utils/formatters'

interface BuildSuggestionProps {
  build: Build
  onUse?: () => void
  onCustomize?: () => void
  onSave?: () => void
}

export const BuildSuggestion = ({
  build,
  onUse,
  onCustomize,
  onSave,
}: BuildSuggestionProps) => {
  const totalPrice = build.components?.reduce(
    (sum, comp) => sum + (comp.component?.price || 0),
    0
  ) || 0

  return (
    <Card shadow="medium" padding="medium" className="bg-gradient-to-r from-green-50 to-emerald-50">
      <div className="mb-4">
        <h3 className="text-xl font-bold text-gray-900 mb-2">{build.name}</h3>
        <p className="text-gray-600">{build.description}</p>
      </div>

      {/* Components Preview */}
      <div className="mb-4 space-y-2">
        {build.components?.slice(0, 5).map((comp, idx) => (
          <div key={idx} className="flex justify-between text-sm">
            <span className="text-gray-700">{comp.component?.name}</span>
            <span className="font-medium text-gray-900">{formatPrice(comp.component?.price || 0)}</span>
          </div>
        ))}
        {build.components && build.components.length > 5 && (
          <p className="text-sm text-gray-600">
            +{build.components.length - 5} more components
          </p>
        )}
      </div>

      {/* Total Cost */}
      <div className="border-t border-gray-200 pt-3 mb-4">
        <div className="flex justify-between items-center">
          <span className="font-semibold text-gray-900">Total Cost</span>
          <span className="text-2xl font-bold text-green-600">
            {formatPrice(totalPrice)}
          </span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 flex-wrap">
        <Button variant="primary" onClick={onUse}>
          Use This Build
        </Button>
        <Button variant="secondary" onClick={onCustomize}>
          Customize
        </Button>
        <Button variant="secondary" onClick={onSave}>
          Save
        </Button>
      </div>
    </Card>
  )
}
