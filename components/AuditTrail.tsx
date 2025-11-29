import React, { useState, useEffect } from 'react';

/**
 * T074-T076 - Audit Trail Component
 * 
 * Displays audit history for journal entries, payments, and other ledger operations
 * with timeline view and filtering capabilities.
 */

interface AuditLog {
  auditId: string;
  entityId: string;
  entityType: string;
  action: string;
  userId: string;
  timestamp: string;
  details?: string;
  oldValue?: string;
  newValue?: string;
  reason?: string;
  tenantId: string;
}

interface AuditTrailProps {
  entityId?: string;
  journalEntryId?: string;
  tenantId: string;
  userId: string;
  showFilters?: boolean;
}

export const AuditTrail: React.FC<AuditTrailProps> = ({
  entityId,
  journalEntryId,
  tenantId,
  userId,
  showFilters = true
}) => {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // T076 - Filter state
  const [filterAction, setFilterAction] = useState<string>('');
  const [filterUser, setFilterUser] = useState<string>('');
  const [filterDateFrom, setFilterDateFrom] = useState<string>('');
  const [filterDateTo, setFilterDateTo] = useState<string>('');

  useEffect(() => {
    fetchAuditTrail();
  }, [entityId, journalEntryId, tenantId]);

  const fetchAuditTrail = async () => {
    try {
      setLoading(true);
      setError(null);

      let url: string;
      
      if (journalEntryId) {
        // T073 - Get audit trail for specific journal entry
        url = `/api/v1/audit/journal-entries/${journalEntryId}?userId=${userId}&tenantId=${tenantId}`;
      } else if (entityId) {
        // Get audit trail for specific entity
        url = `/api/v1/audit/entity/${entityId}?userId=${userId}&tenantId=${tenantId}`;
      } else {
        // Get all tenant audit logs
        url = `/api/v1/audit/tenant/${tenantId}`;
      }

      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error(`Failed to fetch audit trail: ${response.statusText}`);
      }

      const data = await response.json();
      setAuditLogs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit trail');
      console.error('Error fetching audit trail:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * T076 - Apply filters to audit logs
   */
  const getFilteredLogs = () => {
    return auditLogs.filter(log => {
      // Filter by action
      if (filterAction && log.action !== filterAction) {
        return false;
      }
      
      // Filter by user
      if (filterUser && log.userId !== filterUser) {
        return false;
      }
      
      // Filter by date range
      const logDate = new Date(log.timestamp);
      if (filterDateFrom) {
        const fromDate = new Date(filterDateFrom);
        if (logDate < fromDate) return false;
      }
      if (filterDateTo) {
        const toDate = new Date(filterDateTo);
        toDate.setHours(23, 59, 59, 999); // End of day
        if (logDate > toDate) return false;
      }
      
      return true;
    });
  };

  const clearFilters = () => {
    setFilterAction('');
    setFilterUser('');
    setFilterDateFrom('');
    setFilterDateTo('');
  };

  /**
   * T075 - Format timestamp for timeline display
   */
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return {
      date: date.toLocaleDateString(),
      time: date.toLocaleTimeString()
    };
  };

  /**
   * Get action icon based on action type
   */
  const getActionIcon = (action: string) => {
    switch (action) {
      case 'CREATE':
        return '✨';
      case 'UPDATE':
      case 'MODIFY':
        return '✏️';
      case 'DELETE':
        return '🗑️';
      case 'REVERSE':
        return '↩️';
      case 'POST':
      case 'POSTED':
        return '✓';
      case 'VIEW':
      case 'VIEW_AUDIT':
        return '👁️';
      default:
        return '📝';
    }
  };

  /**
   * Get action color based on action type
   */
  const getActionColor = (action: string) => {
    switch (action) {
      case 'CREATE':
        return 'text-green-600';
      case 'UPDATE':
      case 'MODIFY':
        return 'text-blue-600';
      case 'DELETE':
        return 'text-red-600';
      case 'REVERSE':
        return 'text-orange-600';
      case 'POST':
      case 'POSTED':
        return 'text-purple-600';
      case 'VIEW':
      case 'VIEW_AUDIT':
        return 'text-gray-600';
      default:
        return 'text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading audit trail...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <h3 className="text-red-800 font-semibold">Error Loading Audit Trail</h3>
        <p className="text-red-600">{error}</p>
        <button
          onClick={fetchAuditTrail}
          className="mt-2 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
        >
          Retry
        </button>
      </div>
    );
  }

  const filteredLogs = getFilteredLogs();
  const uniqueActions = Array.from(new Set(auditLogs.map(log => log.action)));
  const uniqueUsers = Array.from(new Set(auditLogs.map(log => log.userId)));

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-800 mb-2">Audit Trail</h2>
        <p className="text-gray-600">
          Complete history of all changes and access events
        </p>
      </div>

      {/* T076 - Filtering UI */}
      {showFilters && (
        <div className="mb-6 p-4 bg-gray-50 rounded-lg">
          <h3 className="text-lg font-semibold text-gray-800 mb-3">Filters</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Action Type
              </label>
              <select
                value={filterAction}
                onChange={(e) => setFilterAction(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Actions</option>
                {uniqueActions.map(action => (
                  <option key={action} value={action}>{action}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                User
              </label>
              <select
                value={filterUser}
                onChange={(e) => setFilterUser(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Users</option>
                {uniqueUsers.map(user => (
                  <option key={user} value={user}>{user.substring(0, 8)}...</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                From Date
              </label>
              <input
                type="date"
                value={filterDateFrom}
                onChange={(e) => setFilterDateFrom(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                To Date
              </label>
              <input
                type="date"
                value={filterDateTo}
                onChange={(e) => setFilterDateTo(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="mt-3 flex justify-end">
            <button
              onClick={clearFilters}
              className="px-4 py-2 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300"
            >
              Clear Filters
            </button>
          </div>
        </div>
      )}

      {/* Summary */}
      <div className="mb-4 flex justify-between items-center">
        <p className="text-gray-600">
          Showing {filteredLogs.length} of {auditLogs.length} audit entries
        </p>
        <button
          onClick={fetchAuditTrail}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          Refresh
        </button>
      </div>

      {/* T075 - Timeline View */}
      <div className="space-y-4">
        {filteredLogs.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No audit entries found
          </div>
        ) : (
          filteredLogs.map((log, index) => {
            const { date, time } = formatTimestamp(log.timestamp);
            
            return (
              <div
                key={log.auditId}
                className="relative pl-8 pb-8 border-l-2 border-gray-300 last:border-0"
              >
                {/* Timeline dot */}
                <div className="absolute left-0 -ml-2 mt-1.5">
                  <div className="w-4 h-4 bg-blue-600 rounded-full border-2 border-white shadow"></div>
                </div>

                {/* Timeline content */}
                <div className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <span className="text-2xl">{getActionIcon(log.action)}</span>
                      <h3 className={`text-lg font-semibold ${getActionColor(log.action)}`}>
                        {log.action}
                      </h3>
                    </div>
                    <div className="text-right text-sm text-gray-500">
                      <div>{date}</div>
                      <div>{time}</div>
                    </div>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex items-start">
                      <span className="font-medium text-gray-700 w-24">Entity:</span>
                      <span className="text-gray-600">{log.entityType}</span>
                    </div>
                    
                    <div className="flex items-start">
                      <span className="font-medium text-gray-700 w-24">User ID:</span>
                      <span className="text-gray-600 font-mono text-xs">{log.userId}</span>
                    </div>

                    {log.details && (
                      <div className="flex items-start">
                        <span className="font-medium text-gray-700 w-24">Details:</span>
                        <span className="text-gray-600">{log.details}</span>
                      </div>
                    )}

                    {/* Show old/new values for modifications */}
                    {(log.oldValue || log.newValue) && (
                      <div className="mt-3 p-3 bg-gray-50 rounded">
                        <p className="font-medium text-gray-700 mb-2">Changes:</p>
                        <div className="grid grid-cols-2 gap-4">
                          {log.oldValue && (
                            <div>
                              <span className="text-xs text-gray-500">Old Value:</span>
                              <div className="text-red-600 font-mono">{log.oldValue}</div>
                            </div>
                          )}
                          {log.newValue && (
                            <div>
                              <span className="text-xs text-gray-500">New Value:</span>
                              <div className="text-green-600 font-mono">{log.newValue}</div>
                            </div>
                          )}
                        </div>
                        {log.reason && (
                          <div className="mt-2">
                            <span className="text-xs text-gray-500">Reason:</span>
                            <div className="text-gray-700">{log.reason}</div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Footer with count */}
      {filteredLogs.length > 10 && (
        <div className="mt-6 pt-4 border-t border-gray-200 text-center text-gray-600">
          {filteredLogs.length} total audit entries
        </div>
      )}
    </div>
  );
};

export default AuditTrail;
