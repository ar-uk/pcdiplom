/**
 * AI Service
 * Handles all AI and recommendation-related API calls
 */

import client from '../client'
import { ENDPOINTS } from '../endpoints'
import { NotImplementedError } from '@/utils/errors'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface RecommendationRequest {
  budget: number
  purpose: string
  resolution?: string
  refreshRate?: number
}

export class AIService {
  /**
   * Send chat message to AI assistant
   */
  static async sendChatMessage(
    message: string,
    conversationHistory?: ChatMessage[]
  ): Promise<string> {
    try {
      const response = await client.post<{ response: string }>(
        ENDPOINTS.AI_CHAT,
        {
          message,
          history: conversationHistory,
        }
      )
      return response.data.response
    } catch (error) {
      throw new NotImplementedError('AI chat not yet implemented')
    }
  }

  /**
   * Get AI recommendation for build
   */
  static async getRecommendation(
    request: RecommendationRequest
  ): Promise<Record<string, unknown>> {
    try {
      const response = await client.post(
        ENDPOINTS.AI_BUILD_RECOMMENDATION,
        request
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError('AI recommendation not yet implemented')
    }
  }

  /**
   * Get build suggestions based on use case
   */
  static async getBuildSuggestions(useCase: string): Promise<Array<Record<string, unknown>>> {
    try {
      const response = await client.post<Array<Record<string, unknown>>>(
        ENDPOINTS.AI_BUILD_RECOMMENDATION,
        { purpose: useCase }
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError(
        'Build suggestions not yet implemented'
      )
    }
  }

  /**
   * Get component recommendations
   */
  static async recommendComponents(criteria: Record<string, unknown>): Promise<Array<Record<string, unknown>>> {
    try {
      const response = await client.post<Array<Record<string, unknown>>>(
        `${ENDPOINTS.AI_CHAT}/components`,
        criteria
      )
      return response.data
    } catch (error) {
      throw new NotImplementedError(
        'Component recommendations not yet implemented'
      )
    }
  }
}
