/**
 * Card Component
 * Reusable container card component
 */

import { ReactNode } from 'react'

interface CardProps {
  children: ReactNode
  className?: string
  shadow?: 'none' | 'light' | 'medium' | 'heavy'
  padding?: 'none' | 'small' | 'medium' | 'large'
  hoverable?: boolean
}

export const Card = ({
  children,
  className = '',
  shadow = 'light',
  padding = 'medium',
  hoverable = false,
}: CardProps) => {
  const shadowStyles = {
    none: '',
    light: 'shadow-sm',
    medium: 'shadow-md',
    heavy: 'shadow-lg',
  }

  const paddingStyles = {
    none: '',
    small: 'p-2',
    medium: 'p-4',
    large: 'p-6',
  }

  const hoverClass = hoverable ? 'hover:shadow-lg hover:scale-105 transition-all duration-200 cursor-pointer' : ''

  return (
    <div
      className={`bg-white rounded-lg border border-gray-200 dark:bg-secondary dark:border-gray-700 ${shadowStyles[shadow]} ${paddingStyles[padding]} ${hoverClass} ${className}`}
    >
      {children}
    </div>
  )
}
