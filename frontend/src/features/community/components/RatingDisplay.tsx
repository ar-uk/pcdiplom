/**
 * RatingDisplay Component
 * Displays star rating and count
 */

import React from 'react'

export interface RatingDisplayProps {
  rating: number // 0-5
  count?: number // number of ratings
  size?: 'sm' | 'md' | 'lg'
}

export const RatingDisplay: React.FC<RatingDisplayProps> = ({
  rating,
  count,
  size = 'md',
}) => {
  const sizeClass = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-lg',
  }[size]

  const renderStars = (value: number) => {
    const stars = []
    for (let i = 1; i <= 5; i++) {
      if (i <= Math.floor(value)) {
        stars.push(
          <span key={i} className="text-yellow-400">
            ★
          </span>
        )
      } else if (i - value < 1) {
        stars.push(
          <span key={i} className="text-yellow-300">
            ★
          </span>
        )
      } else {
        stars.push(
          <span key={i} className="text-gray-300">
            ★
          </span>
        )
      }
    }
    return stars
  }

  return (
    <div className={`flex items-center gap-2 ${sizeClass}`}>
      <div className="flex gap-0.5">{renderStars(rating)}</div>
      <div className="font-medium">
        {rating.toFixed(1)}
        {count !== undefined && <span className="text-gray-600"> ({count})</span>}
      </div>
    </div>
  )
}

export default RatingDisplay
