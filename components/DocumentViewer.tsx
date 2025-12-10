import React, { useState, useEffect } from 'react';
import { Download, FileText, AlertCircle } from 'lucide-react';
import { PdfViewer } from './PdfViewer/PdfViewer';
import { SubmissionDocument, FormProvenance, BoundingBox } from '../types';
import { useToast } from '../contexts/ToastContext';
import { parseFieldProvenance, downloadDocument } from '../utils/documentUtils';

interface DocumentViewerProps {
  document: SubmissionDocument;
  submissionId: string;
  onClose?: () => void;
  highlightedField?: {
    fieldName: string;
    boundingBox?: BoundingBox;
    formType: string;
    pageNumber: number;
    confidence?: number;
  };
}

export function DocumentViewer({
  document,
  submissionId,
  onClose,
  highlightedField
}: DocumentViewerProps) {
  const [pdfData, setPdfData] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [provenance, setProvenance] = useState<FormProvenance[]>([]);
  const { showToast } = useToast();

  useEffect(() => {
    loadDocument();
  }, [document.id, document.fieldProvenance, submissionId]);

  const loadDocument = async () => {
    setLoading(true);
    setError('');

    try {
      // Load document provenance for field highlighting
      const parsedProvenance = parseFieldProvenance(document.fieldProvenance);
      setProvenance(parsedProvenance);

      // In production, this would fetch the actual PDF from the backend
      // For now, we'll use a placeholder or mock data
      // The API endpoint is: /api/v1/submissions/{submissionId}/documents/{documentId}
      const response = await fetch(
        `/api/v1/submissions/${submissionId}/documents/${document.id}`
      );

      if (!response.ok) {
        throw new Error('Failed to load document');
      }

      // Check if it's a PDF
      const contentType = response.headers.get('content-type');
      if (contentType?.includes('application/pdf')) {
        const blob = await response.blob();
        const reader = new FileReader();
        reader.onloadend = () => {
          setPdfData(reader.result as string);
          setLoading(false);
        };
        reader.readAsDataURL(blob);
      } else if (document.base64Data) {
        // Use embedded base64 data if available
        setPdfData(document.base64Data);
        setLoading(false);
      } else {
        throw new Error('Document is not a PDF or no data available');
      }
    } catch (err) {
      console.error('Error loading document:', err);
      setError(err instanceof Error ? err.message : 'Failed to load document');
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    try {
      await downloadDocument(submissionId, document.id, document.fileName);
      showToast('success', 'Document downloaded successfully');
    } catch (err) {
      console.error('Error downloading document:', err);
      showToast('error', 'Failed to download document');
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-8 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#970bed] mx-auto mb-4"></div>
          <p className="text-[#5d6567]">Loading document...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-8">
        <div className="text-center">
          <AlertCircle className="w-12 h-12 text-[#ec1656] mx-auto mb-4" />
          <h3 className="text-lg font-bold text-[#0f1012] mb-2">Failed to Load Document</h3>
          <p className="text-[#5d6567] mb-4">{error}</p>
          <button
            onClick={loadDocument}
            className="px-4 py-2 bg-[#469fe8] text-white rounded-lg hover:bg-[#3a8cce]"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      {/* Header */}
      <div className="px-4 py-3 border-b border-[#dcdede] flex items-center justify-between">
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <FileText className="w-5 h-5 text-[#469fe8] flex-shrink-0" />
          <h3 className="font-bold text-[#0f1012] truncate">{document.fileName}</h3>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleDownload}
            className="flex items-center gap-2 px-3 py-1.5 text-sm text-[#469fe8] hover:bg-[#e0f4ff] rounded-lg transition-colors"
            aria-label="Download document"
          >
            <Download className="w-4 h-4" />
            Download
          </button>
          {onClose && (
            <button
              onClick={onClose}
              className="px-3 py-1.5 text-sm text-[#5d6567] hover:bg-[#f8f9fa] rounded-lg transition-colors"
            >
              Close
            </button>
          )}
        </div>
      </div>

      {/* PDF Viewer */}
      <div className="p-4">
        {pdfData ? (
          <PdfViewer
            pdfData={pdfData}
            highlightedField={highlightedField}
            fieldProvenances={provenance}
            className="h-[600px]"
          />
        ) : (
          <div className="flex items-center justify-center h-[600px] bg-[#f8f9fa] rounded-lg">
            <p className="text-[#5d6567]">No document data available</p>
          </div>
        )}
      </div>
    </div>
  );
}
