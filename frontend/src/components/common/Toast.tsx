/**
 * Toast Notification Component
 * Auto-dismissing toast notification with smooth animations
 */

import { useEffect } from 'react'
import { UI_DEFAULTS } from '@/utils/constants'

interface ToastProps {
  message: string
  type?: 'success' | 'error' | 'info' | 'warning'
  duration?: number
  onClose: () => void
}

export const Toast = ({
  message,
  type = 'info',
  duration = UI_DEFAULTS.TOAST_DURATION,
  onClose,
}: ToastProps) => {
  useEffect(() => {
    const timer = setTimeout(onClose, duration)
    return () => clearTimeout(timer)
  }, [duration, onClose])

  const typeStyles = {
    success: 'bg-success text-white',
    error: 'bg-danger text-white',
    info: 'bg-primary text-white',
    warning: 'bg-warning text-gray-900',
  }

  const typeIcons = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
    warning: '⚠',
  }

  return (
    <div
      className="fixed bottom-4 right-4 z-600 animate-slideInUp"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      <div
        className={`${typeStyles[type]} px-6 py-4 rounded-lg shadow-lg flex items-center gap-3 max-w-sm`}
      >
        <span className="text-lg font-semibold">{typeIcons[type]}</span>
        <span className="flex-1">{message}</span>
        <button
          onClick={onClose}
          className="text-xl leading-none hover:opacity-75 transition-opacity p-1"
          aria-label="close toast"
        >
          ×
        </button>
      </div>
    </div>
  )
}
