/**
 * BuilderPage Component
 * Main PC builder interface with 3-column layout
 */

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Component, ComponentCategory } from '@/types'
import {
  ComponentSelector,
  ComponentList,
  BuildSummary,
  SaveBuildModal,
} from '../components'
import { useBuild } from '../hooks'

export const BuilderPage: React.FC = () => {
  const navigate = useNavigate()
  const [selectedCategory, setSelectedCategory] = useState<ComponentCategory | null>(null)
  const [selectedComponent, setSelectedComponent] = useState<Component | undefined>()
  const [showSaveModal, setShowSaveModal] = useState(false)

  const {
    components,
    totalCost,
    totalPower,
    compatibility,
    isLoading,
    error,
    addComponent,
    removeComponent,
    clearBuild,
    saveBuild,

  } = useBuild()

  // Handle component selection
  const handleSelectComponent = (component: Component) => {
    setSelectedComponent(component)
    addComponent(component.category, component)
  }

  // Handle save build
  const handleSaveBuild = async (
    name: string,
    description?: string,
    isPublic?: boolean
  ) => {
    try {
      await saveBuild(name, description, isPublic)
      setShowSaveModal(false)
      // Show success message and optionally redirect
      // Toast notification would be here
    } catch (err) {
      console.error('Failed to save build:', err)
    }
  }



  return (
    <div className="flex flex-col h-full bg-gray-100">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="max-w-7xl mx-auto">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">PC Builder</h1>
              <p className="text-gray-600 mt-1">
                Select components and build your custom PC
              </p>
            </div>
            {components.size > 0 && (
              <button
                onClick={() => navigate('/community')}
                className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg transition-colors text-sm"
              >
                View Community Builds
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Content - 3 column layout */}
      <div className="flex-1 overflow-hidden p-4">
        <div className="max-w-7xl mx-auto h-full">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 h-full">
            {/* Left: Category Selector (1 col) */}
            <div className="md:col-span-1 bg-white rounded-lg shadow overflow-hidden flex flex-col">
              <ComponentSelector
                selectedCategory={selectedCategory}
                onCategoryChange={setSelectedCategory}
              />
            </div>

            {/* Center: Component List (2 cols) */}
            <div className="md:col-span-2 bg-white rounded-lg shadow overflow-hidden flex flex-col">
              <ComponentList
                category={selectedCategory}
                selectedComponent={selectedComponent}
                onSelectComponent={handleSelectComponent}
              />
            </div>

            {/* Right: Build Summary (1 col) */}
            <div className="md:col-span-1 bg-white rounded-lg shadow overflow-hidden flex flex-col">
              <BuildSummary
                components={components}
                totalCost={totalCost}
                totalPower={totalPower}
                compatibility={compatibility}
                onRemoveComponent={removeComponent}
                onClearBuild={clearBuild}
                onSaveBuild={() => setShowSaveModal(true)}
                isLoading={isLoading}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Save Build Modal */}
      <SaveBuildModal
        isOpen={showSaveModal}
        isLoading={isLoading}
        error={error || undefined}
        onClose={() => setShowSaveModal(false)}
        onSubmit={handleSaveBuild}
      />

      {/* Error Toast (if needed) */}
      {error && (
        <div className="fixed bottom-4 right-4 bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
          {error}
        </div>
      )}
    </div>
  )
}

export default BuilderPage
