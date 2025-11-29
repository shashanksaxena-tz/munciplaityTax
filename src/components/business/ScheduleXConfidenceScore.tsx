/**
 * ScheduleXConfidenceScore Component (T018)
 * 
 * Displays AI extraction confidence score with color coding (FR-042)
 * Clickable to view PDF bounding box region (Research R1)
 */

import React from 'react';
import { formatConfidenceScore } from '../../utils/scheduleXFormatting';

export interface ScheduleXConfidenceScoreProps {
  score: number;
  onViewBoundingBox?: () => void;
  className?: string;
}

export const ScheduleXConfidenceScore: React.FC<ScheduleXConfidenceScoreProps> = ({
  score,
  onViewBoundingBox,
  className = '',
}) => {
  const { formatted, badgeVariant } = formatConfidenceScore(score);

  const bgColor = {
    success: 'bg-green-100 text-green-800',
    warning: 'bg-yellow-100 text-yellow-800',
    danger: 'bg-red-100 text-red-800',
  }[badgeVariant];

  return (
    <button
      type="button"
      onClick={onViewBoundingBox}
      className={`px-2 py-1 rounded-full text-xs font-medium ${bgColor} 
                ${onViewBoundingBox ? 'hover:opacity-80 cursor-pointer' : 'cursor-default'}
                ${className}`}
      disabled={!onViewBoundingBox}
      title={onViewBoundingBox ? 'Click to view extracted region in PDF' : undefined}
    >
      AI: {formatted}
    </button>
  );
};

export default ScheduleXConfidenceScore;
