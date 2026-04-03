/**
 * Server Error (500) Page
 * Shown when a server error occurs
 */

import { Link } from 'react-router-dom'

export const ServerErrorPage = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-100 px-4">
      <div className="text-center max-w-md">
        <div className="mb-8">
          <h1 className="text-6xl font-bold text-red-600 mb-2">500</h1>
          <h2 className="text-3xl font-bold text-gray-900">Server Error</h2>
        </div>

        <p className="text-gray-600 text-lg mb-6">
          Something went wrong on our end. We're working to fix it.
        </p>

        <div className="space-y-3">
          <p className="text-gray-500 text-sm">
            Please try again later, or contact our support team if the problem persists.
          </p>
          <p className="text-gray-500 text-sm">
            Support: <a href="mailto:support@pcbuilder.com" className="text-blue-600 hover:text-blue-700">support@pcbuilder.com</a>
          </p>
        </div>

        <div className="mt-8 flex gap-4 justify-center">
          <Link
            to="/"
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Go Home
          </Link>
          <button
            onClick={() => window.location.reload()}
            className="px-6 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Reload Page
          </button>
        </div>
      </div>
    </div>
  )
}
