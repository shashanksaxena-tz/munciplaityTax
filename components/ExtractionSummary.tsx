
import React from 'react';
import { TaxFormData, TaxFormType } from '../types';
import { FileText, CheckCircle, AlertTriangle, ArrowRight, FilePlus } from 'lucide-react';

interface ExtractionSummaryProps {
  forms: TaxFormData[];
  onConfirm: () => void;
  onCancel: () => void;
}

export const ExtractionSummary: React.FC<ExtractionSummaryProps> = ({ forms, onConfirm, onCancel }) => {
  
  const counts = forms.reduce((acc, form) => {
    acc[form.formType] = (acc[form.formType] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="max-w-3xl mx-auto animate-fadeIn">
      <div className="bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 px-8 py-6 text-white">
          <h2 className="text-2xl font-bold flex items-center gap-3">
            <CheckCircle className="w-8 h-8 text-green-300" />
            Extraction Complete!
          </h2>
          <p className="text-indigo-100 mt-2">
            We identified <strong className="text-white">{forms.length}</strong> distinct tax form{forms.length !== 1 && 's'} in your document.
          </p>
        </div>

        <div className="p-8">
          <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4">Identified Documents</h3>
          
          <div className="grid gap-3 mb-8">
            {forms.map((form, idx) => (
              <div key={idx} className="flex items-center justify-between p-4 bg-slate-50 border border-slate-200 rounded-xl hover:border-indigo-300 transition-colors">
                <div className="flex items-center gap-4">
                  <div className={`p-2.5 rounded-lg ${
                    form.confidenceScore && form.confidenceScore < 0.85 ? 'bg-amber-100 text-amber-700' : 'bg-white border border-slate-200 text-indigo-600'
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
                
                {form.confidenceScore && form.confidenceScore < 0.85 ? (
                  <div className="flex items-center gap-1.5 px-3 py-1 bg-amber-50 text-amber-700 text-xs font-bold rounded-full border border-amber-200">
                    <AlertTriangle className="w-3.5 h-3.5" /> Needs Review
                  </div>
                ) : (
                  <div className="flex items-center gap-1.5 px-3 py-1 bg-green-50 text-green-700 text-xs font-bold rounded-full border border-green-200">
                    <CheckCircle className="w-3.5 h-3.5" /> High Confidence
                  </div>
                )}
              </div>
            ))}
          </div>

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
  if (form.formType.includes('Dublin')) return `Local Return (Verification)`;
  return 'Tax Document';
}
