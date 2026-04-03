/**
 * Header Component
 * Main navigation header with user menu
 */

import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import './Header.css'

export const Header: React.FC = () => {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    setMenuOpen(false)
    navigate('/login')
  }

  return (
    <header className="header">
      <div className="header-container">
        <Link to="/" className="logo">
          <span className="logo-icon">⚙️</span>
          <span className="logo-text">PCBuilder</span>
        </Link>

        <nav className="nav-links">
          {isAuthenticated ? (
            <>
              <Link to="/builder">Builder</Link>
              <Link to="/community">Community</Link>
              <Link to="/ai-assistant">AI Assistant</Link>
              <Link to="/profile">Profile</Link>
            </>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <Link to="/register">Register</Link>
            </>
          )}
        </nav>

        {isAuthenticated && (
          <div className="user-menu">
            <button
              className="user-button"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {user?.username || 'User'}
            </button>

            {menuOpen && (
              <div className="dropdown-menu">
                <Link to="/profile" onClick={() => setMenuOpen(false)}>
                  Profile
                </Link>
                <Link to="/settings" onClick={() => setMenuOpen(false)}>
                  Settings
                </Link>
                <button onClick={handleLogout} className="logout-btn">
                  Logout
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  )
}
