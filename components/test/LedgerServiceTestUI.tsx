/**
 * Ledger Service Standalone Test UI
 * Tests ledger-service endpoints independently
 */

import React, { useState, useEffect } from 'react';
import { BookOpen, DollarSign, FileText, RefreshCw, TrendingUp, AlertCircle } from 'lucide-react';

const API_BASE = 'http://localhost:8087/api/ledger';

interface JournalEntry {
  id: string;
  entryDate: string;
  description: string;
  debitAccount: string;
  creditAccount: string;
  amount: number;
  tenantId: string;
  entityId: string;
}

interface TrialBalanceEntry {
  accountCode: string;
  accountName: string;
  debitBalance: number;
  creditBalance: number;
}

export const LedgerServiceTestUI: React.FC = () => {
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [trialBalance, setTrialBalance] = useState<TrialBalanceEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [testResult, setTestResult] = useState<string>('');
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('testing');
  const [testTenantId] = useState('test-tenant-001');

  useEffect(() => {
    testConnection();
  }, []);

  const testConnection = async () => {
    setConnectionStatus('testing');
    try {
      const response = await fetch(`http://localhost:8087/actuator/health`, {
        method: 'GET',
      });
      
      if (response.ok) {
        setConnectionStatus('connected');
        setTestResult('✅ Successfully connected to Ledger Service');
      } else {
        setConnectionStatus('disconnected');
        setTestResult(`❌ Connection failed: ${response.statusText}`);
      }
    } catch (err) {
      setConnectionStatus('disconnected');
      setTestResult(`❌ Connection failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  const loadJournalEntries = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/journal-entries?tenantId=${testTenantId}&page=0&size=20`, {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      setEntries(data.content || data || []);
      setTestResult(`✅ Loaded ${Array.isArray(data) ? data.length : data.content?.length || 0} journal entries`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Failed to load entries: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const loadTrialBalance = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/trial-balance/${testTenantId}`, {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      setTrialBalance(data.entries || []);
      setTestResult(`✅ Loaded trial balance with ${data.entries?.length || 0} accounts`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Failed to load trial balance: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <BookOpen className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Ledger Service Test UI</h1>
                <p className="text-sm text-slate-600">Standalone testing interface for ledger-service</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                connectionStatus === 'connected' ? 'bg-green-100 text-green-800' :
                connectionStatus === 'disconnected' ? 'bg-red-100 text-red-800' :
                'bg-yellow-100 text-yellow-800'
              }`}>
                {connectionStatus === 'connected' ? '● Connected' :
                 connectionStatus === 'disconnected' ? '● Disconnected' :
                 '● Testing...'}
              </div>
              <button
                onClick={testConnection}
                className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                title="Test connection"
              >
                <RefreshCw className="w-5 h-5 text-slate-600" />
              </button>
            </div>
          </div>
        </div>

        {/* Test Result Banner */}
        {testResult && (
          <div className={`rounded-lg p-4 mb-6 ${
            testResult.startsWith('✅') ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
          }`}>
            <p className="font-medium">{testResult}</p>
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p className="text-red-800 font-medium">Error: {error}</p>
          </div>
        )}

        {/* Connection Info */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <h3 className="font-semibold text-blue-900 mb-2">Service Information</h3>
          <div className="text-sm text-blue-800 space-y-1">
            <p><strong>Service URL:</strong> {API_BASE}</p>
            <p><strong>Port:</strong> 8087</p>
            <p><strong>Profile:</strong> standalone</p>
            <p><strong>Features:</strong> Journal entries, Trial balance, Payments, Reconciliation</p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          <button
            onClick={loadJournalEntries}
            disabled={loading}
            className="bg-indigo-600 text-white px-6 py-3 rounded-md hover:bg-indigo-700 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            <FileText className="w-5 h-5" />
            {loading ? 'Loading...' : 'Load Journal Entries'}
          </button>
          <button
            onClick={loadTrialBalance}
            disabled={loading}
            className="bg-green-600 text-white px-6 py-3 rounded-md hover:bg-green-700 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            <TrendingUp className="w-5 h-5" />
            {loading ? 'Loading...' : 'Load Trial Balance'}
          </button>
        </div>

        {/* Journal Entries */}
        {entries.length > 0 && (
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <FileText className="w-5 h-5" />
              Journal Entries
            </h2>
            <div className="space-y-3">
              {entries.map((entry) => (
                <div key={entry.id} className="border border-slate-200 rounded-lg p-4 hover:bg-slate-50">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="font-semibold text-slate-900">{entry.description}</h3>
                        <span className="text-xs bg-slate-100 text-slate-700 px-2 py-1 rounded">
                          {new Date(entry.entryDate).toLocaleDateString()}
                        </span>
                      </div>
                      <div className="grid grid-cols-3 gap-4 text-sm text-slate-600">
                        <div>
                          <span className="font-medium">Debit:</span> {entry.debitAccount}
                        </div>
                        <div>
                          <span className="font-medium">Credit:</span> {entry.creditAccount}
                        </div>
                        <div>
                          <span className="font-medium">Amount:</span> {formatCurrency(entry.amount)}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Trial Balance */}
        {trialBalance.length > 0 && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Trial Balance
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-slate-200">
                    <th className="text-left py-3 px-4 font-semibold text-slate-700">Account Code</th>
                    <th className="text-left py-3 px-4 font-semibold text-slate-700">Account Name</th>
                    <th className="text-right py-3 px-4 font-semibold text-slate-700">Debit</th>
                    <th className="text-right py-3 px-4 font-semibold text-slate-700">Credit</th>
                  </tr>
                </thead>
                <tbody>
                  {trialBalance.map((account, idx) => (
                    <tr key={idx} className="border-b border-slate-100 hover:bg-slate-50">
                      <td className="py-3 px-4 text-slate-900">{account.accountCode}</td>
                      <td className="py-3 px-4 text-slate-900">{account.accountName}</td>
                      <td className="py-3 px-4 text-right text-slate-900">
                        {account.debitBalance > 0 ? formatCurrency(account.debitBalance) : '-'}
                      </td>
                      <td className="py-3 px-4 text-right text-slate-900">
                        {account.creditBalance > 0 ? formatCurrency(account.creditBalance) : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default LedgerServiceTestUI;
