import React from 'react';
import { FileText, MapPin, Sparkles } from 'lucide-react';
import { FormProvenance, FieldProvenance } from '../types';

interface ExtractionProvenanceDisplayProps {
  provenance: FormProvenance[];
  onFieldClick?: (field: FieldProvenance, formProvenance: FormProvenance) => void;
  selectedFieldName?: string;
}

export function ExtractionProvenanceDisplay({
  provenance,
  onFieldClick,
  selectedFieldName
}: ExtractionProvenanceDisplayProps) {
  
  const getConfidenceColor = (confidence: number): string => {
    if (confidence >= 0.9) return 'text-[#10b981] bg-[#d5faeb]';
    if (confidence >= 0.7) return 'text-[#f59e0b] bg-[#fef3c7]';
    return 'text-[#ec1656] bg-[#fecaca]';
  };

  const formatFieldName = (fieldName: string): string => {
    // Convert camelCase or snake_case to readable format
    return fieldName
      .replace(/([A-Z])/g, ' $1')
      .replace(/_/g, ' ')
      .trim()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  if (!provenance || provenance.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-6 text-center">
        <Sparkles className="w-12 h-12 text-[#dcdede] mx-auto mb-3" />
        <p className="text-[#5d6567]">No extraction data available</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="p-4 border-b border-[#dcdede]">
        <h3 className="text-lg font-bold text-[#0f1012] flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-[#970bed]" />
          Extracted Fields
        </h3>
        <p className="text-sm text-[#5d6567] mt-1">
          Click on a field to highlight it in the PDF
        </p>
      </div>
      
      <div className="divide-y divide-[#dcdede] max-h-[600px] overflow-y-auto">
        {provenance.map((formProv, formIdx) => (
          <div key={formIdx} className="p-4">
            <div className="flex items-center gap-2 mb-3">
              <FileText className="w-4 h-4 text-[#469fe8]" />
              <h4 className="font-semibold text-[#0f1012]">{formProv.formType}</h4>
              <span className="text-xs text-[#5d6567]">
                Page {formProv.pageNumber}
              </span>
              {formProv.formConfidence !== undefined && (
                <span className={`text-xs px-2 py-0.5 rounded-full ${getConfidenceColor(formProv.formConfidence)}`}>
                  {Math.round(formProv.formConfidence * 100)}% confidence
                </span>
              )}
            </div>
            
            {formProv.extractionReason && (
              <p className="text-xs text-[#5d6567] mb-3 italic">
                {formProv.extractionReason}
              </p>
            )}

            <div className="space-y-2">
              {formProv.fields && formProv.fields.length > 0 ? (
                formProv.fields.map((field, fieldIdx) => {
                  const isSelected = selectedFieldName === field.fieldName;
                  
                  return (
                    <div
                      key={fieldIdx}
                      onClick={() => onFieldClick?.(field, formProv)}
                      className={`p-3 rounded-lg border cursor-pointer transition-all ${
                        isSelected 
                          ? 'border-[#469fe8] bg-[#e0f4ff] shadow-sm' 
                          : 'border-[#dcdede] hover:border-[#469fe8] hover:bg-[#f8f9fa]'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <MapPin className="w-3.5 h-3.5 text-[#469fe8] flex-shrink-0" />
                            <span className="font-medium text-sm text-[#0f1012]">
                              {formatFieldName(field.fieldName)}
                            </span>
                          </div>
                          
                          <div className="ml-5 space-y-1">
                            {field.rawValue && (
                              <div className="text-xs text-[#5d6567]">
                                <span className="font-medium">Raw:</span> {field.rawValue}
                              </div>
                            )}
                            {field.processedValue && field.processedValue !== field.rawValue && (
                              <div className="text-xs text-[#102124]">
                                <span className="font-medium">Processed:</span> {field.processedValue}
                              </div>
                            )}
                            {field.pageNumber && (
                              <div className="text-xs text-[#5d6567]">
                                <span className="font-medium">Page:</span> {field.pageNumber}
                              </div>
                            )}
                          </div>
                        </div>
                        
                        {field.confidence !== undefined && (
                          <span className={`text-xs px-2 py-1 rounded-full flex-shrink-0 ${getConfidenceColor(field.confidence)}`}>
                            {Math.round(field.confidence * 100)}%
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })
              ) : (
                <p className="text-xs text-[#5d6567] italic">No fields extracted</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
