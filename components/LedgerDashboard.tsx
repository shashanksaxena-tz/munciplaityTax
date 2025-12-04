/**
 * T081-T083: Ledger Dashboard Component
 * Provides overview of ledger metrics with role-based views
 * Integrates payment, statement, reconciliation, and trial balance features
 * Connects to ledger-service backend APIs
 */

import React, { useState, useEffect } from 'react';
import { safeLocalStorage } from '../utils/safeStorage';
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
  municipalityId?: string; // Optional: For reconciliation
}

// Backend response types (matching ledger-service DTOs)
interface PaymentTransaction {
  id: string;
  paymentId: string;
  filerId: string;
  amount: number;
  paymentMethod: string;
  status: string;
  transactionDate: string;
  confirmationNumber?: string;
}

interface AccountStatementResponse {
  accountName: string;
  statementDate: string;
  beginningBalance: number;
  endingBalance: number;
  totalDebits: number;
  totalCredits: number;
  transactions: StatementTransaction[];
}

interface StatementTransaction {
  transactionId: string;
  date: string;
  description: string;
  amount: number;
  debitCredit: 'DEBIT' | 'CREDIT';
  status: string;
}

interface TrialBalanceResponse {
  asOfDate: string;
  accounts: AccountBalance[];
  totalDebits: number;
  totalCredits: number;
  difference: number;
  isBalanced: boolean;
  status: string;
  accountCount: number;
  tenantId: string;
}

interface AccountBalance {
  accountCode: string;
  accountName: string;
  accountType: string;
  balance: number;
}

interface ReconciliationResponse {
  reportDate: string;
  municipalityAR: number;
  filerLiabilities: number;
  arVariance: number;
  municipalityCash: number;
  filerPayments: number;
  cashVariance: number;
  status: string;
  discrepancies: DiscrepancyDetail[];
}

interface DiscrepancyDetail {
  filerId: string;
  municipalityBalance: number;
  filerBalance: number;
  variance: number;
}

// Mock data for demo mode when backend is not available
const getMockDashboardMetrics = (): DashboardMetrics => ({
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

// API service for ledger operations - connects to ledger-service backend
const ledgerApi = {
  getAuthHeaders() {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${safeLocalStorage.getItem('auth_token')}`
    };
  },

  // Get payments for a specific filer
  async getFilerPayments(filerId: string): Promise<PaymentTransaction[]> {
    const response = await fetch(`/api/v1/payments/filer/${filerId}`, {
      headers: this.getAuthHeaders()
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch payments: ${response.status}`);
    }
    return response.json();
  },

  // Get account statement for a filer
  async getAccountStatement(tenantId: string, filerId: string, startDate?: string, endDate?: string): Promise<AccountStatementResponse> {
    let url = `/api/v1/statements/filer/${tenantId}/${filerId}`;
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (params.toString()) url += `?${params.toString()}`;
    
    const response = await fetch(url, {
      headers: this.getAuthHeaders()
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch statement: ${response.status}`);
    }
    return response.json();
  },

  // Get trial balance
  async getTrialBalance(tenantId: string, asOfDate?: string): Promise<TrialBalanceResponse> {
    let url = `/api/v1/trial-balance?tenantId=${tenantId}`;
    if (asOfDate) url += `&asOfDate=${asOfDate}`;
    
    const response = await fetch(url, {
      headers: this.getAuthHeaders()
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch trial balance: ${response.status}`);
    }
    return response.json();
  },

  // Get reconciliation report
  async getReconciliationReport(tenantId: string, municipalityId: string): Promise<ReconciliationResponse> {
    const response = await fetch(`/api/v1/reconciliation/report/${tenantId}/${municipalityId}`, {
      headers: this.getAuthHeaders()
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch reconciliation: ${response.status}`);
    }
    return response.json();
  },

  // Get journal entries for an entity
  async getJournalEntries(tenantId: string, entityId: string): Promise<Record<string, unknown>[]> {
    const response = await fetch(`/api/v1/journal-entries/entity/${tenantId}/${entityId}`, {
      headers: this.getAuthHeaders()
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch journal entries: ${response.status}`);
    }
    return response.json();
  }
};

const LedgerDashboard: React.FC<LedgerDashboardProps> = ({ 
  userRole, 
  tenantId,
  filerId,
  municipalityId 
}) => {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentTransactions, setRecentTransactions] = useState<RecentTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [useMockData, setUseMockData] = useState<boolean>(() => {
    const saved = safeLocalStorage.getItem('ledger_dashboard_use_mock');
    return saved === 'true';
  });
  const [dataSource, setDataSource] = useState<'backend' | 'mock'>('backend');

  useEffect(() => {
    fetchDashboardData();
    // Refresh every 5 minutes
    const interval = setInterval(fetchDashboardData, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [userRole, tenantId, filerId, municipalityId, useMockData]);

  const toggleDataSource = () => {
    const newValue = !useMockData;
    setUseMockData(newValue);
    safeLocalStorage.setItem('ledger_dashboard_use_mock', String(newValue));
  };

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    
    // If mock mode is explicitly enabled, use mock data
    if (useMockData) {
      setMetrics(getMockDashboardMetrics());
      setRecentTransactions(getMockTransactions());
      setDataSource('mock');
      setLoading(false);
      return;
    }
    
    try {
      if (userRole === 'filer' && filerId) {
        // Fetch filer-specific data
        await fetchFilerData(filerId);
        setDataSource('backend');
      } else {
        // Fetch municipality/admin data
        await fetchMunicipalityData();
        setDataSource('backend');
      }

    } catch (err) {
      // Fall back to mock data when backend is not available
      console.warn('Backend not available, using mock data:', err);
      setMetrics(getMockDashboardMetrics());
      setRecentTransactions(getMockTransactions());
      setDataSource('mock');
      setError(null); // Clear error since we have fallback data
    } finally {
      setLoading(false);
    }
  };

  const fetchFilerData = async (fId: string) => {
    // Get account statement for filer
    const statement = await ledgerApi.getAccountStatement(tenantId, fId);
    
    // Get filer payments
    const payments = await ledgerApi.getFilerPayments(fId);
    
    // Transform data for dashboard
    const transformedMetrics: DashboardMetrics = {
      totalRevenue: 0, // Not applicable for filer
      outstandingAR: Math.abs(Number(statement.endingBalance) || 0),
      recentTransactionsCount: statement.transactions?.length || 0,
      trialBalanceStatus: 'balanced',
      totalFilers: 0,
      paymentsToday: payments.filter((p: any) => 
        new Date(p.transactionDate).toDateString() === new Date().toDateString()
      ).length,
      pendingRefunds: 0,
      lastReconciliationDate: ''
    };
    
    setMetrics(transformedMetrics);
    
    // Transform transactions
    const txns: RecentTransaction[] = (statement.transactions || []).slice(0, 10).map((t: any) => ({
      id: t.transactionId || t.id,
      date: t.transactionDate || t.date,
      description: t.description,
      amount: Number(t.amount),
      type: t.debitCredit === 'CREDIT' ? 'payment' : 'assessment',
      status: t.status || 'posted'
    }));
    
    setRecentTransactions(txns);
  };

  const fetchMunicipalityData = async () => {
    // Fetch trial balance for municipality overview
    const trialBalance = await ledgerApi.getTrialBalance(tenantId);
    
    // Fetch reconciliation if municipalityId is provided
    let reconciliation = null;
    let lastReconDate = '';
    if (municipalityId) {
      try {
        reconciliation = await ledgerApi.getReconciliationReport(tenantId, municipalityId);
        lastReconDate = reconciliation.reportDate || '';
      } catch {
        // Reconciliation may not exist yet
      }
    }
    
    // Transform data for dashboard
    const transformedMetrics: DashboardMetrics = {
      totalRevenue: Number(trialBalance.totalCredits) || 0,
      outstandingAR: Number(trialBalance.totalDebits) - Number(trialBalance.totalCredits) || 0,
      recentTransactionsCount: trialBalance.accountCount || 0,
      trialBalanceStatus: trialBalance.isBalanced ? 'balanced' : 'unbalanced',
      totalFilers: trialBalance.accountCount || 0,
      paymentsToday: 0, // Would need separate endpoint
      pendingRefunds: reconciliation?.discrepancies?.length || 0,
      lastReconciliationDate: lastReconDate
    };
    
    setMetrics(transformedMetrics);
    
    // For municipality, show account balances as "transactions"
    const accounts = trialBalance.accounts || [];
    const txns: RecentTransaction[] = accounts.slice(0, 10).map((acc: any) => ({
      id: acc.accountCode || acc.id,
      date: trialBalance.asOfDate || new Date().toISOString(),
      description: acc.accountName,
      amount: Number(acc.balance) || 0,
      type: acc.balance >= 0 ? 'assessment' : 'payment',
      status: 'posted'
    }));
    
    setRecentTransactions(txns);
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
          <div className="flex items-center gap-3">
            {/* Data Source Toggle */}
            <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-100 rounded-lg">
              <span className={`text-xs font-medium ${dataSource === 'backend' ? 'text-green-600' : 'text-orange-600'}`}>
                {dataSource === 'backend' ? '● Backend' : '● Mock'}
              </span>
              <button
                onClick={toggleDataSource}
                className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
                  useMockData ? 'bg-orange-500' : 'bg-green-500'
                }`}
                title={useMockData ? 'Switch to Backend Data' : 'Switch to Mock Data'}
              >
                <span
                  className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform ${
                    useMockData ? 'translate-x-5' : 'translate-x-1'
                  }`}
                />
              </button>
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
