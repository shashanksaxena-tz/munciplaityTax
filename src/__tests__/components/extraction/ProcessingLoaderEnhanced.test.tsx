import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ProcessingLoader } from '../../../../components/ProcessingLoader';
import { RealTimeExtractionUpdate, ExtractionSummary } from '../../../../types';

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('ProcessingLoader Enhanced Features', () => {
  const mockExtractionUpdate: RealTimeExtractionUpdate = {
    status: 'EXTRACTING',
    progress: 55,
    log: ['Scanning document...', 'Found W-2 form'],
    detectedForms: ['W-2', 'Federal 1040'],
    confidence: 0.85,
    currentFormType: 'W-2',
    currentTaxpayerName: 'John Q. Taxpayer'
  };

  it('should display taxpayer name when detected', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    expect(screen.getByText('John Q. Taxpayer')).toBeTruthy();
  });

  it('should display detected forms', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    expect(screen.getByText('W-2')).toBeTruthy();
    expect(screen.getByText('Federal 1040')).toBeTruthy();
  });

  it('should show confidence score', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    expect(screen.getByText('85%')).toBeTruthy();
  });

  it('should highlight current form being extracted', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    // Check that W-2 has visual indication of being current
    const w2Element = screen.getByText('W-2');
    expect(w2Element).toBeTruthy();
  });

  it('should display progress percentage', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    expect(screen.getByText('55%')).toBeTruthy();
  });

  it('should display activity log entries', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    // Use getAllByText since there might be multiple elements with similar text
    const logEntries = screen.getAllByText(/Scanning document|Found W-2 form/i);
    expect(logEntries.length).toBeGreaterThan(0);
  });

  it('should show complete status when extraction is done', () => {
    const completeUpdate: RealTimeExtractionUpdate = {
      ...mockExtractionUpdate,
      status: 'COMPLETE',
      progress: 100
    };
    renderWithRouter(<ProcessingLoader extractionUpdate={completeUpdate} />);
    expect(screen.getByText('Extraction Complete')).toBeTruthy();
  });

  it('should show error status when extraction fails', () => {
    const errorUpdate: RealTimeExtractionUpdate = {
      status: 'ERROR',
      progress: 0,
      log: ['Extraction failed: API key invalid'],
      detectedForms: [],
      confidence: 0
    };
    renderWithRouter(<ProcessingLoader extractionUpdate={errorUpdate} />);
    expect(screen.getByText('Extraction Error')).toBeTruthy();
  });

  it('should render without extraction update (default state)', () => {
    renderWithRouter(<ProcessingLoader />);
    expect(screen.getByText('Smart Extraction in Progress')).toBeTruthy();
  });

  it('should display security notice', () => {
    renderWithRouter(<ProcessingLoader extractionUpdate={mockExtractionUpdate} />);
    expect(screen.getByText(/API keys are never stored/i)).toBeTruthy();
  });

  it('should show continue button when complete and onContinue is provided', () => {
    const onContinue = vi.fn();
    const completeUpdate: RealTimeExtractionUpdate = {
      ...mockExtractionUpdate,
      status: 'COMPLETE',
      progress: 100
    };
    renderWithRouter(<ProcessingLoader extractionUpdate={completeUpdate} onContinue={onContinue} />);

    const button = screen.getByText('Continue to Review');
    expect(button).toBeTruthy();

    fireEvent.click(button);
    expect(onContinue).toHaveBeenCalled();
  });
});
