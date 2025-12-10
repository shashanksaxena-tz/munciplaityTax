/**
 * Utility functions for document handling
 */

import { FormProvenance, SubmissionDocument } from '../types';

/**
 * Gets the upload date from a SubmissionDocument, handling both field names
 * @param doc - The submission document
 * @returns The upload date string, or empty string if not available
 */
export function getDocumentUploadDate(doc: SubmissionDocument): string {
  return doc.uploadedAt || doc.uploadDate || '';
}

/**
 * Safely parses JSON field provenance data
 * @param fieldProvenanceJson - JSON string containing field provenance data
 * @returns Array of FormProvenance objects, or empty array if parsing fails
 */
export function parseFieldProvenance(fieldProvenanceJson?: string): FormProvenance[] {
  if (!fieldProvenanceJson) {
    return [];
  }

  try {
    const parsed = JSON.parse(fieldProvenanceJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.warn('Failed to parse field provenance:', error);
    return [];
  }
}

/**
 * Normalizes a value for comparison (handles null, undefined, empty string)
 * @param value - The value to normalize
 * @returns Normalized string value
 */
export function normalizeValue(value: any): string {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value).trim();
}

/**
 * Checks if two values are different after normalization
 * @param value1 - First value
 * @param value2 - Second value
 * @returns True if values are different
 */
export function isDifferentValue(value1: any, value2: any): boolean {
  return normalizeValue(value1) !== normalizeValue(value2);
}

/**
 * Downloads a document from the API
 * @param submissionId - The submission ID
 * @param documentId - The document ID
 * @param fileName - The filename to save as
 * @returns Promise that resolves when download completes
 * @throws Error if download fails
 */
export async function downloadDocument(
  submissionId: string,
  documentId: string,
  fileName: string
): Promise<void> {
  const response = await fetch(
    `/api/v1/submissions/${submissionId}/documents/${documentId}`
  );

  if (!response.ok) {
    throw new Error('Failed to download document');
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
