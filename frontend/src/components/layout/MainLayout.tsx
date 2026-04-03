/**
 * MainLayout Component
 * Main layout wrapper for authenticated pages
 */

import React from 'react'
import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Sidebar } from './Sidebar'
import './MainLayout.css'

export const MainLayout: React.FC = () => {
  return (
    <div className="main-layout">
      <Header />
      <div className="layout-container">
        <Sidebar />
        <main className="main-content">
          <Outlet />
        </main>
      </div>
      <footer className="footer">
        <p>&copy; 2026 PCBuilder. All rights reserved.</p>
      </footer>
    </div>
  )
}
