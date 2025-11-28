import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface ApportionmentBreakdown {
  propertyFactorPercentage: number;
  propertyFactorWeight: number;
  propertyFactorWeightedContribution: number;
  payrollFactorPercentage: number;
  payrollFactorWeight: number;
  payrollFactorWeightedContribution: number;
  salesFactorPercentage: number;
  salesFactorWeight: number;
  salesFactorWeightedContribution: number;
  totalWeight: number;
  finalApportionmentPercentage: number;
}

export interface ApportionmentCalculationRequest {
  propertyFactorPercentage?: number;
  payrollFactorPercentage?: number;
  salesFactorPercentage: number;
  formula: 'FOUR_FACTOR_DOUBLE_SALES' | 'TRADITIONAL_THREE_FACTOR' | 'SINGLE_SALES_FACTOR';
}

export interface FormulaComparisonRequest {
  propertyFactorPercentage?: number;
  payrollFactorPercentage?: number;
  salesFactorPercentage: number;
  traditionalFormula: 'FOUR_FACTOR_DOUBLE_SALES' | 'TRADITIONAL_THREE_FACTOR';
}

export interface FormulaComparisonResult {
  traditionalApportionment: number;
  singleSalesApportionment: number;
  recommendation: 'FOUR_FACTOR_DOUBLE_SALES' | 'TRADITIONAL_THREE_FACTOR' | 'SINGLE_SALES_FACTOR';
  difference: number;
}

/**
 * Service for apportionment calculation operations.
 * Provides methods for calculating apportionment percentages and comparing formula options.
 */
class ApportionmentService {
  /**
   * Calculate apportionment percentage with detailed breakdown.
   * POST /api/apportionment/calculate
   *
   * @param request Apportionment calculation request
   * @returns Apportionment breakdown with factor contributions
   */
  async calculateApportionment(
    request: ApportionmentCalculationRequest
  ): Promise<ApportionmentBreakdown> {
    try {
      const response = await axios.post<ApportionmentBreakdown>(
        `${API_BASE_URL}/api/apportionment/calculate`,
        request
      );
      return response.data;
    } catch (error) {
      console.error('Error calculating apportionment:', error);
      throw error;
    }
  }

  /**
   * Compare traditional formula vs single-sales-factor formula.
   * POST /api/apportionment/compare
   *
   * @param request Formula comparison request
   * @returns Comparison results with recommendation
   */
  async compareFormulas(
    request: FormulaComparisonRequest
  ): Promise<FormulaComparisonResult> {
    try {
      const response = await axios.post<FormulaComparisonResult>(
        `${API_BASE_URL}/api/apportionment/compare`,
        request
      );
      return response.data;
    } catch (error) {
      console.error('Error comparing formulas:', error);
      throw error;
    }
  }

  /**
   * Get apportionment breakdown for a filed Schedule Y.
   * GET /api/schedule-y/{id}/breakdown
   *
   * @param scheduleYId Schedule Y ID
   * @returns Apportionment breakdown
   */
  async getScheduleYBreakdown(scheduleYId: string): Promise<ApportionmentBreakdown> {
    try {
      const response = await axios.get<ApportionmentBreakdown>(
        `${API_BASE_URL}/api/schedule-y/${scheduleYId}/breakdown`
      );
      return response.data;
    } catch (error) {
      console.error('Error getting Schedule Y breakdown:', error);
      throw error;
    }
  }

  /**
   * Calculate property factor percentage.
   *
   * @param ohioProperty Ohio property value
   * @param totalProperty Total property value
   * @returns Property factor percentage (0-100)
   */
  calculatePropertyFactorPercentage(ohioProperty: number, totalProperty: number): number {
    if (totalProperty === 0) {
      return 0;
    }
    return (ohioProperty / totalProperty) * 100;
  }

  /**
   * Calculate payroll factor percentage.
   *
   * @param ohioPayroll Ohio payroll value
   * @param totalPayroll Total payroll value
   * @returns Payroll factor percentage (0-100)
   */
  calculatePayrollFactorPercentage(ohioPayroll: number, totalPayroll: number): number {
    if (totalPayroll === 0) {
      return 0;
    }
    return (ohioPayroll / totalPayroll) * 100;
  }

  /**
   * Calculate sales factor percentage.
   *
   * @param ohioSales Ohio sales value
   * @param totalSales Total sales value
   * @returns Sales factor percentage (0-100)
   */
  calculateSalesFactorPercentage(ohioSales: number, totalSales: number): number {
    if (totalSales === 0) {
      return 0;
    }
    return (ohioSales / totalSales) * 100;
  }

  /**
   * Format percentage for display.
   *
   * @param percentage Percentage value
   * @param decimals Number of decimal places (default 2)
   * @returns Formatted percentage string
   */
  formatPercentage(percentage: number, decimals: number = 2): string {
    return `${percentage.toFixed(decimals)}%`;
  }

  /**
   * Validate factor percentage is within valid range (0-100).
   *
   * @param percentage Percentage to validate
   * @param fieldName Field name for error message
   * @throws Error if percentage is invalid
   */
  validateFactorPercentage(percentage: number, fieldName: string): void {
    if (percentage < 0 || percentage > 100) {
      throw new Error(`${fieldName} must be between 0 and 100, got: ${percentage}`);
    }
  }
}

export const apportionmentService = new ApportionmentService();
export default apportionmentService;
