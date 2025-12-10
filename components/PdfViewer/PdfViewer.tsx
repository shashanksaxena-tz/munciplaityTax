import React, { useState, useCallback, useRef, useEffect } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { ZoomIn, ZoomOut, ChevronLeft, ChevronRight, Maximize2 } from 'lucide-react';
import { BoundingBox, FieldProvenance } from '../../types';
import { HighlightOverlay } from './HighlightOverlay';
import { FieldSourceTooltip } from './FieldSourceTooltip';

// Configure PDF.js worker - use HTTPS for security
pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface PdfViewerProps {
  pdfData: string; // Base64 encoded PDF or URL
  currentPage?: number;
  onPageChange?: (page: number) => void;
  highlightedField?: {
    fieldName: string;
    boundingBox?: BoundingBox;
    formType: string;
    pageNumber: number;
    confidence?: number;
  };
  fieldProvenances?: FieldProvenance[];
  className?: string;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({
  pdfData,
  currentPage = 1,
  onPageChange,
  highlightedField,
  fieldProvenances,
  className = ''
}) => {
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState<number>(currentPage);
  const [zoom, setZoom] = useState<number>(1.0);
  const [pageWidth, setPageWidth] = useState<number>(0);
  const [pageHeight, setPageHeight] = useState<number>(0);
  const [showTooltip, setShowTooltip] = useState<boolean>(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const pageRef = useRef<HTMLDivElement>(null);

  // Sync with external page changes
  useEffect(() => {
    if (highlightedField?.pageNumber && highlightedField.pageNumber !== pageNumber) {
      setPageNumber(highlightedField.pageNumber);
    }
  }, [highlightedField?.pageNumber]);

  const onDocumentLoadSuccess = useCallback(({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
  }, []);

  const onPageLoadSuccess = useCallback(({ width, height }: { width: number; height: number }) => {
    setPageWidth(width);
    setPageHeight(height);
  }, []);

  const goToPrevPage = () => {
    const newPage = Math.max(1, pageNumber - 1);
    setPageNumber(newPage);
    onPageChange?.(newPage);
  };

  const goToNextPage = () => {
    const newPage = Math.min(numPages, pageNumber + 1);
    setPageNumber(newPage);
    onPageChange?.(newPage);
  };

  const zoomIn = () => setZoom(prev => Math.min(3, prev + 0.25));
  const zoomOut = () => setZoom(prev => Math.max(0.5, prev - 0.25));
  const resetZoom = () => setZoom(1.0);

  // Prepare PDF source
  const pdfSource = pdfData.startsWith('data:') 
    ? pdfData 
    : pdfData.startsWith('http') 
      ? pdfData 
      : `data:application/pdf;base64,${pdfData}`;

  // Get fields for current page
  const currentPageFields = fieldProvenances?.filter(f => f.pageNumber === pageNumber) || [];

  return (
    <div className={`flex flex-col bg-[#f8f9fa] rounded-xl border border-[#dcdede] overflow-hidden ${className}`}>
      {/* Toolbar */}
      <div className="flex items-center justify-between px-4 py-2 bg-white border-b border-[#dcdede]">
        <div className="flex items-center gap-2">
          <button
            onClick={goToPrevPage}
            disabled={pageNumber <= 1}
            className="p-1.5 rounded-lg hover:bg-[#f8f9fa] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            title="Previous page"
          >
            <ChevronLeft className="w-5 h-5 text-[#5d6567]" />
          </button>
          <span className="text-sm font-medium text-[#102124] min-w-[80px] text-center">
            Page {pageNumber} of {numPages}
          </span>
          <button
            onClick={goToNextPage}
            disabled={pageNumber >= numPages}
            className="p-1.5 rounded-lg hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            title="Next page"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
        
        <div className="flex items-center gap-2">
          <button
            onClick={zoomOut}
            className="p-1.5 rounded-lg hover:bg-[#f8f9fa] transition-colors"
            title="Zoom out"
          >
            <ZoomOut className="w-5 h-5" />
          </button>
          <span className="text-sm font-medium text-[#5d6567] min-w-[60px] text-center">
            {Math.round(zoom * 100)}%
          </span>
          <button
            onClick={zoomIn}
            className="p-1.5 rounded-lg hover:bg-[#f8f9fa] transition-colors"
            title="Zoom in"
          >
            <ZoomIn className="w-5 h-5" />
          </button>
          <button
            onClick={resetZoom}
            className="p-1.5 rounded-lg hover:bg-[#f8f9fa] transition-colors"
            title="Reset zoom"
          >
            <Maximize2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* PDF Content */}
      <div 
        ref={containerRef}
        className="flex-1 overflow-auto p-4 flex items-start justify-center"
        style={{ minHeight: '500px' }}
      >
        <div 
          ref={pageRef}
          className="relative shadow-lg"
          style={{ transform: `scale(${zoom})`, transformOrigin: 'top center' }}
        >
          <Document
            file={pdfSource}
            onLoadSuccess={onDocumentLoadSuccess}
            loading={
              <div className="flex items-center justify-center p-8">
                <div className="animate-spin w-8 h-8 border-4 border-[#469fe8] border-t-transparent rounded-full" />
              </div>
            }
            error={
              <div className="p-8 text-center text-[#ec1656]">
                Failed to load PDF. Please try again.
              </div>
            }
          >
            <Page
              pageNumber={pageNumber}
              onLoadSuccess={onPageLoadSuccess}
              renderTextLayer={true}
              renderAnnotationLayer={true}
              className="bg-white"
            />
          </Document>

          {/* Highlight Overlay */}
          {highlightedField?.boundingBox && highlightedField.pageNumber === pageNumber && (
            <HighlightOverlay
              boundingBox={highlightedField.boundingBox}
              pageWidth={pageWidth}
              pageHeight={pageHeight}
              fieldName={highlightedField.fieldName}
              formType={highlightedField.formType}
              confidence={highlightedField.confidence}
              onMouseEnter={() => setShowTooltip(true)}
              onMouseLeave={() => setShowTooltip(false)}
            />
          )}

          {/* Field markers for all extracted fields on current page */}
          {currentPageFields.map((field, idx) => (
            field.boundingBox && (
              <HighlightOverlay
                key={`${field.fieldName}-${idx}`}
                boundingBox={field.boundingBox}
                pageWidth={pageWidth}
                pageHeight={pageHeight}
                fieldName={field.fieldName}
                isActive={highlightedField?.fieldName === field.fieldName}
                confidence={field.confidence}
                variant="marker"
              />
            )
          ))}

          {/* Tooltip */}
          {showTooltip && highlightedField && (
            <FieldSourceTooltip
              fieldName={highlightedField.fieldName}
              formType={highlightedField.formType}
              pageNumber={highlightedField.pageNumber}
              confidence={highlightedField.confidence}
              boundingBox={highlightedField.boundingBox}
              pageWidth={pageWidth}
              pageHeight={pageHeight}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default PdfViewer;
