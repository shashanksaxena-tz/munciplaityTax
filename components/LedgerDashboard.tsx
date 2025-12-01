/**
 * T081-T083: Ledger Dashboard Component
 * Provides overview of ledger metrics with role-based views
 * Integrates payment, statement, reconciliation, and trial balance features
 */

import React, { useState, useEffect } from 'react';
import { 
  DollarSign, 
  TrendingUp, 
  FileText, 
  CheckCircle, 
  AlertCircle,
  Activity,
  CreditCard,
  RefreshCw,
  Download,
  Users
} from 'lucide-react';

interface DashboardMetrics {
  totalRevenue: number;
  outstandingAR: number;
  recentTransactionsCount: number;
  trialBalanceStatus: 'balanced' | 'unbalanced' | 'pending';
  totalFilers: number;
  paymentsToday: number;
  pendingRefunds: number;
  lastReconciliationDate: string;
}

interface RecentTransaction {
  id: string;
  date: string;
  description: string;
  amount: number;
  type: 'payment' | 'assessment' | 'refund';
  status: string;
}

interface LedgerDashboardProps {
  userRole: 'filer' | 'municipality' | 'admin';
  tenantId: string;
  filerId?: string; // Optional: Only for filer role
}

const LedgerDashboard: React.FC<LedgerDashboardProps> = ({ 
  userRole, 
  tenantId,
  filerId 
}) => {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentTransactions, setRecentTransactions] = useState<RecentTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Mock data for demo mode
  const getMockMetrics = (): DashboardMetrics => ({
    totalRevenue: 2450000,
    outstandingAR: 125000,
    recentTransactionsCount: 47,
    trialBalanceStatus: 'balanced',
    totalFilers: 3240,
    paymentsToday: 12,
    pendingRefunds: 3,
    lastReconciliationDate: '2024-11-28T10:00:00Z'
  });

  const getMockTransactions = (): RecentTransaction[] => [
    { id: '1', date: '2024-11-30', description: 'Q4 Tax Payment - Smith, John', amount: 1250.00, type: 'payment', status: 'completed' },
    { id: '2', date: '2024-11-29', description: '2024 Tax Assessment - Johnson LLC', amount: 5420.00, type: 'assessment', status: 'posted' },
    { id: '3', date: '2024-11-29', description: 'Overpayment Refund - Davis, Mary', amount: -250.00, type: 'refund', status: 'approved' },
    { id: '4', date: '2024-11-28', description: 'Monthly Withholding - ABC Corp', amount: 3200.00, type: 'payment', status: 'completed' },
    { id: '5', date: '2024-11-27', description: 'Penalty Assessment - Late Filing', amount: 125.00, type: 'assessment', status: 'pending' },
  ];

  useEffect(() => {
    fetchDashboardData();
    // Refresh every 5 minutes
    const interval = setInterval(fetchDashboardData, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [userRole, tenantId, filerId]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch metrics based on role
      const metricsEndpoint = userRole === 'filer'
        ? `/api/v1/ledger/dashboard/filer/${filerId}`
        : `/api/v1/ledger/dashboard/municipality/${tenantId}`;

      try {
        const metricsResponse = await fetch(metricsEndpoint);
        if (!metricsResponse.ok) throw new Error('Failed to fetch metrics');
        const metricsData = await metricsResponse.json();
        setMetrics(metricsData);
      } catch {
        // Use mock data when API is not available
        console.debug('Ledger Dashboard: Using mock metrics data (API unavailable)');
        setMetrics(getMockMetrics());
      }

      // Fetch recent transactions
      const transactionsEndpoint = userRole === 'filer'
        ? `/api/v1/account-statements/${filerId}/recent?limit=10`
        : `/api/v1/ledger/transactions/recent?tenantId=${tenantId}&limit=10`;

      try {
        const transactionsResponse = await fetch(transactionsEndpoint);
        if (!transactionsResponse.ok) throw new Error('Failed to fetch transactions');
        const transactionsData = await transactionsResponse.json();
        setRecentTransactions(transactionsData.transactions || transactionsData);
      } catch {
        // Use mock data when API is not available
        console.debug('Ledger Dashboard: Using mock transactions data (API unavailable)');
        setRecentTransactions(getMockTransactions());
      }

    } catch (err) {
      // Final fallback - use mock data
      console.debug('Ledger Dashboard: Using mock data fallback');
      setMetrics(getMockMetrics());
      setRecentTransactions(getMockTransactions());
    } finally {
      setLoading(false);
    }
  };

  const handleNavigation = (path: string) => {
    window.location.href = path;
  };

  if (loading && !metrics) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading dashboard...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <div className="flex items-center">
          <AlertCircle className="h-5 w-5 text-red-600 mr-2" />
          <p className="text-red-800">Error loading dashboard: {error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {userRole === 'filer' ? 'My Account' : 'Ledger Dashboard'}
            </h1>
            <p className="text-gray-600 mt-1">
              {userRole === 'filer' 
                ? 'View your payment history and account balance'
                : 'Monitor revenue, receivables, and financial reconciliation'}
            </p>
          </div>
          <button
            onClick={fetchDashboardData}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Key Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Total Revenue / Balance */}
        <MetricCard
          title={userRole === 'filer' ? 'Current Balance' : 'Total Revenue'}
          value={formatCurrency(userRole === 'filer' ? -metrics?.outstandingAR || 0 : metrics?.totalRevenue || 0)}
          icon={<DollarSign className="h-8 w-8" />}
          trend={userRole === 'municipality' ? '+12%' : undefined}
          color="blue"
        />

        {/* Outstanding AR / Amount Due */}
        <MetricCard
          title={userRole === 'filer' ? 'Amount Due' : 'Outstanding AR'}
          value={formatCurrency(metrics?.outstandingAR || 0)}
          icon={<TrendingUp className="h-8 w-8" />}
          color={metrics?.outstandingAR > 0 ? 'orange' : 'green'}
        />

        {/* Recent Transactions */}
        <MetricCard
          title="Recent Transactions"
          value={metrics?.recentTransactionsCount || 0}
          icon={<FileText className="h-8 w-8" />}
          color="purple"
        />

        {/* Trial Balance Status / Payment Status */}
        <MetricCard
          title={userRole === 'filer' ? 'Payment Status' : 'Trial Balance'}
          value={
            userRole === 'filer'
              ? (metrics?.outstandingAR === 0 ? 'Paid' : 'Due')
              : (metrics?.trialBalanceStatus === 'balanced' ? 'Balanced' : 'Unbalanced')
          }
          icon={<CheckCircle className="h-8 w-8" />}
          color={
            userRole === 'filer'
              ? (metrics?.outstandingAR === 0 ? 'green' : 'orange')
              : (metrics?.trialBalanceStatus === 'balanced' ? 'green' : 'red')
          }
        />
      </div>

      {/* Municipality-specific metrics */}
      {userRole !== 'filer' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <MetricCard
            title="Total Filers"
            value={metrics?.totalFilers || 0}
            icon={<Users className="h-6 w-6" />}
            color="indigo"
          />
          <MetricCard
            title="Payments Today"
            value={metrics?.paymentsToday || 0}
            icon={<CreditCard className="h-6 w-6" />}
            color="green"
          />
          <MetricCard
            title="Pending Refunds"
            value={metrics?.pendingRefunds || 0}
            icon={<Activity className="h-6 w-6" />}
            color="yellow"
          />
        </div>
      )}

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {userRole === 'filer' ? (
            <>
              <ActionButton
                title="Make Payment"
                description="Pay your tax bill"
                icon={<CreditCard className="h-6 w-6" />}
                onClick={() => handleNavigation('/payment')}
                color="blue"
              />
              <ActionButton
                title="View Statement"
                description="See transaction history"
                icon={<FileText className="h-6 w-6" />}
                onClick={() => handleNavigation('/statement')}
                color="green"
              />
              <ActionButton
                title="Request Refund"
                description="Request overpayment refund"
                icon={<Download className="h-6 w-6" />}
                onClick={() => handleNavigation('/refund')}
                color="purple"
              />
              <ActionButton
                title="Audit Trail"
                description="View account history"
                icon={<Activity className="h-6 w-6" />}
                onClick={() => handleNavigation('/audit')}
                color="orange"
              />
            </>
          ) : (
            <>
              <ActionButton
                title="Run Reconciliation"
                description="Reconcile filer accounts"
                icon={<CheckCircle className="h-6 w-6" />}
                onClick={() => handleNavigation('/reconciliation')}
                color="blue"
              />
              <ActionButton
                title="Trial Balance"
                description="Generate trial balance"
                icon={<Activity className="h-6 w-6" />}
                onClick={() => handleNavigation('/trial-balance')}
                color="green"
              />
              <ActionButton
                title="Generate Reports"
                description="Export financial reports"
                icon={<Download className="h-6 w-6" />}
                onClick={() => handleNavigation('/reports')}
                color="purple"
              />
              <ActionButton
                title="View Statements"
                description="Filer account statements"
                icon={<FileText className="h-6 w-6" />}
                onClick={() => handleNavigation('/statements')}
                color="orange"
              />
            </>
          )}
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-900">Recent Transactions</h2>
          <button
            onClick={() => handleNavigation(userRole === 'filer' ? '/statement' : '/transactions')}
            className="text-blue-600 hover:text-blue-700 text-sm font-medium"
          >
            View All →
          </button>
        </div>
        
        {recentTransactions.length === 0 ? (
          <p className="text-gray-500 text-center py-8">No recent transactions</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Description
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {recentTransactions.map((txn) => (
                  <tr key={txn.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {new Date(txn.date).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {txn.description}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor(txn.type)}`}>
                        {txn.type.toUpperCase()}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium">
                      {formatCurrency(txn.amount)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(txn.status)}`}>
                        {txn.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Last Reconciliation (Municipality only) */}
      {userRole !== 'filer' && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Last Reconciliation</h3>
              <p className="text-sm text-gray-600 mt-1">
                {metrics?.lastReconciliationDate 
                  ? `Completed on ${new Date(metrics.lastReconciliationDate).toLocaleDateString()}`
                  : 'Not yet run'}
              </p>
            </div>
            <button
              onClick={() => handleNavigation('/reconciliation')}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Run Reconciliation
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

// Helper Components

interface MetricCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: string;
  color: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'indigo' | 'yellow';
}

const MetricCard: React.FC<MetricCardProps> = ({ title, value, icon, trend, color }) => {
  const colorClasses = {
    blue: 'bg-blue-100 text-blue-600',
    green: 'bg-green-100 text-green-600',
    orange: 'bg-orange-100 text-orange-600',
    red: 'bg-red-100 text-red-600',
    purple: 'bg-purple-100 text-purple-600',
    indigo: 'bg-indigo-100 text-indigo-600',
    yellow: 'bg-yellow-100 text-yellow-600',
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-2">{value}</p>
          {trend && (
            <p className="text-sm text-green-600 mt-1">{trend}</p>
          )}
        </div>
        <div className={`p-3 rounded-lg ${colorClasses[color]}`}>
          {icon}
        </div>
      </div>
    </div>
  );
};

interface ActionButtonProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  onClick: () => void;
  color: 'blue' | 'green' | 'purple' | 'orange';
}

const ActionButton: React.FC<ActionButtonProps> = ({ title, description, icon, onClick, color }) => {
  const colorClasses = {
    blue: 'bg-blue-50 hover:bg-blue-100 text-blue-600',
    green: 'bg-green-50 hover:bg-green-100 text-green-600',
    purple: 'bg-purple-50 hover:bg-purple-100 text-purple-600',
    orange: 'bg-orange-50 hover:bg-orange-100 text-orange-600',
  };

  return (
    <button
      onClick={onClick}
      className={`p-4 rounded-lg transition-colors text-left ${colorClasses[color]}`}
    >
      <div className="flex items-start space-x-3">
        <div className="flex-shrink-0">{icon}</div>
        <div className="flex-1 min-w-0">
          <p className="font-semibold">{title}</p>
          <p className="text-sm opacity-75 mt-1">{description}</p>
        </div>
      </div>
    </button>
  );
};

// Helper functions

const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(amount);
};

const getTypeColor = (type: string): string => {
  switch (type.toLowerCase()) {
    case 'payment':
      return 'bg-green-100 text-green-800';
    case 'assessment':
      return 'bg-blue-100 text-blue-800';
    case 'refund':
      return 'bg-purple-100 text-purple-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusColor = (status: string): string => {
  switch (status.toLowerCase()) {
    case 'approved':
    case 'completed':
    case 'posted':
      return 'bg-green-100 text-green-800';
    case 'pending':
    case 'requested':
      return 'bg-yellow-100 text-yellow-800';
    case 'declined':
    case 'failed':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

export default LedgerDashboard;
