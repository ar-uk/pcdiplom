/**
 * ProfilePage
 * Main user profile view with tabs for builds, activity, and settings
 */

import { useState } from 'react'
import { UserInfo, SavedBuildsList, ActivityFeed, ProfileStats } from '../components'
import { useProfile } from '../hooks'
import { Tabs, Loading, Button } from '@/components/common'
import { useNavigate } from 'react-router-dom'

export const ProfilePage = () => {
  const navigate = useNavigate()
  const { user, savedBuilds, stats, isLoading, error, updateProfile, changePassword } = useProfile()
  const [isEditing, setIsEditing] = useState(false)
  const [editData, setEditData] = useState<Record<string, string>>({})
  const [showPasswordForm, setShowPasswordForm] = useState(false)
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loading />
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-red-100">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-red-800">Error</h2>
          <p className="text-red-700 mt-2">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            Go Home
          </button>
        </div>
      </div>
    )
  }

  const handleEdit = () => {
    setEditData({
      username: user?.username || '',
      email: user?.email || '',
    })
    setIsEditing(true)
  }

  const handleEditorChange = (field: string, value: string) => {
    setEditData({ ...editData, [field]: value })
  }

  const handleSave = async () => {
    try {
      await updateProfile(editData)
      setIsEditing(false)
    } catch (err) {
      console.error('Failed to update profile:', err)
    }
  }

  const handleChangePassword = async () => {
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      alert('Passwords do not match')
      return
    }
    try {
      await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
      setShowPasswordForm(false)
      alert('Password changed successfully')
    } catch (err) {
      console.error('Failed to change password:', err)
    }
  }

  // Mock activity data
  const activities = [
    {
      id: '1',
      type: 'build_created' as const,
      title: 'Created "Gaming PC Pro"',
      description: 'Published a high-end gaming build with RTX 4090',
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: '2',
      type: 'build_upvoted' as const,
      title: 'Got upvoted on "Budget Build"',
      description: 'Your build received 15 upvotes from the community',
      timestamp: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(),
    },
  ]

  const tabs = [
    {
      id: 'builds',
      label: 'Saved Builds',
      content: (
        <SavedBuildsList
          builds={savedBuilds}
          onView={(id) => navigate(`/builder/${id}`)}
          onEdit={(id) => navigate(`/builder/${id}`)}
          onShare={(id) => {
            const url = `${window.location.origin}/community/${id}`
            navigator.clipboard.writeText(url)
            alert('Share link copied!')
          }}
          onDelete={(_id) => {
            if (confirm('Are you sure?')) {
              // TODO: implement delete
            }
          }}
        />
      ),
    },
    {
      id: 'activity',
      label: 'Activity',
      content: <ActivityFeed activities={activities} />,
    },
    {
      id: 'settings',
      label: 'Settings',
      content: (
        <div className="space-y-6">
          {showPasswordForm ? (
            <div className="bg-gray-50 p-6 rounded-lg">
              <h3 className="text-lg font-semibold mb-4">Change Password</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Current Password
                  </label>
                  <input
                    type="password"
                    value={passwordForm.oldPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, oldPassword: e.target.value })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    New Password
                  </label>
                  <input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirm Password
                  </label>
                  <input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) =>
                      setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg"
                  />
                </div>
                <div className="flex gap-2">
                  <Button variant="primary" onClick={handleChangePassword}>
                    Update Password
                  </Button>
                  <Button variant="secondary" onClick={() => setShowPasswordForm(false)}>
                    Cancel
                  </Button>
                </div>
              </div>
            </div>
          ) : (
            <Button variant="secondary" onClick={() => setShowPasswordForm(true)}>
              Change Password
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* User Info Section */}
        <UserInfo
          user={user}
          isOwn={true}
          onEditClick={handleEdit}
          isEditing={isEditing}
          editData={editData}
          onEditChange={handleEditorChange}
          onSave={handleSave}
        />

        {/* Stats Section */}
        <ProfileStats
          totalBuilds={stats?.totalBuilds || savedBuilds.length}
          totalUpvotes={stats?.totalUpvotes || 0}
          favoriteComponent={stats?.favoriteComponents?.[0]}
          totalCostInvested={stats?.totalCostInvested || 0}
        />

        {/* Tabs Section */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <Tabs tabs={tabs} defaultTab="builds" />
        </div>
      </div>
    </div>
  )
}
