/**
 * Reconciliation Status Badge Component
 * Displays status indicator for reconciliation issues
 */

import React from 'react';
import { AlertTriangle, CheckCircle, AlertOctagon, Info, XCircle } from 'lucide-react';
import { ReconciliationIssueSeverity } from '../types';

interface ReconciliationStatusBadgeProps {
  severity: ReconciliationIssueSeverity | string;
  resolved?: boolean;
  className?: string;
}

export const ReconciliationStatusBadge: React.FC<ReconciliationStatusBadgeProps> = ({ 
  severity, 
  resolved = false,
  className = '' 
}) => {
  if (resolved) {
    return (
      <span
        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium border bg-[#d5faeb] text-[#10b981] border-[#10b981]/30 ${className}`}
      >
        <CheckCircle className="w-4 h-4" />
        Resolved
      </span>
    );
  }

  const getStatusConfig = () => {
    switch (severity) {
      case ReconciliationIssueSeverity.CRITICAL:
        return {
          label: 'Critical',
          color: 'bg-[#fee2e2] text-[#dc2626] border-[#dc2626]/30',
          icon: XCircle,
        };
      case ReconciliationIssueSeverity.HIGH:
        return {
          label: 'High',
          color: 'bg-[#ffedd5] text-[#ea580c] border-[#ea580c]/30',
          icon: AlertOctagon,
        };
      case ReconciliationIssueSeverity.MEDIUM:
        return {
          label: 'Medium',
          color: 'bg-[#fef3c7] text-[#d97706] border-[#d97706]/30',
          icon: AlertTriangle,
        };
      case ReconciliationIssueSeverity.LOW:
        return {
          label: 'Low',
          color: 'bg-[#dbeafe] text-[#2563eb] border-[#2563eb]/30',
          icon: Info,
        };
      default:
        return {
          label: severity,
          color: 'bg-[#f8f9fa] text-[#5d6567] border-[#dcdede]',
          icon: Info,
        };
    }
  };

  const config = getStatusConfig();
  const Icon = config.icon;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-sm font-medium border ${config.color} ${className}`}
    >
      <Icon className="w-4 h-4" />
      {config.label}
    </span>
  );
};

export default ReconciliationStatusBadge;
