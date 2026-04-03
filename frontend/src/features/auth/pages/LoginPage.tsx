/**
 * LoginPage
 * User login page with form and validation
 */

import React from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useLogin } from '../hooks'
import { LoginForm } from '../components'
import { LoginRequest } from '@/types'
import './AuthPages.css'

export const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const { isLoading, error, login } = useLogin()

  const handleLogin = async (data: LoginRequest) => {
    const success = await login(data)
    if (success) {
      navigate('/builder')
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Login</h1>
        <LoginForm
          onSubmit={handleLogin}
          isLoading={isLoading}
          error={error}
        />
        <div className="auth-footer">
          <p>Don't have an account? <Link to="/register">Register here</Link></p>
        </div>
      </div>
    </div>
  )
}
