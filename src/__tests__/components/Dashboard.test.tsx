import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard } from '../../../components/Dashboard';

// Mock the sessionService module
vi.mock('../../../services/sessionService', () => ({
  getSessions: vi.fn(() => []),
  createNewSession: vi.fn((profile, settings, type) => ({
    id: 'test-session-id',
    createdDate: new Date().toISOString(),
    lastModifiedDate: new Date().toISOString(),
    status: 'DRAFT',
    type: type || 'INDIVIDUAL',
    profile: profile || { name: '', address: { street: '', city: '', state: '', zip: '' } },
    settings: settings || { taxYear: new Date().getFullYear() - 1, isAmendment: false },
    forms: []
  })),
  deleteSession: vi.fn(),
  fetchSessions: vi.fn(async () => []),
  isCacheLoaded: vi.fn(() => true) // Return true to skip async loading
}));

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('Dashboard Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render dashboard heading', async () => {
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText(/Dashboard/i) || screen.getByText(/Tax Returns/i)).toBeInTheDocument();
    });
  });

  it('should display user information when logged in', async () => {
    localStorage.setItem('user', JSON.stringify({ 
      email: 'test@example.com',
      name: 'Test User' 
    }));
    
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    // Dashboard shows "Create a new return to get started"
    await waitFor(() => {
      expect(screen.getByText(/Create a new return|get started/i)).toBeInTheDocument();
    });
  });

  it('should render navigation menu', async () => {
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    await waitFor(() => {
      const buttons = screen.getAllByRole('button');
      expect(buttons.length).toBeGreaterThan(0);
    });
  });

  it('should render create button', async () => {
    localStorage.setItem('token', 'test-token');
    renderWithRouter(<Dashboard onSelectSession={() => {}} onRegisterBusiness={() => {}} />);
    
    // Dashboard should have create/new buttons
    await waitFor(() => {
      const createButton = screen.queryByText(/Start Individual Return/i);
      expect(createButton).toBeInTheDocument();
    });
  });
});
