/**
 * BuildDetailsPage Component
 * View, edit, and manage specific builds
 */

import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { SavedBuild } from '@/types'
import { BuildEditorForm } from '../components'
import { useBuild } from '../hooks'
import { formatCurrency } from '@/utils/formatters'

export const BuildDetailsPage: React.FC = () => {
  const { buildId } = useParams<{ buildId: string }>()
  const navigate = useNavigate()
  const [build] = useState<SavedBuild | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const { loadBuild } = useBuild()

  useEffect(() => {
    if (!buildId) {
      setError('Build not found')
      return
    }

    const fetchBuild = async () => {
      try {
        setIsLoading(true)
        await loadBuild(buildId)
        // In a real implementation, we'd fetch the build data here
        // For now, we'll use the store state
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load build')
      } finally {
        setIsLoading(false)
      }
    }

    fetchBuild()
  }, [buildId, loadBuild])

  const handleDuplicate = async () => {
    if (!build) return
    try {
      // Create new build with copied data
      navigate('/builder')
      // would populate the builder with this build's components
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to duplicate build')
    }
  }

  const handleShare = async () => {
    if (!buildId) return
    try {
      const url = `${window.location.origin}/shared-builds/${buildId}`
      await navigator.clipboard.writeText(url)
      // Show success toast
      alert('Build link copied to clipboard!')
    } catch (err) {
      setError('Failed to copy link')
    }
  }

  const handleEdit = async (_updates: {
    name: string
    description?: string
    isPublic: boolean
  }) => {
    if (!buildId) return
    try {
      setIsLoading(true)
      // Update build in backend
      // await updateBuild(buildId, updates)
      setIsEditing(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update build')
    } finally {
      setIsLoading(false)
    }
  }

  const handleDelete = async () => {
    if (!buildId) return
    try {
      setIsLoading(true)
      await deleteBuild(buildId)
      navigate('/builder')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete build')
    } finally {
      setIsLoading(false)
    }
  }

  if (isLoading && !build) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <p className="text-gray-600">Loading build...</p>
        </div>
      </div>
    )
  }

  if (error && !build) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error}</p>
          <button
            onClick={() => navigate('/builder')}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg"
          >
            Back to Builder
          </button>
        </div>
      </div>
    )
  }

  if (!build) return null

  return (
    <div className="min-h-screen bg-gray-100 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{build.name}</h1>
              {build.description && (
                <p className="text-gray-600 mt-2">{build.description}</p>
              )}
              <div className="flex items-center gap-4 mt-3 text-sm text-gray-600">
                <span>Created: {new Date(build.createdAt).toLocaleDateString()}</span>
                <span>Status: {build.status}</span>
                {build.isPublic && (
                  <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-medium">
                    Public
                  </span>
                )}
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-2">
              <button
                onClick={handleDuplicate}
                className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors text-sm"
              >
                Duplicate
              </button>
              <button
                onClick={handleShare}
                className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors text-sm"
              >
                Share
              </button>
              <button
                onClick={() => setIsEditing(true)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors text-sm"
              >
                Edit
              </button>
            </div>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4">
            <div className="p-4 bg-gray-50 rounded border border-gray-200">
              <p className="text-sm text-gray-600 mb-1">Total Cost</p>
              <p className="text-2xl font-bold text-gray-900">
                {formatCurrency(build.estimatedPrice)}
              </p>
            </div>
            <div className="p-4 bg-gray-50 rounded border border-gray-200">
              <p className="text-sm text-gray-600 mb-1">Components</p>
              <p className="text-2xl font-bold text-gray-900">
                {build.components.length}
              </p>
            </div>
            <div className="p-4 bg-gray-50 rounded border border-gray-200">
              <p className="text-sm text-gray-600 mb-1">Views</p>
              <p className="text-2xl font-bold text-gray-900">
                {build.views.toLocaleString()}
              </p>
            </div>
          </div>
        </div>

        {/* Components */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Components</h2>
          <div className="space-y-3">
            {build.components.map((comp, idx) => (
              <div key={idx} className="p-4 border border-gray-200 rounded flex justify-between items-start hover:shadow-sm transition-shadow">
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-600">
                    {comp.component?.category || 'Component'}
                  </p>
                  <p className="text-lg font-medium text-gray-900 mt-1">
                    {comp.component?.name || 'Unknown'}
                  </p>
                  <p className="text-sm text-gray-600 mt-1">
                    Qty: {comp.quantity} • {formatCurrency(comp.component?.price || 0)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Compatibility */}
        {build.compatibility && (
          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <h2 className="text-xl font-bold text-gray-900 mb-4">Compatibility</h2>
            {build.compatibility.isCompatible ? (
              <div className="p-4 bg-green-50 border border-green-200 rounded">
                <p className="text-sm font-medium text-green-800">
                  ✓ Build is compatible
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {build.compatibility.issues.length > 0 && (
                  <div>
                    <h3 className="text-sm font-semibold text-red-900 mb-2">Issues</h3>
                    {build.compatibility.issues.map((issue, idx) => (
                      <div
                        key={idx}
                        className="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700 mb-2"
                      >
                        {issue.message}
                      </div>
                    ))}
                  </div>
                )}
                {build.compatibility.warnings.length > 0 && (
                  <div>
                    <h3 className="text-sm font-semibold text-yellow-900 mb-2">Warnings</h3>
                    {build.compatibility.warnings.map((warning, idx) => (
                      <div
                        key={idx}
                        className="p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-700 mb-2"
                      >
                        {warning.message}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Back Button */}
        <div className="flex gap-3">
          <button
            onClick={() => navigate('/builder')}
            className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors"
          >
            Back to Builder
          </button>
        </div>
      </div>

      {/* Edit Modal */}
      {isEditing && (
        <BuildEditorForm
          build={build}
          isLoading={isLoading}
          error={error || undefined}
          onClose={() => setIsEditing(false)}
          onSubmit={handleEdit}
          onDelete={handleDelete}
        />
      )}
    </div>
  )
}

export default BuildDetailsPage
