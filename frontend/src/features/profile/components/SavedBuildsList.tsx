/**
 * SavedBuildsList Component
 * Display user's saved builds in a table/list
 */

import { SavedBuild } from '@/types'

interface SavedBuildsListProps {
  builds: SavedBuild[]
  isLoading?: boolean
  onView?: (buildId: string) => void
  onEdit?: (buildId: string) => void
  onDelete?: (buildId: string) => void
  onShare?: (buildId: string) => void
}

export const SavedBuildsList = ({
  builds,
  isLoading = false,
  onView,
  onEdit,
  onDelete,
  onShare,
}: SavedBuildsListProps) => {
  if (isLoading) {
    return <div className="text-center py-8 text-gray-500">Loading builds...</div>
  }

  if (builds.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p className="text-lg">No saved builds yet</p>
        <p className="text-sm">Create your first PC build to get started</p>
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-gray-200">
          <tr>
            <th className="py-3 px-4 font-semibold text-gray-900">Build Name</th>
            <th className="py-3 px-4 font-semibold text-gray-900">Components</th>
            <th className="py-3 px-4 font-semibold text-gray-900">Cost</th>
            <th className="py-3 px-4 font-semibold text-gray-900">Created</th>
            <th className="py-3 px-4 font-semibold text-gray-900">Status</th>
            <th className="py-3 px-4 font-semibold text-gray-900">Actions</th>
          </tr>
        </thead>
        <tbody>
          {builds.map((build) => (
            <tr
              key={build.id}
              className="border-b border-gray-100 hover:bg-gray-50"
            >
              <td className="py-3 px-4 font-medium text-gray-900">
                {build.name}
              </td>
              <td className="py-3 px-4 text-gray-600">
                {build.components?.length || 0} parts
              </td>
              <td className="py-3 px-4 text-gray-900 font-semibold">
                ${(build.estimatedPrice || 0).toLocaleString()}
              </td>
              <td className="py-3 px-4 text-gray-600">
                {new Date(build.createdAt || Date.now()).toLocaleDateString()}
              </td>
              <td className="py-3 px-4">
                <span className="inline-block px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs font-medium">
                  {build.status || 'Saved'}
                </span>
              </td>
              <td className="py-3 px-4">
                <div className="flex gap-2">
                  {onView && (
                    <button
                      onClick={() => onView(build.id)}
                      className="text-blue-600 hover:text-blue-800 text-xs font-medium"
                    >
                      View
                    </button>
                  )}
                  {onEdit && (
                    <button
                      onClick={() => onEdit(build.id)}
                      className="text-yellow-600 hover:text-yellow-800 text-xs font-medium"
                    >
                      Edit
                    </button>
                  )}
                  {onShare && (
                    <button
                      onClick={() => onShare(build.id)}
                      className="text-green-600 hover:text-green-800 text-xs font-medium"
                    >
                      Share
                    </button>
                  )}
                  {onDelete && (
                    <button
                      onClick={() => onDelete(build.id)}
                      className="text-red-600 hover:text-red-800 text-xs font-medium"
                    >
                      Delete
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
