import React, { useCallback, useState } from 'react';
import { Upload, FileText, X, FileStack } from 'lucide-react';
import { TaxFormData } from '../types';
import { api } from '../services/api';
import { mapExtractionResultToForms } from '../services/extractionMapper';
import { ProcessingLoader } from './ProcessingLoader';

interface ExtractionResult {
  forms: TaxFormData[];
  extractedProfile?: any;
  extractedSettings?: any;
}

interface UploadSectionProps {
  onDataExtracted: (data: TaxFormData[] | ExtractionResult) => void;
}

export const UploadSection: React.FC<UploadSectionProps> = ({ onDataExtracted }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

    try {
      const allExtractedForms: TaxFormData[] = [];
      let extractedProfile: any = undefined;
      let extractedSettings: any = undefined;

      for (const file of files) {
        await new Promise<void>((resolve, reject) => {
          api.extraction.uploadAndExtract(file, (update) => {
            if (update.status === 'COMPLETE' && update.result) {
              try {
                const result = mapExtractionResultToForms(update.result, file.name);
                allExtractedForms.push(...result.forms);
                if (!extractedProfile && result.extractedProfile) {
                  extractedProfile = result.extractedProfile;
                }
                if (!extractedSettings && result.extractedSettings) {
                  extractedSettings = result.extractedSettings;
                }
                resolve();
              } catch (e) {
                reject(e);
              }
            } else if (update.status === 'ERROR') { // Assuming backend might send ERROR status
              reject(new Error(update.log ? update.log.join(' ') : 'Extraction failed'));
            }
          }).catch(reject);
        });
      }

      if (allExtractedForms.length === 0) {
        throw new Error("No recognizable tax forms were found in the document.");
      }

      if (extractedProfile || extractedSettings) {
        onDataExtracted({
          forms: allExtractedForms,
          extractedProfile,
          extractedSettings
        });
      } else {
        onDataExtracted(allExtractedForms);
      }
    } catch (err: any) {
      console.error(err);
      setError(err.message || "Failed to process documents. Please try again.");
      setIsProcessing(false);
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
  }, [onDataExtracted]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files) as File[];
      processFiles(files);
    }
  };

  if (isProcessing) {
    return <ProcessingLoader />;
  }

  return (
    <div className="w-full max-w-2xl mx-auto animate-fadeIn">
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
              Supports multi-page PDFs containing mixed W-2s, 1099s, and Schedules.
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