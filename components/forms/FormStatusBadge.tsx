/**
 * Form Status Badge Component
 * Displays status indicator for generated forms
 */

import React from 'react';
import { FileCheck, FileClock, FileUp, FileEdit, FileX } from 'lucide-react';
import { FormStatus } from '../../src/types/formTypes';

interface FormStatusBadgeProps {
  status: FormStatus | string;
  className?: string;
}

export const FormStatusBadge: React.FC<FormStatusBadgeProps> = ({ status, className = '' }) => {
  const getStatusConfig = () => {
    switch (status) {
      case FormStatus.DRAFT:
        return {
          label: 'Draft',
          color: 'bg-[#fff5e6] text-[#f59e0b] border-[#f59e0b]/30',
          icon: FileClock,
        };
      case FormStatus.FINAL:
        return {
          label: 'Final',
          color: 'bg-[#ebf4ff] text-[#469fe8] border-[#469fe8]/30',
          icon: FileCheck,
        };
      case FormStatus.SUBMITTED:
        return {
          label: 'Submitted',
          color: 'bg-[#d5faeb] text-[#10b981] border-[#10b981]/30',
          icon: FileUp,
        };
      case FormStatus.AMENDED:
        return {
          label: 'Amended',
          color: 'bg-[#f3e8ff] text-[#970bed] border-[#970bed]/30',
          icon: FileEdit,
        };
      case FormStatus.SUPERSEDED:
        return {
          label: 'Superseded',
          color: 'bg-[#f8f9fa] text-[#5d6567] border-[#dcdede]',
          icon: FileX,
        };
      default:
        return {
          label: status,
          color: 'bg-[#f8f9fa] text-[#5d6567] border-[#dcdede]',
          icon: FileCheck,
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

export default FormStatusBadge;
