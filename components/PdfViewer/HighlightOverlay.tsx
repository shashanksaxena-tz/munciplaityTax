import React from 'react';
import { BoundingBox } from '../../types';

interface HighlightOverlayProps {
  boundingBox: BoundingBox;
  pageWidth: number;
  pageHeight: number;
  fieldName: string;
  formType?: string;
  confidence?: number;
  isActive?: boolean;
  variant?: 'highlight' | 'marker';
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
}

export const HighlightOverlay: React.FC<HighlightOverlayProps> = ({
  boundingBox,
  pageWidth,
  pageHeight,
  fieldName,
  formType,
  confidence,
  isActive = true,
  variant = 'highlight',
  onMouseEnter,
  onMouseLeave
}) => {
  // Convert normalized coordinates (0-1) to pixel positions
  const left = boundingBox.x * pageWidth;
  const top = boundingBox.y * pageHeight;
  const width = boundingBox.width * pageWidth;
  const height = boundingBox.height * pageHeight;

  // Determine color based on confidence
  const getHighlightColor = () => {
    if (confidence === undefined) return 'rgba(70, 159, 232, 0.3)'; // #469fe8
    if (confidence >= 0.9) return 'rgba(16, 185, 129, 0.3)'; // #10b981 green
    if (confidence >= 0.7) return 'rgba(245, 158, 11, 0.3)'; // #f59e0b amber
    return 'rgba(236, 22, 86, 0.3)'; // #ec1656 red
  };

  const getBorderColor = () => {
    if (confidence === undefined) return 'rgba(70, 159, 232, 0.8)';
    if (confidence >= 0.9) return 'rgba(16, 185, 129, 0.8)';
    if (confidence >= 0.7) return 'rgba(245, 158, 11, 0.8)';
    return 'rgba(236, 22, 86, 0.8)';
  };

  if (variant === 'marker' && !isActive) {
    // Small marker style for background field indicators
    return (
      <div
        className="absolute cursor-pointer transition-all duration-200 hover:scale-110"
        style={{
          left: `${left + width / 2 - 6}px`,
          top: `${top - 10}px`,
          width: '12px',
          height: '12px',
          backgroundColor: getBorderColor(),
          borderRadius: '50%',
          border: '2px solid white',
          boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
          zIndex: 10
        }}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        title={`${fieldName}${formType ? ` (${formType})` : ''}`}
      />
    );
  }

  // Full highlight style for active field - respects reduced motion preference
  return (
    <div
      className="absolute pointer-events-none"
      style={{
        left: `${left}px`,
        top: `${top}px`,
        width: `${width}px`,
        height: `${height}px`,
        backgroundColor: getHighlightColor(),
        border: `2px solid ${getBorderColor()}`,
        borderRadius: '4px',
        zIndex: 20,
        animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite'
      }}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      {/* Field label badge */}
      <div
        className="absolute -top-6 left-0 px-2 py-0.5 rounded text-xs font-medium text-white whitespace-nowrap"
        style={{
          backgroundColor: getBorderColor(),
          boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
        }}
      >
        {fieldName}
        {confidence !== undefined && (
          <span className="ml-1 opacity-80">({Math.round(confidence * 100)}%)</span>
        )}
      </div>
    </div>
  );
};

export default HighlightOverlay;
