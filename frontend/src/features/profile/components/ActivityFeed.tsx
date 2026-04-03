/**
 * ActivityFeed Component
 * Display user activity timeline
 */

interface ActivityItem {
  id: string
  type: 'build_created' | 'build_upvoted' | 'comment_added'
  title: string
  description: string
  timestamp: string
}

interface ActivityFeedProps {
  activities?: ActivityItem[]
  isLoading?: boolean
}

export const ActivityFeed = ({
  activities = [],
  isLoading = false,
}: ActivityFeedProps) => {
  if (isLoading) {
    return <div className="text-center py-8 text-gray-500">Loading activity...</div>
  }

  if (activities.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p className="text-lg">No activity yet</p>
        <p className="text-sm">Start creating and sharing builds!</p>
      </div>
    )
  }

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'build_created':
        return '🏗️'
      case 'build_upvoted':
        return '👍'
      case 'comment_added':
        return '💬'
      default:
        return '📌'
    }
  }

  const getActivityColor = (type: string) => {
    switch (type) {
      case 'build_created':
        return 'border-l-blue-500 bg-blue-50'
      case 'build_upvoted':
        return 'border-l-green-500 bg-green-50'
      case 'comment_added':
        return 'border-l-purple-500 bg-purple-50'
      default:
        return 'border-l-gray-500 bg-gray-50'
    }
  }

  return (
    <div className="space-y-4">
      {activities.map((activity) => (
        <div
          key={activity.id}
          className={`border-l-4 border-gray-200 ${getActivityColor(activity.type)} p-4 rounded`}
        >
          <div className="flex items-start gap-3">
            <span className="text-2xl">{getActivityIcon(activity.type)}</span>
            <div className="flex-1">
              <h3 className="font-semibold text-gray-900">{activity.title}</h3>
              <p className="text-sm text-gray-600">{activity.description}</p>
              <p className="text-xs text-gray-500 mt-2">
                {new Date(activity.timestamp).toLocaleDateString()} at{' '}
                {new Date(activity.timestamp).toLocaleTimeString()}
              </p>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
