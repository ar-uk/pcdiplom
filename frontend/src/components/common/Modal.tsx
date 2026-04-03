/**
 * Modal Component
 * Reusable modal dialog with overlay and animations
 */

import { ReactNode } from 'react'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  className?: string
  closeButton?: boolean
  size?: 'small' | 'medium' | 'large'
}

export const Modal = ({
  isOpen,
  onClose,
  title,
  children,
  className = '',
  closeButton = true,
  size = 'medium',
}: ModalProps) => {
  if (!isOpen) return null

  const sizeClasses = {
    small: 'max-w-sm',
    medium: 'max-w-lg',
    large: 'max-w-2xl',
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fadeIn"
      role="presentation"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black bg-opacity-50 animate-fadeIn"
        onClick={onClose}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Escape') onClose()
        }}
        aria-label="close modal"
      />

      {/* Modal Content */}
      <div
        className={`relative bg-white dark:bg-secondary rounded-lg shadow-xl w-full max-h-[90vh] overflow-auto animate-scaleIn ${sizeClasses[size]} ${className}`}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'modal-title' : undefined}
      >
        {/* Header */}
        {(title || closeButton) && (
          <div className="sticky top-0 bg-white dark:bg-secondary border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex items-center justify-between">
            {title && (
              <h2
                id="modal-title"
                className="text-xl font-semibold text-primary"
              >
                {title}
              </h2>
            )}
            {closeButton && (
              <button
                onClick={onClose}
                className="ml-auto text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 text-2xl leading-none hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg p-1 w-8 h-8 flex items-center justify-center transition-colors"
                aria-label="close modal"
              >
                ×
              </button>
            )}
          </div>
        )}

        {/* Body */}
        <div className="px-6 py-4">
          {children}
        </div>
      </div>
    </div>
  )
}
