import React from 'react';
import { MapPin, Gauge, FileText, Info } from 'lucide-react';
import { BoundingBox } from '../../types';

interface FieldSourceTooltipProps {
  fieldName: string;
  formType: string;
  pageNumber: number;
  confidence?: number;
  boundingBox?: BoundingBox;
  pageWidth: number;
  pageHeight: number;
  rawValue?: string;
  processedValue?: string;
}

export const FieldSourceTooltip: React.FC<FieldSourceTooltipProps> = ({
  fieldName,
  formType,
  pageNumber,
  confidence,
  boundingBox,
  pageWidth,
  pageHeight,
  rawValue,
  processedValue
}) => {
  // Position tooltip near the highlighted area
  const tooltipLeft = boundingBox 
    ? Math.min(boundingBox.x * pageWidth + 20, pageWidth - 200)
    : 20;
  const tooltipTop = boundingBox
    ? Math.max(boundingBox.y * pageHeight - 120, 10)
    : 10;

  const getConfidenceColor = () => {
    if (confidence === undefined) return 'text-[#5d6567] bg-[#f8f9fa]';
    if (confidence >= 0.9) return 'text-[#10b981] bg-[#d5faeb]';
    if (confidence >= 0.7) return 'text-[#f59e0b] bg-[#fff5e6]';
    return 'text-[#ec1656] bg-[#fff5f5]';
  };

  const getConfidenceLabel = () => {
    if (confidence === undefined) return 'Unknown';
    if (confidence >= 0.9) return 'High Confidence';
    if (confidence >= 0.7) return 'Needs Review';
    return 'Low Confidence';
  };

  return (
    <div
      className="absolute z-50 bg-white rounded-lg shadow-xl border border-[#dcdede] p-4 min-w-[240px] max-w-[320px] transition-opacity duration-200"
      style={{
        left: `${tooltipLeft}px`,
        top: `${tooltipTop}px`
      }}
    >
      {/* Header */}
      <div className="flex items-start gap-2 mb-3 pb-3 border-b border-[#dcdede]">
        <FileText className="w-5 h-5 text-[#469fe8] mt-0.5" />
        <div>
          <div className="font-bold text-[#0f1012]">{fieldName}</div>
          <div className="text-xs text-[#5d6567]">{formType}</div>
        </div>
      </div>

      {/* Details */}
      <div className="space-y-2">
        {/* Page Location */}
        <div className="flex items-center gap-2 text-sm">
          <MapPin className="w-4 h-4 text-[#babebf]" />
          <span className="text-[#5d6567]">Page {pageNumber}</span>
          {boundingBox && (
            <span className="text-xs text-[#babebf]">
              ({Math.round(boundingBox.x * 100)}%, {Math.round(boundingBox.y * 100)}%)
            </span>
          )}
        </div>

        {/* Confidence */}
        {confidence !== undefined && (
          <div className="flex items-center gap-2 text-sm">
            <Gauge className="w-4 h-4 text-[#babebf]" />
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getConfidenceColor()}`}>
              {Math.round(confidence * 100)}% - {getConfidenceLabel()}
            </span>
          </div>
        )}

        {/* Raw vs Processed Values */}
        {(rawValue || processedValue) && (
          <div className="mt-3 pt-3 border-t border-[#dcdede]">
            {rawValue && (
              <div className="flex items-start gap-2 text-sm mb-2">
                <Info className="w-4 h-4 text-[#babebf] mt-0.5" />
                <div>
                  <div className="text-xs text-[#5d6567] mb-0.5">Raw Text Detected:</div>
                  <div className="text-[#102124] font-mono text-xs bg-[#f8f9fa] px-2 py-1 rounded">
                    {rawValue}
                  </div>
                </div>
              </div>
            )}
            {processedValue && rawValue !== processedValue && (
              <div className="flex items-start gap-2 text-sm">
                <Info className="w-4 h-4 text-slate-400 mt-0.5" />
                <div>
                  <div className="text-xs text-slate-500 mb-0.5">Processed Value:</div>
                  <div className="text-slate-700 font-medium">
                    {processedValue}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Low confidence warning */}
      {confidence !== undefined && confidence < 0.7 && (
        <div className="mt-3 pt-3 border-t border-slate-100">
          <div className="text-xs text-amber-700 bg-amber-50 p-2 rounded-lg">
            ⚠️ This value may need manual verification due to low extraction confidence.
          </div>
        </div>
      )}
    </div>
  );
};

export default FieldSourceTooltip;
