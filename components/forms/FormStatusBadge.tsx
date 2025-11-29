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
          color: 'bg-yellow-100 text-yellow-800 border-yellow-300',
          icon: FileClock,
        };
      case FormStatus.FINAL:
        return {
          label: 'Final',
          color: 'bg-blue-100 text-blue-800 border-blue-300',
          icon: FileCheck,
        };
      case FormStatus.SUBMITTED:
        return {
          label: 'Submitted',
          color: 'bg-green-100 text-green-800 border-green-300',
          icon: FileUp,
        };
      case FormStatus.AMENDED:
        return {
          label: 'Amended',
          color: 'bg-purple-100 text-purple-800 border-purple-300',
          icon: FileEdit,
        };
      case FormStatus.SUPERSEDED:
        return {
          label: 'Superseded',
          color: 'bg-gray-100 text-gray-800 border-gray-300',
          icon: FileX,
        };
      default:
        return {
          label: status,
          color: 'bg-gray-100 text-gray-800 border-gray-300',
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
