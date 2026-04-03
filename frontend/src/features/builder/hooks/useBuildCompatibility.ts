/**
 * useBuildCompatibility Hook
 * Custom hook for checking build compatibility
 */

import { useMemo } from 'react'
import { Component, ComponentCategory } from '@/types'
import { validateBuildCompatibility } from '@/utils/validation'

export function useBuildCompatibility(
  components: Map<ComponentCategory, Component>
) {
  const issues = useMemo(
    () => validateBuildCompatibility(components),
    [components]
  )

  const errors = useMemo(() => issues.filter((i) => i.type === 'error'), [issues])

  const warnings = useMemo(() => issues.filter((i) => i.type === 'warning'), [issues])

  const isCompatible = useMemo(
    () =>
      components.has(ComponentCategory.CPU) &&
      components.has(ComponentCategory.PSU) &&
      errors.length === 0,
    [components, errors]
  )

  return {
    warnings,
    errors,
    issues,
    isCompatible,
    hasIssues: issues.length > 0,
    hasErrors: errors.length > 0,
    hasWarnings: warnings.length > 0,
  }
}
