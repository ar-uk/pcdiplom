/**
 * Sidebar Component
 * Navigation sidebar (minimal Phase 2, expanded in Phase 3+)
 */

import React from 'react'
import { Link } from 'react-router-dom'
import './Sidebar.css'

export const Sidebar: React.FC = () => {
  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <h3>Menu</h3>
        <ul>
          <li>
            <Link to="/builder">Builder</Link>
          </li>
          <li>
            <Link to="/community">Community</Link>
          </li>
          <li>
            <Link to="/ai-assistant">AI Assistant</Link>
          </li>
          <li>
            <Link to="/profile">Profile</Link>
          </li>
        </ul>
      </nav>
    </aside>
  )
}
