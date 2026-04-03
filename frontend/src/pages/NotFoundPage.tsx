/**
 * NotFoundPage
 * 404 error page
 */

import React from 'react'
import { Link } from 'react-router-dom'
import './NotFoundPage.css'

export const NotFoundPage: React.FC = () => {
  return (
    <div className="not-found-page">
      <div className="not-found-content">
        <div className="error-code">404</div>
        <h1>Page Not Found</h1>
        <p>The page you're looking for doesn't exist or has been moved.</p>
        <Link to="/" className="home-link">
          Go Back Home
        </Link>
      </div>
    </div>
  )
}
