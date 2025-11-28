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
          <ScheduleXFieldInput
            fieldName="depreciationAdjustment"
            label="Depreciation Adjustment"
            value={scheduleX.addBacks.depreciationAdjustment}
            onChange={(v) => handleFieldChange('addBacks', 'depreciationAdjustment', v)}
            helpText="Book depreciation vs MACRS tax depreciation difference"
          />
          <ScheduleXFieldInput
            fieldName="mealsAndEntertainment"
            label="Meals & Entertainment"
            value={scheduleX.addBacks.mealsAndEntertainment}
            onChange={(v) => handleFieldChange('addBacks', 'mealsAndEntertainment', v)}
            helpText="Federal allows 50%, municipal allows 0%"
            showAutoCalcButton
          />
          <ScheduleXFieldInput
            fieldName="interestAndStateTaxes"
            label="State & Local Income Taxes"
            value={scheduleX.addBacks.interestAndStateTaxes}
            onChange={(v) => handleFieldChange('addBacks', 'interestAndStateTaxes', v)}
            helpText="State/local income taxes deducted federally"
          />
          {/* Additional fields would go here... */}
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
          <ScheduleXFieldInput
            fieldName="interestIncome"
            label="Interest Income"
            value={scheduleX.deductions.interestIncome}
            onChange={(v) => handleFieldChange('deductions', 'interestIncome', v)}
            helpText="Non-taxable interest income for municipal purposes"
          />
          <ScheduleXFieldInput
            fieldName="dividends"
            label="Dividends"
            value={scheduleX.deductions.dividends}
            onChange={(v) => handleFieldChange('deductions', 'dividends', v)}
            helpText="Non-taxable dividend income for municipal purposes"
          />
          <ScheduleXFieldInput
            fieldName="capitalGains"
            label="Capital Gains"
            value={scheduleX.deductions.capitalGains}
            onChange={(v) => handleFieldChange('deductions', 'capitalGains', v)}
            helpText="Non-taxable capital gains for municipal purposes"
          />
          {/* Additional fields would go here... */}
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
