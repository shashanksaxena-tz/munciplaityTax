
import React, { useState } from 'react';
import { TaxFormData, TaxFormType, ExtractionSummary as ExtractionSummaryType, FormProvenance, FieldConfidenceInfo } from '../types';
import { FileText, CheckCircle, AlertTriangle, ArrowRight, FilePlus, Clock, Gauge, Eye, MapPin, Info, SplitSquareHorizontal, X } from 'lucide-react';
import { SplitViewLayout } from './ExtractionReview';

interface ExtractionSummaryProps {
  forms: TaxFormData[];
  summary?: ExtractionSummaryType;
  formProvenances?: FormProvenance[];
  pdfData?: string; // Base64 PDF data for split view
  onConfirm: () => void;
  onCancel: () => void;
}

export const ExtractionSummary: React.FC<ExtractionSummaryProps> = ({ 
  forms, 
  summary, 
  formProvenances,
  pdfData,
  onConfirm, 
  onCancel 
}) => {
  const [showSplitView, setShowSplitView] = useState(false);
  
  const counts = forms.reduce((acc, form) => {
    acc[form.formType] = (acc[form.formType] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.9) return 'text-green-600 bg-green-50 border-green-200';
    if (confidence >= 0.7) return 'text-amber-600 bg-amber-50 border-amber-200';
    return 'text-red-600 bg-red-50 border-red-200';
  };

  const getConfidenceLabel = (confidence: number) => {
    if (confidence >= 0.9) return 'High Confidence';
    if (confidence >= 0.7) return 'Needs Review';
    return 'Low Confidence';
  };

  const getWeightBadge = (weight: string) => {
    const colors: Record<string, string> = {
      CRITICAL: 'bg-red-100 text-red-700 border-red-200',
      HIGH: 'bg-orange-100 text-orange-700 border-orange-200',
      MEDIUM: 'bg-blue-100 text-blue-700 border-blue-200',
      LOW: 'bg-slate-100 text-slate-600 border-slate-200'
    };
    return colors[weight] || colors.MEDIUM;
  };

  // Split View Modal
  if (showSplitView && pdfData) {
    return (
      <div className="fixed inset-0 z-50 bg-slate-900/80 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-[95vw] h-[90vh] flex flex-col overflow-hidden">
          {/* Modal Header */}
          <div className="flex items-center justify-between px-6 py-4 bg-gradient-to-r from-indigo-600 to-blue-600 text-white">
            <div className="flex items-center gap-3">
              <SplitSquareHorizontal className="w-6 h-6" />
              <h2 className="text-xl font-bold">Review Extraction with PDF Source</h2>
            </div>
            <button
              onClick={() => setShowSplitView(false)}
              className="p-2 rounded-lg hover:bg-white/20 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          {/* Split View Content */}
          <div className="flex-1 overflow-hidden">
            <SplitViewLayout
              pdfData={pdfData}
              forms={forms}
              formProvenances={formProvenances}
              summary={summary}
            />
          </div>
          
          {/* Modal Footer */}
          <div className="flex items-center justify-end gap-4 px-6 py-4 bg-slate-50 border-t border-slate-200">
            <button
              onClick={() => setShowSplitView(false)}
              className="px-6 py-2.5 bg-white border border-slate-300 text-slate-700 font-medium rounded-xl hover:bg-slate-50 transition-colors"
            >
              Close Preview
            </button>
            <button
              onClick={() => {
                setShowSplitView(false);
                onConfirm();
              }}
              className="px-6 py-2.5 bg-indigo-600 text-white font-bold rounded-xl shadow-lg hover:bg-indigo-700 transition-colors flex items-center gap-2"
            >
              Proceed to Review <ArrowRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto animate-fadeIn">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 px-8 py-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold flex items-center gap-3">
                <CheckCircle className="w-8 h-8 text-green-300" />
                Extraction Complete!
              </h2>
              <p className="text-indigo-100 mt-2">
                We identified <strong className="text-white">{forms.length}</strong> distinct tax form{forms.length !== 1 && 's'} in your document.
              </p>
            </div>
            <div className="flex items-center gap-4">
              {/* View with PDF button */}
              {pdfData && (
                <button
                  onClick={() => setShowSplitView(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-white/20 hover:bg-white/30 text-white rounded-xl transition-colors"
                  title="View extracted data alongside the original PDF"
                >
                  <SplitSquareHorizontal className="w-5 h-5" />
                  <span className="text-sm font-medium">View with PDF</span>
                </button>
              )}
              {summary && (
                <div className="text-right">
                  <div className="text-3xl font-bold">{Math.round(summary.overallConfidence * 100)}%</div>
                  <div className="text-xs text-indigo-200">Overall Confidence</div>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="p-8">
          {/* Extraction Stats */}
          {summary && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200">
                <div className="flex items-center gap-2 text-slate-500 text-sm mb-1">
                  <FileText className="w-4 h-4" />
                  <span>Pages Scanned</span>
                </div>
                <div className="text-2xl font-bold text-slate-800">{summary.totalPagesScanned}</div>
              </div>
              <div className="p-4 bg-green-50 rounded-xl border border-green-200">
                <div className="flex items-center gap-2 text-green-600 text-sm mb-1">
                  <CheckCircle className="w-4 h-4" />
                  <span>Forms Extracted</span>
                </div>
                <div className="text-2xl font-bold text-green-700">{summary.formsExtracted}</div>
              </div>
              <div className="p-4 bg-amber-50 rounded-xl border border-amber-200">
                <div className="flex items-center gap-2 text-amber-600 text-sm mb-1">
                  <AlertTriangle className="w-4 h-4" />
                  <span>Forms Skipped</span>
                </div>
                <div className="text-2xl font-bold text-amber-700">{summary.formsSkipped}</div>
              </div>
              <div className="p-4 bg-blue-50 rounded-xl border border-blue-200">
                <div className="flex items-center gap-2 text-blue-600 text-sm mb-1">
                  <Clock className="w-4 h-4" />
                  <span>Duration</span>
                </div>
                <div className="text-2xl font-bold text-blue-700">{(summary.extractionDurationMs / 1000).toFixed(1)}s</div>
              </div>
            </div>
          )}

          {/* Extracted Forms */}
          <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">Extracted Documents</h3>
          
          <div className="grid gap-3 mb-8">
            {forms.map((form, idx) => {
              const provenance = formProvenances?.find(p => p.formType === form.formType);
              const formConfidence = summary?.confidenceByFormType?.[form.formType] || form.confidenceScore || 0.8;
              
              return (
                <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-xl hover:border-indigo-300 transition-colors">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className={`p-2.5 rounded-lg ${
                        formConfidence < 0.85 ? 'bg-amber-100 text-amber-700' : 'bg-white border border-slate-200 text-indigo-600'
                      }`}>
                        <FileText className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="font-bold text-slate-800">{form.formType}</div>
                        <div className="text-xs text-slate-500">
                          {getFormDescription(form)}
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-3">
                      {/* Page Provenance */}
                      {(provenance?.pageNumber || form.sourcePage) && (
                        <div className="flex items-center gap-1.5 px-2.5 py-1 bg-slate-100 text-slate-600 text-xs font-medium rounded-full">
                          <MapPin className="w-3 h-3" />
                          Page {provenance?.pageNumber || form.sourcePage}
                        </div>
                      )}
                      
                      {/* Confidence Badge */}
                      <div className={`flex items-center gap-1.5 px-3 py-1 text-xs font-bold rounded-full border ${getConfidenceColor(formConfidence)}`}>
                        <Gauge className="w-3.5 h-3.5" />
                        {Math.round(formConfidence * 100)}%
                      </div>
                      
                      {/* Status Badge */}
                      {formConfidence < 0.85 ? (
                        <div className="flex items-center gap-1.5 px-3 py-1 bg-amber-50 text-amber-700 text-xs font-bold rounded-full border border-amber-200">
                          <AlertTriangle className="w-3.5 h-3.5" /> Review
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 px-3 py-1 bg-green-50 text-green-700 text-xs font-bold rounded-full border border-green-200">
                          <CheckCircle className="w-3.5 h-3.5" /> Verified
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {/* Field Confidence Details (collapsed by default) */}
                  {form.fieldConfidence && Object.keys(form.fieldConfidence).length > 0 && (
                    <details className="mt-3 pt-3 border-t border-slate-200">
                      <summary className="text-xs text-slate-500 cursor-pointer hover:text-indigo-600 flex items-center gap-1">
                        <Eye className="w-3 h-3" />
                        View field confidence scores ({Object.keys(form.fieldConfidence).length} fields)
                      </summary>
                      <div className="mt-2 grid grid-cols-2 sm:grid-cols-3 gap-2">
                        {Object.entries(form.fieldConfidence).slice(0, 9).map(([field, conf]) => (
                          <div key={field} className="flex items-center justify-between p-2 bg-white rounded-lg text-xs">
                            <span className="text-slate-600 truncate">{field}</span>
                            <span className={`font-bold ${conf >= 0.9 ? 'text-green-600' : conf >= 0.7 ? 'text-amber-600' : 'text-red-600'}`}>
                              {Math.round(conf * 100)}%
                            </span>
                          </div>
                        ))}
                      </div>
                    </details>
                  )}
                </div>
              );
            })}
          </div>

          {/* Skipped Forms */}
          {summary?.skippedForms && summary.skippedForms.length > 0 && (
            <div className="mb-8">
              <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">Skipped Pages</h3>
              <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                {summary.skippedForms.map((skipped, idx) => (
                  <div key={idx} className="flex items-start gap-3 py-2">
                    <AlertTriangle className="w-4 h-4 text-amber-600 mt-0.5" />
                    <div>
                      <span className="font-medium text-amber-800">Page {skipped.pageNumber}</span>
                      <span className="text-amber-700 text-sm"> - {skipped.reason}</span>
                      {skipped.suggestion && (
                        <p className="text-xs text-amber-600 mt-1">{skipped.suggestion}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Model Info */}
          {summary?.modelUsed && (
            <div className="mb-6 p-3 bg-slate-50 rounded-lg border border-slate-200 flex items-center gap-2 text-xs text-slate-500">
              <Info className="w-4 h-4" />
              <span>Extracted using <strong className="text-slate-700">{summary.modelUsed}</strong></span>
            </div>
          )}

          {/* Actions */}
          <div className="flex flex-col sm:flex-row gap-4 pt-6 border-t border-slate-100">
            <button 
              onClick={onCancel}
              className="flex-1 px-6 py-3 bg-white border border-slate-300 text-slate-700 font-medium rounded-xl hover:bg-slate-50 transition-colors"
            >
              Upload Different File
            </button>
            <button 
              onClick={onConfirm}
              className="flex-[2] px-6 py-3 bg-indigo-600 text-white font-bold rounded-xl shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:scale-[1.01] transition-all flex items-center justify-center gap-2"
            >
              Proceed to Review <ArrowRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

function getFormDescription(form: TaxFormData): string {
  if (form.formType === TaxFormType.W2) return `Employer: ${(form as any).employer}`;
  if (form.formType === TaxFormType.W2G) return `Payer: ${(form as any).payer}`;
  if (form.formType.includes('1099')) return `Payer: ${(form as any).payer}`;
  if (form.formType === TaxFormType.SCHEDULE_C) return `Business: ${(form as any).businessName}`;
  if (form.formType === TaxFormType.SCHEDULE_E) return `Rentals & Partnerships`;
  if (form.formType === TaxFormType.FEDERAL_1040) return `Individual Return`;
  if (form.formType === TaxFormType.FORM_1120) return `Corporation Return`;
  if (form.formType === TaxFormType.FORM_1065) return `Partnership Return`;
  if (form.formType === TaxFormType.FORM_27) return `Net Profits Return`;
  if (form.formType.includes('Dublin') || form.formType === TaxFormType.FORM_R) return `Local Return (Verification)`;
  return 'Tax Document';
}
