import React, { useState } from 'react';
import { SubmissionDocument, TaxFormData, FieldAuditTrail } from '../types';
import { FileText, Upload, Trash2, Eye, Link, CheckCircle, AlertTriangle, Paperclip, ExternalLink } from 'lucide-react';

interface SubmissionDocumentsProps {
  documents: SubmissionDocument[];
  forms: TaxFormData[];
  auditTrails?: FieldAuditTrail[];
  onUpload?: (files: FileList) => void;
  onRemove?: (documentId: string) => void;
  onViewDocument?: (document: SubmissionDocument) => void;
  readOnly?: boolean;
  showAuditTrail?: boolean;
}

export const SubmissionDocuments: React.FC<SubmissionDocumentsProps> = ({
  documents,
  forms,
  auditTrails,
  onUpload,
  onRemove,
  onViewDocument,
  readOnly = false,
  showAuditTrail = false
}) => {
  const [isDragging, setIsDragging] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!readOnly) setIsDragging(true);
  };

  const handleDragLeave = () => setIsDragging(false);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (!readOnly && onUpload && e.dataTransfer.files.length > 0) {
      onUpload(e.dataTransfer.files);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (onUpload && e.target.files && e.target.files.length > 0) {
      onUpload(e.target.files);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Get corrections that reference a document
  const getDocumentCorrections = (documentId: string): FieldAuditTrail[] => {
    return auditTrails?.filter(at => at.sourceDocumentId === documentId) || [];
  };

  // Get forms that are linked to this document
  const getLinkedForms = (documentId: string): TaxFormData[] => {
    return forms.filter(f => f.fileName === documents.find(d => d.id === documentId)?.fileName);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Paperclip className="w-5 h-5 text-[#babebf]" />
          <h3 className="text-lg font-semibold text-[#0f1012]">Supporting Documents</h3>
          <span className="px-2 py-0.5 text-xs font-medium bg-[#f0f0f0] text-[#5d6567] rounded-full">
            {documents.length} document{documents.length !== 1 ? 's' : ''}
          </span>
        </div>
      </div>

      {/* Upload Area (if not read-only) */}
      {!readOnly && (
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`
            relative border-2 border-dashed rounded-xl p-6 text-center transition-all
            ${isDragging 
              ? 'border-indigo-500 bg-[#ebf4ff]' 
              : 'border-[#dcdede] hover:border-slate-400 hover:bg-[#f8f9fa]'
            }
          `}
        >
          <input
            type="file"
            accept="application/pdf,image/jpeg,image/png,image/gif"
            multiple
            onChange={handleFileInput}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
          />
          <Upload className={`w-8 h-8 mx-auto mb-2 ${isDragging ? 'text-[#469fe8]' : 'text-[#babebf]'}`} />
          <p className="text-sm text-[#5d6567]">
            <span className="font-medium text-[#970bed]">Click to upload</span> or drag and drop
          </p>
          <p className="text-xs text-[#5d6567] mt-1">PDF or images up to 10MB</p>
        </div>
      )}

      {/* Document List */}
      <div className="space-y-3">
        {documents.length === 0 ? (
          <div className="text-center py-8 text-[#5d6567]">
            <FileText className="w-12 h-12 mx-auto mb-3 text-[#dcdede]" />
            <p className="text-sm">No supporting documents attached</p>
            {!readOnly && (
              <p className="text-xs mt-1">Upload documents to provide evidence for your return</p>
            )}
          </div>
        ) : (
          documents.map((doc) => {
            const linkedForms = getLinkedForms(doc.id);
            const corrections = getDocumentCorrections(doc.id);
            
            return (
              <div 
                key={doc.id}
                className="bg-white border border-[#dcdede] rounded-xl p-4 hover:border-[#dcdede] transition-colors"
              >
                <div className="flex items-start gap-4">
                  {/* Thumbnail or Icon */}
                  <div className="flex-shrink-0 w-12 h-12 bg-[#f0f0f0] rounded-lg flex items-center justify-center">
                    {doc.thumbnailUrl ? (
                      <img 
                        src={doc.thumbnailUrl} 
                        alt={doc.fileName}
                        className="w-full h-full object-cover rounded-lg"
                      />
                    ) : (
                      <FileText className="w-6 h-6 text-[#babebf]" />
                    )}
                  </div>
                  
                  {/* Document Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <h4 className="font-medium text-[#0f1012] truncate">{doc.fileName}</h4>
                      {linkedForms.length > 0 && (
                        <span className="flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-[#d5faeb] text-[#10b981] rounded-full">
                          <Link className="w-3 h-3" />
                          {linkedForms.length} form{linkedForms.length !== 1 ? 's' : ''} linked
                        </span>
                      )}
                    </div>
                    
                    <div className="flex items-center gap-3 mt-1 text-xs text-[#5d6567]">
                      <span>{formatFileSize(doc.fileSize)}</span>
                      <span>•</span>
                      <span>Uploaded {formatDate(doc.uploadedAt)}</span>
                    </div>

                    {/* Linked Forms */}
                    {linkedForms.length > 0 && (
                      <div className="flex flex-wrap gap-1.5 mt-2">
                        {linkedForms.map((form, idx) => (
                          <span 
                            key={idx}
                            className="px-2 py-0.5 text-xs bg-[#f0f0f0] text-[#5d6567] rounded"
                          >
                            {form.formType}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Corrections linked to this document */}
                    {showAuditTrail && corrections.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-[#dcdede]">
                        <p className="text-xs font-medium text-[#5d6567] mb-2">
                          Corrections using this document:
                        </p>
                        <div className="space-y-1">
                          {corrections.slice(0, 3).map((correction, idx) => (
                            <div key={idx} className="flex items-center gap-2 text-xs">
                              <AlertTriangle className="w-3 h-3 text-amber-500" />
                              <span className="text-[#5d6567]">
                                <strong>{correction.fieldName}</strong>: {String(correction.originalValue)} → {String(correction.correctedValue)}
                              </span>
                            </div>
                          ))}
                          {corrections.length > 3 && (
                            <p className="text-xs text-[#babebf]">+{corrections.length - 3} more</p>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                  
                  {/* Actions */}
                  <div className="flex items-center gap-2">
                    {onViewDocument && (
                      <button
                        onClick={() => onViewDocument(doc)}
                        className="p-2 text-[#babebf] hover:text-[#970bed] hover:bg-[#ebf4ff] rounded-lg transition-colors"
                        title="View document"
                      >
                        <Eye className="w-5 h-5" />
                      </button>
                    )}
                    {!readOnly && onRemove && (
                      <button
                        onClick={() => onRemove(doc.id)}
                        className="p-2 text-[#babebf] hover:text-[#ec1656] hover:bg-[#ec1656]/10 rounded-lg transition-colors"
                        title="Remove document"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Warning for missing documents */}
      {!readOnly && documents.length === 0 && forms.length > 0 && (
        <div className="flex items-start gap-3 p-4 bg-[#f59e0b]/10 border border-[#f59e0b]/20 rounded-xl">
          <AlertTriangle className="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-[#f59e0b]">No supporting documents attached</p>
            <p className="text-sm text-[#f59e0b] mt-1">
              Submissions without supporting documents may take longer to process. 
              Consider uploading your W-2s, 1099s, and other tax documents.
            </p>
          </div>
        </div>
      )}

      {/* Auditor Info (read-only mode) */}
      {readOnly && documents.length > 0 && (
        <div className="flex items-start gap-3 p-4 bg-[#f8f9fa] border border-[#dcdede] rounded-xl">
          <CheckCircle className="w-5 h-5 text-[#10b981] flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-[#0f1012]">Documents permanently attached</p>
            <p className="text-sm text-[#5d6567] mt-1">
              These documents are linked to this submission and cannot be modified. 
              Click any document to view or download.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SubmissionDocuments;
