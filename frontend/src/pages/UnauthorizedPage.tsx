/**
 * Unauthorized (401) Error Page
 * Shown when user lacks permission to access a resource
 */

import { Link } from 'react-router-dom'

export const UnauthorizedPage = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="text-center max-w-md">
        <div className="mb-8">
          <h1 className="text-6xl font-bold text-red-600 mb-2">401</h1>
          <h2 className="text-3xl font-bold text-gray-900">Unauthorized</h2>
        </div>

        <p className="text-gray-600 text-lg mb-6">
          You don't have permission to access this page
        </p>

        <div className="space-y-3">
          <p className="text-gray-500 text-sm">
            Try logging in with an account that has the necessary permissions, or
            contact support if you believe this is an error.
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
            onClick={() => window.history.back()}
            className="px-6 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Go Back
          </button>
        </div>
      </div>
    </div>
  )
}
