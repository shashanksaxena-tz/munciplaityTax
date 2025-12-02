
import React, { useState, useEffect } from 'react';
import { Loader2, Lightbulb, FileSearch, BrainCircuit, Sparkles, CheckCircle, AlertCircle, FileText, User, Shield } from 'lucide-react';
import { RealTimeExtractionUpdate, FormProvenance, FieldConfidenceInfo } from '../types';

const FACTS = [
  "Did you know? Dublin's local income tax rate is currently 2.0%.",
  "Fact: You can receive up to a 100% credit for taxes paid to other municipalities, capped at 2.0%.",
  "Tax Tip: W-2G Gambling Winnings are fully taxable in Dublin without deduction for losses.",
  "Filing Deadline: Your Dublin return is generally due on April 15th, matching the Federal deadline.",
  "Rule: Qualifying wages are typically the highest of Box 5 (Medicare) or Box 18 (Local) wages.",
  "History: The City of Dublin uses income tax revenue to fund capital improvements and safety services.",
  "Schedule E: Losses from one rental property can offset gains from another rental property in Dublin calculations.",
  "JEDD: If you work in a Joint Economic Development District, special tax rules may apply to your withholding."
];

const STATUS_MESSAGES: Record<string, string[]> = {
  SCANNING: ["Scanning document structure...", "Detecting page boundaries...", "Analyzing document quality..."],
  ANALYZING: ["Analyzing with Gemini 2.5 Flash AI...", "Identifying tax form types...", "Detecting taxpayer information..."],
  EXTRACTING: ["Extracting field values...", "Validating data integrity...", "Computing confidence scores..."],
  COMPLETE: ["Extraction complete!", "Ready for review"],
  ERROR: ["An error occurred", "Please try again"]
};

interface ProcessingLoaderProps {
  extractionUpdate?: RealTimeExtractionUpdate;
}

export const ProcessingLoader: React.FC<ProcessingLoaderProps> = ({ extractionUpdate }) => {
  const [factIndex, setFactIndex] = useState(0);
  const [statusMessageIndex, setStatusMessageIndex] = useState(0);

  // Cycle facts every 5 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      setFactIndex((prev) => (prev + 1) % FACTS.length);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  // Cycle status messages every 2 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      setStatusMessageIndex((prev) => (prev + 1) % 3);
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const status = extractionUpdate?.status || 'SCANNING';
  const progress = extractionUpdate?.progress || 0;
  const detectedForms = extractionUpdate?.detectedForms || [];
  const currentFormType = extractionUpdate?.currentFormType;
  const taxpayerName = extractionUpdate?.currentTaxpayerName;
  const logs = extractionUpdate?.log || [];
  const confidence = extractionUpdate?.confidence || 0;
  const formProvenances = extractionUpdate?.formProvenances || [];

  const statusMessages = STATUS_MESSAGES[status] || STATUS_MESSAGES.SCANNING;
  const currentStatusMessage = logs.length > 0 ? logs[logs.length - 1] : statusMessages[statusMessageIndex % statusMessages.length];

  return (
    <div className="w-full max-w-3xl mx-auto bg-white rounded-2xl shadow-xl border border-indigo-100 overflow-hidden animate-fadeIn">
      {/* Header Section */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-8 py-6 text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="relative">
              {status === 'COMPLETE' ? (
                <CheckCircle className="w-10 h-10 text-green-300" />
              ) : status === 'ERROR' ? (
                <AlertCircle className="w-10 h-10 text-red-300" />
              ) : (
                <Loader2 className="w-10 h-10 animate-spin" />
              )}
            </div>
            <div>
              <h2 className="text-2xl font-bold">
                {status === 'COMPLETE' ? 'Extraction Complete' : status === 'ERROR' ? 'Extraction Error' : 'Smart Extraction in Progress'}
              </h2>
              <p className="text-indigo-100 text-sm mt-1">
                Powered by Gemini 2.5 Flash AI
              </p>
            </div>
          </div>
          {confidence > 0 && (
            <div className="text-right">
              <div className="text-3xl font-bold">{Math.round(confidence * 100)}%</div>
              <div className="text-xs text-indigo-200">Confidence</div>
            </div>
          )}
        </div>
      </div>

      <div className="p-8">
        {/* Progress Bar */}
        <div className="mb-6">
          <div className="flex justify-between text-sm text-slate-600 mb-2">
            <span className="font-medium">{currentStatusMessage}</span>
            <span className="font-bold">{Math.round(progress)}%</span>
          </div>
          <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-500 ease-out rounded-full"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        {/* Detected Taxpayer Name */}
        {taxpayerName && (
          <div className="mb-6 p-4 bg-blue-50 border border-blue-100 rounded-xl flex items-center gap-3">
            <div className="p-2 bg-white rounded-full shadow-sm">
              <User className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <div className="text-xs text-blue-600 font-semibold uppercase tracking-wider">Detected Taxpayer</div>
              <div className="text-lg font-bold text-slate-800">{taxpayerName}</div>
            </div>
          </div>
        )}

        {/* Detected Forms */}
        {detectedForms.length > 0 && (
          <div className="mb-6">
            <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-3">Detected Forms</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {detectedForms.map((form, idx) => {
                const provenance = formProvenances.find(p => p.formType === form);
                const isCurrentForm = form === currentFormType;
                
                return (
                  <div 
                    key={idx}
                    className={`p-3 rounded-xl border transition-all duration-300 ${
                      isCurrentForm 
                        ? 'bg-indigo-50 border-indigo-300 scale-[1.02] shadow-md' 
                        : 'bg-slate-50 border-slate-200'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <FileText className={`w-4 h-4 ${isCurrentForm ? 'text-indigo-600' : 'text-slate-400'}`} />
                      <span className="font-bold text-sm text-slate-800">{form}</span>
                    </div>
                    {provenance && (
                      <div className="text-xs text-slate-500">
                        Page {provenance.pageNumber} • {Math.round(provenance.formConfidence * 100)}% confident
                      </div>
                    )}
                    {isCurrentForm && (
                      <div className="mt-2 flex items-center gap-1 text-xs text-indigo-600">
                        <Loader2 className="w-3 h-3 animate-spin" />
                        <span>Extracting...</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Extraction Log */}
        {logs.length > 0 && (
          <div className="mb-6">
            <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-3">Activity Log</h3>
            <div className="bg-slate-50 rounded-xl p-4 max-h-32 overflow-y-auto">
              {logs.slice(-5).map((log, idx) => (
                <div key={idx} className="flex items-center gap-2 text-sm text-slate-600 py-1">
                  <CheckCircle className="w-3 h-3 text-green-500 flex-shrink-0" />
                  <span>{log}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Fact Card */}
        <div className="bg-gradient-to-br from-slate-50 to-indigo-50/50 border border-indigo-100 rounded-xl p-6 relative">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white border border-indigo-100 px-3 py-1 rounded-full shadow-sm flex items-center gap-1.5">
            <Lightbulb className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
            <span className="text-xs font-bold text-slate-600 uppercase tracking-wider">Muni Tax Fact</span>
          </div>
          
          <div className="min-h-[60px] flex items-center justify-center">
            <p className="text-slate-700 text-sm leading-relaxed italic text-center">
              "{FACTS[factIndex]}"
            </p>
          </div>
        </div>

        {/* Security Notice */}
        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-400">
          <Shield className="w-3 h-3" />
          <span>Your data is processed securely • API keys are never stored</span>
        </div>
      </div>
    </div>
  );
};
