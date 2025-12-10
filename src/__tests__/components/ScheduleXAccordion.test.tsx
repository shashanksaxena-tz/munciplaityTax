/**
 * Component tests for ScheduleXAccordion
 * Tests rendering of all 29 fields and conditional rendering for entity types
 * 
 * Feature: Comprehensive Business Schedule X Reconciliation (29 Fields)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ScheduleXAccordion } from '../../components/business/ScheduleXAccordion';
import { createEmptyScheduleXDetails } from '../../types/scheduleX';

describe('ScheduleXAccordion Component', () => {
  
  it('should render all add-back field categories', () => {
    // Arrange
    const scheduleX = createEmptyScheduleXDetails(500000);
    const mockOnUpdate = vi.fn();

    // Act
    render(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="C-CORP"
      />
    );

    // Assert - Check for category headers (allowing for multiple matches)
    expect(screen.getByText(/Depreciation & Amortization/i)).toBeInTheDocument();
    expect(screen.getByText(/Taxes & State Adjustments/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Meals & Entertainment/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Related-Party & Officer Expenses/i)).toBeInTheDocument();
    expect(screen.getByText(/Non-Deductible Expenses/i)).toBeInTheDocument();
    expect(screen.getByText(/Capital & Losses/i)).toBeInTheDocument();
    expect(screen.getByText(/Intangible Income Expenses/i)).toBeInTheDocument();
    expect(screen.getByText(/Other Adjustments/i)).toBeInTheDocument();
  });

  it('should render deduction field categories', () => {
    // Arrange
    const scheduleX = createEmptyScheduleXDetails(500000);
    const mockOnUpdate = vi.fn();

    // Act
    render(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="C-CORP"
      />
    );

    // Assert - Check for deduction categories (using more specific text)
    expect(screen.getByText(/Intangible Income \(Non-Taxable\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Deductions \(Decrease Federal Income\)/i)).toBeInTheDocument();
  });

  it('should show guaranteed payments for partnerships only', () => {
    // Arrange
    const scheduleX = createEmptyScheduleXDetails(300000);
    const mockOnUpdate = vi.fn();

    // Act - Render for partnership
    const { rerender } = render(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="PARTNERSHIP"
      />
    );

    // Assert - Guaranteed payments should be visible for partnerships
    expect(screen.getByText(/Guaranteed Payments to Partners/i)).toBeInTheDocument();

    // Act - Re-render for C-Corp
    rerender(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="C-CORP"
      />
    );

    // Assert - Guaranteed payments should NOT be visible for C-Corps
    expect(screen.queryByText(/Guaranteed Payments to Partners/i)).not.toBeInTheDocument();
  });

  it('should display calculated totals correctly', () => {
    // Arrange - Schedule X with add-backs $75K, deductions $10K
    const scheduleX = createEmptyScheduleXDetails(500000);
    scheduleX.addBacks.depreciationAdjustment = 50000;
    scheduleX.addBacks.mealsAndEntertainment = 15000;
    scheduleX.addBacks.incomeAndStateTaxes = 10000;
    scheduleX.deductions.interestIncome = 5000;
    scheduleX.deductions.dividends = 3000;
    scheduleX.deductions.capitalGains = 2000;
    
    // Recalculate totals
    scheduleX.calculatedFields.totalAddBacks = 75000;
    scheduleX.calculatedFields.totalDeductions = 10000;
    scheduleX.calculatedFields.adjustedMunicipalIncome = 565000;

    const mockOnUpdate = vi.fn();

    // Act
    render(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="C-CORP"
      />
    );

    // Assert - Check for calculated totals in summary
    expect(screen.getByText('$500,000.00')).toBeInTheDocument(); // Federal income (unique)
    expect(screen.getByText('$565,000.00')).toBeInTheDocument(); // Adjusted municipal income (unique)
    
    // Check that totals exist (they appear in multiple places)
    const allAddBackAmounts = screen.getAllByText('$75,000.00');
    expect(allAddBackAmounts.length).toBeGreaterThan(0); // Total add-backs in summary and accordion
    
    const allDeductionAmounts = screen.getAllByText('$10,000.00');
    expect(allDeductionAmounts.length).toBeGreaterThan(0); // Total deductions in summary and accordion
  });

  it('should display Schedule X summary with correct labels', () => {
    // Arrange
    const scheduleX = createEmptyScheduleXDetails(500000);
    const mockOnUpdate = vi.fn();

    // Act
    render(
      <ScheduleXAccordion
        scheduleX={scheduleX}
        onUpdate={mockOnUpdate}
        entityType="C-CORP"
      />
    );

    // Assert - Check for summary labels
    expect(screen.getByText(/Federal Taxable Income/i)).toBeInTheDocument();
    expect(screen.getByText(/\+ Total Add-Backs/i)).toBeInTheDocument();
    expect(screen.getByText(/- Total Deductions/i)).toBeInTheDocument();
    expect(screen.getByText(/Adjusted Municipal Income/i)).toBeInTheDocument();
  });
});
