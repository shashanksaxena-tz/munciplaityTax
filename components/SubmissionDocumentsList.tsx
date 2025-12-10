import React from 'react';
import { FileText, Download, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react';
import { SubmissionDocument, ExtractionStatus } from '../types';

interface SubmissionDocumentsListProps {
  documents: SubmissionDocument[];
  selectedDocumentId?: string;
  onDocumentSelect: (document: SubmissionDocument) => void;
  onDocumentDownload: (document: SubmissionDocument) => void;
}

export function SubmissionDocumentsList({
  documents,
  selectedDocumentId,
  onDocumentSelect,
  onDocumentDownload
}: SubmissionDocumentsListProps) {
  
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatDate = (dateStr: string): string => {
    return new Date(dateStr).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getExtractionStatusDisplay = (doc: SubmissionDocument) => {
    const status = doc.extractionStatus || 
      (doc.extractionConfidence !== undefined && doc.extractionConfidence !== null 
        ? ExtractionStatus.COMPLETED 
        : ExtractionStatus.NOT_APPLICABLE);

    switch (status) {
      case ExtractionStatus.COMPLETED:
        return {
          icon: <CheckCircle className="w-4 h-4 text-[#10b981]" />,
          text: 'Extracted',
          color: 'text-[#10b981]',
          confidence: doc.extractionConfidence
        };
      case ExtractionStatus.PROCESSING:
        return {
          icon: <Clock className="w-4 h-4 text-[#469fe8]" />,
          text: 'Processing',
          color: 'text-[#469fe8]'
        };
      case ExtractionStatus.FAILED:
        return {
          icon: <XCircle className="w-4 h-4 text-[#ec1656]" />,
          text: 'Failed',
          color: 'text-[#ec1656]'
        };
      case ExtractionStatus.PENDING:
        return {
          icon: <AlertCircle className="w-4 h-4 text-[#f59e0b]" />,
          text: 'Pending',
          color: 'text-[#f59e0b]'
        };
      default:
        return {
          icon: <FileText className="w-4 h-4 text-[#5d6567]" />,
          text: 'N/A',
          color: 'text-[#5d6567]'
        };
    }
  };

  if (documents.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-6 text-center">
        <FileText className="w-12 h-12 text-[#dcdede] mx-auto mb-3" />
        <p className="text-[#5d6567]">No documents attached to this submission</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="p-4 border-b border-[#dcdede]">
        <h3 className="text-lg font-bold text-[#0f1012] flex items-center gap-2">
          <FileText className="w-5 h-5" />
          Attached Documents ({documents.length})
        </h3>
      </div>
      
      <div className="divide-y divide-[#dcdede]">
        {documents.map((doc) => {
          const uploadDate = doc.uploadedAt || doc.uploadDate || '';
          const extractionStatus = getExtractionStatusDisplay(doc);
          const isSelected = selectedDocumentId === doc.id;

          return (
            <div
              key={doc.id}
              className={`p-4 cursor-pointer transition-colors hover:bg-[#f8f9fa] ${
                isSelected ? 'bg-[#e0f4ff] border-l-4 border-[#469fe8]' : ''
              }`}
              onClick={() => onDocumentSelect(doc)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <FileText className="w-5 h-5 text-[#469fe8] flex-shrink-0" />
                    <h4 className="font-semibold text-[#0f1012] truncate">
                      {doc.fileName}
                    </h4>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm text-[#5d6567] ml-7">
                    {doc.formType && (
                      <div>
                        <span className="font-medium">Type:</span> {doc.formType}
                      </div>
                    )}
                    <div>
                      <span className="font-medium">Size:</span> {formatFileSize(doc.fileSize)}
                    </div>
                    {uploadDate && (
                      <div>
                        <span className="font-medium">Uploaded:</span> {formatDate(uploadDate)}
                      </div>
                    )}
                    {doc.pageCount && (
                      <div>
                        <span className="font-medium">Pages:</span> {doc.pageCount}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3 mt-2 ml-7">
                    <div className={`flex items-center gap-1.5 text-sm ${extractionStatus.color}`}>
                      {extractionStatus.icon}
                      <span className="font-medium">{extractionStatus.text}</span>
                      {extractionStatus.confidence !== undefined && (
                        <span className="text-xs">
                          ({Math.round(extractionStatus.confidence * 100)}%)
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDocumentDownload(doc);
                  }}
                  className="ml-3 p-2 text-[#469fe8] hover:bg-[#e0f4ff] rounded-lg transition-colors flex-shrink-0"
                  title="Download document"
                >
                  <Download className="w-5 h-5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
