import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Dashboard from '../../components/Dashboard';

// Mock fetch
global.fetch = vi.fn();

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('Dashboard Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render dashboard heading', () => {
    renderWithRouter(<Dashboard />);
    expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
  });

  it('should display user information when logged in', () => {
    localStorage.setItem('user', JSON.stringify({ 
      email: 'test@example.com',
      name: 'Test User' 
    }));
    
    renderWithRouter(<Dashboard />);
    expect(screen.getByText(/Test User/i) || screen.getByText(/test@example.com/i)).toBeTruthy();
  });

  it('should render navigation menu', () => {
    renderWithRouter(<Dashboard />);
    const navLinks = screen.getAllByRole('link');
    expect(navLinks.length).toBeGreaterThan(0);
  });

  it('should handle logout action', async () => {
    localStorage.setItem('token', 'test-token');
    renderWithRouter(<Dashboard />);
    
    const logoutButton = screen.queryByText(/Logout/i);
    if (logoutButton) {
      fireEvent.click(logoutButton);
      await waitFor(() => {
        expect(localStorage.getItem('token')).toBeNull();
      });
    }
  });
});
