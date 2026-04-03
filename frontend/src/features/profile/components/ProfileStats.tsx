/**
 * ProfileStats Component
 * Display user statistics
 */

interface ProfileStatsProps {
  totalBuilds?: number
  totalUpvotes?: number
  favoriteComponent?: string
  totalCostInvested?: number
}

export const ProfileStats = ({
  totalBuilds = 0,
  totalUpvotes = 0,
  favoriteComponent,
  totalCostInvested = 0,
}: ProfileStatsProps) => {
  const stats = [
    {
      label: 'Total Builds',
      value: totalBuilds.toString(),
      color: 'bg-blue-100 text-blue-800',
    },
    {
      label: 'Upvotes Received',
      value: totalUpvotes.toString(),
      color: 'bg-green-100 text-green-800',
    },
    {
      label: 'Favorite Component',
      value: favoriteComponent || 'N/A',
      color: 'bg-purple-100 text-purple-800',
    },
    {
      label: 'Total Invested',
      value: `$${totalCostInvested.toLocaleString()}`,
      color: 'bg-orange-100 text-orange-800',
    },
  ]

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {stats.map((stat, idx) => (
        <div
          key={idx}
          className={`${stat.color} rounded-lg p-4 text-center`}
        >
          <p className="text-sm font-medium opacity-75">{stat.label}</p>
          <p className="text-2xl font-bold mt-2">{stat.value}</p>
        </div>
      ))}
    </div>
  )
}
