/**
 * Form Generation Service
 * API client for form generation operations
 */

import type {
  FormGenerationRequest,
  FormGenerationResponse,
  FilingPackageRequest,
  FilingPackageResponse,
  GeneratedForm,
  FormTemplate,
} from '../types/formTypes';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

class FormGenerationService {
  /**
   * Generate a new form
   */
  async generateForm(request: FormGenerationRequest): Promise<FormGenerationResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/forms/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error generating form:', error);
      throw error;
    }
  }

  /**
   * Get form metadata by ID
   */
  async getForm(formId: string): Promise<FormGenerationResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/forms/${formId}`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching form:', error);
      throw error;
    }
  }

  /**
   * Get download URL for form
   */
  getDownloadUrl(formId: string): string {
    return `${API_BASE_URL}/api/forms/${formId}/download`;
  }

  /**
   * Download form PDF
   */
  async downloadForm(formId: string): Promise<Blob> {
    try {
      const response = await fetch(this.getDownloadUrl(formId));

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const blob = await response.blob();
      return blob;
    } catch (error) {
      console.error('Error downloading form:', error);
      throw error;
    }
  }

  /**
   * Generate filing package
   */
  async generateFilingPackage(request: FilingPackageRequest): Promise<FilingPackageResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/filing-packages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error generating filing package:', error);
      throw error;
    }
  }

  /**
   * Get all form templates
   */
  async getFormTemplates(): Promise<FormTemplate[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/form-templates`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching form templates:', error);
      return [];
    }
  }
}

export const formGenerationService = new FormGenerationService();
export default formGenerationService;
