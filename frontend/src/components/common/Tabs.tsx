/**
 * Tabs Component
 * Accessible tab navigation component
 */

import { useState } from 'react'

interface TabItem {
  id: string
  label: string
  content: React.ReactNode
  badge?: string | number
}

interface TabsProps {
  tabs: TabItem[]
  defaultTab?: string
  className?: string
  onChange?: (tabId: string) => void
}

export const Tabs = ({ tabs, defaultTab, className = '', onChange }: TabsProps) => {
  const [activeTab, setActiveTab] = useState(defaultTab || tabs[0]?.id || '')

  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId)
    onChange?.(tabId)
  }

  const activeTabItem = tabs.find((tab) => tab.id === activeTab)

  return (
    <div className={className}>
      {/* Tab Buttons */}
      <div
        className="border-b border-gray-200 dark:border-gray-700 flex gap-4 overflow-x-auto"
        role="tablist"
      >
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => handleTabChange(tab.id)}
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-controls={`panel-${tab.id}`}
            id={`tab-${tab.id}`}
            className={`py-3 px-4 font-medium text-base whitespace-nowrap border-b-2 transition-all duration-200 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
              activeTab === tab.id
                ? 'text-primary border-primary'
                : 'text-secondary border-transparent hover:text-primary hover:border-primary dark:text-gray-400 dark:hover:text-primary'
            }`}
          >
            {tab.label}
            {tab.badge !== undefined && (
              <span className="ml-2 inline-flex items-center justify-center w-6 h-6 bg-primary bg-opacity-20 text-primary text-xs font-semibold rounded-full">
                {tab.badge}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="pt-4">
        {activeTabItem && (
          <div
            id={`panel-${activeTab}`}
            role="tabpanel"
            aria-labelledby={`tab-${activeTab}`}
          >
            {activeTabItem.content}
          </div>
        )}
      </div>
    </div>
  )
}
