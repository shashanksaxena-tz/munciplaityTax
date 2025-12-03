import React, { useState } from 'react';
import { TaxFormData, FormProvenance, ExtractionSummary, BoundingBox, FieldProvenance } from '../../types';
import { PdfViewer } from '../PdfViewer/PdfViewer';
import { FieldWithSource } from './FieldWithSource';
import { ExtractionFailures } from './ExtractionFailures';
import { FileText, SplitSquareHorizontal, Maximize2, Minimize2, Eye, EyeOff } from 'lucide-react';

interface SplitViewLayoutProps {
  pdfData: string;
  forms: TaxFormData[];
  formProvenances?: FormProvenance[];
  summary?: ExtractionSummary;
  onFieldClick?: (field: FieldProvenance, form: TaxFormData) => void;
}

export const SplitViewLayout: React.FC<SplitViewLayoutProps> = ({
  pdfData,
  forms,
  formProvenances,
  summary,
  onFieldClick
}) => {
  const [highlightedField, setHighlightedField] = useState<{
    fieldName: string;
    boundingBox?: BoundingBox;
    formType: string;
    pageNumber: number;
    confidence?: number;
  } | undefined>();
  const [currentPage, setCurrentPage] = useState(1);
  const [splitRatio, setSplitRatio] = useState<'equal' | 'pdf-large' | 'data-large'>('equal');
  const [showPdf, setShowPdf] = useState(true);

  // Handle field click - highlight in PDF
  const handleFieldClick = (fieldName: string, form: TaxFormData) => {
    // Find the provenance for this field
    const formProv = formProvenances?.find(p => p.formType === form.formType);
    const fieldProv = formProv?.fields.find(f => f.fieldName === fieldName);

    if (fieldProv) {
      setHighlightedField({
        fieldName: fieldProv.fieldName,
        boundingBox: fieldProv.boundingBox,
        formType: form.formType,
        pageNumber: fieldProv.pageNumber,
        confidence: fieldProv.confidence
      });
      setCurrentPage(fieldProv.pageNumber);
      
      if (onFieldClick) {
        onFieldClick(fieldProv, form);
      }
    } else if (formProv) {
      // Fall back to form-level provenance
      setHighlightedField({
        fieldName,
        boundingBox: formProv.boundingBox,
        formType: form.formType,
        pageNumber: formProv.pageNumber,
        confidence: formProv.formConfidence
      });
      setCurrentPage(formProv.pageNumber);
    }
  };

  // Clear highlight
  const clearHighlight = () => setHighlightedField(undefined);

  // Get all field provenances for current page
  const allFieldProvenances = formProvenances?.flatMap(fp => fp.fields) || [];

  // Determine split widths using standard Tailwind classes
  const getGridCols = () => {
    if (!showPdf) return 'grid-cols-1';
    switch (splitRatio) {
      case 'pdf-large': return 'grid-cols-3'; // PDF gets 2/3, data gets 1/3
      case 'data-large': return 'grid-cols-3'; // PDF gets 1/3, data gets 2/3
      default: return 'grid-cols-2';
    }
  };
  
  // Get col-span for PDF panel based on split ratio
  const getPdfColSpan = () => {
    if (splitRatio === 'pdf-large') return 'col-span-2';
    if (splitRatio === 'data-large') return 'col-span-1';
    return '';
  };
  
  // Get col-span for data panel based on split ratio
  const getDataColSpan = () => {
    if (splitRatio === 'pdf-large') return 'col-span-1';
    if (splitRatio === 'data-large') return 'col-span-2';
    return '';
  };

  return (
    <div className="h-full flex flex-col bg-slate-50 rounded-xl overflow-hidden">
      {/* Toolbar */}
      <div className="flex items-center justify-between px-4 py-2 bg-white border-b border-slate-200">
        <div className="flex items-center gap-2">
          <SplitSquareHorizontal className="w-5 h-5 text-slate-400" />
          <span className="text-sm font-medium text-slate-700">Split View</span>
        </div>
        
        <div className="flex items-center gap-2">
          {/* Toggle PDF visibility */}
          <button
            onClick={() => setShowPdf(!showPdf)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              showPdf 
                ? 'bg-indigo-100 text-indigo-700' 
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {showPdf ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
            {showPdf ? 'PDF Visible' : 'Show PDF'}
          </button>

          {/* Split ratio controls */}
          {showPdf && (
            <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden">
              <button
                onClick={() => setSplitRatio('pdf-large')}
                className={`px-3 py-1.5 text-sm ${splitRatio === 'pdf-large' ? 'bg-indigo-100 text-indigo-700' : 'hover:bg-slate-100'}`}
                title="Larger PDF"
              >
                <Maximize2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => setSplitRatio('equal')}
                className={`px-3 py-1.5 text-sm border-x border-slate-200 ${splitRatio === 'equal' ? 'bg-indigo-100 text-indigo-700' : 'hover:bg-slate-100'}`}
                title="Equal split"
              >
                50/50
              </button>
              <button
                onClick={() => setSplitRatio('data-large')}
                className={`px-3 py-1.5 text-sm ${splitRatio === 'data-large' ? 'bg-indigo-100 text-indigo-700' : 'hover:bg-slate-100'}`}
                title="Larger data panel"
              >
                <Minimize2 className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Split Content */}
      <div className={`flex-1 grid ${getGridCols()} gap-4 p-4 overflow-hidden`}>
        {/* PDF Panel */}
        {showPdf && (
          <div className={`h-full overflow-hidden ${getPdfColSpan()}`}>
            <PdfViewer
              pdfData={pdfData}
              currentPage={currentPage}
              onPageChange={setCurrentPage}
              highlightedField={highlightedField}
              fieldProvenances={allFieldProvenances}
              className="h-full"
            />
          </div>
        )}

        {/* Data Panel */}
        <div className={`h-full overflow-auto ${getDataColSpan()}`}>
          <div className="space-y-4">
            {/* Extraction Failures */}
            {summary?.skippedForms && summary.skippedForms.length > 0 && (
              <ExtractionFailures skippedForms={summary.skippedForms} />
            )}

            {/* Extracted Forms */}
            {forms.map((form, idx) => (
              <div key={idx} className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                <div className="px-4 py-3 bg-slate-50 border-b border-slate-200">
                  <div className="flex items-center gap-2">
                    <FileText className="w-5 h-5 text-indigo-600" />
                    <h3 className="font-bold text-slate-800">{form.formType}</h3>
                    {form.confidenceScore && (
                      <span className={`ml-auto px-2 py-0.5 text-xs font-medium rounded-full ${
                        form.confidenceScore >= 0.9 
                          ? 'bg-green-100 text-green-700' 
                          : form.confidenceScore >= 0.7 
                            ? 'bg-amber-100 text-amber-700'
                            : 'bg-red-100 text-red-700'
                      }`}>
                        {Math.round(form.confidenceScore * 100)}%
                      </span>
                    )}
                  </div>
                </div>
                
                <div className="p-4">
                  <FieldWithSource
                    form={form}
                    formProvenance={formProvenances?.find(p => p.formType === form.formType)}
                    highlightedField={highlightedField?.fieldName}
                    onFieldClick={(fieldName) => handleFieldClick(fieldName, form)}
                    onClearHighlight={clearHighlight}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SplitViewLayout;
