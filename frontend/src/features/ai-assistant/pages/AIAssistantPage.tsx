/**
 * AIAssistantPage
 * Main AI assistant view with chat and context panel
 */

import { ChatInterface } from '../components'
import { useAIAssistant } from '../hooks'
import { Card } from '@/components/common'
import { formatComponentSpecs } from '@/utils/formatters'

export const AIAssistantPage = () => {
  const { messages, isLoading, currentBuild, sendMessage, clearChat } =
    useAIAssistant()

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-6">
          PC Builder AI Assistant
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Chat Interface - 70% width */}
          <div className="lg:col-span-2">
            <ChatInterface
              messages={messages}
              isLoading={isLoading}
              onSend={sendMessage}
              onClear={clearChat}
            />
          </div>

          {/* Context Panel - 30% width */}
          <div className="space-y-4">
            {currentBuild ? (
              <>
                <Card shadow="light" padding="medium">
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Current Build
                  </h2>
                  <p className="text-sm text-gray-600 mb-3">{currentBuild.name}</p>

                  {/* Components List */}
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {currentBuild.components?.map((comp, idx) => (
                      <div
                        key={idx}
                        className="text-xs text-gray-700 p-2 bg-gray-50 rounded"
                      >
                        <p className="font-medium">{comp.component?.name}</p>
                        <p className="text-gray-500">
                          {comp.component && formatComponentSpecs(comp.component)}
                        </p>
                        <p className="font-semibold text-blue-600">
                          ${comp.component?.price?.toLocaleString()}
                        </p>
                      </div>
                    ))}
                  </div>

                  {/* Total */}
                  <div className="border-t border-gray-200 mt-3 pt-3">
                    <p className="text-sm font-semibold text-gray-900">
                      Total:{' '}
                      <span className="text-blue-600">
                        $
                        {(
                          currentBuild.components?.reduce(
                            (sum, c) => sum + (c.component?.price || 0),
                            0
                          ) || 0
                        ).toLocaleString()}
                      </span>
                    </p>
                  </div>
                </Card>
              </>
            ) : (
              <Card shadow="light" padding="medium">
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                  Get Started
                </h2>
                <p className="text-sm text-gray-600 mb-4">
                  Describe your ideal PC build and I'll help you find the perfect
                  components.
                </p>

                <div className="space-y-2">
                  <p className="text-xs font-semibold text-gray-700">
                    Quick prompts:
                  </p>
                  <ul className="text-xs text-gray-600 space-y-1">
                    <li>• "Gaming PC $1500"</li>
                    <li>• "Budget build under $700"</li>
                    <li>• "Workstation for 3D rendering"</li>
                    <li>• "What's the best CPU for streaming?"</li>
                  </ul>
                </div>
              </Card>
            )}

            {/* Tips Card */}
            <Card shadow="light" padding="medium" className="bg-blue-50">
              <h3 className="font-semibold text-blue-900 mb-2">💡 Tips</h3>
              <ul className="text-xs text-blue-800 space-y-1">
                <li>• Be specific about your budget</li>
                <li>• Mention your intended use</li>
                <li>• Ask about compatibility</li>
                <li>• Request performance comparisons</li>
              </ul>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
