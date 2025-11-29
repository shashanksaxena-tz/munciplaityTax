/**
 * Form generation types
 */

export enum FormStatus {
  DRAFT = 'DRAFT',
  FINAL = 'FINAL',
  SUBMITTED = 'SUBMITTED',
  AMENDED = 'AMENDED',
  SUPERSEDED = 'SUPERSEDED',
}

export enum PackageType {
  ORIGINAL = 'ORIGINAL',
  AMENDED = 'AMENDED',
  EXTENSION = 'EXTENSION',
}

export interface FormTemplate {
  templateId: string;
  tenantId?: string;
  formCode: string;
  formName: string;
  templateFilePath: string;
  revisionDate: string;
  applicableYears: number[];
  fieldMappings: Record<string, string>;
  validationRules: Record<string, any>;
  isActive: boolean;
  createdAt: string;
}

export interface GeneratedForm {
  generatedFormId: string;
  tenantId: string;
  returnId: string;
  businessId: string;
  formCode: string;
  taxYear: number;
  version: number;
  status: FormStatus;
  generatedDate: string;
  generatedBy: string;
  pdfFilePath: string;
  xmlFilePath?: string;
  isWatermarked: boolean;
  pageCount: number;
  fileSizeBytes: number;
  formData: Record<string, any>;
}

export interface FormGenerationRequest {
  formCode: string;
  taxYear: number;
  returnId: string;
  businessId: string;
  tenantId: string;
  formData: Record<string, any>;
  includeWatermark?: boolean;
  generateXml?: boolean;
  userId?: string;
}

export interface FormGenerationResponse {
  generatedFormId?: string;
  formCode?: string;
  formName?: string;
  taxYear?: number;
  version?: number;
  status?: string;
  pdfUrl?: string;
  xmlUrl?: string;
  pageCount?: number;
  fileSizeBytes?: number;
  isWatermarked?: boolean;
  generatedDate?: string;
  message?: string;
  success: boolean;
}

export interface FilingPackage {
  packageId: string;
  tenantId: string;
  returnId: string;
  businessId: string;
  taxYear: number;
  packageType: PackageType;
  createdDate: string;
  createdBy: string;
  totalPages: number;
  packagePdfPath: string;
  tableOfContents: Record<string, number>;
  submissionDate?: string;
  confirmationNumber?: string;
  status: string;
}

export interface FilingPackageRequest {
  returnId: string;
  businessId: string;
  tenantId: string;
  taxYear: number;
  packageType: string;
  includedFormIds: string[];
  generateTableOfContents?: boolean;
  addBookmarks?: boolean;
  optimizeFileSize?: boolean;
  userId?: string;
}

export interface FilingPackageResponse {
  packageId?: string;
  returnId?: string;
  taxYear?: number;
  packageType?: string;
  totalPages?: number;
  formCount?: number;
  packagePdfUrl?: string;
  fileSizeBytes?: number;
  tableOfContents?: Record<string, number>;
  createdDate?: string;
  status?: string;
  message?: string;
  success: boolean;
}
