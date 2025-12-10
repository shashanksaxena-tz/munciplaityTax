/**
 * STUB: AI Extraction Foundation (T023-T026)
 * 
 * These services require integration with:
 * - extraction-service (Gemini AI)
 * - Bounding box coordinate extraction
 * - Model definitions for extraction results
 * 
 * Implementation notes for future development:
 * 
 * T023: Update GeminiExtractionService.java
 * - Add extractScheduleM1Fields() method
 * - Parse Form 1120 Schedule M-1 lines 1-10
 * - Parse Form 4562 depreciation schedules
 * - Return ScheduleXExtractionResult with 29 fields
 * 
 * T024: Update ExtractionPromptBuilder.java
 * - Add prompts for each of 29 Schedule X fields
 * - Include bounding box extraction requirements
 * - Add field-specific validation prompts
 * 
 * T025: Create ScheduleXExtractionResult.java
 * - Map of field name → ScheduleXFieldExtraction
 * - Each extraction includes: value, confidence, boundingBox, sourceDocument
 * - Support for multiple source documents (Form 1120 + Form 4562)
 * 
 * T026: Create ExtractionBoundingBox.java
 * - Page number, 4 vertices (x, y coordinates)
 * - Methods to highlight region in PDF viewer
 * 
 * Key Requirements:
 * - FR-039: AI extraction <10 seconds
 * - FR-042: Confidence scores per field
 * - Research R1: Bounding box coordinates for Constitution IV compliance
 */

// Placeholder - requires extraction-service integration
export const EXTRACTION_STUB = {
  note: "AI Extraction components (T023-T026) require integration with extraction-service and Gemini API",
  tasksRemaining: ["T023", "T024", "T025", "T026"],
  dependencies: [
    "backend/extraction-service configuration",
    "Gemini Vision API credentials",
    "Sample Form 1120/1065/4562 PDFs for testing"
  ]
};
