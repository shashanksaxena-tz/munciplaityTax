import React, { useState, useEffect } from 'react';
import { TaxFormData, FormProvenance, TaxFormType } from '../../types';
import { Eye, MapPin, Gauge, AlertTriangle } from 'lucide-react';
import { getDisplayFields } from '../../services/formSchemaService';

interface FieldWithSourceProps {
  form: TaxFormData;
  formProvenance?: FormProvenance;
  highlightedField?: string;
  onFieldClick: (fieldName: string) => void;
  onClearHighlight: () => void;
}

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
  const [displayFields, setDisplayFields] = useState<Array<{ label: string; key: string; format?: 'currency' | 'percentage' | 'text' }>>([]);
  const [isLoadingSchema, setIsLoadingSchema] = useState(true);

  // Load display fields from schema
  useEffect(() => {
    async function loadFields() {
      console.log(`[FieldWithSource] Loading fields for form type: "${form.formType}"`);
      setIsLoadingSchema(true);
      try {
        const fields = await getDisplayFields(form.formType);
        console.log(`[FieldWithSource] Loaded ${fields.length} fields for "${form.formType}":`, fields);
        setDisplayFields(fields);
      } catch (error) {
        console.error(`[FieldWithSource] Error loading form schema for "${form.formType}":`, error);
        setDisplayFields([]);
      } finally {
        setIsLoadingSchema(false);
      }
    }
    loadFields();
  }, [form.formType]);

  // Fallback: show all numeric and string values if no schema fields loaded
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

  if (isLoadingSchema) {
    return (
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-8 text-center">
        <div className="animate-pulse text-gray-400">Loading form schema...</div>
      </div>
    );
  }

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
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-500" aria-label="Low confidence - verify manually" />
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
