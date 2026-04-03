/**
 * useAIAssistant Hook
 * Custom hook for interacting with AI assistant
 */

import { useCallback } from 'react'
import { useAIStore } from '../store/aiStore'
import { AIService } from '@/api/services'
import { ChatMessage } from '@/types'
import { FORM_LIMITS } from '@/utils/constants'

export const useAIAssistant = () => {
  const { messages, isLoading, currentBuild, addMessage, clearMessages, setIsLoading } =
    useAIStore()

  const sendMessage = useCallback(
    async (text: string) => {
      // Validate message length
      if (!text.trim() || text.length > FORM_LIMITS.CHAT_MESSAGE_MAX) {
        return
      }

      // Add user message
      const userMessage: ChatMessage = {
        id: `msg-${Date.now()}`,
        role: 'user',
        content: text.trim(),
        timestamp: new Date().toISOString(),
      }

      addMessage(userMessage)
      setIsLoading(true)

      try {
        // Get AI response
        const response = await AIService.sendChatMessage(text, messages)

        // Add assistant message
        const assistantMessage: ChatMessage = {
          id: `msg-${Date.now()}`,
          role: 'assistant',
          content: response,
          timestamp: new Date().toISOString(),
        }

        addMessage(assistantMessage)
      } catch (error) {
        // Add error message
        const errorMessage: ChatMessage = {
          id: `msg-${Date.now()}`,
          role: 'assistant',
          content:
            'Sorry, I encountered an error. Please try again. ' +
            (error instanceof Error ? error.message : ''),
          timestamp: new Date().toISOString(),
        }
        addMessage(errorMessage)
      } finally {
        setIsLoading(false)
      }
    },
    [messages, addMessage, setIsLoading]
  )

  const clearChat = useCallback(() => {
    clearMessages()
  }, [clearMessages])

  return {
    messages,
    isLoading,
    currentBuild,
    sendMessage,
    clearChat,
  }
}
