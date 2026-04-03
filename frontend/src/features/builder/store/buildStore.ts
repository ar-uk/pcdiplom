/**
 * Build Store
 * Zustand store for managing PC builder state
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { Component, ComponentCategory, SavedBuild } from '@/types'
import {
  validateBuildCompatibility,
  calculateBuildCost,
  calculateBuildPowerConsumption,
} from '@/utils/validation'
import { BuildService } from '@/api/services/buildService'

interface BuildState {
  // Current build state
  components: Map<ComponentCategory, Component>
  totalCost: number
  totalPower: number
  compatibility: ReturnType<typeof validateBuildCompatibility>
  isLoading: boolean
  error: string | null

  // Saved builds
  savedBuilds: SavedBuild[]
  currentBuildId: string | null

  // Actions
  addComponent: (category: ComponentCategory, component: Component) => void
  removeComponent: (category: ComponentCategory) => void
  clearBuild: () => void
  calculateState: () => void

  // Build CRUD operations
  saveBuild: (name: string, description?: string, isPublic?: boolean) => Promise<void>
  loadBuild: (id: string) => Promise<void>
  deleteBuild: (id: string) => Promise<void>
  loadSavedBuilds: () => Promise<void>
  setCurrentBuildId: (id: string | null) => void

  // Error handling
  setError: (error: string | null) => void
  clearError: () => void
}

// Serialization helpers for Map
const serializeComponents = (components: Map<ComponentCategory, Component>) => {
  return Array.from(components.entries())
}

const deserializeComponents = (
  entries: Array<[ComponentCategory, Component]> | undefined
): Map<ComponentCategory, Component> => {
  if (!entries) return new Map()
  return new Map(entries)
}

export const useBuildStore = create<BuildState>()(
  persist(
    (set, get) => ({
      components: new Map(),
      totalCost: 0,
      totalPower: 0,
      compatibility: [],
      isLoading: false,
      error: null,
      savedBuilds: [],
      currentBuildId: null,

      addComponent: (category: ComponentCategory, component: Component) => {
        set((state) => {
          const newComponents = new Map(state.components)
          newComponents.set(category, component)

          // Recalculate state
          const totalCost = calculateBuildCost(newComponents)
          const totalPower = calculateBuildPowerConsumption(newComponents)
          const compatibility = validateBuildCompatibility(newComponents)

          return {
            components: newComponents,
            totalCost,
            totalPower,
            compatibility,
          }
        })
      },

      removeComponent: (category: ComponentCategory) => {
        set((state) => {
          const newComponents = new Map(state.components)
          newComponents.delete(category)

          const totalCost = calculateBuildCost(newComponents)
          const totalPower = calculateBuildPowerConsumption(newComponents)
          const compatibility = validateBuildCompatibility(newComponents)

          return {
            components: newComponents,
            totalCost,
            totalPower,
            compatibility,
          }
        })
      },

      clearBuild: () => {
        set({
          components: new Map(),
          totalCost: 0,
          totalPower: 0,
          compatibility: [],
          currentBuildId: null,
        })
      },

      calculateState: () => {
        set((state) => {
          const totalCost = calculateBuildCost(state.components)
          const totalPower = calculateBuildPowerConsumption(state.components)
          const compatibility = validateBuildCompatibility(state.components)

          return {
            totalCost,
            totalPower,
            compatibility,
          }
        })
      },

      saveBuild: async (name: string, description?: string, isPublic = false) => {

        set({ isLoading: true, error: null })

        try {
          const buildData = {
            name,
            description,
            isPublic,
          }

          const savedBuild = await BuildService.createBuild(buildData)
          
          set((state) => ({
            currentBuildId: savedBuild.id,
            savedBuilds: [...state.savedBuilds, savedBuild],
            isLoading: false,
          }))
        } catch (error) {
          const errorMsg = error instanceof Error ? error.message : 'Failed to save build'
          set({ error: errorMsg, isLoading: false })
          throw error
        }
      },

      loadBuild: async (id: string) => {
        set({ isLoading: true, error: null })

        try {
          const build = await BuildService.getBuildById(id)

          // Reconstruct components map from build
          const componentsMap = new Map<ComponentCategory, Component>()
          if (build.components && Array.isArray(build.components)) {
            for (const buildComponent of build.components) {
              if (buildComponent.component) {
                const category = buildComponent.component.category
                componentsMap.set(category, buildComponent.component)
              }
            }
          }

          set({
            components: componentsMap,
            totalCost: build.estimatedPrice,
            currentBuildId: id,
            isLoading: false,
          })

          get().calculateState()
        } catch (error) {
          const errorMsg = error instanceof Error ? error.message : 'Failed to load build'
          set({ error: errorMsg, isLoading: false })
          throw error
        }
      },

      deleteBuild: async (id: string) => {
        set({ isLoading: true, error: null })

        try {
          await BuildService.deleteBuild(id)

          set((state) => ({
            savedBuilds: state.savedBuilds.filter((b) => b.id !== id),
            currentBuildId: state.currentBuildId === id ? null : state.currentBuildId,
            isLoading: false,
          }))
        } catch (error) {
          const errorMsg = error instanceof Error ? error.message : 'Failed to delete build'
          set({ error: errorMsg, isLoading: false })
          throw error
        }
      },

      loadSavedBuilds: async () => {
        set({ isLoading: true, error: null })

        try {
          const builds = await BuildService.getUserBuilds()
          set({
            savedBuilds: builds,
            isLoading: false,
          })
        } catch (error) {
          const errorMsg = error instanceof Error ? error.message : 'Failed to load builds'
          set({ error: errorMsg, isLoading: false })
          throw error
        }
      },

      setCurrentBuildId: (id: string | null) => {
        set({ currentBuildId: id })
      },

      setError: (error: string | null) => {
        set({ error })
      },

      clearError: () => {
        set({ error: null })
      },
    }),
    {
      name: 'build-store',
      storage: {
        getItem: (key) => {
          const item = localStorage.getItem(key)
          if (!item) return null

          const parsed = JSON.parse(item)
          if (parsed.state.components) {
            // Reconstruct Map from stored array
            parsed.state.components = deserializeComponents(parsed.state.components)
          }
          return parsed
        },
        setItem: (key, value) => {
          const toStore = {
            ...value,
            state: {
              ...value.state,
              components: serializeComponents(value.state.components),
            },
          }
          localStorage.setItem(key, JSON.stringify(toStore))
        },
        removeItem: (key) => localStorage.removeItem(key),
      },
    }
  )
)

// Selector hooks for performance
export const useBuildComponents = () => useBuildStore((state) => state.components)
export const useBuildCost = () => useBuildStore((state) => state.totalCost)
export const useBuildPower = () => useBuildStore((state) => state.totalPower)
export const useBuildCompatibility = () => useBuildStore((state) => state.compatibility)
export const useBuildLoading = () => useBuildStore((state) => state.isLoading)
