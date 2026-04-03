/**
 * ChatInterface Component
 * Main chat container with auto-scroll
 */

import { useEffect, useRef } from 'react'
import { ChatMessage } from '@/types'
import { ChatMessage as ChatMessageComponent } from './ChatMessage'
import { ChatInput } from './ChatInput'

interface ChatInterfaceProps {
  messages: ChatMessage[]
  isLoading: boolean
  onSend: (message: string) => void
  onClear: () => void
}

export const ChatInterface = ({
  messages,
  isLoading,
  onSend,
  onClear,
}: ChatInterfaceProps) => {
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  return (
    <div className="flex flex-col h-full bg-white rounded-lg border border-gray-200 shadow">
      {/* Messages container */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {messages.length === 0 ? (
          <div className="h-full flex items-center justify-center text-gray-400">
            <p>Start a conversation!</p>
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <ChatMessageComponent key={msg.id} message={msg} />
            ))}
            {isLoading && (
              <ChatMessageComponent
                message={{
                  id: 'typing',
                  role: 'assistant',
                  content: 'Thinking...',
                  timestamp: new Date().toISOString(),
                }}
                isLoading
              />
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Input area */}
      <ChatInput
        onSend={onSend}
        onClear={onClear}
        disabled={isLoading}
      />
    </div>
  )
}
