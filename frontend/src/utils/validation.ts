/**
 * Validation Utilities
 * Compatibility and build validation functions
 */

import { Component, ComponentCategory } from '@/types/components'
import { CompatibilityIssue } from '@/types/builds'

/**
 * Validate build compatibility and return issues + warnings
 */
export function validateBuildCompatibility(
  components: Map<ComponentCategory, Component>
): CompatibilityIssue[] {
  const issues: CompatibilityIssue[] = []

  // Must have at least CPU and PSU
  if (!components.has(ComponentCategory.CPU)) {
    issues.push({
      type: 'error',
      message: 'CPU is required for a valid build',
      components: [ComponentCategory.CPU],
    })
  }

  if (!components.has(ComponentCategory.PSU)) {
    issues.push({
      type: 'error',
      message: 'PSU is required for a valid build',
      components: [ComponentCategory.PSU],
    })
  }

  // CPU + Motherboard socket compatibility
  const cpu = components.get(ComponentCategory.CPU)
  const motherboard = components.get(ComponentCategory.MOTHERBOARD)

  if (cpu && motherboard) {
    const cpuSocket = cpu.specs.socket as string
    const moboSocket = motherboard.specs.socket as string

    if (cpuSocket !== moboSocket) {
      issues.push({
        type: 'error',
        message: `CPU socket ${cpuSocket} doesn't match motherboard socket ${moboSocket}`,
        components: [ComponentCategory.CPU, ComponentCategory.MOTHERBOARD],
      })
    }
  }

  // RAM + Motherboard DDR type compatibility
  const ram = components.get(ComponentCategory.RAM)
  if (ram && motherboard) {
    const ramType = ram.specs.type as string
    // Check if motherboard supports this RAM type (inferred from socket/chipset)
    const moboSocket = motherboard.specs.socket as string
    const isLGA1700 = moboSocket === 'LGA1700'


    // AM5 and recent LGA1700 support DDR5, older DDR4
    if (isLGA1700 && ramType === 'DDR5') {
      // LGA1700 has limited DDR5 support on high-end boards
      issues.push({
        type: 'warning',
        message: 'DDR5 compatibility may be limited on some Z890 boards',
        components: [ComponentCategory.RAM, ComponentCategory.MOTHERBOARD],
      })
    }
  }

  // GPU power requirements vs PSU wattage
  const gpu = components.get(ComponentCategory.GPU)
  const psu = components.get(ComponentCategory.PSU)

  if (gpu && psu) {
    const gpuPower = (gpu.specs.powerRequirementW as number) || 0
    const cpuPower = (cpu?.specs.tdpW as number) || 0
    const sysPower = gpuPower + cpuPower + 100 // 100W base system

    const psuWattage = psu.specs.wattage as number

    if (sysPower > psuWattage * 0.85) {
      // PSU should only be used at 85% of rated capacity
      issues.push({
        type: 'error',
        message: `System power draw (${Math.round(sysPower)}W) exceeds safe PSU capacity (${Math.round(psuWattage * 0.85)}W)`,
        components: [ComponentCategory.GPU, ComponentCategory.PSU],
      })
    } else if (sysPower > psuWattage * 0.75) {
      issues.push({
        type: 'warning',
        message: `PSU is operating near capacity. Consider upgrading for stability.`,
        components: [ComponentCategory.GPU, ComponentCategory.PSU],
      })
    }
  }

  // Case size + Motherboard form factor
  const pcCase = components.get(ComponentCategory.CASE)
  if (pcCase && motherboard) {
    const moboFormFactor = motherboard.specs.formFactor as string
    const caseSize = pcCase.specs.supportedFormFactors as string | string[] | undefined

    const supportedFormFactors = Array.isArray(caseSize) ? caseSize : [caseSize]

    if (
      caseSize &&
      !supportedFormFactors.includes(moboFormFactor) &&
      moboFormFactor !== 'ITX'
    ) {
      issues.push({
        type: 'error',
        message: `Motherboard size ${moboFormFactor} may not fit in ${pcCase.name}`,
        components: [ComponentCategory.CASE, ComponentCategory.MOTHERBOARD],
      })
    }
  }

  // RAM capacity warning
  if (ram && motherboard) {
    const ramCapacity = ram.specs.capacity as string
    const maxMemory = motherboard.specs.maxMemory as number | string

    // Simple check - if max is less than 32GB and we're adding 64GB
    if (ramCapacity.includes('64GB') && maxMemory === '32GB') {
      issues.push({
        type: 'warning',
        message: 'RAM capacity exceeds motherboard maximum',
        components: [ComponentCategory.RAM, ComponentCategory.MOTHERBOARD],
      })
    }
  }

  return issues
}

/**
 * Calculate total power consumption of build
 */
export function calculateBuildPowerConsumption(
  components: Map<ComponentCategory, Component>
): number {
  let totalPower = 0

  const cpu = components.get(ComponentCategory.CPU)
  const gpu = components.get(ComponentCategory.GPU)

  if (cpu) {
    totalPower += (cpu.specs.tdpW as number) || 0
  }

  if (gpu) {
    totalPower += (gpu.specs.powerRequirementW as number) || 0
  }

  // Base system power (motherboard, RAM, storage, fans)
  totalPower += 100

  return totalPower
}

/**
 * Calculate total cost of build
 */
export function calculateBuildCost(
  components: Map<ComponentCategory, Component>
): number {
  let totalCost = 0

  components.forEach((component) => {
    totalCost += component.price
  })

  return totalCost
}

/**
 * Format wattage with proper unit
 */
export function formatWattage(watts: number): string {
  if (watts >= 1000) {
    return `${(watts / 1000).toFixed(1)}kW`
  }
  return `${watts}W`
}

/**
 * Format currency
 */
export function formatCurrency(amount: number): string {
  return `$${amount.toLocaleString('en-US', { maximumFractionDigits: 2 })}`
}
