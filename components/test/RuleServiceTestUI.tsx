/**
 * Rule Service Standalone Test UI
 * Tests rule-service endpoints independently
 */

import React, { useState, useEffect } from 'react';
import { Settings, Plus, Edit, Trash2, CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react';
import { ApiConfigPanel } from '../ApiConfigPanel';
import { apiConfig } from '../../services/apiConfig';

interface TaxRule {
  id: string;
  tenantId: string;
  ruleCode: string;
  ruleName: string;
  description: string;
  category: string;
  valueType: string;
  value: string;
  effectiveDate: string;
  expiryDate?: string;
  approvalStatus: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export const RuleServiceTestUI: React.FC = () => {
  const [rules, setRules] = useState<TaxRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [testResult, setTestResult] = useState<string>('');
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('testing');
  const [serviceUrl, setServiceUrl] = useState('');

  const [newRule, setNewRule] = useState({
    tenantId: 'test-tenant-001',
    ruleCode: 'TEST_RULE',
    ruleName: 'Test Rule',
    description: 'A test rule for validation',
    category: 'TAX_RATE',
    valueType: 'PERCENTAGE',
    value: '2.5',
    effectiveDate: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    const url = apiConfig.getServiceUrl('/rules');
    setServiceUrl(url);
    testConnection(url);
  }, []);

  const testConnection = async (url: string = serviceUrl) => {
    setConnectionStatus('testing');
    try {
      const response = await fetch(`${url}/actuator/health`, {
        method: 'GET',
      });
      
      if (response.ok) {
        setConnectionStatus('connected');
        setTestResult(`✅ Successfully connected to Rule Service at ${url}`);
      } else {
        setConnectionStatus('disconnected');
        setTestResult(`❌ Connection failed: ${response.statusText}`);
      }
    } catch (err) {
      setConnectionStatus('disconnected');
      setTestResult(`❌ Connection failed: ${err instanceof Error ? err.message : 'Unknown error'}. Make sure service is running at ${url}`);
    }
  };

  const loadRules = async () => {
    setLoading(true);
    setError('');
    try {
      const url = apiConfig.buildUrl('/rules', '/api/rules?page=0&size=20');
      const response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      setRules(data.content || []);
      setTestResult(`✅ Loaded ${data.content?.length || 0} rules successfully`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Failed to load rules: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const createRule = async () => {
    setLoading(true);
    setError('');
    try {
      const url = apiConfig.buildUrl('/rules', '/api/rules');
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(newRule),
      });
      
      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }
      
      const created = await response.json();
      setTestResult(`✅ Rule created successfully: ${created.ruleCode}`);
      await loadRules();
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Failed to create rule: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case 'REJECTED':
        return <XCircle className="w-4 h-4 text-red-600" />;
      case 'PENDING':
        return <Clock className="w-4 h-4 text-yellow-600" />;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Settings className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Rule Service Test UI</h1>
                <p className="text-sm text-slate-600">Standalone testing interface for rule-service</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <ApiConfigPanel />
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
                onClick={() => testConnection()}
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
            <p><strong>Configured URL:</strong> <code className="bg-blue-100 px-2 py-1 rounded">{serviceUrl}</code></p>
            <p><strong>Default Port:</strong> 8084</p>
            <p><strong>Profile:</strong> standalone</p>
            <p><strong>Features:</strong> Create, Read, Update rules with approval workflow</p>
            <p className="mt-2 text-xs">💡 Click "API Configuration" to change the service URL</p>
          </div>
        </div>

        {/* Create Rule Form */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Plus className="w-5 h-5" />
            Create Test Rule
          </h2>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Rule Code</label>
              <input
                type="text"
                value={newRule.ruleCode}
                onChange={(e) => setNewRule({ ...newRule, ruleCode: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Rule Name</label>
              <input
                type="text"
                value={newRule.ruleName}
                onChange={(e) => setNewRule({ ...newRule, ruleName: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Category</label>
              <select
                value={newRule.category}
                onChange={(e) => setNewRule({ ...newRule, category: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md"
              >
                <option value="TAX_RATE">Tax Rate</option>
                <option value="EXEMPTION">Exemption</option>
                <option value="DEDUCTION">Deduction</option>
                <option value="CREDIT">Credit</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Value</label>
              <input
                type="text"
                value={newRule.value}
                onChange={(e) => setNewRule({ ...newRule, value: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md"
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
              <textarea
                value={newRule.description}
                onChange={(e) => setNewRule({ ...newRule, description: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md"
                rows={2}
              />
            </div>
          </div>
          <button
            onClick={createRule}
            disabled={loading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 disabled:opacity-50"
          >
            {loading ? 'Creating...' : 'Create Rule'}
          </button>
        </div>

        {/* Rules List */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-slate-900">Rules</h2>
            <button
              onClick={loadRules}
              disabled={loading}
              className="bg-slate-600 text-white px-4 py-2 rounded-md hover:bg-slate-700 disabled:opacity-50 flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              {loading ? 'Loading...' : 'Load Rules'}
            </button>
          </div>

          {rules.length === 0 ? (
            <p className="text-slate-600 text-center py-8">No rules loaded. Click "Load Rules" to fetch data.</p>
          ) : (
            <div className="space-y-3">
              {rules.map((rule) => (
                <div key={rule.id} className="border border-slate-200 rounded-lg p-4 hover:bg-slate-50">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="font-semibold text-slate-900">{rule.ruleName}</h3>
                        <span className="text-xs bg-slate-100 text-slate-700 px-2 py-1 rounded">
                          {rule.ruleCode}
                        </span>
                        <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-1 rounded">
                          {rule.category}
                        </span>
                        <div className="flex items-center gap-1">
                          {getStatusIcon(rule.approvalStatus)}
                          <span className="text-xs text-slate-600">{rule.approvalStatus}</span>
                        </div>
                      </div>
                      <p className="text-sm text-slate-600 mb-2">{rule.description}</p>
                      <div className="flex gap-4 text-xs text-slate-500">
                        <span>Value: {rule.value} {rule.valueType}</span>
                        <span>Effective: {rule.effectiveDate}</span>
                        <span>Version: {rule.version}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RuleServiceTestUI;
