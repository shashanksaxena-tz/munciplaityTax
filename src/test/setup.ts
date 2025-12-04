import '@testing-library/jest-dom';
import { vi } from 'vitest';
import React from 'react';

// Add custom matchers for better assertions
// This file runs before each test file

// Mock react-pdf for test environment (requires browser-specific APIs)
vi.mock('react-pdf', () => ({
  Document: ({ children }: { children: React.ReactNode }) => {
    return React.createElement('div', { 'data-testid': 'pdf-document' }, children);
  },
  Page: ({ pageNumber }: { pageNumber: number }) => {
    return React.createElement('div', { 'data-testid': 'pdf-page' }, `Page ${pageNumber}`);
  },
  pdfjs: {
    GlobalWorkerOptions: {
      workerSrc: ''
    },
    version: '3.0.0'
  }
}));

// Mock DOMMatrix for PDF.js canvas operations
if (typeof globalThis.DOMMatrix === 'undefined') {
  (globalThis as any).DOMMatrix = class DOMMatrix {
    constructor() {}
    static fromMatrix() { return new DOMMatrix(); }
    static fromFloat32Array() { return new DOMMatrix(); }
    static fromFloat64Array() { return new DOMMatrix(); }
    multiply() { return this; }
    inverse() { return this; }
    translate() { return this; }
    scale() { return this; }
    rotate() { return this; }
    a = 1; b = 0; c = 0; d = 1; e = 0; f = 0;
  };
}
