/**
 * RegisterPage
 * User registration page with form and validation
 */

import React from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useRegister } from '../hooks'
import { RegisterForm } from '../components'
import { UserRole } from '@/types'
import './AuthPages.css'

interface RegisterPayload {
  email: string
  username: string
  password: string
  role: UserRole
}

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
  const { isLoading, error, register } = useRegister()

  const handleRegister = async (data: RegisterPayload) => {
    const success = await register(data)
    if (success) {
      navigate('/builder')
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Register</h1>
        <RegisterForm
          onSubmit={handleRegister}
          isLoading={isLoading}
          error={error}
        />
        <div className="auth-footer">
          <p>Already have an account? <Link to="/login">Login here</Link></p>
        </div>
      </div>
    </div>
  )
}
