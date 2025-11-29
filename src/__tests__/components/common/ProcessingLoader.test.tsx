import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ProcessingLoader } from '../../../../components/ProcessingLoader';

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('ProcessingLoader Component', () => {
  it('should render loading indicator', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    // Check for loading animation elements - loader uses spinner
    expect(container.querySelector('svg, .spinner, .loader, [role="status"]')).toBeTruthy();
  });

  it('should display loader animation', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    // Check for loading animation elements
    expect(container.querySelector('svg, .spinner, .loader')).toBeTruthy();
  });

  it('should render successfully', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    expect(container).toBeTruthy();
  });
});
