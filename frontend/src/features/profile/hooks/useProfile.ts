/**
 * useProfile Hook
 * Custom hook for managing user profile data
 */

import { useState, useCallback } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { ProfileService } from '@/api/services'
import { User, SavedBuild } from '@/types'

export interface ProfileStats {
  totalBuilds: number
  totalUpvotes: number
  favoriteComponents?: string[]
  totalCostInvested: number
}

export interface UseProfileReturn {
  user?: User
  savedBuilds: SavedBuild[]
  stats?: ProfileStats
  isLoading: boolean
  error?: string
  updateProfile: (data: Partial<User>) => Promise<void>
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>
  getSavedBuilds: () => void
}

export const useProfile = (): UseProfileReturn => {
  const [error, setError] = useState<string>()

  // Fetch user profile
  const {
    data: user,
    isLoading,
    error: profileError,
  } = useQuery({
    queryKey: ['profile'],
    queryFn: () => ProfileService.getProfile(),
  })

  // Fetch saved builds
  const {
    data: savedBuilds = [],
    refetch: refetchBuilds,
  } = useQuery({
    queryKey: ['profile', 'builds'],
    queryFn: () => ProfileService.getSavedBuilds(),
  })

  // Fetch stats
  const {
    data: stats,
  } = useQuery({
    queryKey: ['profile', 'stats'],
    queryFn: () => ProfileService.getUserStats?.(),
  })

  // Update profile mutation
  const updateMutation = useMutation({
    mutationFn: (data: Partial<User>) =>
      ProfileService.updateProfile(data as any),
    onError: (error: any) => {
      setError(error.message || 'Failed to update profile')
    },
  })

  // Change password mutation
  const changePasswordMutation = useMutation({
    mutationFn: (params: { oldPassword: string; newPassword: string }) =>
      ProfileService.changePassword(params.oldPassword, params.newPassword),
    onError: (error: any) => {
      setError(error.message || 'Failed to change password')
    },
  })

  const updateProfile = useCallback(
    async (data: Partial<User>) => {
      setError(undefined)
      await updateMutation.mutateAsync(data)
    },
    [updateMutation]
  )

  const changePassword = useCallback(
    async (oldPassword: string, newPassword: string) => {
      setError(undefined)
      await changePasswordMutation.mutateAsync({ oldPassword, newPassword })
    },
    [changePasswordMutation]
  )

  const getSavedBuilds = useCallback(() => {
    refetchBuilds()
  }, [refetchBuilds])

  return {
    user,
    savedBuilds,
    stats,
    isLoading,
    error: error || profileError?.message,
    updateProfile,
    changePassword,
    getSavedBuilds,
  }
}
