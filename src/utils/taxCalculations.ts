/**
 * Calculate tax amount based on income and rate
 */
export function calculateTaxAmount(income: number, rate: number): number {
  if (income <= 0) return 0;
  return Math.round(income * rate * 100) / 100;
}

/**
 * Calculate withholding amount
 */
export function calculateWithholding(wages: number, rate: number): number {
  if (wages <= 0) return 0;
  return Math.round(wages * rate * 100) / 100;
}

/**
 * Validate tax data
 */
export function validateTaxData(data: any): boolean {
  if (!data) return false;
  
  // Check required fields
  if (!data.taxYear || !data.income || data.withholding === undefined || !data.taxRate) {
    return false;
  }
  
  // Validate tax year
  const currentYear = new Date().getFullYear();
  if (data.taxYear < 2000 || data.taxYear > currentYear + 1) {
    return false;
  }
  
  // Validate positive values
  if (data.income < 0 || data.withholding < 0 || data.taxRate < 0) {
    return false;
  }
  
  return true;
}
