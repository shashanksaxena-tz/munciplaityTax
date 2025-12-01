import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock fetch
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Import after mocking fetch
import { api } from '../../../../services/api';

describe('Extraction API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock localStorage
    Storage.prototype.getItem = vi.fn(() => 'mock-jwt-token');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('uploadAndExtract', () => {
    it('should call extraction endpoint with file', async () => {
      // Create a mock ReadableStream
      const mockStream = {
        getReader: () => ({
          read: vi.fn()
            .mockResolvedValueOnce({ 
              done: false, 
              value: new TextEncoder().encode('data: {"status":"SCANNING","progress":20}\n\n') 
            })
            .mockResolvedValueOnce({ 
              done: false, 
              value: new TextEncoder().encode('data: {"status":"COMPLETE","progress":100}\n\n') 
            })
            .mockResolvedValueOnce({ done: true, value: undefined })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const onProgress = vi.fn();

      await api.extraction.uploadAndExtract(file, onProgress);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/extraction/extract',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-jwt-token'
          })
        })
      );
      expect(onProgress).toHaveBeenCalled();
    });

    it('should include user API key in headers when provided', async () => {
      const mockStream = {
        getReader: () => ({
          read: vi.fn().mockResolvedValueOnce({ done: true, value: undefined })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const onProgress = vi.fn();

      await api.extraction.uploadAndExtract(file, onProgress, {
        geminiApiKey: 'user-provided-api-key',
        geminiModel: 'gemini-2.5-flash'
      });

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/extraction/extract',
        expect.objectContaining({
          headers: expect.objectContaining({
            'X-Gemini-Api-Key': 'user-provided-api-key',
            'X-Gemini-Model': 'gemini-2.5-flash'
          })
        })
      );
    });

    it('should not include API key header when not provided', async () => {
      const mockStream = {
        getReader: () => ({
          read: vi.fn().mockResolvedValueOnce({ done: true, value: undefined })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const onProgress = vi.fn();

      await api.extraction.uploadAndExtract(file, onProgress);

      const callHeaders = mockFetch.mock.calls[0][1].headers;
      expect(callHeaders['X-Gemini-Api-Key']).toBeUndefined();
    });

    it('should throw error when response is not ok', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        text: () => Promise.resolve('Unauthorized')
      });

      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const onProgress = vi.fn();

      await expect(api.extraction.uploadAndExtract(file, onProgress))
        .rejects
        .toThrow('Extraction failed: 401 Unauthorized');
    });

    it('should parse SSE events correctly', async () => {
      const events = [
        { status: 'SCANNING', progress: 15 },
        { status: 'ANALYZING', progress: 35 },
        { status: 'EXTRACTING', progress: 70 },
        { status: 'COMPLETE', progress: 100, result: { forms: [] } }
      ];

      let eventIndex = 0;
      const mockStream = {
        getReader: () => ({
          read: vi.fn().mockImplementation(() => {
            if (eventIndex < events.length) {
              const event = events[eventIndex];
              eventIndex++;
              return Promise.resolve({
                done: false,
                value: new TextEncoder().encode(`data: ${JSON.stringify(event)}\n\n`)
              });
            }
            return Promise.resolve({ done: true, value: undefined });
          })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const file = new File(['test content'], 'test.pdf', { type: 'application/pdf' });
      const receivedEvents: any[] = [];
      const onProgress = vi.fn((event) => receivedEvents.push(event));

      await api.extraction.uploadAndExtract(file, onProgress);

      expect(receivedEvents.length).toBe(4);
      expect(receivedEvents[0].status).toBe('SCANNING');
      expect(receivedEvents[3].status).toBe('COMPLETE');
    });
  });

  describe('uploadAndExtractBatch', () => {
    it('should call batch extraction endpoint with multiple files', async () => {
      const mockStream = {
        getReader: () => ({
          read: vi.fn().mockResolvedValueOnce({ done: true, value: undefined })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const files = [
        new File(['content1'], 'file1.pdf', { type: 'application/pdf' }),
        new File(['content2'], 'file2.pdf', { type: 'application/pdf' })
      ];
      const onProgress = vi.fn();

      await api.extraction.uploadAndExtractBatch(files, onProgress);

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/extraction/extract/batch',
        expect.objectContaining({
          method: 'POST'
        })
      );
    });

    it('should track file index in batch processing', async () => {
      const events = [
        { status: 'SCANNING', progress: 50 },
        { status: 'COMPLETE', progress: 100 },
        { status: 'SCANNING', progress: 50 },
        { status: 'COMPLETE', progress: 100 }
      ];

      let eventIndex = 0;
      const mockStream = {
        getReader: () => ({
          read: vi.fn().mockImplementation(() => {
            if (eventIndex < events.length) {
              const event = events[eventIndex];
              eventIndex++;
              return Promise.resolve({
                done: false,
                value: new TextEncoder().encode(`data: ${JSON.stringify(event)}\n\n`)
              });
            }
            return Promise.resolve({ done: true, value: undefined });
          })
        })
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        body: mockStream
      });

      const files = [
        new File(['content1'], 'file1.pdf', { type: 'application/pdf' }),
        new File(['content2'], 'file2.pdf', { type: 'application/pdf' })
      ];
      
      const progressCalls: { event: any; fileIndex: number; fileName: string }[] = [];
      const onProgress = vi.fn((event, fileIndex, fileName) => {
        progressCalls.push({ event, fileIndex, fileName });
      });

      await api.extraction.uploadAndExtractBatch(files, onProgress);

      // First file should have index 0
      expect(progressCalls.some(c => c.fileIndex === 0)).toBe(true);
    });
  });
});
