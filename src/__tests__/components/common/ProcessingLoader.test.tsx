import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ProcessingLoader } from '../../../../components/ProcessingLoader';

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('ProcessingLoader Component', () => {
  it('should render loading indicator with spinner', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    // ProcessingLoader uses Loader2 icon from lucide-react which renders as SVG
    expect(container.querySelector('svg')).toBeTruthy();
  });

  it('should display loader animation', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    // Check for SVG animation element
    expect(container.querySelector('svg')).toBeTruthy();
  });

  it('should render successfully', () => {
    const { container } = renderWithRouter(<ProcessingLoader />);
    expect(container).toBeTruthy();
  });
});
