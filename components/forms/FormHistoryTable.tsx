/**
 * Form History Table Component
 * Displays list of generated forms with versions and status
 */

import React from 'react';
import { Download, Eye, Calendar, FileText } from 'lucide-react';
import { FormStatusBadge } from './FormStatusBadge';
import { formGenerationService } from '../../src/services/formGenerationService';
import type { GeneratedForm } from '../../src/types/formTypes';

interface FormHistoryTableProps {
  forms: GeneratedForm[];
  onViewForm?: (form: GeneratedForm) => void;
  onDownloadForm?: (form: GeneratedForm) => void;
  className?: string;
}

export const FormHistoryTable: React.FC<FormHistoryTableProps> = ({
  forms,
  onViewForm,
  onDownloadForm,
  className = '',
}) => {
  const handleDownload = (form: GeneratedForm) => {
    const downloadUrl = formGenerationService.getDownloadUrl(form.generatedFormId);
    window.open(downloadUrl, '_blank');
    onDownloadForm?.(form);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  if (forms.length === 0) {
    return (
      <div className={`text-center py-8 text-gray-500 ${className}`}>
        <FileText className="w-12 h-12 mx-auto mb-3 opacity-50" />
        <p>No forms generated yet</p>
      </div>
    );
  }

  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="min-w-full bg-white border border-gray-200 rounded-lg">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Form
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Version
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Generated
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Details
            </th>
            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {forms.map((form) => (
            <tr key={form.generatedFormId} className="hover:bg-gray-50">
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center">
                  <FileText className="w-5 h-5 text-blue-600 mr-2" />
                  <div>
                    <div className="text-sm font-medium text-gray-900">Form {form.formCode}</div>
                    <div className="text-sm text-gray-500">Tax Year {form.taxYear}</div>
                  </div>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className="text-sm text-gray-900">v{form.version}</span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <FormStatusBadge status={form.status} />
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div className="flex items-center text-sm text-gray-500">
                  <Calendar className="w-4 h-4 mr-1" />
                  {formatDate(form.generatedDate)}
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                <div>{form.pageCount} pages</div>
                <div className="text-xs">{formatFileSize(form.fileSizeBytes)}</div>
                {form.isWatermarked && (
                  <div className="text-xs text-yellow-600">DRAFT watermark</div>
                )}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <div className="flex items-center justify-end gap-2">
                  {onViewForm && (
                    <button
                      onClick={() => onViewForm(form)}
                      className="text-blue-600 hover:text-blue-900"
                      title="View form"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={() => handleDownload(form)}
                    className="text-green-600 hover:text-green-900"
                    title="Download PDF"
                  >
                    <Download className="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default FormHistoryTable;
