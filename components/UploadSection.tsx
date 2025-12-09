import React, { useCallback, useState } from 'react';
import { Upload, FileText, X, FileStack, Key, Eye, EyeOff, Settings } from 'lucide-react';
import { TaxFormData, RealTimeExtractionUpdate } from '../types';
import { api } from '../services/api';
import { mapExtractionResultToForms } from '../services/extractionMapper';
import { ProcessingLoader } from './ProcessingLoader';
import { GEMINI_DEFAULT_MODEL } from '../constants';

interface ExtractionResult {
  forms: TaxFormData[];
  extractedProfile?: any;
  extractedSettings?: any;
  summary?: any;
  pdfData?: string;
  formProvenances?: any[];
}

interface UploadSectionProps {
  onDataExtracted: (data: TaxFormData[] | ExtractionResult) => void;
}

export const UploadSection: React.FC<UploadSectionProps> = ({ onDataExtracted }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showApiKeyInput, setShowApiKeyInput] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);
  const [extractionUpdate, setExtractionUpdate] = useState<RealTimeExtractionUpdate | undefined>();
  const [finalResult, setFinalResult] = useState<any>(null);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setIsDragging(true);
    } else if (e.type === 'dragleave') {
      setIsDragging(false);
    }
  }, []);

  const processFiles = async (files: File[]) => {
    if (files.length === 0) return;

    setIsProcessing(true);
    setError(null);
    setExtractionUpdate(undefined);
    setFinalResult(null);

    try {
      const allExtractedForms: TaxFormData[] = [];
      let extractedProfile: any = undefined;
      let extractedSettings: any = undefined;
      let lastSummary: any = undefined;
      let pdfDataBase64: string | undefined = undefined;
      let allFormProvenances: any[] = [];

      for (const file of files) {
        // Convert file to base64 for PDF preview
        if (file.type === 'application/pdf') {
          const reader = new FileReader();
          const base64Promise = new Promise<string>((resolve) => {
            reader.onload = () => {
              const base64 = (reader.result as string).split(',')[1];
              resolve(base64);
            };
            reader.readAsDataURL(file);
          });
          pdfDataBase64 = await base64Promise;
        }

        await new Promise<void>((resolve, reject) => {
          api.extraction.uploadAndExtract(
            file,
            (update: RealTimeExtractionUpdate) => {
              setExtractionUpdate(update);

              if (update.status === 'COMPLETE' && update.result) {
                try {
                  console.log('[UploadSection] Extraction complete, raw result:', update.result);
                  const result = mapExtractionResultToForms(update.result, file.name);
                  console.log('[UploadSection] Mapped forms:', result.forms);
                  allExtractedForms.push(...result.forms);
                  if (!extractedProfile && result.extractedProfile) {
                    extractedProfile = result.extractedProfile;
                  }
                  if (!extractedSettings && result.extractedSettings) {
                    extractedSettings = result.extractedSettings;
                  }
                  if (update.summary) {
                    lastSummary = update.summary;
                  }
                  if (update.formProvenances) {
                    allFormProvenances.push(...update.formProvenances);
                  }
                  resolve();
                } catch (e) {
                  reject(e);
                }
              } else if (update.status === 'ERROR') {
                reject(new Error(update.log ? update.log.join(' ') : 'Extraction failed'));
              }
            },
            {
              geminiApiKey: apiKey || undefined,
              geminiModel: GEMINI_DEFAULT_MODEL
            }
          ).catch(reject);
        });
      }

      if (allExtractedForms.length === 0) {
        throw new Error("No recognizable tax forms were found in the document.");
      }

      const result = {
        forms: allExtractedForms,
        extractedProfile,
        extractedSettings,
        summary: lastSummary,
        pdfData: pdfDataBase64,
        formProvenances: allFormProvenances.length > 0 ? allFormProvenances : undefined
      };

      setFinalResult(result);
    } catch (err: any) {
      console.error(err);
      setError(err.message || "Failed to process documents. Please try again.");
      setIsProcessing(false);
    }
  };

  const handleContinue = () => {
    if (finalResult) {
      onDataExtracted(finalResult);
    }
  };

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files) as File[];
    if (files.length > 0) {
      processFiles(files);
    }
  }, [onDataExtracted, apiKey]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files) as File[];
      processFiles(files);
    }
  };

  if (isProcessing) {
    return <ProcessingLoader extractionUpdate={extractionUpdate} onContinue={handleContinue} />;
  }

  return (
    <div className="w-full max-w-2xl mx-auto animate-fadeIn">
      {/* API Key Configuration */}
      <div className="mb-6">
        <button
          onClick={() => setShowApiKeyInput(!showApiKeyInput)}
          className="flex items-center gap-2 text-sm text-slate-600 hover:text-indigo-600 transition-colors"
        >
          <Settings className="w-4 h-4" />
          <span>{showApiKeyInput ? 'Hide' : 'Configure'} Gemini API Key</span>
        </button>

        {showApiKeyInput && (
          <div className="mt-3 p-4 bg-slate-50 rounded-xl border border-slate-200 animate-slideUp">
            <div className="flex items-start gap-3 mb-3">
              <Key className="w-5 h-5 text-indigo-500 mt-0.5" />
              <div>
                <h4 className="font-semibold text-slate-800">Your Gemini API Key</h4>
                <p className="text-xs text-slate-500 mt-1">
                  Provide your own API key for extraction. Keys are never stored and used only for this session.
                </p>
              </div>
            </div>
            <div className="relative">
              <input
                type={showApiKey ? 'text' : 'password'}
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                placeholder="Enter your Gemini API key..."
                className="w-full px-4 py-2.5 pr-12 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
              />
              <button
                type="button"
                onClick={() => setShowApiKey(!showApiKey)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-2">
              Get your API key from{' '}
              <a
                href="https://aistudio.google.com/app/apikey"
                target="_blank"
                rel="noopener noreferrer"
                className="text-indigo-600 hover:underline"
              >
                Google AI Studio
              </a>
            </p>
          </div>
        )}
      </div>

      <div
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        className={`
          relative border-2 border-dashed rounded-2xl p-12 text-center transition-all duration-300 cursor-pointer group
          ${isDragging
            ? 'border-indigo-500 bg-indigo-50 scale-[1.02] shadow-xl'
            : 'border-slate-300 hover:border-indigo-400 bg-white hover:bg-slate-50 hover:shadow-md'}
        `}
      >
        <input
          type="file"
          multiple
          accept="application/pdf,image/jpeg,image/png"
          onChange={handleFileInput}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
        />

        <div className="flex flex-col items-center justify-center space-y-5 pointer-events-none">
          <div className={`p-5 rounded-full transition-colors duration-300 ${isDragging ? 'bg-indigo-100 text-indigo-600' : 'bg-slate-100 text-slate-500 group-hover:bg-indigo-50 group-hover:text-indigo-500'}`}>
            <Upload className="w-10 h-10" />
          </div>

          <div className="space-y-2">
            <h3 className="text-2xl font-bold text-slate-800">
              Upload Tax Documents
            </h3>
            <p className="text-slate-500 text-base max-w-sm mx-auto">
              Drop your <span className="font-semibold text-indigo-600">PDF, JPG, or PNG</span> files here.
            </p>
            <p className="text-xs text-slate-400 pt-2">
              Supports multi-page PDFs containing mixed W-2s, 1099s, Schedules, and more.
            </p>
          </div>
        </div>
      </div>

      {error && (
        <div className="mt-6 p-4 bg-red-50 border border-red-100 rounded-xl flex items-start gap-3 text-red-700 animate-slideUp shadow-sm">
          <X className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div>
            <span className="font-bold block">Upload Failed</span>
            <span className="text-sm opacity-90">{error}</span>
          </div>
        </div>
      )}

      <div className="mt-10 grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm text-slate-600">
        <div className="flex items-center gap-3 p-3 bg-white rounded-lg border border-slate-200 shadow-sm">
          <div className="p-2 bg-green-50 rounded-md text-green-600"><FileStack className="w-4 h-4" /></div>
          <span className="font-medium">Bulk Extraction</span>
        </div>
        <div className="flex items-center gap-3 p-3 bg-white rounded-lg border border-slate-200 shadow-sm">
          <div className="p-2 bg-blue-50 rounded-md text-blue-600"><FileText className="w-4 h-4" /></div>
          <span className="font-medium">Smart Filtering</span>
        </div>
        <div className="flex items-center gap-3 p-3 bg-white rounded-lg border border-slate-200 shadow-sm">
          <div className="p-2 bg-purple-50 rounded-md text-purple-600"><FileText className="w-4 h-4" /></div>
          <span className="font-medium">Auto-Mapping</span>
        </div>
      </div>
    </div>
  );
};