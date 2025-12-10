/**
 * ScheduleXAccordion Component (T015)
 * 
 * Main Schedule X UI with collapsible Add-Backs vs Deductions sections (FR-031)
 * Displays all 27 Schedule X fields organized in accordion sections
 */

import React from 'react';
import CollapsibleAccordion, { AccordionSection } from '../shared/CollapsibleAccordion';
import ScheduleXFieldInput from './ScheduleXFieldInput';
import { BusinessScheduleXDetails } from '../../types/scheduleX';
import { formatCurrency } from '../../utils/scheduleXFormatting';
import {
  calculateTotalAddBacks,
  calculateTotalDeductions,
  calculateAdjustedMunicipalIncome,
} from '../../utils/scheduleXCalculations';

export interface ScheduleXAccordionProps {
  scheduleX: BusinessScheduleXDetails;
  onUpdate: (updated: BusinessScheduleXDetails) => void;
  entityType?: 'C-CORP' | 'PARTNERSHIP' | 'S-CORP';
  className?: string;
}

export const ScheduleXAccordion: React.FC<ScheduleXAccordionProps> = ({
  scheduleX,
  onUpdate,
  entityType = 'C-CORP',
  className = '',
}) => {
  const handleFieldChange = (section: 'addBacks' | 'deductions', fieldName: string, value: number) => {
    const updated = {
      ...scheduleX,
      [section]: {
        ...scheduleX[section],
        [fieldName]: value,
      },
    };
    onUpdate(updated);
  };

  // Calculate totals
  const totalAddBacks = calculateTotalAddBacks(scheduleX.addBacks);
  const totalDeductions = calculateTotalDeductions(scheduleX.deductions);
  const adjustedIncome = calculateAdjustedMunicipalIncome(
    scheduleX.fedTaxableIncome,
    totalAddBacks,
    totalDeductions
  );

  const sections: AccordionSection[] = [
    {
      id: 'add-backs',
      title: 'Add-Backs (Increase Federal Income)',
      subtitle: `${Object.keys(scheduleX.addBacks).filter(k => (scheduleX.addBacks as any)[k] > 0).length} adjustments`,
      badge: <span className="text-lg font-bold text-blue-600">{formatCurrency(totalAddBacks)}</span>,
      defaultExpanded: true,
      content: (
        <div className="space-y-3">
          {/* Depreciation & Amortization */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Depreciation & Amortization</h4>
            <ScheduleXFieldInput
              fieldName="depreciationAdjustment"
              label="Depreciation Adjustment (Book vs Tax)"
              value={scheduleX.addBacks.depreciationAdjustment}
              onChange={(v) => handleFieldChange('addBacks', 'depreciationAdjustment', v)}
              helpText="Add back if book depreciation < MACRS tax depreciation. Example: Book $80K, MACRS $130K → Add-back $50K"
            />
            <ScheduleXFieldInput
              fieldName="amortizationAdjustment"
              label="Amortization Adjustment (Intangibles)"
              value={scheduleX.addBacks.amortizationAdjustment}
              onChange={(v) => handleFieldChange('addBacks', 'amortizationAdjustment', v)}
              helpText="Difference between book and tax amortization for goodwill, patents, etc."
            />
            <ScheduleXFieldInput
              fieldName="section179Excess"
              label="Section 179 Excess"
              value={scheduleX.addBacks.section179Excess}
              onChange={(v) => handleFieldChange('addBacks', 'section179Excess', v)}
              helpText="Section 179 expensing exceeding municipal limits"
            />
            <ScheduleXFieldInput
              fieldName="bonusDepreciation"
              label="Bonus Depreciation"
              value={scheduleX.addBacks.bonusDepreciation}
              onChange={(v) => handleFieldChange('addBacks', 'bonusDepreciation', v)}
              helpText="100% bonus depreciation allowed federally but not municipally"
            />
          </div>

          {/* Taxes & State Adjustments */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Taxes & State Adjustments</h4>
            <ScheduleXFieldInput
              fieldName="incomeAndStateTaxes"
              label="State & Local Income Taxes"
              value={scheduleX.addBacks.incomeAndStateTaxes}
              onChange={(v) => handleFieldChange('addBacks', 'incomeAndStateTaxes', v)}
              helpText="State/local income taxes deducted federally (add back for municipal)"
            />
          </div>

          {/* Partnership-Specific */}
          {entityType === 'PARTNERSHIP' && (
            <div className="mb-4">
              <h4 className="font-semibold text-gray-700 mb-2">Partnership-Specific</h4>
              <ScheduleXFieldInput
                fieldName="guaranteedPayments"
                label="Guaranteed Payments to Partners"
                value={scheduleX.addBacks.guaranteedPayments}
                onChange={(v) => handleFieldChange('addBacks', 'guaranteedPayments', v)}
                helpText="Form 1065 Line 10 guaranteed payments (deductible federally, not municipally)"
              />
            </div>
          )}

          {/* Meals & Entertainment */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Meals & Entertainment</h4>
            <ScheduleXFieldInput
              fieldName="mealsAndEntertainment"
              label="Meals & Entertainment (100% Add-Back)"
              value={scheduleX.addBacks.mealsAndEntertainment}
              onChange={(v) => handleFieldChange('addBacks', 'mealsAndEntertainment', v)}
              helpText="Federal allows 50%, municipal allows 0%. Add back full amount."
              showAutoCalcButton
            />
          </div>

          {/* Related-Party & Officer Expenses */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Related-Party & Officer Expenses</h4>
            <ScheduleXFieldInput
              fieldName="relatedPartyExcess"
              label="Related-Party Excess Expenses"
              value={scheduleX.addBacks.relatedPartyExcess}
              onChange={(v) => handleFieldChange('addBacks', 'relatedPartyExcess', v)}
              helpText="Payments to related parties above fair market value. Example: Paid $10K rent, FMV $7.5K → Add-back $2.5K"
            />
            <ScheduleXFieldInput
              fieldName="officerLifeInsurance"
              label="Officer Life Insurance Premiums"
              value={scheduleX.addBacks.officerLifeInsurance}
              onChange={(v) => handleFieldChange('addBacks', 'officerLifeInsurance', v)}
              helpText="Life insurance premiums where corporation is beneficiary (non-deductible)"
            />
          </div>

          {/* Non-Deductible Expenses */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Non-Deductible Expenses</h4>
            <ScheduleXFieldInput
              fieldName="penaltiesAndFines"
              label="Penalties & Fines"
              value={scheduleX.addBacks.penaltiesAndFines}
              onChange={(v) => handleFieldChange('addBacks', 'penaltiesAndFines', v)}
              helpText="Government penalties/fines (already non-deductible federally)"
            />
            <ScheduleXFieldInput
              fieldName="politicalContributions"
              label="Political Contributions"
              value={scheduleX.addBacks.politicalContributions}
              onChange={(v) => handleFieldChange('addBacks', 'politicalContributions', v)}
              helpText="Political campaign contributions (non-deductible)"
            />
            <ScheduleXFieldInput
              fieldName="clubDues"
              label="Club Dues"
              value={scheduleX.addBacks.clubDues}
              onChange={(v) => handleFieldChange('addBacks', 'clubDues', v)}
              helpText="Non-deductible club dues and membership fees (social, athletic, or sporting clubs)"
            />
            <ScheduleXFieldInput
              fieldName="pensionProfitSharingLimits"
              label="Pension/Profit-Sharing Excess"
              value={scheduleX.addBacks.pensionProfitSharingLimits}
              onChange={(v) => handleFieldChange('addBacks', 'pensionProfitSharingLimits', v)}
              helpText="Pension/profit-sharing contributions exceeding IRS limits (IRC Section 404)"
            />
          </div>

          {/* Capital & Losses */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Capital & Losses</h4>
            <ScheduleXFieldInput
              fieldName="capitalLossExcess"
              label="Capital Loss Excess"
              value={scheduleX.addBacks.capitalLossExcess}
              onChange={(v) => handleFieldChange('addBacks', 'capitalLossExcess', v)}
              helpText="Capital losses exceeding capital gains (Form 1120 Line 8 carryforward)"
            />
          </div>

          {/* Intangible Income Expenses */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Intangible Income Expenses (5% Rule)</h4>
            <ScheduleXFieldInput
              fieldName="expensesOnIntangibleIncome"
              label="Expenses on Intangible Income (5% Rule)"
              value={scheduleX.addBacks.expensesOnIntangibleIncome}
              onChange={(v) => handleFieldChange('addBacks', 'expensesOnIntangibleIncome', v)}
              helpText="5% of non-taxable intangible income (interest + dividends + capital gains)"
              showAutoCalcButton
            />
          </div>

          {/* Other Adjustments */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Other Adjustments</h4>
            <ScheduleXFieldInput
              fieldName="federalTaxRefunds"
              label="Federal Tax Refunds"
              value={scheduleX.addBacks.federalTaxRefunds}
              onChange={(v) => handleFieldChange('addBacks', 'federalTaxRefunds', v)}
              helpText="Prior year federal income tax refunds included in income"
            />
            <ScheduleXFieldInput
              fieldName="badDebtReserveIncrease"
              label="Bad Debt Reserve Increase"
              value={scheduleX.addBacks.badDebtReserveIncrease}
              onChange={(v) => handleFieldChange('addBacks', 'badDebtReserveIncrease', v)}
              helpText="Increase in bad debt reserve (reserve method vs direct write-off)"
            />
            <ScheduleXFieldInput
              fieldName="charitableContributionExcess"
              label="Charitable Contribution Excess"
              value={scheduleX.addBacks.charitableContributionExcess}
              onChange={(v) => handleFieldChange('addBacks', 'charitableContributionExcess', v)}
              helpText="Contributions exceeding 10% limit (federal error, municipal follows 10% rule)"
            />
            <ScheduleXFieldInput
              fieldName="domesticProductionActivities"
              label="Domestic Production Activities Deduction (DPAD)"
              value={scheduleX.addBacks.domesticProductionActivities}
              onChange={(v) => handleFieldChange('addBacks', 'domesticProductionActivities', v)}
              helpText="Section 199 DPAD deduction (pre-TCJA, no longer applies for most)"
            />
            <ScheduleXFieldInput
              fieldName="stockCompensationAdjustment"
              label="Stock Compensation Adjustment"
              value={scheduleX.addBacks.stockCompensationAdjustment}
              onChange={(v) => handleFieldChange('addBacks', 'stockCompensationAdjustment', v)}
              helpText="Difference between book expense (ASC 718) and tax deduction"
            />
            <ScheduleXFieldInput
              fieldName="inventoryMethodChange"
              label="Inventory Method Change (Section 481(a))"
              value={scheduleX.addBacks.inventoryMethodChange}
              onChange={(v) => handleFieldChange('addBacks', 'inventoryMethodChange', v)}
              helpText="Section 481(a) adjustment for inventory method change (LIFO → FIFO)"
            />
            <ScheduleXFieldInput
              fieldName="otherAddBacks"
              label="Other Add-Backs"
              value={scheduleX.addBacks.otherAddBacks}
              onChange={(v) => handleFieldChange('addBacks', 'otherAddBacks', v)}
              helpText="Catch-all for adjustments not covered by specific fields (requires description)"
            />
          </div>

          <div className="pt-4 border-t border-gray-200">
            <div className="flex justify-between items-center text-lg font-semibold">
              <span>Total Add-Backs:</span>
              <span className="text-blue-600">{formatCurrency(totalAddBacks)}</span>
            </div>
          </div>
        </div>
      ),
    },
    {
      id: 'deductions',
      title: 'Deductions (Decrease Federal Income)',
      subtitle: `${Object.keys(scheduleX.deductions).filter(k => (scheduleX.deductions as any)[k] > 0).length} adjustments`,
      badge: <span className="text-lg font-bold text-green-600">{formatCurrency(totalDeductions)}</span>,
      defaultExpanded: true,
      content: (
        <div className="space-y-3">
          {/* Intangible Income (Non-Taxable) */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Intangible Income (Non-Taxable)</h4>
            <ScheduleXFieldInput
              fieldName="interestIncome"
              label="Interest Income"
              value={scheduleX.deductions.interestIncome}
              onChange={(v) => handleFieldChange('deductions', 'interestIncome', v)}
              helpText="Taxable interest income (non-taxable for municipal purposes). Includes interest from savings, bonds, CDs."
            />
            <ScheduleXFieldInput
              fieldName="dividends"
              label="Dividends"
              value={scheduleX.deductions.dividends}
              onChange={(v) => handleFieldChange('deductions', 'dividends', v)}
              helpText="Qualified and ordinary dividends (non-taxable municipally)"
            />
            <ScheduleXFieldInput
              fieldName="capitalGains"
              label="Net Capital Gains"
              value={scheduleX.deductions.capitalGains}
              onChange={(v) => handleFieldChange('deductions', 'capitalGains', v)}
              helpText="Net capital gains (non-taxable municipally)"
            />
          </div>

          {/* Other Deductions */}
          <div className="mb-4">
            <h4 className="font-semibold text-gray-700 mb-2">Other Deductions</h4>
            <ScheduleXFieldInput
              fieldName="section179Recapture"
              label="Section 179 Recapture"
              value={scheduleX.deductions.section179Recapture}
              onChange={(v) => handleFieldChange('deductions', 'section179Recapture', v)}
              helpText="Recapture of Section 179 deduction (if asset sold before recovery period)"
            />
            <ScheduleXFieldInput
              fieldName="municipalBondInterest"
              label="Municipal Bond Interest"
              value={scheduleX.deductions.municipalBondInterest}
              onChange={(v) => handleFieldChange('deductions', 'municipalBondInterest', v)}
              helpText="Municipal bond interest taxable at different jurisdiction (cross-jurisdiction bonds)"
            />
            <ScheduleXFieldInput
              fieldName="depletionDifference"
              label="Depletion Difference"
              value={scheduleX.deductions.depletionDifference}
              onChange={(v) => handleFieldChange('deductions', 'depletionDifference', v)}
              helpText="Percentage depletion (oil/gas) exceeding cost depletion"
            />
            <ScheduleXFieldInput
              fieldName="otherDeductions"
              label="Other Deductions"
              value={scheduleX.deductions.otherDeductions}
              onChange={(v) => handleFieldChange('deductions', 'otherDeductions', v)}
              helpText="Catch-all for deductions not covered by specific fields (requires description)"
            />
          </div>

          <div className="pt-4 border-t border-gray-200">
            <div className="flex justify-between items-center text-lg font-semibold">
              <span>Total Deductions:</span>
              <span className="text-green-600">{formatCurrency(totalDeductions)}</span>
            </div>
          </div>
        </div>
      ),
    },
  ];

  return (
    <div className={className}>
      <div className="mb-4 p-4 bg-gray-50 rounded-lg">
        <h3 className="text-lg font-semibold mb-2">Schedule X Summary</h3>
        <div className="space-y-2">
          <div className="flex justify-between">
            <span>Federal Taxable Income:</span>
            <span className="font-semibold">{formatCurrency(scheduleX.fedTaxableIncome)}</span>
          </div>
          <div className="flex justify-between text-blue-600">
            <span>+ Total Add-Backs:</span>
            <span className="font-semibold">{formatCurrency(totalAddBacks)}</span>
          </div>
          <div className="flex justify-between text-green-600">
            <span>- Total Deductions:</span>
            <span className="font-semibold">{formatCurrency(totalDeductions)}</span>
          </div>
          <div className="flex justify-between pt-2 border-t-2 border-gray-300 text-lg">
            <span className="font-bold">Adjusted Municipal Income:</span>
            <span className="font-bold text-purple-600">{formatCurrency(adjustedIncome)}</span>
          </div>
        </div>
      </div>
      
      <CollapsibleAccordion sections={sections} allowMultipleExpanded />
    </div>
  );
};

export default ScheduleXAccordion;
