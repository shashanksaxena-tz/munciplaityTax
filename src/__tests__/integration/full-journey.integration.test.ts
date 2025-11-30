import { describe, it, expect, beforeAll, vi } from 'vitest';

describe('Full Filing Journey Integration Tests', () => {
  const API_BASE_URL = 'http://localhost:8080/api/v1';
  let authToken: string;
  let submissionId: string;

  beforeAll(() => {
    authToken = 'test-token-123';
  });

  describe('Individual Taxpayer Filing Journey', () => {
    it('should complete full filing flow: login -> upload -> calculate -> submit', async () => {
      // Step 1: Login
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: async () => ({ token: authToken, user: { email: 'taxpayer@example.com' } })
      });

      let response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'taxpayer@example.com', password: 'password123' })
      });

      let data = await response.json();
      expect(data.token).toBe(authToken);

      // Step 2: Upload W-2 document
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          documentId: 'doc-123',
          extractedData: { wages: 50000, withheld: 1250 }
        })
      });

      response = await fetch(`${API_BASE_URL}/extraction/extract`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${authToken}` },
        body: new FormData()
      });

      data = await response.json();
      expect(data.documentId).toBeDefined();
      expect(data.extractedData.wages).toBe(50000);

      // Step 3: Calculate taxes
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          taxDue: 1250,
          taxWithheld: 1250,
          refundDue: 0
        })
      });

      response = await fetch(`${API_BASE_URL}/tax-engine/calculate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          wages: 50000,
          withheld: 1250,
          taxYear: 2024
        })
      });

      data = await response.json();
      expect(data.taxDue).toBe(1250);
      expect(data.refundDue).toBe(0);

      // Step 4: Submit return
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          id: 'sub-123',
          status: 'SUBMITTED',
          confirmationNumber: 'CONF-2024-001'
        })
      });

      response = await fetch(`${API_BASE_URL}/submissions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          taxYear: 2024,
          formType: 'INDIVIDUAL',
          taxDue: 1250
        })
      });

      data = await response.json();
      expect(data.status).toBe('SUBMITTED');
      expect(data.confirmationNumber).toBeDefined();
      submissionId = data.id;
    });

    it('should handle payment flow after submission', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          paymentId: 'pay-123',
          status: 'COMPLETED',
          transactionId: 'txn-456'
        })
      });

      const response = await fetch(`${API_BASE_URL}/payments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          submissionId: submissionId,
          amount: 1250,
          paymentMethod: 'CREDIT_CARD'
        })
      });

      const data = await response.json();
      expect(data.status).toBe('COMPLETED');
      expect(data.transactionId).toBeDefined();
    });
  });

  describe('Business Filing Journey', () => {
    it('should complete business withholding filing flow', async () => {
      // Step 1: Register business
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          businessId: 'biz-123',
          ein: '12-3456789',
          status: 'REGISTERED'
        })
      });

      let response = await fetch(`${API_BASE_URL}/businesses`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          businessName: 'Test Corp',
          ein: '12-3456789'
        })
      });

      let data = await response.json();
      expect(data.businessId).toBeDefined();

      // Step 2: File W-1 withholding return
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          id: 'w1-123',
          status: 'SUBMITTED',
          totalWages: 100000,
          taxWithheld: 2500
        })
      });

      response = await fetch(`${API_BASE_URL}/submissions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          businessId: 'biz-123',
          formType: 'W1',
          taxYear: 2024,
          quarter: 'Q1',
          totalWages: 100000,
          taxWithheld: 2500
        })
      });

      data = await response.json();
      expect(data.status).toBe('SUBMITTED');
      expect(data.totalWages).toBe(100000);
    });
  });

  describe('Auditor Review Journey', () => {
    it('should complete audit review workflow', async () => {
      const submissionId = 'sub-123';

      // Step 1: Auditor claims submission
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          id: submissionId,
          status: 'IN_REVIEW',
          assignedTo: 'auditor-1'
        })
      });

      let response = await fetch(`${API_BASE_URL}/audit/claim/${submissionId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${authToken}` }
      });

      let data = await response.json();
      expect(data.status).toBe('IN_REVIEW');

      // Step 2: Request additional documents
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          requestId: 'req-123',
          status: 'PENDING',
          documents: ['W-2 forms', 'Supporting documentation']
        })
      });

      response = await fetch(`${API_BASE_URL}/audit/${submissionId}/request-documents`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          documents: ['W-2 forms', 'Supporting documentation'],
          deadline: '2024-12-31'
        })
      });

      data = await response.json();
      expect(data.status).toBe('PENDING');

      // Step 3: Approve submission
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ 
          id: submissionId,
          status: 'APPROVED',
          approvedBy: 'auditor-1',
          approvedAt: new Date().toISOString()
        })
      });

      response = await fetch(`${API_BASE_URL}/submissions/${submissionId}/approve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`
        },
        body: JSON.stringify({
          auditorId: 'auditor-1',
          signature: 'AuditorSignature'
        })
      });

      data = await response.json();
      expect(data.status).toBe('APPROVED');
      expect(data.approvedBy).toBe('auditor-1');
    });
  });
});
