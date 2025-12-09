
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
  "JEDD: If you work in a Joint Economic Development District, special tax rules may apply to your withholding.",
  "Pro Tip: Keep all W-2s, 1099s, and supporting documents for at least 3 years after filing.",
  "Reminder: Military compensation and unemployment benefits are exempt from Dublin's local income tax.",
  "Deduction: Moving expenses are no longer deductible for most taxpayers after federal tax reform.",
  "Business Owners: Net operating losses can be carried forward to offset future taxable income.",
  "Rental Income: Schedule E properties in Dublin are taxed on net profit after allowable expenses.",
  "Partnership Income: K-1 distributions from partnerships are subject to Dublin's 2.0% tax rate.",
  "Withholding: Your employer should withhold 2.0% if you live or work in Dublin.",
  "Extension: Filing an extension gives you more time to file, but not to pay taxes owed.",
  "Estimated Tax: If you're self-employed, you may need to make quarterly estimated tax payments.",
  "Form 1040: Dublin's tax return starts with your Federal Adjusted Gross Income (AGI).",
  "Seniors: Age doesn't exempt you from Dublin tax, but certain retirement income may be exempt.",
  "Multi-State: Working remotely for an out-of-state employer? Dublin taxes your income from home.",
  "Capital Gains: Investment income and capital gains are generally not subject to Dublin's local tax.",
  "Social Security: Social Security benefits are exempt from Dublin municipal income tax.",
  "Interest & Dividends: Most investment income is not subject to local taxation in Dublin.",
  "Pension Income: Some pension and retirement income may be exempt - check the specific rules."
];

const STATUS_MESSAGES: Record<string, string[]> = {
  SCANNING: ["Scanning document structure...", "Detecting page boundaries...", "Analyzing document quality..."],
  ANALYZING: ["Analyzing with Gemini 2.5 Flash AI...", "Identifying tax form types...", "Detecting taxpayer information..."],
  EXTRACTING: ["Extracting field values...", "Validating data integrity...", "Computing confidence scores..."],
  COMPLETE: ["Extraction complete!", "Ready for review"],
  ERROR: ["An error occurred", "Please try again"]
};

// Personalized messages based on detected forms
const getPersonalizedMessage = (forms: string[], taxpayerName?: string): string => {
  const name = taxpayerName ? taxpayerName.split(' ')[0] : 'there';

  if (forms.includes('W-2')) {
    return `Great news, ${name}! We detected your W-2 form. This contains your wage information and withholding details.`;
  }
  if (forms.includes('1099-MISC') || forms.includes('1099-NEC')) {
    return `${name}, we found your 1099 income. We'll help you calculate the correct tax on your self-employment income.`;
  }
  if (forms.includes('Schedule E')) {
    return `${name}, your Schedule E shows rental property income. We'll compute your net rental profit for Dublin tax.`;
  }
  if (forms.includes('Schedule C')) {
    return `${name}, your Schedule C business income will be calculated with all allowable deductions.`;
  }
  if (forms.includes('K-1')) {
    return `${name}, we detected partnership K-1 income. This will be included in your taxable Dublin income.`;
  }
  if (forms.length > 2) {
    return `${name}, you have multiple income sources. Our AI is carefully analyzing each form to ensure accuracy.`;
  }

  return `${name}, your documents are being analyzed with precision to ensure accurate tax calculations.`;
};

interface ProcessingLoaderProps {
  extractionUpdate?: RealTimeExtractionUpdate;
  onContinue?: () => void;
}

export const ProcessingLoader: React.FC<ProcessingLoaderProps> = ({ extractionUpdate, onContinue }) => {
  const [factIndex, setFactIndex] = useState(0);
  const [statusMessageIndex, setStatusMessageIndex] = useState(0);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [startTime] = useState(Date.now());

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

  // Track elapsed time
  useEffect(() => {
    const interval = setInterval(() => {
      setElapsedTime(Math.floor((Date.now() - startTime) / 1000));
    }, 1000);
    return () => clearInterval(interval);
  }, [startTime]);

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
  const personalizedMessage = getPersonalizedMessage(detectedForms, taxpayerName);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;
  };

  return (
    <div className="w-full max-w-3xl mx-auto bg-white rounded-2xl shadow-xl border border-[#dcdede] overflow-hidden animate-fadeIn">
      {/* Header Section */}
      <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] px-8 py-6 text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="relative">
              {status === 'COMPLETE' ? (
                <CheckCircle className="w-10 h-10 text-[#d5faeb]" />
              ) : status === 'ERROR' ? (
                <AlertCircle className="w-10 h-10 text-[#fff5f5]" />
              ) : (
                <Loader2 className="w-10 h-10 animate-spin" />
              )}
            </div>
            <div>
              <h2 className="text-2xl font-bold">
                {status === 'COMPLETE' ? 'Extraction Complete' : status === 'ERROR' ? 'Extraction Error' : 'Smart Extraction in Progress'}
              </h2>
              <p className="text-white/90 text-sm mt-1">
                Powered by Gemini 2.5 Flash AI • {formatTime(elapsedTime)} elapsed
              </p>
            </div>
          </div>
          <div className="text-right">
            {confidence > 0 ? (
              <>
                <div className="text-3xl font-bold">{Math.round(confidence * 100)}%</div>
                <div className="text-xs text-white/80">Confidence</div>
              </>
            ) : (
              <>
                <div className="text-2xl font-bold">{formatTime(elapsedTime)}</div>
                <div className="text-xs text-white/80">Processing</div>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="p-8">
        {/* Progress Bar */}
        <div className="mb-6">
          <div className="flex justify-between text-sm text-[#5d6567] mb-2">
            <span className="font-medium">{currentStatusMessage}</span>
            <span className="font-bold">{Math.round(progress)}%</span>
          </div>
          <div className="w-full h-3 bg-[#f8f9fa] rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-[#970bed] to-[#469fe8] transition-all duration-500 ease-out rounded-full"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        {/* Detected Taxpayer Name */}
        {taxpayerName && (
          <div className="mb-6 p-4 bg-[#ebf4ff] border border-[#469fe8]/20 rounded-xl flex items-center gap-3 animate-slideIn">
            <div className="p-2 bg-white rounded-full shadow-sm">
              <User className="w-5 h-5 text-[#469fe8]" />
            </div>
            <div className="flex-1">
              <div className="text-xs text-blue-600 font-semibold uppercase tracking-wider">Detected Taxpayer</div>
              <div className="text-lg font-bold text-slate-800 mb-1">{taxpayerName}</div>
              <div className="text-sm text-slate-600 italic">"{personalizedMessage}"</div>
            </div>
          </div>
        )}

        {/* Detected Forms */}
        {detectedForms.length > 0 && (
          <div className="mb-6">
            <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-3">
              Detected Forms ({detectedForms.length})
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {detectedForms.map((form, idx) => {
                const provenance = formProvenances.find(p => p.formType === form);
                const isCurrentForm = form === currentFormType;

                return (
                  <div
                    key={idx}
                    style={{ animationDelay: `${idx * 100}ms` }}
                    className={`p-3 rounded-xl border transition-all duration-300 animate-slideIn ${isCurrentForm
                      ? 'bg-indigo-50 border-indigo-300 scale-[1.02] shadow-md ring-2 ring-indigo-200'
                      : 'bg-slate-50 border-slate-200 hover:border-slate-300'
                      }`}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <FileText className={`w-4 h-4 ${isCurrentForm ? 'text-indigo-600' : 'text-slate-400'}`} />
                      <span className="font-bold text-sm text-slate-800">{form}</span>
                    </div>
                    {provenance && (
                      <div className="text-xs text-slate-500 mb-1">
                        Page {provenance.pageNumber} • {Math.round(provenance.formConfidence * 100)}% match
                      </div>
                    )}
                    {isCurrentForm && (
                      <div className="mt-2 flex items-center gap-1 text-xs text-indigo-600 font-medium">
                        <Loader2 className="w-3 h-3 animate-spin" />
                        <span>Extracting data...</span>
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
        <div className="mt-10 bg-gradient-to-br from-slate-50 to-indigo-50/50 border border-indigo-100 rounded-xl p-6 pt-8 relative">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white border border-indigo-100 px-3 py-1 rounded-full shadow-sm flex items-center gap-1.5 z-10">
            <Lightbulb className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
            <span className="text-xs font-bold text-slate-600 uppercase tracking-wider">Dublin Tax Insight</span>
          </div>

          <div className="min-h-[80px] flex items-center justify-center">
            <p className="text-slate-700 text-sm leading-relaxed italic text-center transition-opacity duration-300">
              "{FACTS[factIndex]}"
            </p>
          </div>

          {/* Progress indicator */}
          <div className="mt-4 flex justify-center gap-1">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className={`w-1.5 h-1.5 rounded-full transition-all duration-300 ${i === factIndex % 5 ? 'bg-indigo-400 w-4' : 'bg-slate-300'
                  }`}
              />
            ))}
          </div>
        </div>

        {/* Continue Button - Only show when extraction is complete */}
        {(status === 'COMPLETE' || progress >= 100) && onContinue && (
          <div className="mt-6 flex flex-col items-center gap-3">
            <button
              onClick={onContinue}
              className="px-8 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700 transition-all duration-300 shadow-lg hover:shadow-xl transform hover:scale-105 flex items-center gap-2"
            >
              <CheckCircle className="w-5 h-5" />
              Continue to Review
            </button>
            <p className="text-xs text-slate-500 italic">Click to view your extracted data</p>
          </div>
        )}

        {/* Security Notice */}
        <div className="mt-6 flex items-center justify-center gap-2 text-xs text-slate-400">
          <Shield className="w-3 h-3" />
          <span>Your data is processed securely • API keys are never stored</span>
        </div>
      </div>
    </div>
  );
};
