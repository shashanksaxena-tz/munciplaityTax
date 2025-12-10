/**
 * Reconciliation Issues List Component
 * Displays all reconciliation issues grouped by severity with details
 */

import React, { useState, useMemo } from 'react';
import { AlertTriangle, CheckCircle, AlertOctagon, Info, XCircle, Check, ChevronDown, ChevronUp } from 'lucide-react';
import { ReconciliationIssue, ReconciliationIssueSeverity } from '../types';
import { ReconciliationStatusBadge } from './ReconciliationStatusBadge';

interface ReconciliationIssuesListProps {
  issues: ReconciliationIssue[];
  onResolveIssue?: (issueId: string, note: string) => void;
}

export const ReconciliationIssuesList: React.FC<ReconciliationIssuesListProps> = ({ 
  issues,
  onResolveIssue 
}) => {
  const [expandedIssues, setExpandedIssues] = useState<Record<string, boolean>>({});
  const [resolutionNotes, setResolutionNotes] = useState<Record<string, string>>({});

  const toggleExpanded = (issueId: string) => {
    setExpandedIssues(prev => ({ ...prev, [issueId]: !prev[issueId] }));
  };

  const handleResolveIssue = (issue: ReconciliationIssue) => {
    const note = resolutionNotes[issue.id] || '';
    if (onResolveIssue) {
      onResolveIssue(issue.id, note);
    }
  };

  const getSeverityColor = (severity: ReconciliationIssueSeverity | string) => {
    switch (severity) {
      case ReconciliationIssueSeverity.CRITICAL:
        return { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700' };
      case ReconciliationIssueSeverity.HIGH:
        return { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-700' };
      case ReconciliationIssueSeverity.MEDIUM:
        return { bg: 'bg-yellow-50', border: 'border-yellow-200', text: 'text-yellow-700' };
      case ReconciliationIssueSeverity.LOW:
        return { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700' };
      default:
        return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700' };
    }
  };

  const getSeverityIcon = (severity: ReconciliationIssueSeverity | string) => {
    switch (severity) {
      case ReconciliationIssueSeverity.CRITICAL:
        return <XCircle className="w-5 h-5 text-red-600" />;
      case ReconciliationIssueSeverity.HIGH:
        return <AlertOctagon className="w-5 h-5 text-orange-600" />;
      case ReconciliationIssueSeverity.MEDIUM:
        return <AlertTriangle className="w-5 h-5 text-yellow-600" />;
      case ReconciliationIssueSeverity.LOW:
        return <Info className="w-5 h-5 text-blue-600" />;
      default:
        return <Info className="w-5 h-5 text-gray-600" />;
    }
  };

  // Group issues by severity
  const groupedIssues = useMemo(() => {
    return {
      critical: issues.filter(i => i.severity === ReconciliationIssueSeverity.CRITICAL),
      high: issues.filter(i => i.severity === ReconciliationIssueSeverity.HIGH),
      medium: issues.filter(i => i.severity === ReconciliationIssueSeverity.MEDIUM),
      low: issues.filter(i => i.severity === ReconciliationIssueSeverity.LOW),
    };
  }, [issues]);

  const renderIssue = (issue: ReconciliationIssue) => {
    const isExpanded = expandedIssues[issue.id];
    const colors = getSeverityColor(issue.severity);

    return (
      <div key={issue.id} className={`${colors.bg} border ${colors.border} rounded-lg overflow-hidden`}>
        {/* Issue Header - Always Visible */}
        <div 
          className="p-4 cursor-pointer hover:bg-white/50 transition-colors"
          onClick={() => toggleExpanded(issue.id)}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 flex-1">
              {getSeverityIcon(issue.severity)}
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <ReconciliationStatusBadge 
                    severity={issue.severity} 
                    resolved={issue.resolved}
                  />
                  <span className="text-xs font-mono text-gray-500">{issue.period}</span>
                </div>
                <div className="font-medium text-gray-900 mb-1">{issue.issueType.replace(/_/g, ' ')}</div>
                <div className={`text-sm ${colors.text}`}>{issue.description}</div>
              </div>
            </div>
            
            <button className="text-gray-400 hover:text-gray-600 transition-colors">
              {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Expanded Details */}
        {isExpanded && (
          <div className="px-4 pb-4 space-y-4 border-t border-gray-200">
            {/* Values Comparison (if applicable) */}
            {(issue.expectedValue !== undefined || issue.actualValue !== undefined) && (
              <div className="flex items-center gap-6 text-sm bg-white rounded-lg p-3 mt-4">
                {issue.expectedValue !== undefined && (
                  <div className="text-center">
                    <div className="text-gray-500 text-xs mb-1">Expected</div>
                    <div className="font-mono font-bold text-gray-800">
                      ${issue.expectedValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </div>
                  </div>
                )}
                {issue.expectedValue !== undefined && issue.actualValue !== undefined && (
                  <div className="text-gray-300">vs</div>
                )}
                {issue.actualValue !== undefined && (
                  <div className="text-center">
                    <div className="text-gray-500 text-xs mb-1">Actual</div>
                    <div className="font-mono font-bold text-gray-800">
                      ${issue.actualValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </div>
                  </div>
                )}
                {issue.variance !== undefined && Math.abs(issue.variance) > 0.01 && (
                  <div className={`${colors.text} font-bold px-3 py-1 rounded text-xs ml-auto bg-white border ${colors.border}`}>
                    Variance: ${Math.abs(issue.variance).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    {issue.variancePercentage && Math.abs(issue.variancePercentage) > 0.1 && 
                      ` (${Math.abs(issue.variancePercentage).toFixed(1)}%)`}
                  </div>
                )}
              </div>
            )}

            {/* Dates (if applicable) */}
            {(issue.dueDate || issue.filingDate) && (
              <div className="flex gap-4 text-sm bg-white rounded-lg p-3">
                {issue.dueDate && (
                  <div>
                    <span className="text-gray-500">Due Date: </span>
                    <span className="font-medium text-gray-900">{issue.dueDate}</span>
                  </div>
                )}
                {issue.filingDate && (
                  <div>
                    <span className="text-gray-500">Filed: </span>
                    <span className="font-medium text-gray-900">{issue.filingDate}</span>
                  </div>
                )}
              </div>
            )}

            {/* Recommended Action */}
            {issue.recommendedAction && (
              <div className="bg-white rounded-lg p-3">
                <div className="text-xs font-bold text-gray-500 mb-1">RECOMMENDED ACTION</div>
                <div className="text-sm text-gray-700">{issue.recommendedAction}</div>
              </div>
            )}

            {/* Resolution Section */}
            {!issue.resolved && issue.severity !== ReconciliationIssueSeverity.CRITICAL && (
              <div className="space-y-2 pt-2">
                <textarea
                  value={resolutionNotes[issue.id] || ''}
                  onChange={(e) => setResolutionNotes(prev => ({ ...prev, [issue.id]: e.target.value }))}
                  placeholder="Enter a note explaining the resolution..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#970bed]"
                  rows={2}
                />
                <button
                  onClick={() => handleResolveIssue(issue)}
                  className="px-4 py-2 bg-[#10b981] text-white text-sm font-medium rounded-lg hover:bg-[#059669] flex items-center gap-2"
                >
                  <Check className="w-4 h-4" />
                  Mark as Resolved
                </button>
              </div>
            )}

            {/* Already Resolved */}
            {issue.resolved && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-3 flex items-start gap-2">
                <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="text-sm font-medium text-green-800">Resolved</div>
                  {issue.resolutionNote && (
                    <div className="text-xs text-green-700 mt-1">{issue.resolutionNote}</div>
                  )}
                  {issue.resolvedDate && (
                    <div className="text-xs text-green-600 mt-1">Resolved on {issue.resolvedDate}</div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  if (issues.length === 0) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-6 flex items-start gap-3">
        <CheckCircle className="w-6 h-6 text-green-600 mt-0.5" />
        <div>
          <h4 className="font-bold text-green-800 text-lg">No Reconciliation Issues</h4>
          <p className="text-sm text-green-700 mt-1">
            All W-1 filings are properly reconciled. No issues detected.
          </p>
        </div>
      </div>
    );
  }

  const totalIssues = issues.length;
  const resolvedCount = issues.filter(i => i.resolved).length;
  const unresolvedCount = totalIssues - resolvedCount;

  return (
    <div className="space-y-4">
      {/* Summary Header */}
      <div className="bg-white border border-gray-200 rounded-xl p-5">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] rounded-full">
            <AlertOctagon className="w-6 h-6 text-white" />
          </div>
          <div>
            <h4 className="font-bold text-gray-900 text-lg">Reconciliation Issues</h4>
            <p className="text-sm text-gray-600">
              {unresolvedCount} unresolved, {resolvedCount} resolved of {totalIssues} total
            </p>
          </div>
        </div>
        
        <div className="grid grid-cols-4 gap-3">
          <div className="bg-red-50 rounded-lg p-3 border border-red-100">
            <div className="text-2xl font-bold text-red-700">{groupedIssues.critical.length}</div>
            <div className="text-xs text-red-600 font-medium">CRITICAL</div>
          </div>
          <div className="bg-orange-50 rounded-lg p-3 border border-orange-100">
            <div className="text-2xl font-bold text-orange-700">{groupedIssues.high.length}</div>
            <div className="text-xs text-orange-600 font-medium">HIGH</div>
          </div>
          <div className="bg-yellow-50 rounded-lg p-3 border border-yellow-100">
            <div className="text-2xl font-bold text-yellow-700">{groupedIssues.medium.length}</div>
            <div className="text-xs text-yellow-600 font-medium">MEDIUM</div>
          </div>
          <div className="bg-blue-50 rounded-lg p-3 border border-blue-100">
            <div className="text-2xl font-bold text-blue-700">{groupedIssues.low.length}</div>
            <div className="text-xs text-blue-600 font-medium">LOW</div>
          </div>
        </div>
      </div>

      {/* CRITICAL Issues */}
      {groupedIssues.critical.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-red-900 text-sm uppercase tracking-wide">
            Critical Issues - Requires Immediate Action
          </h5>
          {groupedIssues.critical.map(renderIssue)}
        </div>
      )}

      {/* HIGH Issues */}
      {groupedIssues.high.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-orange-900 text-sm uppercase tracking-wide">
            High Priority Issues
          </h5>
          {groupedIssues.high.map(renderIssue)}
        </div>
      )}

      {/* MEDIUM Issues */}
      {groupedIssues.medium.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-yellow-900 text-sm uppercase tracking-wide">
            Medium Priority Issues
          </h5>
          {groupedIssues.medium.map(renderIssue)}
        </div>
      )}

      {/* LOW Issues */}
      {groupedIssues.low.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-blue-900 text-sm uppercase tracking-wide">
            Low Priority - Informational
          </h5>
          {groupedIssues.low.map(renderIssue)}
        </div>
      )}
    </div>
  );
};

export default ReconciliationIssuesList;
