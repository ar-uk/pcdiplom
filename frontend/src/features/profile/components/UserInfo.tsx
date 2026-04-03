/**
 * UserInfo Component
 * Display user profile information
 */

import { User } from '@/types'

interface UserInfoProps {
  user?: User
  isOwn?: boolean
  onEditClick?: () => void
  isEditing?: boolean
  editData?: Partial<User>
  onEditChange?: (field: string, value: string) => void
  onSave?: () => void
}

export const UserInfo = ({
  user,
  isOwn = false,
  onEditClick,
  isEditing = false,
  editData,
  onEditChange,
  onSave,
}: UserInfoProps) => {
  if (!user) {
    return <div className="text-gray-500">Loading user information...</div>
  }

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6">
      <div className="flex items-start justify-between mb-6">
        <div className="flex items-center gap-4">
          {/* Avatar Placeholder */}
          <div className="w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-2xl font-bold">
            {user.username?.charAt(0).toUpperCase()}
          </div>

          {/* User Info */}
          <div>
            {isEditing ? (
              <div className="space-y-2">
                <input
                  type="text"
                  value={editData?.username || ''}
                  onChange={(e) =>
                    onEditChange?.('username', e.target.value)
                  }
                  className="border border-gray-300 rounded px-3 py-1 w-full"
                  placeholder="Username"
                />
                <input
                  type="email"
                  value={editData?.email || ''}
                  onChange={(e) => onEditChange?.('email', e.target.value)}
                  className="border border-gray-300 rounded px-3 py-1 w-full"
                  placeholder="Email"
                />
              </div>
            ) : (
              <>
                <h2 className="text-2xl font-bold text-gray-900">
                  {user.username}
                </h2>
                <p className="text-gray-600">{user.email}</p>
              </>
            )}
          </div>
        </div>

        {isOwn && !isEditing && (
          <button
            onClick={onEditClick}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Edit Profile
          </button>
        )}

        {isOwn && isEditing && (
          <button
            onClick={onSave}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
          >
            Save Changes
          </button>
        )}
      </div>

      {/* User Stats */}
      <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
        <div>
          <p className="text-sm text-gray-600">Role</p>
          <p className="text-lg font-semibold text-gray-900">
            <span className="inline-block px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-xs font-medium">
              {user.role}
            </span>
          </p>
        </div>
        <div>
          <p className="text-sm text-gray-600">Member Since</p>
          <p className="text-lg font-semibold text-gray-900">
            {new Date(user.createdAt || Date.now()).toLocaleDateString()}
          </p>
        </div>
        <div>
          <p className="text-sm text-gray-600">Status</p>
          <p className="text-lg font-semibold text-green-600">Active</p>
        </div>
      </div>
    </div>
  )
}
