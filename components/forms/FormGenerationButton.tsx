/**
 * Form Generation Button Component
 * Quick action button to generate a form
 */

import React, { useState } from 'react';
import { FileText, Download, Loader2 } from 'lucide-react';
import { formGenerationService } from '../../src/services/formGenerationService';
import type { FormGenerationRequest } from '../../src/types/formTypes';

interface FormGenerationButtonProps {
  formCode: string;
  formName: string;
  taxYear: number;
  returnId: string;
  businessId: string;
  tenantId: string;
  formData: Record<string, any>;
  onSuccess?: (response: any) => void;
  onError?: (error: any) => void;
  className?: string;
  disabled?: boolean;
}

export const FormGenerationButton: React.FC<FormGenerationButtonProps> = ({
  formCode,
  formName,
  taxYear,
  returnId,
  businessId,
  tenantId,
  formData,
  onSuccess,
  onError,
  className = '',
  disabled = false,
}) => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedFormId, setGeneratedFormId] = useState<string | null>(null);

  const handleGenerate = async () => {
    setIsGenerating(true);
    try {
      const request: FormGenerationRequest = {
        formCode,
        taxYear,
        returnId,
        businessId,
        tenantId,
        formData,
        includeWatermark: true,
      };

      const response = await formGenerationService.generateForm(request);
      
      if (response.success && response.generatedFormId) {
        setGeneratedFormId(response.generatedFormId);
        onSuccess?.(response);
      } else {
        onError?.(new Error(response.message || 'Form generation failed'));
      }
    } catch (error) {
      console.error('Error generating form:', error);
      onError?.(error);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDownload = () => {
    if (generatedFormId) {
      const downloadUrl = formGenerationService.getDownloadUrl(generatedFormId);
      window.open(downloadUrl, '_blank');
    }
  };

  if (generatedFormId) {
    return (
      <button
        onClick={handleDownload}
        className={`flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition-colors ${className}`}
      >
        <Download className="w-4 h-4" />
        Download {formName}
      </button>
    );
  }

  return (
    <button
      onClick={handleGenerate}
      disabled={isGenerating || disabled}
      className={`flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors ${className}`}
    >
      {isGenerating ? (
        <>
          <Loader2 className="w-4 h-4 animate-spin" />
          Generating...
        </>
      ) : (
        <>
          <FileText className="w-4 h-4" />
          Generate {formName}
        </>
      )}
    </button>
  );
};

export default FormGenerationButton;
