import React, { useState, useEffect } from 'react';
import {
  AuditQueue,
  AuditTrail,
  AuditAction,
  DocumentRequest,
  AuditReport,
  AuditStatus,
  DocumentType,
  SubmissionDocument,
  FormProvenance,
  FieldProvenance
} from '../types';
import {
  FileText,
  CheckCircle,
  XCircle,
  FileQuestion,
  History,
  AlertTriangle,
  ArrowLeft
} from 'lucide-react';
import { useToast } from '../contexts/ToastContext';
import { SubmissionDocumentsList } from './SubmissionDocumentsList';
import { DocumentViewer } from './DocumentViewer';
import { ExtractionProvenanceDisplay } from './ExtractionProvenanceDisplay';
import { parseFieldProvenance } from '../utils/documentUtils';

interface ReturnReviewPanelProps {
  returnId: string;
  userId: string;
  onBack: () => void;
}

export function ReturnReviewPanel({ returnId, userId, onBack }: ReturnReviewPanelProps) {
  const [queueEntry, setQueueEntry] = useState<AuditQueue | null>(null);
  const [auditTrail, setAuditTrail] = useState<AuditTrail[]>([]);
  const [auditReport, setAuditReport] = useState<AuditReport | null>(null);
  const [documents, setDocuments] = useState<SubmissionDocument[]>([]);
  const [selectedDocument, setSelectedDocument] = useState<SubmissionDocument | null>(null);
  const [selectedFieldProvenance, setSelectedFieldProvenance] = useState<{
    field: FieldProvenance;
    formProvenance: FormProvenance;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();
  
  // Dialogs
  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [showDocRequestDialog, setShowDocRequestDialog] = useState(false);
  
  // Form states
  const [eSignature, setESignature] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectionDetails, setRejectionDetails] = useState('');
  const [resubmitDeadline, setResubmitDeadline] = useState('');
  
  // Document request states
  const [docRequestType, setDocRequestType] = useState<DocumentType | ''>('');
  const [docRequestDescription, setDocRequestDescription] = useState('');
  const [docRequestDeadline, setDocRequestDeadline] = useState('');

  useEffect(() => {
    loadReturnData();
  }, [returnId]);

  const loadReturnData = async () => {
    setLoading(true);
    try {
      // Load queue entry
      const queueRes = await fetch(`/api/v1/audit/queue/${returnId}`);
      if (queueRes.ok) {
        const queueData = await queueRes.json();
        setQueueEntry(queueData);
      }
      
      // Load audit trail
      const trailRes = await fetch(`/api/v1/audit/trail/${returnId}`);
      if (trailRes.ok) {
        const trailData = await trailRes.json();
        setAuditTrail(trailData);
      }
      
      // Load audit report
      const reportRes = await fetch(`/api/v1/audit/report/${returnId}`);
      if (reportRes.ok) {
        const reportData = await reportRes.json();
        setAuditReport(reportData);
      }
      
      // Load submission documents
      const docsRes = await fetch(`/api/v1/submissions/${returnId}/documents`);
      if (docsRes.ok) {
        const docsData = await docsRes.json();
        setDocuments(docsData);
        // Auto-select first document if available
        if (docsData.length > 0) {
          setSelectedDocument(docsData[0]);
        }
      }
    } catch (error) {
      console.error('Error loading return data:', error);
      showToast('error', 'Failed to load return data');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!eSignature) {
      showToast('warning', 'Please enter your password for e-signature');
      return;
    }
    
    try {
      // NOTE: In production, implement proper cryptographic signing
      // using Web Crypto API or server-side signing with PKI certificates.
      // The current btoa() encoding is a placeholder and NOT secure.
      const response = await fetch('/api/v1/audit/approve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          returnId,
          auditorId: userId,
          eSignature: btoa(eSignature) // TODO: Replace with proper crypto signing
        })
      });
      
      if (response.ok) {
        showToast('success', 'Return approved successfully');
        onBack();
      } else {
        showToast('error', 'Failed to approve return');
      }
    } catch (error) {
      console.error('Error approving return:', error);
      showToast('error', 'Error approving return');
    }
  };

  const handleReject = async () => {
    if (!rejectionReason || !rejectionDetails || !resubmitDeadline) {
      showToast('warning', 'Please fill in all required fields');
      return;
    }
    
    if (rejectionDetails.length < 50) {
      showToast('warning', 'Detailed explanation must be at least 50 characters');
      return;
    }
    
    try {
      const response = await fetch('/api/v1/audit/reject', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          returnId,
          auditorId: userId,
          reason: rejectionReason,
          detailedExplanation: rejectionDetails,
          resubmitDeadline
        })
      });
      
      if (response.ok) {
        showToast('success', 'Return rejected successfully');
        onBack();
      } else {
        showToast('error', 'Failed to reject return');
      }
    } catch (error) {
      console.error('Error rejecting return:', error);
      showToast('error', 'Error rejecting return');
    }
  };

  const handleRequestDocuments = async () => {
    if (!docRequestType || !docRequestDescription || !docRequestDeadline) {
      showToast('warning', 'Please fill in all required fields');
      return;
    }
    
    if (docRequestDescription.length < 20) {
      showToast('warning', 'Description must be at least 20 characters');
      return;
    }
    
    try {
      const response = await fetch('/api/v1/audit/request-docs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          returnId,
          auditorId: userId,
          documentType: docRequestType,
          description: docRequestDescription,
          deadline: docRequestDeadline,
          tenantId: queueEntry?.tenantId
        })
      });
      
      if (response.ok) {
        showToast('success', 'Document request sent successfully');
        setShowDocRequestDialog(false);
        setDocRequestType('');
        setDocRequestDescription('');
        setDocRequestDeadline('');
        loadReturnData(); // Reload to show updated status
      } else {
        showToast('error', 'Failed to send document request');
      }
    } catch (error) {
      console.error('Error requesting documents:', error);
      showToast('error', 'Error requesting documents');
    }
  };

  const handleDocumentSelect = (document: SubmissionDocument) => {
    setSelectedDocument(document);
    setSelectedFieldProvenance(null); // Clear field selection when switching documents
  };

  const handleDocumentDownload = async (document: SubmissionDocument) => {
    try {
      const response = await fetch(
        `/api/v1/submissions/${returnId}/documents/${document.id}`
      );

      if (!response.ok) {
        throw new Error('Failed to download document');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = document.fileName;
      window.document.body.appendChild(a);
      a.click();
      window.document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      
      showToast('success', 'Document downloaded successfully');
    } catch (error) {
      console.error('Error downloading document:', error);
      showToast('error', 'Failed to download document');
    }
  };

  const handleFieldClick = (field: FieldProvenance, formProvenance: FormProvenance) => {
    setSelectedFieldProvenance({ field, formProvenance });
  };

  const getHighlightedField = () => {
    if (!selectedFieldProvenance) return undefined;

    const { field, formProvenance } = selectedFieldProvenance;
    return {
      fieldName: field.fieldName,
      boundingBox: field.boundingBox,
      formType: formProvenance.formType,
      pageNumber: field.pageNumber || formProvenance.pageNumber,
      confidence: field.confidence
    };
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#970bed]"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8f9fa] p-6">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={onBack}
          className="flex items-center gap-2 text-[#5d6567] hover:text-[#0f1012] mb-4"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to Queue
        </button>
        
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-[#0f1012]">Return Review</h1>
            <p className="text-[#5d6567] mt-1">
              Taxpayer: {queueEntry?.taxpayerName || 'N/A'} | 
              Tax Year: {queueEntry?.taxYear || 'N/A'} | 
              Return ID: {returnId}
            </p>
          </div>
          
          {queueEntry?.status === AuditStatus.IN_REVIEW && (
            <div className="flex gap-3">
              <button
                onClick={() => setShowApproveDialog(true)}
                className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-[#10b981] to-[#059669] text-white rounded-lg hover:from-[#059669] hover:to-[#047857]"
              >
                <CheckCircle className="w-5 h-5" />
                Approve
              </button>
              <button
                onClick={() => setShowRejectDialog(true)}
                className="flex items-center gap-2 px-4 py-2 bg-[#ec1656] text-white rounded-lg hover:bg-[#d01149]"
              >
                <XCircle className="w-5 h-5" />
                Reject
              </button>
              <button
                onClick={() => setShowDocRequestDialog(true)}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                <FileQuestion className="w-5 h-5" />
                Request Docs
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Return Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Queue Info */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-bold text-[#0f1012] mb-4">Return Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-[#5d6567]">Status</p>
                <p className="font-semibold">{queueEntry?.status}</p>
              </div>
              <div>
                <p className="text-sm text-[#5d6567]">Priority</p>
                <p className="font-semibold">{queueEntry?.priority}</p>
              </div>
              <div>
                <p className="text-sm text-[#5d6567]">Risk Score</p>
                <p className={`font-semibold ${
                  (queueEntry?.riskScore || 0) >= 61 ? 'text-[#ec1656]' :
                  (queueEntry?.riskScore || 0) >= 21 ? 'text-[#f59e0b]' :
                  'text-[#10b981]'
                }`}>
                  {queueEntry?.riskScore || 0}
                </p>
              </div>
              <div>
                <p className="text-sm text-[#5d6567]">Flagged Issues</p>
                <p className="font-semibold">{queueEntry?.flaggedIssuesCount || 0}</p>
              </div>
              <div>
                <p className="text-sm text-[#5d6567]">Submission Date</p>
                <p className="font-semibold">
                  {queueEntry?.submissionDate ? formatDate(queueEntry.submissionDate) : 'N/A'}
                </p>
              </div>
              <div>
                <p className="text-sm text-[#5d6567]">Days in Queue</p>
                <p className="font-semibold">{queueEntry?.daysInQueue || 0} days</p>
              </div>
            </div>
          </div>

          {/* Audit Report */}
          {auditReport && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-bold text-[#0f1012] mb-4 flex items-center gap-2">
                <AlertTriangle className="w-6 h-6 text-yellow-500" />
                Automated Audit Report
              </h2>
              
              <div className="mb-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium">Risk Level</span>
                  <span className={`px-3 py-1 rounded-full text-sm font-semibold ${
                    auditReport.riskLevel === 'HIGH' ? 'bg-[#ec1656]/10 text-[#ec1656]' :
                    auditReport.riskLevel === 'MEDIUM' ? 'bg-[#f59e0b]/10 text-[#f59e0b]' :
                    'bg-[#d5faeb] text-[#10b981]'
                  }`}>
                    {auditReport.riskLevel}
                  </span>
                </div>
              </div>
              
              {auditReport.flaggedItems && auditReport.flaggedItems.length > 0 && (
                <div className="mb-4">
                  <h3 className="font-semibold mb-2">Flagged Items:</h3>
                  <ul className="list-disc list-inside space-y-1">
                    {auditReport.flaggedItems.map((item, idx) => (
                      <li key={idx} className="text-sm text-[#102124]">{item}</li>
                    ))}
                  </ul>
                </div>
              )}
              
              {auditReport.recommendedActions && auditReport.recommendedActions.length > 0 && (
                <div>
                  <h3 className="font-semibold mb-2">Recommended Actions:</h3>
                  <ul className="list-disc list-inside space-y-1">
                    {auditReport.recommendedActions.map((action, idx) => (
                      <li key={idx} className="text-sm text-[#102124]">{action}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Document List */}
          <SubmissionDocumentsList
            documents={documents}
            selectedDocumentId={selectedDocument?.id}
            onDocumentSelect={handleDocumentSelect}
            onDocumentDownload={handleDocumentDownload}
          />

          {/* Document Viewer */}
          {selectedDocument && (
            <DocumentViewer
              document={selectedDocument}
              submissionId={returnId}
              highlightedField={getHighlightedField()}
            />
          )}

          {/* Extraction Provenance */}
          {selectedDocument && selectedDocument.fieldProvenance && (
            <ExtractionProvenanceDisplay
              provenance={parseFieldProvenance(selectedDocument.fieldProvenance)}
              onFieldClick={handleFieldClick}
              selectedFieldName={selectedFieldProvenance?.field.fieldName}
            />
          )}
        </div>

        {/* Right Column - Audit Trail */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-bold text-[#0f1012] mb-4 flex items-center gap-2">
              <History className="w-6 h-6" />
              Audit Trail
            </h2>
            
            <div className="space-y-4">
              {auditTrail.length === 0 ? (
                <p className="text-[#5d6567] text-sm">No audit trail entries</p>
              ) : (
                auditTrail.map((entry) => (
                  <div key={entry.trailId} className="border-l-2 border-[#dcdede] pl-4 pb-4">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="font-semibold text-sm">{entry.eventType}</p>
                        <p className="text-xs text-[#5d6567]">
                          {formatDate(entry.timestamp)}
                        </p>
                        {entry.eventDetails && (
                          <p className="text-sm text-[#102124] mt-1">{entry.eventDetails}</p>
                        )}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Approval Dialog */}
      {showApproveDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="text-xl font-bold mb-4">Approve Return</h3>
            <p className="text-[#5d6567] mb-4">
              Are you sure you want to approve this return? This action cannot be undone.
            </p>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-[#102124] mb-2">
                Enter Password for E-Signature *
              </label>
              <input
                type="password"
                value={eSignature}
                onChange={(e) => setESignature(e.target.value)}
                className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                placeholder="Your password"
              />
            </div>
            
            <div className="flex items-center gap-3">
              <button
                onClick={handleApprove}
                className="flex-1 px-4 py-2 bg-gradient-to-r from-[#10b981] to-[#059669] text-white rounded-lg hover:from-[#059669] hover:to-[#047857]"
              >
                Approve
              </button>
              <button
                onClick={() => {
                  setShowApproveDialog(false);
                  setESignature('');
                }}
                className="flex-1 px-4 py-2 bg-gray-200 text-[#102124] rounded-lg hover:bg-gray-300"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Rejection Dialog */}
      {showRejectDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="text-xl font-bold mb-4">Reject Return</h3>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Rejection Reason *
                </label>
                <select
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                >
                  <option value="">Select reason...</option>
                  <option value="MISSING_SCHEDULES">Missing Schedules</option>
                  <option value="CALCULATION_ERRORS">Calculation Errors</option>
                  <option value="UNSUPPORTED_DEDUCTIONS">Unsupported Deductions</option>
                  <option value="MISSING_DOCUMENTATION">Missing Documentation</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Detailed Explanation * (min 50 characters)
                </label>
                <textarea
                  value={rejectionDetails}
                  onChange={(e) => setRejectionDetails(e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                  rows={4}
                  placeholder="Provide detailed explanation..."
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Resubmission Deadline *
                </label>
                <input
                  type="date"
                  value={resubmitDeadline}
                  onChange={(e) => setResubmitDeadline(e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                  min={new Date().toISOString().split('T')[0]}
                />
              </div>
            </div>
            
            <div className="flex items-center gap-3 mt-6">
              <button
                onClick={handleReject}
                className="flex-1 px-4 py-2 bg-[#ec1656] text-white rounded-lg hover:bg-[#d01149]"
              >
                Reject
              </button>
              <button
                onClick={() => {
                  setShowRejectDialog(false);
                  setRejectionReason('');
                  setRejectionDetails('');
                  setResubmitDeadline('');
                }}
                className="flex-1 px-4 py-2 bg-gray-200 text-[#102124] rounded-lg hover:bg-gray-300"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Document Request Dialog */}
      {showDocRequestDialog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h3 className="text-xl font-bold mb-4 flex items-center gap-2">
              <FileQuestion className="w-6 h-6 text-[#469fe8]" />
              Request Additional Documentation
            </h3>
            <p className="text-[#5d6567] mb-4 text-sm">
              Request specific documents from the taxpayer to support their tax return.
            </p>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Document Type *
                </label>
                <select
                  value={docRequestType}
                  onChange={(e) => setDocRequestType(e.target.value as DocumentType)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                >
                  <option value="">Select document type...</option>
                  <option value="GENERAL_LEDGER">General Ledger</option>
                  <option value="BANK_STATEMENTS">Bank Statements</option>
                  <option value="DEPRECIATION_SCHEDULE">Depreciation Schedule</option>
                  <option value="CONTRACTS">Contracts</option>
                  <option value="INVOICES">Invoices</option>
                  <option value="RECEIPTS">Receipts</option>
                  <option value="PAYROLL_RECORDS">Payroll Records</option>
                  <option value="TAX_RETURNS_PRIOR_YEAR">Prior Year Tax Returns</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Description * (min 20 characters)
                </label>
                <textarea
                  value={docRequestDescription}
                  onChange={(e) => setDocRequestDescription(e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                  rows={4}
                  placeholder="Please describe what specific documents are needed and why..."
                />
                <p
                  className={`text-xs mt-1 ${
                    docRequestDescription.length < 20 ? 'text-[#ec1656]' : 'text-[#10b981]'
                  }`}
                >
                  {docRequestDescription.length < 20
                    ? `${20 - docRequestDescription.length} more characters required`
                    : `${docRequestDescription.length} characters`}
                </p>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-[#102124] mb-2">
                  Submission Deadline *
                </label>
                <input
                  type="date"
                  value={docRequestDeadline}
                  onChange={(e) => setDocRequestDeadline(e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg"
                  min={new Date(Date.now() + 24*60*60*1000).toISOString().split('T')[0]}
                />
                <p className="text-xs text-[#5d6567] mt-1">
                  Taxpayer will be notified via email with upload instructions (minimum 1 day from now)
                </p>
              </div>
            </div>
            
            <div className="flex items-center gap-3 mt-6">
              <button
                onClick={handleRequestDocuments}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                Send Request
              </button>
              <button
                onClick={() => {
                  setShowDocRequestDialog(false);
                  setDocRequestType('');
                  setDocRequestDescription('');
                  setDocRequestDeadline('');
                }}
                className="flex-1 px-4 py-2 bg-gray-200 text-[#102124] rounded-lg hover:bg-gray-300"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
