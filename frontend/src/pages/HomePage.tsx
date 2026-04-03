/**
 * HomePage
 * Landing page for unauthenticated users
 */

import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import './HomePage.css'

export const HomePage: React.FC = () => {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/builder')
    }
  }, [isAuthenticated, navigate])

  if (isAuthenticated) {
    return null
  }

  return (
    <div className="home-page">
      <section className="hero">
        <div className="hero-content">
          <h1>Welcome to PCBuilder</h1>
          <p>Build your perfect PC with our intelligent component selector</p>
          <div className="hero-buttons">
            <button
              onClick={() => navigate('/login')}
              className="btn btn-primary"
            >
              Login
            </button>
            <button
              onClick={() => navigate('/register')}
              className="btn btn-secondary"
            >
              Register
            </button>
          </div>
        </div>
        <div className="hero-image">
          <div className="icon">⚙️</div>
        </div>
      </section>

      <section className="features">
        <h2>Features</h2>
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">🎯</div>
            <h3>Smart Selection</h3>
            <p>Get recommendations based on your needs and budget</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">✅</div>
            <h3>Compatibility Check</h3>
            <p>Ensure all components work together seamlessly</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">👥</div>
            <h3>Community Builds</h3>
            <p>Share and explore builds from the community</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🤖</div>
            <h3>AI Assistant</h3>
            <p>Get expert advice from our AI assistant</p>
          </div>
        </div>
      </section>
    </div>
  )
}
