/**
 * useBuild Hook
 * Custom hook for managing PC build state
 */

import { useBuildStore } from '../store/buildStore'
import { ComponentCategory } from '@/types'

export function useBuild() {
  const store = useBuildStore()

  return {
    // State
    components: store.components,
    totalCost: store.totalCost,
    totalPower: store.totalPower,
    compatibility: store.compatibility,
    isLoading: store.isLoading,
    error: store.error,
    currentBuildId: store.currentBuildId,
    savedBuilds: store.savedBuilds,

    // Actions
    addComponent: store.addComponent,
    removeComponent: store.removeComponent,
    clearBuild: store.clearBuild,

    // Build CRUD
    saveBuild: store.saveBuild,
    loadBuild: store.loadBuild,
    deleteBuild: store.deleteBuild,
    loadSavedBuilds: store.loadSavedBuilds,

    // Helpers
    hasErrors: store.compatibility.some((issue) => issue.type === 'error'),
    hasWarnings: store.compatibility.some((issue) => issue.type === 'warning'),
    isValid: store.components.has(ComponentCategory.CPU) &&
      store.components.has(ComponentCategory.PSU) &&
      store.compatibility.filter((i) => i.type === 'error').length === 0,
  }
}
