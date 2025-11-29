import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Login from '../../../components/auth/Login';

// Mock fetch
global.fetch = vi.fn();

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('Login Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should render login form', () => {
    renderWithRouter(<Login />);
    expect(screen.getByLabelText(/email/i) || screen.getByPlaceholderText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i) || screen.getByPlaceholderText(/password/i)).toBeInTheDocument();
  });

  it('should render login button', () => {
    renderWithRouter(<Login />);
    expect(screen.getByRole('button', { name: /login/i }) || screen.getByText(/login/i)).toBeInTheDocument();
  });

  it('should handle email input change', () => {
    renderWithRouter(<Login />);
    const emailInput = screen.getByLabelText(/email/i) || screen.getByPlaceholderText(/email/i);
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    expect(emailInput).toHaveValue('test@example.com');
  });

  it('should handle password input change', () => {
    renderWithRouter(<Login />);
    const passwordInput = screen.getByLabelText(/password/i) || screen.getByPlaceholderText(/password/i);
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    expect(passwordInput).toHaveValue('password123');
  });

  it('should submit login form with valid credentials', async () => {
    const mockResponse = {
      token: 'test-token',
      user: { email: 'test@example.com', name: 'Test User' }
    };
    
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse
    });

    renderWithRouter(<Login />);
    
    const emailInput = screen.getByLabelText(/email/i) || screen.getByPlaceholderText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i) || screen.getByPlaceholderText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login/i }) || screen.getByText(/login/i);

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/login'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          }),
          body: expect.stringContaining('test@example.com')
        })
      );
    });
  });

  it('should display error on invalid credentials', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ message: 'Invalid credentials' })
    });

    renderWithRouter(<Login />);
    
    const emailInput = screen.getByLabelText(/email/i) || screen.getByPlaceholderText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i) || screen.getByPlaceholderText(/password/i);
    const submitButton = screen.getByRole('button', { name: /login/i }) || screen.getByText(/login/i);

    fireEvent.change(emailInput, { target: { value: 'wrong@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'wrongpassword' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/invalid/i) || screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('should validate required fields', async () => {
    renderWithRouter(<Login />);
    const submitButton = screen.getByRole('button', { name: /login/i }) || screen.getByText(/login/i);
    
    fireEvent.click(submitButton);
    
    // Should not call API if validation fails
    expect(global.fetch).not.toHaveBeenCalled();
  });
});
