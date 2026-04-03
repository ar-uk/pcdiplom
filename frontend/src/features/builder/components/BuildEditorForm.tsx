/**
 * BuildEditorForm Component
 * Form for editing an existing build
 */

import React, { useState } from 'react'
import { SavedBuild } from '@/types'

interface BuildEditorFormProps {
  build: SavedBuild
  isLoading: boolean
  error?: string
  onClose: () => void
  onSubmit: (updates: {
    name: string
    description?: string
    isPublic: boolean
  }) => Promise<void>
  onDelete: () => Promise<void>
}

export const BuildEditorForm: React.FC<BuildEditorFormProps> = ({
  build,
  isLoading,
  error,
  onClose,
  onSubmit,
  onDelete,
}) => {
  const [buildName, setBuildName] = useState(build.name)
  const [description, setDescription] = useState(build.description || '')
  const [isPublic, setIsPublic] = useState(build.isPublic)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [isEmpty, setIsEmpty] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!buildName.trim()) {
      setIsEmpty(true)
      return
    }

    try {
      await onSubmit({
        name: buildName,
        description: description || undefined,
        isPublic,
      })
      onClose()
    } catch (err) {
      // Error is handled by parent
    }
  }

  const handleDelete = async () => {
    try {
      await onDelete()
      onClose()
    } catch (err) {
      // Error is handled by parent
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-md w-full shadow-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 sticky top-0 bg-white">
          <h2 className="text-lg font-bold text-gray-900">Edit Build</h2>
          <p className="text-sm text-gray-600 mt-1">Update your build details</p>
        </div>

        {!showDeleteConfirm ? (
          <>
            {/* Content */}
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {/* Build Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Build Name
                  <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={buildName}
                  onChange={(e) => {
                    setBuildName(e.target.value)
                    setIsEmpty(false)
                  }}
                  placeholder="e.g., Gaming Beast 2024"
                  maxLength={100}
                  className={`w-full px-4 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 ${
                    isEmpty
                      ? 'border-red-500 focus:ring-red-500'
                      : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                <p className="text-xs text-gray-500 mt-1">
                  {buildName.length}/100 characters
                </p>
                {isEmpty && (
                  <p className="text-xs text-red-500 mt-1">Build name is required</p>
                )}
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description (Optional)
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g., High-end gaming build for 4K gaming at 120fps"
                  maxLength={500}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
                <p className="text-xs text-gray-500 mt-1">
                  {description.length}/500 characters
                </p>
              </div>

              {/* Public Checkbox */}
              <label className="flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={isPublic}
                  onChange={(e) => setIsPublic(e.target.checked)}
                  className="w-4 h-4 border border-gray-300 rounded cursor-pointer focus:ring-2 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700">
                  Make this build public (share with community)
                </span>
              </label>

              {/* Error Message */}
              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
                  {error}
                </div>
              )}
            </form>

            {/* Footer */}
            <div className="p-6 border-t border-gray-200 space-y-3">
              {/* Delete Button */}
              <button
                onClick={() => setShowDeleteConfirm(true)}
                disabled={isLoading}
                className="w-full px-4 py-2 bg-red-50 hover:bg-red-100 disabled:opacity-50 disabled:cursor-not-allowed text-red-700 font-medium rounded-lg transition-colors text-sm border border-red-200"
              >
                Delete Build
              </button>

              {/* Action Buttons */}
              <div className="flex items-center gap-3 justify-end">
                <button
                  onClick={onClose}
                  disabled={isLoading}
                  className="px-4 py-2 border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed text-gray-700 font-medium rounded-lg transition-colors text-sm"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={isLoading}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors text-sm"
                >
                  {isLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </div>
          </>
        ) : (
          <>
            {/* Delete Confirmation */}
            <div className="p-6 space-y-4">
              <div className="p-4 bg-red-50 border border-red-200 rounded">
                <p className="text-sm font-medium text-red-900">
                  Are you sure you want to delete this build?
                </p>
                <p className="text-xs text-red-700 mt-2">
                  This action cannot be undone.
                </p>
              </div>

              <div className="flex items-center gap-3 justify-end pt-4 border-t border-gray-200">
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={isLoading}
                  className="px-4 py-2 border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed text-gray-700 font-medium rounded-lg transition-colors text-sm"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDelete}
                  disabled={isLoading}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors text-sm"
                >
                  {isLoading ? 'Deleting...' : 'Delete Build'}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default BuildEditorForm
