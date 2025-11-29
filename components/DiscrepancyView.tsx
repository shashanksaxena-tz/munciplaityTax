
import React, { useState, useCallback } from 'react';
import { AlertTriangle, CheckCircle, AlertOctagon, Info, XCircle, Check, AlertCircle } from 'lucide-react';
import { DiscrepancyReport, DiscrepancyIssue } from '../types';

interface DiscrepancyViewProps {
  report: DiscrepancyReport;
  onAcceptIssue?: (issueId: string, note: string) => void;
}

// Constants
const MINIMUM_DIFFERENCE_THRESHOLD = 0.01;

export const DiscrepancyView: React.FC<DiscrepancyViewProps> = ({ report, onAcceptIssue }) => {
  const [acceptanceNotes, setAcceptanceNotes] = useState<Record<string, string>>({});
  const [expandedIssues, setExpandedIssues] = useState<Record<string, boolean>>({});

  const getSeverityIcon = useCallback((severity: string) => {
    switch (severity) {
      case 'HIGH': return <XCircle className="w-5 h-5 text-red-600" />;
      case 'MEDIUM': return <AlertTriangle className="w-5 h-5 text-yellow-600" />;
      case 'LOW': return <Info className="w-5 h-5 text-blue-600" />;
      default: return <AlertCircle className="w-5 h-5 text-gray-600" />;
    }
  }, []);

  const getSeverityColor = useCallback((severity: string) => {
    switch (severity) {
      case 'HIGH': return { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700', badge: 'bg-red-100 text-red-800' };
      case 'MEDIUM': return { bg: 'bg-yellow-50', border: 'border-yellow-200', text: 'text-yellow-700', badge: 'bg-yellow-100 text-yellow-800' };
      case 'LOW': return { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', badge: 'bg-blue-100 text-blue-800' };
      default: return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', badge: 'bg-gray-100 text-gray-800' };
    }
  }, []);

  const handleAcceptIssue = useCallback((issue: DiscrepancyIssue) => {
    const note = acceptanceNotes[issue.issueId] || '';
    if (onAcceptIssue) {
      onAcceptIssue(issue.issueId, note);
    }
  }, [acceptanceNotes, onAcceptIssue]);

  const toggleExpanded = useCallback((issueId: string) => {
    setExpandedIssues(prev => ({ ...prev, [issueId]: !prev[issueId] }));
  }, []);

  const renderIssue = useCallback((issue: DiscrepancyIssue, colors: ReturnType<typeof getSeverityColor>) => {
    const isExpanded = expandedIssues[issue.issueId];

    return (
      <div className={`${colors.bg} border ${colors.border} rounded-lg p-4 space-y-3`}>
        {/* Issue Header */}
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3 flex-1">
            {getSeverityIcon(issue.severity)}
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-bold uppercase tracking-wider text-gray-500">{issue.category}</span>
                <span className={`text-xs font-bold px-2 py-0.5 rounded ${colors.badge}`}>{issue.severity}</span>
              </div>
              <div className="font-medium text-gray-900">{issue.field}</div>
              <div className={`text-sm ${colors.text} mt-1`}>{issue.message}</div>
            </div>
          </div>
          
          {issue.severity !== 'HIGH' && !issue.isAccepted && (
            <button
              onClick={() => toggleExpanded(issue.issueId)}
              className="text-sm font-medium text-gray-600 hover:text-gray-900 whitespace-nowrap"
            >
              {isExpanded ? 'Hide Details' : 'View Details'}
            </button>
          )}
        </div>

        {/* Values Comparison */}
        <div className="flex items-center gap-6 text-sm bg-white rounded-lg p-3">
          <div className="text-center">
            <div className="text-gray-500 text-xs mb-1">Calculated</div>
            <div className="font-mono font-bold text-gray-800">${issue.sourceValue.toLocaleString()}</div>
          </div>
          <div className="text-gray-300">vs</div>
          <div className="text-center">
            <div className="text-gray-500 text-xs mb-1">Reported</div>
            <div className="font-mono font-bold text-gray-800">${issue.formValue.toLocaleString()}</div>
          </div>
          {Math.abs(issue.difference) > MINIMUM_DIFFERENCE_THRESHOLD && (
            <div className={`${colors.badge} font-bold px-3 py-1 rounded text-xs ml-auto`}>
              Diff: ${Math.abs(issue.difference).toLocaleString()}
              {issue.differencePercent && Math.abs(issue.differencePercent) > 0.1 && 
                ` (${Math.abs(issue.differencePercent).toFixed(1)}%)`}
            </div>
          )}
        </div>

        {/* Expanded Details */}
        {isExpanded && (
          <div className="space-y-3 pt-2 border-t border-gray-200">
            {issue.recommendedAction && (
              <div className="bg-white rounded-lg p-3">
                <div className="text-xs font-bold text-gray-500 mb-1">RECOMMENDED ACTION</div>
                <div className="text-sm text-gray-700">{issue.recommendedAction}</div>
              </div>
            )}

            {issue.ruleId && (
              <div className="text-xs text-gray-500">
                Rule ID: {issue.ruleId}
              </div>
            )}

            {/* Accept Button for LOW/MEDIUM severity */}
            {issue.severity !== 'HIGH' && !issue.isAccepted && (
              <div className="space-y-2">
                <textarea
                  value={acceptanceNotes[issue.issueId] || ''}
                  onChange={(e) => setAcceptanceNotes(prev => ({ ...prev, [issue.issueId]: e.target.value }))}
                  placeholder="Enter a note explaining why you're accepting this issue..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={2}
                />
                <button
                  onClick={() => handleAcceptIssue(issue)}
                  className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 flex items-center gap-2"
                >
                  <Check className="w-4 h-4" />
                  Accept & Continue
                </button>
              </div>
            )}

            {/* Accepted Status */}
            {issue.isAccepted && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-3 flex items-start gap-2">
                <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="text-sm font-medium text-green-800">Accepted</div>
                  {issue.acceptanceNote && (
                    <div className="text-xs text-green-700 mt-1">{issue.acceptanceNote}</div>
                  )}
                  {issue.acceptedDate && (
                    <div className="text-xs text-green-600 mt-1">Accepted on {issue.acceptedDate}</div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    );
  }, [expandedIssues, acceptanceNotes, getSeverityIcon, getSeverityColor, toggleExpanded, handleAcceptIssue]);

  if (!report.hasDiscrepancies) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-start gap-3">
        <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
        <div>
          <h4 className="font-bold text-green-800">No Discrepancies Found</h4>
          <p className="text-sm text-green-700">Your uploaded Local 1040 matches the calculated values from your source documents.</p>
        </div>
      </div>
    );
  }

  // Group issues by severity
  const highIssues = report.issues.filter(i => i.severity === 'HIGH');
  const mediumIssues = report.issues.filter(i => i.severity === 'MEDIUM');
  const lowIssues = report.issues.filter(i => i.severity === 'LOW');

  return (
    <div className="space-y-4">
      {/* Summary Header */}
      <div className="bg-white border border-gray-200 rounded-xl p-5">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-red-100 rounded-full">
            <AlertOctagon className="w-6 h-6 text-red-600" />
          </div>
          <div>
            <h4 className="font-bold text-gray-900 text-lg">Validation Results</h4>
            <p className="text-sm text-gray-600">
              {report.summary ? `${report.summary.totalIssues} issue${report.summary.totalIssues !== 1 ? 's' : ''} detected` : `${report.issues.length} issues detected`}
            </p>
          </div>
        </div>
        
        {report.summary && (
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-red-50 rounded-lg p-3 border border-red-100">
              <div className="text-2xl font-bold text-red-700">{report.summary.highSeverityCount}</div>
              <div className="text-xs text-red-600 font-medium">HIGH - Must Fix</div>
            </div>
            <div className="bg-yellow-50 rounded-lg p-3 border border-yellow-100">
              <div className="text-2xl font-bold text-yellow-700">{report.summary.mediumSeverityCount}</div>
              <div className="text-xs text-yellow-600 font-medium">MEDIUM - Warning</div>
            </div>
            <div className="bg-blue-50 rounded-lg p-3 border border-blue-100">
              <div className="text-2xl font-bold text-blue-700">{report.summary.lowSeverityCount}</div>
              <div className="text-xs text-blue-600 font-medium">LOW - Info</div>
            </div>
          </div>
        )}

        {report.summary?.blocksFiling && (
          <div className="mt-4 bg-red-100 border border-red-300 rounded-lg p-3 flex items-start gap-2">
            <XCircle className="w-5 h-5 text-red-700 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-red-800 font-medium">
              Filing is blocked until all HIGH severity issues are resolved.
            </p>
          </div>
        )}
      </div>

      {/* HIGH Severity Issues */}
      {highIssues.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-red-900 text-sm uppercase tracking-wide">High Severity - Must Fix</h5>
          {highIssues.map(issue => (
            <React.Fragment key={issue.issueId}>
              {renderIssue(issue, getSeverityColor('HIGH'))}
            </React.Fragment>
          ))}
        </div>
      )}

      {/* MEDIUM Severity Issues */}
      {mediumIssues.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-yellow-900 text-sm uppercase tracking-wide">Medium Severity - Warnings</h5>
          {mediumIssues.map(issue => (
            <React.Fragment key={issue.issueId}>
              {renderIssue(issue, getSeverityColor('MEDIUM'))}
            </React.Fragment>
          ))}
        </div>
      )}

      {/* LOW Severity Issues */}
      {lowIssues.length > 0 && (
        <div className="space-y-3">
          <h5 className="font-bold text-blue-900 text-sm uppercase tracking-wide">Low Severity - Informational</h5>
          {lowIssues.map(issue => (
            <React.Fragment key={issue.issueId}>
              {renderIssue(issue, getSeverityColor('LOW'))}
            </React.Fragment>
          ))}
        </div>
      )}
    </div>
  );
};
