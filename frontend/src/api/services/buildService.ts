/**
 * Build Service
 * Handles all PC build-related API calls
 */

import client from '../client'
import { ENDPOINTS } from '../endpoints'
import {
  SavedBuild,
  BuildCreateRequest,
  BuildUpdateRequest,
  AddComponentRequest,
  RemoveComponentRequest,
} from '@/types'
import { NotImplementedError } from '@/utils/errors'
import { mockComponents, mockBuilds } from '../mock/mockData'

import { useAuthStore } from '@/features/auth'

export class BuildService {
  /**
   * Get all available components
   */
  static async getComponents(category?: string): Promise<Component[]> {
    try {
      const url = category
        ? ENDPOINTS.COMPONENTS_BY_CATEGORY(category)
        : ENDPOINTS.COMPONENTS_LIST
      const response = await client.get<Component[]>(url)
      return response.data
    } catch (error) {
      // Mock fallback
      return category
        ? mockComponents.filter((c) =>
            c.category.toString().toLowerCase() === category.toLowerCase()
          )
        : mockComponents
    }
  }

  /**
   * Get component by ID
   */
  static async getComponentById(id: string): Promise<Component> {
    try {
      const response = await client.get<Component>(
        ENDPOINTS.COMPONENT_DETAIL(id)
      )
      return response.data
    } catch (error) {
      // Mock fallback
      const component = mockComponents.find((c) => c.id === id)
      if (!component) {
        throw new Error(`Component ${id} not found`)
      }
      return component
    }
  }

  /**
   * Create new build
   */
  static async createBuild(request: BuildCreateRequest): Promise<SavedBuild> {
    try {
      const response = await client.post<SavedBuild>(
        ENDPOINTS.BUILD_CREATE,
        request
      )
      return response.data
    } catch (error) {
      // Mock fallback - create build locally
      const user = useAuthStore.getState().user
      const newBuild: SavedBuild = {
        id: 'build-' + Date.now(),
        name: request.name,
        description: request.description,
        userId: user?.id || 'user-' + Date.now(),
        status: BuildStatus.SAVED,
        isPublic: request.isPublic || false,
        components: [],
        estimatedPrice: 0,
        compatibility: {
          isCompatible: false,
          issues: [],
          warnings: [],
        },
        views: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      
      // Store in localStorage
      const savedBuilds = JSON.parse(localStorage.getItem('user-builds') || '[]')
      savedBuilds.push(newBuild)
      localStorage.setItem('user-builds', JSON.stringify(savedBuilds))
      
      return newBuild
    }
  }

  /**
   * Get build by ID
   */
  static async getBuildById(id: string): Promise<SavedBuild> {
    try {
      const response = await client.get<SavedBuild>(ENDPOINTS.BUILD_DETAIL(id))
      return response.data
    } catch (error) {
      // Mock fallback
      const build = mockBuilds.find((b) => b.id === id)
      if (!build) {
        throw new Error(`Build ${id} not found`)
      }
      return build
    }
  }

  /**
   * Get all user builds
   */
  static async getUserBuilds(): Promise<SavedBuild[]> {
    try {
      const response = await client.get<SavedBuild[]>(ENDPOINTS.PROFILE_BUILDS)
      return response.data
    } catch (error) {
      // Mock fallback
      return mockBuilds
    }
  }

  /**
   * Update build
   */
  static async updateBuild(
    id: string,
    request: BuildUpdateRequest
  ): Promise<SavedBuild> {
    try {
      const response = await client.put<SavedBuild>(
        ENDPOINTS.BUILD_UPDATE(id),
        request
      )
      return response.data
    } catch (error) {
      // Mock fallback - update in localStorage
      const savedBuilds = JSON.parse(localStorage.getItem('user-builds') || '[]')
      const buildIndex = savedBuilds.findIndex((b: SavedBuild) => b.id === id)
      
      if (buildIndex === -1) {
        throw new Error('Build not found')
      }
      
      const updatedBuild = {
        ...savedBuilds[buildIndex],
        ...request,
        updatedAt: new Date().toISOString(),
      }
      
      savedBuilds[buildIndex] = updatedBuild
      localStorage.setItem('user-builds', JSON.stringify(savedBuilds))
      
      return updatedBuild
    }
  }

  /**
   * Delete build
   */
  static async deleteBuild(id: string): Promise<void> {
    try {
      await client.delete(ENDPOINTS.BUILD_DELETE(id))
    } catch (error) {
      // Mock fallback - remove from localStorage
      const savedBuilds = JSON.parse(localStorage.getItem('user-builds') || '[]')
      const filtered = savedBuilds.filter((b: SavedBuild) => b.id !== id)
      localStorage.setItem('user-builds', JSON.stringify(filtered))
    }
  }

  /**
   * Add component to build
   */
  static async addComponentToBuild(
    buildId: string,
    request: AddComponentRequest
  ): Promise<SavedBuild> {
    try {
      const response = await client.post<SavedBuild>(
        `${ENDPOINTS.BUILD_DETAIL(buildId)}/components`,
        request
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError('Add component to build not yet implemented')
    }
  }

  /**
   * Remove component from build
   */
  static async removeComponentFromBuild(
    buildId: string,
    request: RemoveComponentRequest
  ): Promise<SavedBuild> {
    try {
      const response = await client.delete<SavedBuild>(
        `${ENDPOINTS.BUILD_DETAIL(buildId)}/components/${request.componentId}`
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError(
        'Remove component from build not yet implemented'
      )
    }
  }

  /**
   * Check build compatibility
   */
  static async checkCompatibility(buildId: string): Promise<Record<string, unknown>> {
    try {
      const response = await client.get(ENDPOINTS.BUILD_COVERAGE(buildId))
      return response.data
    } catch (error) {
      throw new NotImplementedError('Compatibility check not yet implemented')
    }
  }
}
