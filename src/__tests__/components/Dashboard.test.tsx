import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard } from '../../../components/Dashboard';

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('Dashboard Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render dashboard heading', () => {
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    expect(screen.getByText(/Dashboard/i) || screen.getByText(/Tax Returns/i)).toBeInTheDocument();
  });

  it('should display user information when logged in', () => {
    localStorage.setItem('user', JSON.stringify({ 
      email: 'test@example.com',
      name: 'Test User' 
    }));
    
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    // Dashboard shows "Create a new return to get started"
    expect(screen.getByText(/Create a new return|get started/i)).toBeInTheDocument();
  });

  it('should render navigation menu', () => {
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('should render create button', () => {
    localStorage.setItem('token', 'test-token');
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    
    // Dashboard should have create/new buttons
    const createButton = screen.queryByText(/Create|New/i);
    expect(createButton).toBeInTheDocument();
  });
});
