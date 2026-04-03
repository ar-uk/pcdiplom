/**
 * Button Component
 * Reusable button with multiple variants and sizes
 */

import { ReactNode } from 'react'

interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'danger' | 'success' | 'warning'
  size?: 'small' | 'medium' | 'large'
  disabled?: boolean
  loading?: boolean
  onClick?: () => void
  children: ReactNode
  className?: string
  type?: 'button' | 'submit' | 'reset'
  fullWidth?: boolean
  outline?: boolean
}

export const Button = ({
  variant = 'primary',
  size = 'medium',
  disabled = false,
  loading = false,
  onClick,
  children,
  className = '',
  type = 'button',
  fullWidth = false,
  outline = false,
}: ButtonProps) => {
  const baseStyles =
    'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200 rounded-md focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

  const variantStyles = {
    primary: outline
      ? 'border-2 border-primary text-primary hover:bg-primary hover:text-white disabled:border-blue-300'
      : 'bg-primary text-white hover:bg-blue-700 active:bg-blue-800 focus-visible:outline-primary disabled:bg-blue-300',
    secondary: outline
      ? 'border-2 border-secondary text-secondary hover:bg-secondary hover:text-white disabled:border-gray-300'
      : 'bg-secondary text-white hover:bg-gray-700 active:bg-gray-800 focus-visible:outline-secondary disabled:bg-gray-300',
    danger: outline
      ? 'border-2 border-danger text-danger hover:bg-danger hover:text-white disabled:border-red-300'
      : 'bg-danger text-white hover:bg-red-700 active:bg-red-800 focus-visible:outline-danger disabled:bg-red-300',
    success: outline
      ? 'border-2 border-success text-success hover:bg-success hover:text-white disabled:border-green-300'
      : 'bg-success text-white hover:bg-green-600 active:bg-green-700 focus-visible:outline-success disabled:bg-green-300',
    warning: outline
      ? 'border-2 border-warning text-warning hover:bg-warning hover:text-white disabled:border-yellow-300'
      : 'bg-warning text-gray-900 hover:bg-yellow-500 active:bg-yellow-600 focus-visible:outline-warning disabled:bg-yellow-200',
  }

  const sizeStyles = {
    small: 'text-sm px-3 py-1.5 min-h-8',
    medium: 'text-base px-4 py-2 min-h-10',
    large: 'text-lg px-6 py-3 min-h-12',
  }

  const windowFullWidth = fullWidth ? 'w-full' : ''

  return (
    <button
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${windowFullWidth} ${className}`}
      aria-busy={loading}
    >
      {loading && (
        <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
      )}
      {children}
    </button>
  )
}
