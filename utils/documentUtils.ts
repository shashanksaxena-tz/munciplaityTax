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
