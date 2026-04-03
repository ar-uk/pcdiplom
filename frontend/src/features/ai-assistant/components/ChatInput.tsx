/**
 * ChatInput Component
 * Message input interface with send button
 */

import { useState } from 'react'
import { FORM_LIMITS } from '@/utils/constants'
import { Button } from '@/components/common'

interface ChatInputProps {
  onSend: (message: string) => void
  onClear: () => void
  disabled?: boolean
}

export const ChatInput = ({
  onSend,
  onClear,
  disabled = false,
}: ChatInputProps) => {
  const [message, setMessage] = useState('')

  const handleSend = () => {
    if (message.trim()) {
      onSend(message)
      setMessage('')
    }
  }

  const remaining = FORM_LIMITS.CHAT_MESSAGE_MAX - message.length

  return (
    <div className="border-t border-gray-200 pt-4 space-y-2">
      {/* Quick suggestions */}
      <div className="flex flex-wrap gap-2 mb-3">
        {[
          'Gaming PC $1500',
          'Budget build',
          'Workstation',
          'What CPU is best?',
        ].map((suggestion) => (
          <button
            key={suggestion}
            onClick={() => {
              setMessage(suggestion)
            }}
            className="text-xs px-3 py-1 bg-blue-50 text-blue-700 rounded-full hover:bg-blue-100 border border-blue-200"
          >
            {suggestion}
          </button>
        ))}
      </div>

      {/* Input area */}
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value.slice(0, FORM_LIMITS.CHAT_MESSAGE_MAX))}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && e.ctrlKey) {
            handleSend()
          }
        }}
        placeholder="Ask me anything about PC builds..."
        disabled={disabled}
        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        rows={3}
      />

      {/* Footer with char count and buttons */}
      <div className="flex items-center justify-between">
        <span className="text-xs text-gray-500">
          {remaining} / {FORM_LIMITS.CHAT_MESSAGE_MAX} characters
        </span>

        <div className="flex gap-2">
          <Button
            variant="secondary"
            size="small"
            onClick={onClear}
            disabled={disabled}
          >
            Clear History
          </Button>

          <Button
            variant="primary"
            size="small"
            onClick={handleSend}
            disabled={disabled || !message.trim()}
            loading={disabled}
          >
            Send
          </Button>
        </div>
      </div>
    </div>
  )
}
