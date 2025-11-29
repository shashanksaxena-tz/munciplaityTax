import { describe, it, expect, beforeAll, afterAll, vi } from 'vitest';

describe('API Integration Tests', () => {
  const API_BASE_URL = 'http://localhost:8080/api/v1';
  let authToken: string;

  beforeAll(async () => {
    // Mock successful authentication
    authToken = 'test-token-123';
  });

  afterAll(() => {
    vi.clearAllMocks();
  });

  describe('Authentication API', () => {
    it('should login with valid credentials', async () => {
      const mockResponse = {
        token: 'test-token',
        user: { email: 'test@example.com', roles: ['ROLE_INDIVIDUAL'] }
      };

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      });

      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'test@example.com', password: 'password123' })
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(data.token).toBeDefined();
      expect(data.user.email).toBe('test@example.com');
    });

    it('should reject invalid credentials', async () => {
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: 'Invalid credentials' })
      });

      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'wrong@example.com', password: 'wrong' })
      });

      expect(response.ok).toBe(false);
      expect(response.status).toBe(401);
    });
  });

  describe('Submission API', () => {
    it('should create a new submission', async () => {
      const mockSubmission = {
        id: 'sub-123',
        tenantId: 'tenant-1',
        userId: 'user-1',
        formType: 'W1',
        status: 'SUBMITTED',
        taxYear: 2024
      };

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockSubmission
      });

      const response = await fetch(`${API_BASE_URL}/submissions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          tenantId: 'tenant-1',
          userId: 'user-1',
          formType: 'W1',
          taxYear: 2024
        })
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(data.status).toBe('SUBMITTED');
      expect(data.id).toBeDefined();
    });

    it('should get all submissions for a tenant', async () => {
      const mockSubmissions = [
        { id: 'sub-1', status: 'SUBMITTED' },
        { id: 'sub-2', status: 'APPROVED' }
      ];

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockSubmissions
      });

      const response = await fetch(`${API_BASE_URL}/submissions?tenantId=tenant-1`, {
        headers: { 'Authorization': `Bearer ${authToken}` }
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(Array.isArray(data)).toBe(true);
      expect(data.length).toBe(2);
    });

    it('should approve a submission', async () => {
      const mockApprovedSubmission = {
        id: 'sub-123',
        status: 'APPROVED',
        reviewedBy: 'auditor-1'
      };

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockApprovedSubmission
      });

      const response = await fetch(`${API_BASE_URL}/submissions/sub-123/approve?auditorId=auditor-1`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${authToken}` }
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(data.status).toBe('APPROVED');
      expect(data.reviewedBy).toBe('auditor-1');
    });

    it('should reject a submission', async () => {
      const mockRejectedSubmission = {
        id: 'sub-123',
        status: 'REJECTED',
        reviewedBy: 'auditor-1',
        auditorComments: 'Missing documentation'
      };

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockRejectedSubmission
      });

      const response = await fetch(`${API_BASE_URL}/submissions/sub-123/reject?auditorId=auditor-1`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify('Missing documentation')
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(data.status).toBe('REJECTED');
      expect(data.auditorComments).toBe('Missing documentation');
    });
  });

  describe('Tax Calculation API', () => {
    it('should calculate tax for withholding', async () => {
      const mockCalculation = {
        totalWages: 100000,
        taxDue: 2500,
        taxRate: 0.025
      };

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => mockCalculation
      });

      const response = await fetch(`${API_BASE_URL}/tax-engine/calculate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          formType: 'W1',
          totalWages: 100000,
          taxYear: 2024
        })
      });

      const data = await response.json();
      expect(response.ok).toBe(true);
      expect(data.taxDue).toBe(2500);
      expect(data.taxRate).toBe(0.025);
    });
  });
});
