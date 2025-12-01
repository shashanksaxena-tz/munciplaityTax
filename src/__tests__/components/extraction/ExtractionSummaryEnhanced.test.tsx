import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ExtractionSummary } from '../../../../components/ExtractionSummary';
import { TaxFormData, TaxFormType, ExtractionSummary as ExtractionSummaryType, FormProvenance } from '../../../../types';

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('ExtractionSummary Enhanced Features', () => {
  const mockForms: TaxFormData[] = [
    {
      id: '1',
      fileName: 'test.pdf',
      taxYear: 2024,
      formType: TaxFormType.W2,
      confidenceScore: 0.95,
      sourcePage: 1,
      extractionReason: 'Standard W-2 form detected',
      fieldConfidence: {
        federalWages: 0.98,
        localWages: 0.92,
        employer: 0.95
      },
      employer: 'Acme Corp',
      employerEin: '12-3456789',
      federalWages: 75000,
      medicareWages: 75000,
      localWages: 75000,
      localWithheld: 1500,
      locality: 'Dublin OH',
      employee: 'John Q. Taxpayer'
    } as any,
    {
      id: '2',
      fileName: 'test.pdf',
      taxYear: 2024,
      formType: TaxFormType.FEDERAL_1040,
      confidenceScore: 0.89,
      sourcePage: 2,
      extractionReason: 'Individual tax return detected',
      fieldConfidence: {
        totalIncome: 0.95,
        adjustedGrossIncome: 0.88
      },
      wages: 75000,
      totalIncome: 75000,
      adjustedGrossIncome: 68000,
      tax: 12500
    } as any
  ];

  const mockSummary: ExtractionSummaryType = {
    totalPagesScanned: 5,
    formsExtracted: 2,
    formsSkipped: 1,
    extractedFormTypes: ['W-2', 'Federal 1040'],
    skippedForms: [
      {
        formType: 'Instructions',
        pageNumber: 3,
        reason: 'Non-data page detected',
        suggestion: 'No action required'
      }
    ],
    overallConfidence: 0.92,
    confidenceByFormType: {
      'W-2': 0.95,
      'Federal 1040': 0.89
    },
    extractionDurationMs: 3500,
    modelUsed: 'gemini-2.5-flash'
  };

  const mockFormProvenances: FormProvenance[] = [
    {
      formType: 'W-2',
      pageNumber: 1,
      extractionReason: 'Standard W-2 form detected',
      formConfidence: 0.95,
      fields: [
        { fieldName: 'federalWages', pageNumber: 1, confidence: 0.98 },
        { fieldName: 'localWages', pageNumber: 1, confidence: 0.92 }
      ]
    },
    {
      formType: 'Federal 1040',
      pageNumber: 2,
      extractionReason: 'Individual tax return detected',
      formConfidence: 0.89,
      fields: [
        { fieldName: 'totalIncome', pageNumber: 2, confidence: 0.95 }
      ]
    }
  ];

  const mockOnConfirm = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display extraction complete message', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        formProvenances={mockFormProvenances}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('Extraction Complete!')).toBeTruthy();
  });

  it('should display form count', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    // There are multiple "2" texts - check for the forms count message
    expect(screen.getByText(/distinct tax form/)).toBeTruthy();
    // Look for the bold count number
    const formCountElements = screen.getAllByText('2');
    expect(formCountElements.length).toBeGreaterThan(0);
  });

  it('should display overall confidence score', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    // Multiple 92% elements might exist
    const confidenceElements = screen.getAllByText('92%');
    expect(confidenceElements.length).toBeGreaterThan(0);
  });

  it('should display extraction statistics', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('Pages Scanned')).toBeTruthy();
    expect(screen.getByText('Forms Extracted')).toBeTruthy();
    expect(screen.getByText('Forms Skipped')).toBeTruthy();
  });

  it('should display extraction duration', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('3.5s')).toBeTruthy();
  });

  it('should display model used', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText(/gemini-2.5-flash/i)).toBeTruthy();
  });

  it('should display extracted form types', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('W-2')).toBeTruthy();
    expect(screen.getByText('Federal 1040')).toBeTruthy();
  });

  it('should display page provenance for forms', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        formProvenances={mockFormProvenances}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    // Should show page numbers
    expect(screen.getByText('Page 1')).toBeTruthy();
    expect(screen.getByText('Page 2')).toBeTruthy();
  });

  it('should display confidence badges for each form', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    // Multiple confidence badges exist - use getAllByText
    const ninetyFivePercent = screen.getAllByText('95%');
    const eightyNinePercent = screen.getAllByText('89%');
    expect(ninetyFivePercent.length).toBeGreaterThan(0);
    expect(eightyNinePercent.length).toBeGreaterThan(0);
  });

  it('should display skipped pages section when forms are skipped', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('Skipped Pages')).toBeTruthy();
    // The reason text is split - use regex
    expect(screen.getByText(/Non-data page detected/i)).toBeTruthy();
  });

  it('should call onConfirm when proceed button is clicked', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    fireEvent.click(screen.getByText('Proceed to Review'));
    expect(mockOnConfirm).toHaveBeenCalledTimes(1);
  });

  it('should call onCancel when upload different file button is clicked', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    fireEvent.click(screen.getByText('Upload Different File'));
    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('should render without summary (backwards compatibility)', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('Extraction Complete!')).toBeTruthy();
    expect(screen.getByText('W-2')).toBeTruthy();
  });

  it('should show verified badge for high confidence forms', () => {
    renderWithRouter(
      <ExtractionSummary 
        forms={mockForms} 
        summary={mockSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    // Should have at least one verified badge
    const verifiedBadges = screen.getAllByText('Verified');
    expect(verifiedBadges.length).toBeGreaterThan(0);
  });

  it('should show review badge for lower confidence forms', () => {
    const lowConfidenceForms = [
      {
        ...mockForms[0],
        confidenceScore: 0.75
      }
    ];
    const lowConfidenceSummary = {
      ...mockSummary,
      overallConfidence: 0.75,
      confidenceByFormType: { 'W-2': 0.75 }
    };
    
    renderWithRouter(
      <ExtractionSummary 
        forms={lowConfidenceForms} 
        summary={lowConfidenceSummary}
        onConfirm={mockOnConfirm} 
        onCancel={mockOnCancel} 
      />
    );
    expect(screen.getByText('Review')).toBeTruthy();
  });
});
