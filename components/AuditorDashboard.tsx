import React, { useState, useEffect } from 'react';
import { 
  AuditQueue, 
  AuditQueueFilters, 
  AuditQueueStats,
  AuditStatus, 
  AuditPriority 
} from '../types';
import { 
  FileText, 
  Filter, 
  AlertCircle, 
  Clock, 
  CheckCircle,
  XCircle,
  Users
} from 'lucide-react';

interface AuditorDashboardProps {
  userId: string;
  onReviewReturn: (returnId: string) => void;
}

export function AuditorDashboard({ userId, onReviewReturn }: AuditorDashboardProps) {
  const [queueItems, setQueueItems] = useState<AuditQueue[]>([]);
  const [stats, setStats] = useState<AuditQueueStats>({ pending: 0, highPriority: 0 });
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<AuditQueueFilters>({
    page: 0,
    size: 20,
    sortBy: 'submissionDate',
    sortDirection: 'DESC'
  });
  const [totalItems, setTotalItems] = useState(0);

  useEffect(() => {
    loadQueue();
    loadStats();
  }, [filters]);

  const loadQueue = async () => {
    setLoading(true);
    try {
      const queryParams = new URLSearchParams();
      if (filters.status) queryParams.append('status', filters.status);
      if (filters.priority) queryParams.append('priority', filters.priority);
      if (filters.auditorId) queryParams.append('auditorId', filters.auditorId);
      queryParams.append('page', filters.page?.toString() || '0');
      queryParams.append('size', filters.size?.toString() || '20');
      queryParams.append('sortBy', filters.sortBy || 'submissionDate');
      queryParams.append('sortDirection', filters.sortDirection || 'DESC');

      const response = await fetch(`/api/v1/audit/queue?${queryParams}`);
      const data = await response.json();
      
      setQueueItems(data.content || []);
      setTotalItems(data.totalElements || 0);
    } catch (error) {
      console.error('Error loading queue:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const response = await fetch('/api/v1/audit/queue/stats');
      const data = await response.json();
      setStats(data);
    } catch (error) {
      console.error('Error loading stats:', error);
    }
  };

  const handleAssignToMe = async (queueId: string) => {
    try {
      await fetch('/api/v1/audit/assign', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          queueId,
          auditorId: userId,
          assignedBy: userId
        })
      });
      loadQueue();
      loadStats();
    } catch (error) {
      console.error('Error assigning return:', error);
    }
  };

  const getPriorityBadge = (priority: AuditPriority) => {
    const colors = {
      HIGH: 'bg-red-100 text-red-800',
      MEDIUM: 'bg-yellow-100 text-yellow-800',
      LOW: 'bg-green-100 text-green-800'
    };
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${colors[priority]}`}>
        {priority}
      </span>
    );
  };

  const getStatusBadge = (status: AuditStatus) => {
    const colors = {
      PENDING: 'bg-gray-100 text-gray-800',
      IN_REVIEW: 'bg-blue-100 text-blue-800',
      AWAITING_DOCUMENTATION: 'bg-purple-100 text-purple-800',
      APPROVED: 'bg-green-100 text-green-800',
      REJECTED: 'bg-red-100 text-red-800',
      AMENDED: 'bg-orange-100 text-orange-800'
    };
    return (
      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${colors[status]}`}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return '$0';
    return new Intl.NumberFormat('en-US', { 
      style: 'currency', 
      currency: 'USD' 
    }).format(amount);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  return (
    <div className="min-h-screen bg-[#fbfbfb] p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-[#0f1012] flex items-center gap-2">
          <FileText className="w-8 h-8" />
          Auditor Dashboard
        </h1>
        <p className="text-[#5d6567] mt-1">Review and process tax return submissions</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-lg shadow p-6 border border-[#f0f0f0]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-[#5d6567]">Pending Review</p>
              <p className="text-2xl font-bold text-[#0f1012]">{stats.pending}</p>
            </div>
            <Clock className="w-8 h-8 text-[#469fe8]" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow p-6 border border-[#f0f0f0]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-[#5d6567]">High Priority</p>
              <p className="text-2xl font-bold text-[#ec1656]">{stats.highPriority}</p>
            </div>
            <AlertCircle className="w-8 h-8 text-[#ec1656]" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow p-6 border border-[#f0f0f0]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-[#5d6567]">My Workload</p>
              <p className="text-2xl font-bold text-[#0f1012]">
                {queueItems.filter(q => q.assignedAuditorId === userId).length}
              </p>
            </div>
            <Users className="w-8 h-8 text-[#10b981]" />
          </div>
        </div>
        
        <div className="bg-white rounded-lg shadow p-6 border border-[#f0f0f0]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-[#5d6567]">Total Items</p>
              <p className="text-2xl font-bold text-[#0f1012]">{totalItems}</p>
            </div>
            <FileText className="w-8 h-8 text-[#970bed]" />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4 mb-6 border border-[#f0f0f0]">
        <div className="flex items-center gap-4 flex-wrap">
          <Filter className="w-5 h-5 text-[#5d6567]" />
          
          <select
            value={filters.status || ''}
            onChange={(e) => setFilters({ ...filters, status: e.target.value ? (e.target.value as AuditStatus) : undefined, page: 0 })}
            className="px-3 py-2 border border-[#dcdede] rounded-lg text-[#0f1012]"
          >
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="AWAITING_DOCUMENTATION">Awaiting Docs</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
          
          <select
            value={filters.priority || ''}
            onChange={(e) => setFilters({ ...filters, priority: e.target.value ? (e.target.value as AuditPriority) : undefined, page: 0 })}
            className="px-3 py-2 border border-[#dcdede] rounded-lg text-[#0f1012]"
          >
            <option value="">All Priorities</option>
            <option value="HIGH">High Priority</option>
            <option value="MEDIUM">Medium Priority</option>
            <option value="LOW">Low Priority</option>
          </select>
          
          <button
            onClick={() => setFilters({ ...filters, auditorId: userId, page: 0 })}
            className={`px-4 py-2 rounded-lg ${
              filters.auditorId === userId
                ? 'bg-gradient-to-r from-[#970bed] to-[#469fe8] text-white'
                : 'bg-[#f0f0f0] text-[#5d6567]'
            }`}
          >
            My Items
          </button>
          
          <button
            onClick={() => setFilters({ page: 0, size: 20, sortBy: 'submissionDate', sortDirection: 'DESC' })}
            className="px-4 py-2 bg-[#f0f0f0] text-[#5d6567] rounded-lg hover:bg-[#dcdede]"
          >
            Clear Filters
          </button>
        </div>
      </div>

      {/* Queue Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden border border-[#f0f0f0]">
        <table className="min-w-full divide-y divide-[#f0f0f0]">
          <thead className="bg-[#fbfbfb]">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Taxpayer
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Return Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Tax Year
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Tax Due
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Priority
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Days in Queue
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Risk Score
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-[#5d6567] uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-[#f0f0f0]">
            {loading ? (
              <tr>
                <td colSpan={9} className="px-6 py-4 text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#970bed] mx-auto"></div>
                </td>
              </tr>
            ) : queueItems.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-6 py-4 text-center text-[#5d6567]">
                  No items in queue
                </td>
              </tr>
            ) : (
              queueItems.map((item) => (
                <tr key={item.queueId} className="hover:bg-[#fbfbfb]">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-[#0f1012]">
                      {item.taxpayerName || 'N/A'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#5d6567]">
                    {item.returnType || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#5d6567]">
                    {item.taxYear || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#0f1012]">
                    {formatCurrency(item.taxDue)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getPriorityBadge(item.priority)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getStatusBadge(item.status)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-[#5d6567]">
                    {item.daysInQueue || 0} days
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`text-sm font-semibold ${
                      item.riskScore >= 61 ? 'text-[#ec1656]' :
                      item.riskScore >= 21 ? 'text-[#f59e0b]' :
                      'text-[#10b981]'
                    }`}>
                      {item.riskScore}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {!item.assignedAuditorId || item.assignedAuditorId === userId ? (
                      <div className="flex gap-2">
                        {!item.assignedAuditorId && (
                          <button
                            onClick={() => handleAssignToMe(item.queueId)}
                            className="text-[#970bed] hover:text-[#469fe8] font-medium"
                          >
                            Assign to Me
                          </button>
                        )}
                        <button
                          onClick={() => onReviewReturn(item.returnId)}
                          className="text-[#469fe8] hover:text-[#970bed] font-medium"
                        >
                          Review
                        </button>
                      </div>
                    ) : (
                      <span className="text-[#babebf]">Assigned to other</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        
        {/* Pagination */}
        {totalItems > (filters.size || 20) && (
          <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200">
            <div className="flex-1 flex justify-between sm:hidden">
              <button
                onClick={() => setFilters({ ...filters, page: Math.max(0, (filters.page || 0) - 1) })}
                disabled={(filters.page || 0) === 0}
                className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                Previous
              </button>
              <button
                onClick={() => setFilters({ ...filters, page: (filters.page || 0) + 1 })}
                disabled={(filters.page || 0) >= Math.floor(totalItems / (filters.size || 20))}
                className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
              >
                Next
              </button>
            </div>
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700">
                  Showing{' '}
                  <span className="font-medium">{(filters.page || 0) * (filters.size || 20) + 1}</span>
                  {' '}to{' '}
                  <span className="font-medium">
                    {Math.min(((filters.page || 0) + 1) * (filters.size || 20), totalItems)}
                  </span>
                  {' '}of{' '}
                  <span className="font-medium">{totalItems}</span>
                  {' '}results
                </p>
              </div>
              <div>
                <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                  <button
                    onClick={() => setFilters({ ...filters, page: Math.max(0, (filters.page || 0) - 1) })}
                    disabled={(filters.page || 0) === 0}
                    className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setFilters({ ...filters, page: (filters.page || 0) + 1 })}
                    disabled={(filters.page || 0) >= Math.floor(totalItems / (filters.size || 20))}
                    className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
                  >
                    Next
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
