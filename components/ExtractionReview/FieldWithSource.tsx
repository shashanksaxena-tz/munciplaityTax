import React from 'react';
import { TaxFormData, FormProvenance, TaxFormType } from '../../types';
import { Eye, MapPin, Gauge, AlertTriangle } from 'lucide-react';

interface FieldWithSourceProps {
  form: TaxFormData;
  formProvenance?: FormProvenance;
  highlightedField?: string;
  onFieldClick: (fieldName: string) => void;
  onClearHighlight: () => void;
}

// Define which fields to display for each form type
const FORM_DISPLAY_FIELDS: Record<string, { label: string; key: string; format?: 'currency' | 'percentage' | 'text' }[]> = {
  [TaxFormType.W2]: [
    { label: 'Employer', key: 'employer' },
    { label: 'Employer EIN', key: 'employerEin' },
    { label: 'Federal Wages (Box 1)', key: 'federalWages', format: 'currency' },
    { label: 'Medicare Wages (Box 5)', key: 'medicareWages', format: 'currency' },
    { label: 'Local Wages (Box 18)', key: 'localWages', format: 'currency' },
    { label: 'Local Tax Withheld (Box 19)', key: 'localWithheld', format: 'currency' },
    { label: 'Locality (Box 20)', key: 'locality' },
  ],
  [TaxFormType.FEDERAL_1040]: [
    { label: 'Total Wages (Line 1z)', key: 'wages', format: 'currency' },
    { label: 'Qualified Dividends (Line 3a)', key: 'qualifiedDividends', format: 'currency' },
    { label: 'Capital Gains (Line 7)', key: 'capitalGains', format: 'currency' },
    { label: 'Total Income (Line 9)', key: 'totalIncome', format: 'currency' },
    { label: 'Adjusted Gross Income (Line 11)', key: 'adjustedGrossIncome', format: 'currency' },
    { label: 'Total Tax (Line 24)', key: 'totalTax', format: 'currency' },
  ],
  [TaxFormType.FORM_1099_NEC]: [
    { label: 'Payer', key: 'payer' },
    { label: 'Income Amount', key: 'incomeAmount', format: 'currency' },
    { label: 'Federal Withheld', key: 'federalWithheld', format: 'currency' },
    { label: 'Locality', key: 'locality' },
  ],
  [TaxFormType.FORM_1099_MISC]: [
    { label: 'Payer', key: 'payer' },
    { label: 'Income Amount', key: 'incomeAmount', format: 'currency' },
    { label: 'Federal Withheld', key: 'federalWithheld', format: 'currency' },
    { label: 'State Withheld', key: 'stateWithheld', format: 'currency' },
  ],
  [TaxFormType.SCHEDULE_C]: [
    { label: 'Business Name', key: 'businessName' },
    { label: 'Business EIN', key: 'businessEin' },
    { label: 'Gross Receipts', key: 'grossReceipts', format: 'currency' },
    { label: 'Total Expenses', key: 'totalExpenses', format: 'currency' },
    { label: 'Net Profit', key: 'netProfit', format: 'currency' },
  ],
  [TaxFormType.SCHEDULE_E]: [
    { label: 'Total Net Income', key: 'totalNetIncome', format: 'currency' },
  ],
  [TaxFormType.W2G]: [
    { label: 'Payer', key: 'payer' },
    { label: 'Gross Winnings', key: 'grossWinnings', format: 'currency' },
    { label: 'Date Won', key: 'dateWon' },
    { label: 'Type of Wager', key: 'typeOfWager' },
    { label: 'Federal Withheld', key: 'federalWithheld', format: 'currency' },
  ],
};

// Properly convert camelCase to Title Case, preserving acronyms
const camelToTitle = (camelCase: string): string => {
  // Handle common acronyms
  const acronyms: Record<string, string> = {
    'ein': 'EIN',
    'ssn': 'SSN',
    'tin': 'TIN',
    'agi': 'AGI',
    'nol': 'NOL',
  };
  
  // Split on capital letters and numbers
  const words = camelCase.replace(/([A-Z])/g, ' $1').split(' ');
  
  return words
    .map(word => {
      const lower = word.toLowerCase();
      return acronyms[lower] || (word.charAt(0).toUpperCase() + word.slice(1));
    })
    .join(' ')
    .trim();
};

export const FieldWithSource: React.FC<FieldWithSourceProps> = ({
  form,
  formProvenance,
  highlightedField,
  onFieldClick,
  onClearHighlight
}) => {
  // Get display fields for this form type
  const displayFields = FORM_DISPLAY_FIELDS[form.formType] || [];
  
  // Fallback: show all numeric and string values if no predefined fields
  const formData = form as Record<string, any>;
  const fieldsToShow = displayFields.length > 0 
    ? displayFields 
    : Object.entries(formData)
        .filter(([key, value]) => 
          !['id', 'fileName', 'formType', 'taxYear', 'confidenceScore', 'fieldConfidence', 'sourcePage', 'extractionReason', 'owner'].includes(key) &&
          (typeof value === 'string' || typeof value === 'number')
        )
        .map(([key, value]) => ({
          label: camelToTitle(key),
          key,
          format: typeof value === 'number' && value > 100 ? 'currency' : 'text'
        }));

  const formatValue = (value: any, format?: string): string => {
    if (value === undefined || value === null) return '—';
    if (format === 'currency' && typeof value === 'number') {
      return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
    }
    if (format === 'percentage' && typeof value === 'number') {
      return `${(value * 100).toFixed(1)}%`;
    }
    return String(value);
  };

  const getFieldConfidence = (fieldKey: string): number | undefined => {
    const fieldConf = form.fieldConfidence?.[fieldKey];
    if (typeof fieldConf === 'number') return fieldConf;
    return formProvenance?.fields.find(f => f.fieldName === fieldKey)?.confidence;
  };

  const getFieldProvenance = (fieldKey: string) => {
    return formProvenance?.fields.find(f => f.fieldName === fieldKey);
  };

  const hasSource = (fieldKey: string): boolean => {
    const prov = getFieldProvenance(fieldKey);
    return !!(prov?.boundingBox || prov?.pageNumber);
  };

  return (
    <div className="space-y-2">
      {fieldsToShow.map(({ label, key, format }) => {
        const value = formData[key];
        const confidence = getFieldConfidence(key);
        const prov = getFieldProvenance(key);
        const isHighlighted = highlightedField === key;
        const isLowConfidence = confidence !== undefined && confidence < 0.7;
        const showSourceButton = hasSource(key) || formProvenance?.pageNumber;

        return (
          <div 
            key={key}
            className={`flex items-center justify-between py-2 px-3 rounded-lg transition-colors ${
              isHighlighted 
                ? 'bg-indigo-50 border border-indigo-200' 
                : 'hover:bg-slate-50'
            }`}
          >
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className="text-sm text-slate-600">{label}</span>
                {isLowConfidence && (
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-500" title="Low confidence - verify manually" />
                )}
              </div>
              <div className={`text-base font-semibold ${isLowConfidence ? 'text-amber-700' : 'text-slate-800'}`}>
                {formatValue(value, format)}
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* Confidence badge */}
              {confidence !== undefined && (
                <div className={`flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full ${
                  confidence >= 0.9 
                    ? 'bg-green-100 text-green-700' 
                    : confidence >= 0.7 
                      ? 'bg-amber-100 text-amber-700'
                      : 'bg-red-100 text-red-700'
                }`}>
                  <Gauge className="w-3 h-3" />
                  {Math.round(confidence * 100)}%
                </div>
              )}

              {/* Page indicator */}
              {prov?.pageNumber && (
                <div className="flex items-center gap-1 px-2 py-0.5 text-xs text-slate-500 bg-slate-100 rounded-full">
                  <MapPin className="w-3 h-3" />
                  P{prov.pageNumber}
                </div>
              )}

              {/* Show source button */}
              {showSourceButton && (
                <button
                  onClick={() => isHighlighted ? onClearHighlight() : onFieldClick(key)}
                  className={`flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-lg transition-colors ${
                    isHighlighted
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-indigo-100 hover:text-indigo-600'
                  }`}
                  title={isHighlighted ? 'Clear highlight' : 'Show source in PDF'}
                >
                  <Eye className="w-3.5 h-3.5" />
                  {isHighlighted ? 'Hide' : 'Source'}
                </button>
              )}
            </div>
          </div>
        );
      })}

      {/* Special handling for Schedule E rentals and partnerships */}
      {form.formType === TaxFormType.SCHEDULE_E && (
        <>
          {(form as any).rentals?.length > 0 && (
            <div className="mt-4 pt-4 border-t border-slate-200">
              <h4 className="text-sm font-semibold text-slate-700 mb-2">Rental Properties</h4>
              {(form as any).rentals.map((rental: any, idx: number) => (
                <div key={idx} className="ml-4 py-2 text-sm">
                  <div className="font-medium">{rental.address || `Property ${idx + 1}`}</div>
                  <div className="text-slate-500">Type: {rental.propertyType || 'N/A'}</div>
                </div>
              ))}
            </div>
          )}
          {(form as any).partnerships?.length > 0 && (
            <div className="mt-4 pt-4 border-t border-slate-200">
              <h4 className="text-sm font-semibold text-slate-700 mb-2">Partnerships/S-Corps</h4>
              {(form as any).partnerships.map((partnership: any, idx: number) => (
                <div key={idx} className="ml-4 py-2 text-sm">
                  <div className="font-medium">{partnership.entityName || `Entity ${idx + 1}`}</div>
                  <div className="text-slate-500">EIN: {partnership.ein || 'N/A'}</div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default FieldWithSource;
