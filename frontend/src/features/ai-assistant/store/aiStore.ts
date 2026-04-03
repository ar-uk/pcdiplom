/**
 * AI Assistant Zustand Store
 * Central state management for AI chat
 */

import { create } from 'zustand'
import { ChatMessage } from '@/types'
import { Build } from '@/types'

interface AIStore {
  messages: ChatMessage[]
  isLoading: boolean
  currentBuild?: Build
  
  // Actions
  addMessage: (message: ChatMessage) => void
  clearMessages: () => void
  setCurrentBuild: (build?: Build) => void
  setIsLoading: (loading: boolean) => void
}

export const useAIStore = create<AIStore>((set) => ({
  messages: [
    {
      id: '0',
      role: 'assistant',
      content:
        "Hi! I'm your PC building assistant. I can help you find the perfect components for your build, answer questions about compatibility, and provide recommendations based on your budget and needs. How can I help you today?",
      timestamp: new Date().toISOString(),
    },
  ],
  isLoading: false,
  currentBuild: undefined,

  addMessage: (message) =>
    set((state) => ({
      messages: [...state.messages, message],
    })),

  clearMessages: () =>
    set({
      messages: [
        {
          id: '0',
          role: 'assistant',
          content:
            "Hi! I'm your PC building assistant. I can help you find the perfect components for your build, answer questions about compatibility, and provide recommendations based on your budget and needs. How can I help you today?",
          timestamp: new Date().toISOString(),
        },
      ],
    }),

  setCurrentBuild: (build) =>
    set({
      currentBuild: build,
    }),

  setIsLoading: (loading) =>
    set({
      isLoading: loading,
    }),
}))
