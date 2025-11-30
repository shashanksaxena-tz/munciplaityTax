/**
 * Tax Engine Service Standalone Test UI
 * Tests tax-engine-service calculation endpoints
 */

import React, { useState, useEffect } from 'react';
import { Calculator, DollarSign, RefreshCw, FileText, TrendingUp } from 'lucide-react';
import { ApiConfigPanel } from '../ApiConfigPanel';
import { apiConfig } from '../../services/apiConfig';

interface TaxCalculationResult {
  totalTax: number;
  effectiveRate: number;
  breakdown: {
    federalTax: number;
    stateTax: number;
    localTax: number;
  };
}

export const TaxEngineServiceTestUI: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [testResult, setTestResult] = useState<string>('');
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('testing');
  const [calculationResult, setCalculationResult] = useState<any>(null);
  const [serviceUrl, setServiceUrl] = useState('');

  const [individualCalcRequest, setIndividualCalcRequest] = useState({
    taxableIncome: 50000,
    taxYear: 2024,
    filingStatus: 'SINGLE',
    residencyDays: 365
  });

  const [businessCalcRequest, setBusinessCalcRequest] = useState({
    year: 2024,
    grossReceipts: 500000,
    expenses: 300000,
    estimates: 15000,
    priorCredit: 0
  });

  useEffect(() => {
    const url = apiConfig.getServiceUrl('/tax-engine');
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
        setTestResult(`✅ Successfully connected to Tax Engine Service at ${url}`);
      } else {
        setConnectionStatus('disconnected');
        setTestResult(`❌ Connection failed: ${response.statusText}`);
      }
    } catch (err) {
      setConnectionStatus('disconnected');
      setTestResult(`❌ Connection failed: ${err instanceof Error ? err.message : 'Unknown error'}. Make sure service is running at ${url}`);
    }
  };

  const calculateIndividualTax = async () => {
    setLoading(true);
    setError('');
    setCalculationResult(null);

    try {
      const requestData = {
        forms: [{
          type: 'W2',
          wages: individualCalcRequest.taxableIncome,
          withheld: individualCalcRequest.taxableIncome * 0.02
        }],
        profile: {
          filingStatus: individualCalcRequest.filingStatus,
          residencyDays: individualCalcRequest.residencyDays,
          taxYear: individualCalcRequest.taxYear
        },
        settings: {
          includeCredits: true,
          includeDeductions: true
        },
        rules: {
          municipalRate: 2.5,
          stateRate: 3.5
        }
      };

      const response = await fetch(apiConfig.buildUrl('/tax-engine', '/api/v1/tax-engine/calculate/individual'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      const data = await response.json();
      setCalculationResult(data);
      setTestResult(`✅ Individual tax calculated successfully! Total: $${data.totalTax?.toFixed(2) || 'N/A'}`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Calculation failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const calculateBusinessTax = async () => {
    setLoading(true);
    setError('');
    setCalculationResult(null);

    try {
      const netProfit = businessCalcRequest.grossReceipts - businessCalcRequest.expenses;
      
      const requestData = {
        year: businessCalcRequest.year,
        estimates: businessCalcRequest.estimates,
        priorCredit: businessCalcRequest.priorCredit,
        schX: {
          grossReceipts: businessCalcRequest.grossReceipts,
          totalDeductions: businessCalcRequest.expenses,
          netProfit: netProfit,
          addBacks: {},
          adjustedNetProfit: netProfit
        },
        schY: {
          totalPropertyFactor: 1.0,
          totalPayrollFactor: 1.0,
          totalSalesFactor: 1.0,
          apportionmentPercentage: 100.0
        },
        nolCarryforward: 0,
        rules: {
          businessTaxRate: 2.5,
          minimumTax: 100
        }
      };

      const response = await fetch(apiConfig.buildUrl('/tax-engine', '/api/v1/tax-engine/calculate/business'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      const data = await response.json();
      setCalculationResult(data);
      setTestResult(`✅ Business tax calculated! Net Profit: $${netProfit.toFixed(2)}`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Calculation failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Calculator className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Tax Engine Service Test UI</h1>
                <p className="text-sm text-slate-600">Standalone testing interface for tax-engine-service</p>
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
            <p><strong>Service URL:</strong> {API_BASE}</p>
            <p><strong>Port:</strong> 8085</p>
            <p><strong>Profile:</strong> standalone</p>
            <p><strong>Features:</strong> Individual tax calculation, Business tax calculation, Penalty calculation, Interest calculation</p>
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-6 mb-6">
          {/* Individual Tax Calculation */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <DollarSign className="w-5 h-5" />
              Individual Tax Calculation
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Tax Year</label>
                <input
                  type="number"
                  value={individualCalcRequest.taxYear}
                  onChange={(e) => setIndividualCalcRequest({ ...individualCalcRequest, taxYear: parseInt(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Taxable Income ($)</label>
                <input
                  type="number"
                  value={individualCalcRequest.taxableIncome}
                  onChange={(e) => setIndividualCalcRequest({ ...individualCalcRequest, taxableIncome: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Filing Status</label>
                <select
                  value={individualCalcRequest.filingStatus}
                  onChange={(e) => setIndividualCalcRequest({ ...individualCalcRequest, filingStatus: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                >
                  <option value="SINGLE">Single</option>
                  <option value="MARRIED_JOINT">Married Filing Jointly</option>
                  <option value="MARRIED_SEPARATE">Married Filing Separately</option>
                  <option value="HEAD_OF_HOUSEHOLD">Head of Household</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Residency Days</label>
                <input
                  type="number"
                  value={individualCalcRequest.residencyDays}
                  onChange={(e) => setIndividualCalcRequest({ ...individualCalcRequest, residencyDays: parseInt(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <button
                onClick={calculateIndividualTax}
                disabled={loading}
                className="w-full bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 disabled:opacity-50"
              >
                {loading ? 'Calculating...' : 'Calculate Individual Tax'}
              </button>
            </div>
          </div>

          {/* Business Tax Calculation */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Business Tax Calculation
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Tax Year</label>
                <input
                  type="number"
                  value={businessCalcRequest.year}
                  onChange={(e) => setBusinessCalcRequest({ ...businessCalcRequest, year: parseInt(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Gross Receipts ($)</label>
                <input
                  type="number"
                  value={businessCalcRequest.grossReceipts}
                  onChange={(e) => setBusinessCalcRequest({ ...businessCalcRequest, grossReceipts: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Total Expenses ($)</label>
                <input
                  type="number"
                  value={businessCalcRequest.expenses}
                  onChange={(e) => setBusinessCalcRequest({ ...businessCalcRequest, expenses: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Estimated Payments ($)</label>
                <input
                  type="number"
                  value={businessCalcRequest.estimates}
                  onChange={(e) => setBusinessCalcRequest({ ...businessCalcRequest, estimates: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <button
                onClick={calculateBusinessTax}
                disabled={loading}
                className="w-full bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:opacity-50"
              >
                {loading ? 'Calculating...' : 'Calculate Business Tax'}
              </button>
            </div>
          </div>
        </div>

        {/* Calculation Results */}
        {calculationResult && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <FileText className="w-5 h-5" />
              Calculation Results
            </h2>
            <div className="bg-slate-50 p-4 rounded-lg">
              <pre className="text-xs text-slate-900 overflow-x-auto whitespace-pre-wrap">
                {JSON.stringify(calculationResult, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TaxEngineServiceTestUI;
