/**
 * Loading Component
 * Reusable loading spinner with optional text
 */

interface LoadingProps {
  size?: 'small' | 'medium' | 'large'
  text?: string
  fullScreen?: boolean
}

export const Loading = ({ size = 'medium', text, fullScreen = false }: LoadingProps) => {
  const sizeStyles = {
    small: 'w-6 h-6 border-2',
    medium: 'w-10 h-10 border-3',
    large: 'w-16 h-16 border-4',
  }

  const containerClass = fullScreen
    ? 'fixed inset-0 z-50 flex items-center justify-center bg-white dark:bg-secondary'
    : 'flex flex-col items-center justify-center'

  return (
    <div className={`${containerClass} gap-3`} role="status" aria-label="Loading">
      <div
        className={`${sizeStyles[size]} border-primary border-opacity-30 border-t-primary rounded-full animate-spin`}
        aria-busy="true"
      />
      {text && (
        <p className="text-gray-600 dark:text-gray-400 text-sm font-medium">
          {text}
        </p>
      )}
      <span className="sr-only">Loading...</span>
    </div>
  )
}
