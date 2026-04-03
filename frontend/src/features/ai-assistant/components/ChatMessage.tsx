/**
 * ChatMessage Component
 * Display individual chat message
 */

import { ChatMessage as ChatMessageType } from '@/types'

interface ChatMessageProps {
  message: ChatMessageType
  isLoading?: boolean
}

export const ChatMessage = ({ message, isLoading = false }: ChatMessageProps) => {
  const isUser = message.role === 'user'

  return (
    <div className={`flex gap-3 mb-4 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {!isUser && (
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
          AI
        </div>
      )}

      <div
        className={`max-w-sm px-4 py-2 rounded-lg ${
          isUser
            ? 'bg-blue-600 text-white rounded-br-none'
            : 'bg-gray-100 text-gray-900 rounded-bl-none'
        }`}
      >
        {isLoading && (
          <div className="flex gap-1 items-center">
            <div className="w-2 h-2 bg-gray-600 rounded-full animate-pulse" />
            <div className="w-2 h-2 bg-gray-600 rounded-full animate-pulse" style={{ animationDelay: '0.1s' }} />
            <div className="w-2 h-2 bg-gray-600 rounded-full animate-pulse" style={{ animationDelay: '0.2s' }} />
          </div>
        )}
        {!isLoading && <p className="whitespace-pre-wrap text-sm">{message.content}</p>}
      </div>

      {isUser && (
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
          U
        </div>
      )}
    </div>
  )
}
