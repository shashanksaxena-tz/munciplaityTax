import React, { useState } from 'react';
import { SkippedForm } from '../../types';
import { AlertTriangle, ChevronDown, ChevronUp, FileQuestion, HelpCircle, RefreshCw, Upload, Edit } from 'lucide-react';

interface ExtractionFailuresProps {
  skippedForms: SkippedForm[];
  onRetry?: (pageNumber: number) => void;
  onManualEntry?: (formType: string) => void;
}

// Categorize failure reasons
type FailureCategory = 'quality' | 'unsupported' | 'blank' | 'partial' | 'other';

const categorizeReason = (reason: string): FailureCategory => {
  const lowerReason = reason.toLowerCase();
  if (lowerReason.includes('blank') || lowerReason.includes('empty')) return 'blank';
  if (lowerReason.includes('quality') || lowerReason.includes('illegible') || lowerReason.includes('blurry')) return 'quality';
  if (lowerReason.includes('unsupported') || lowerReason.includes('unrecognized') || lowerReason.includes('unknown')) return 'unsupported';
  if (lowerReason.includes('partial') || lowerReason.includes('incomplete') || lowerReason.includes('obscured')) return 'partial';
  return 'other';
};

const getCategoryIcon = (category: FailureCategory) => {
  switch (category) {
    case 'blank': return <FileQuestion className="w-4 h-4" />;
    case 'quality': return <AlertTriangle className="w-4 h-4" />;
    case 'unsupported': return <HelpCircle className="w-4 h-4" />;
    case 'partial': return <Edit className="w-4 h-4" />;
    default: return <AlertTriangle className="w-4 h-4" />;
  }
};

const getCategoryColor = (category: FailureCategory) => {
  switch (category) {
    case 'blank': return 'text-slate-500 bg-slate-100';
    case 'quality': return 'text-amber-600 bg-amber-50';
    case 'unsupported': return 'text-blue-600 bg-blue-50';
    case 'partial': return 'text-orange-600 bg-orange-50';
    default: return 'text-red-600 bg-red-50';
  }
};

const getCategoryLabel = (category: FailureCategory) => {
  switch (category) {
    case 'blank': return 'Blank/Empty Page';
    case 'quality': return 'Image Quality Issue';
    case 'unsupported': return 'Unsupported Form';
    case 'partial': return 'Partial Extraction';
    default: return 'Extraction Failed';
  }
};

export const ExtractionFailures: React.FC<ExtractionFailuresProps> = ({
  skippedForms,
  onRetry,
  onManualEntry
}) => {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!skippedForms || skippedForms.length === 0) {
    return null;
  }

  // Group by category
  const groupedFailures = skippedForms.reduce((acc, form) => {
    const category = categorizeReason(form.reason);
    if (!acc[category]) acc[category] = [];
    acc[category].push(form);
    return acc;
  }, {} as Record<FailureCategory, SkippedForm[]>);

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-xl overflow-hidden">
      {/* Header */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-amber-100 transition-colors"
      >
        <div className="flex items-center gap-2">
          <AlertTriangle className="w-5 h-5 text-amber-600" />
          <span className="font-semibold text-amber-800">
            {skippedForms.length} Page{skippedForms.length !== 1 ? 's' : ''} Could Not Be Extracted
          </span>
        </div>
        {isExpanded ? (
          <ChevronUp className="w-5 h-5 text-amber-600" />
        ) : (
          <ChevronDown className="w-5 h-5 text-amber-600" />
        )}
      </button>

      {/* Content */}
      {isExpanded && (
        <div className="px-4 pb-4 space-y-3">
          {/* Summary by category */}
          <div className="flex flex-wrap gap-2">
            {Object.entries(groupedFailures).map(([category, forms]) => (
              <div 
                key={category}
                className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium ${getCategoryColor(category as FailureCategory)}`}
              >
                {getCategoryIcon(category as FailureCategory)}
                <span>{forms.length} {getCategoryLabel(category as FailureCategory)}</span>
              </div>
            ))}
          </div>

          {/* Detailed list */}
          <div className="space-y-2">
            {skippedForms.map((form, idx) => {
              const category = categorizeReason(form.reason);
              
              return (
                <div 
                  key={idx}
                  className="flex items-start gap-3 p-3 bg-white rounded-lg border border-amber-100"
                >
                  <div className={`p-1.5 rounded-lg ${getCategoryColor(category)}`}>
                    {getCategoryIcon(category)}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-800">
                        Page {form.pageNumber}
                      </span>
                      {form.formType && form.formType !== 'Unknown' && (
                        <span className="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                          {form.formType}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-slate-600 mt-0.5">{form.reason}</p>
                    
                    {/* Suggestion */}
                    {form.suggestion && (
                      <p className="text-xs text-amber-700 mt-1 flex items-start gap-1">
                        <span className="font-medium">💡</span>
                        {form.suggestion}
                      </p>
                    )}

                    {/* Action buttons */}
                    <div className="flex gap-2 mt-2">
                      {onRetry && category !== 'blank' && (
                        <button
                          onClick={() => onRetry(form.pageNumber)}
                          className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition-colors"
                        >
                          <RefreshCw className="w-3 h-3" />
                          Retry
                        </button>
                      )}
                      {category === 'quality' && (
                        <button
                          className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-indigo-600 bg-indigo-50 rounded-lg hover:bg-indigo-100 transition-colors"
                        >
                          <Upload className="w-3 h-3" />
                          Upload Better Scan
                        </button>
                      )}
                      {onManualEntry && category !== 'blank' && (
                        <button
                          onClick={() => onManualEntry(form.formType || 'Unknown')}
                          className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-amber-600 bg-amber-100 rounded-lg hover:bg-amber-200 transition-colors"
                        >
                          <Edit className="w-3 h-3" />
                          Enter Manually
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Help text */}
          <div className="text-xs text-amber-700 bg-amber-100/50 p-3 rounded-lg">
            <strong>Need help?</strong> If pages consistently fail to extract:
            <ul className="list-disc ml-4 mt-1 space-y-0.5">
              <li>Ensure documents are scanned at 300 DPI or higher</li>
              <li>Check that all text is clearly visible and not cut off</li>
              <li>Try scanning in black and white for better OCR accuracy</li>
              <li>Split large documents into smaller files (under 20 pages)</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExtractionFailures;
