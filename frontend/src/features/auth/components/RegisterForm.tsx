/**
 * RegisterForm Component
 * Extracted registration form with validation
 */

import React, { useState } from 'react'
import { z } from 'zod'
import { UserRole } from '@/types'

const registerSchema = z
  .object({
    email: z.string().email('Invalid email address'),
    username: z.string().min(3, 'Username must be at least 3 characters'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number'),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type RegisterFormData = z.infer<typeof registerSchema>

interface RegisterFormProps {
  onSubmit: (data: Omit<RegisterFormData, 'confirmPassword'> & { role: UserRole }) => void
  isLoading?: boolean
  error?: string | null
}

export const RegisterForm: React.FC<RegisterFormProps> = ({
  onSubmit,
  isLoading = false,
  error,
}) => {
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    role: UserRole.USER,
  })
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({})

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    // Clear validation error for this field on change
    if (validationErrors[name]) {
      setValidationErrors((prev) => ({ ...prev, [name]: '' }))
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setValidationErrors({})

    const result = registerSchema.safeParse(formData)
    if (!result.success) {
      const errors: Record<string, string> = {}
      result.error.errors.forEach((error) => {
        const path = error.path[0]
        if (path) {
          errors[path as string] = error.message
        }
      })
      setValidationErrors(errors)
      return
    }

    onSubmit({
      email: result.data.email,
      username: result.data.username,
      password: result.data.password,
      role: formData.role as UserRole,
    })
  }

  return (
    <form onSubmit={handleSubmit} className="auth-form">
      <div className="form-group">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          placeholder="your@email.com"
          disabled={isLoading}
          className={validationErrors.email ? 'error' : ''}
        />
        {validationErrors.email && (
          <span className="error-message">{validationErrors.email}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="username">Username</label>
        <input
          id="username"
          name="username"
          type="text"
          value={formData.username}
          onChange={handleChange}
          placeholder="choose a username"
          disabled={isLoading}
          className={validationErrors.username ? 'error' : ''}
        />
        {validationErrors.username && (
          <span className="error-message">{validationErrors.username}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          value={formData.password}
          onChange={handleChange}
          placeholder="••••••••"
          disabled={isLoading}
          className={validationErrors.password ? 'error' : ''}
        />
        {validationErrors.password && (
          <span className="error-message">{validationErrors.password}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="confirmPassword">Confirm Password</label>
        <input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          value={formData.confirmPassword}
          onChange={handleChange}
          placeholder="••••••••"
          disabled={isLoading}
          className={validationErrors.confirmPassword ? 'error' : ''}
        />
        {validationErrors.confirmPassword && (
          <span className="error-message">{validationErrors.confirmPassword}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="role">Account Type</label>
        <select
          id="role"
          name="role"
          value={formData.role}
          onChange={handleChange}
          disabled={isLoading}
        >
          <option value={UserRole.USER}>User</option>
          <option value={UserRole.ADMIN}>Admin</option>
        </select>
      </div>

      {error && <div className="error-alert">{error}</div>}

      <button type="submit" disabled={isLoading} className="submit-btn">
        {isLoading ? 'Registering...' : 'Register'}
      </button>
    </form>
  )
}
