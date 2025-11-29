import React, { useState, useEffect } from 'react';
import { AlertTriangle, Info, TrendingDown, TrendingUp, Calendar, DollarSign, FileText } from 'lucide-react';

/**
 * NOL Schedule View Component
 * 
 * Displays comprehensive Net Operating Loss tracking information including:
 * - NOL vintage table with multi-year tracking
 * - Current year calculation breakdown
 * - Expiration alerts
 * - Carryback election interface
 * - Available balance summary
 * 
 * Implements:
 * - FR-004: Display NOL schedule
 * - FR-012: Display calculation breakdown
 * - FR-024: Expiration alerts
 * - FR-036: Form 27-NOL generation
 * 
 * @see NOLService.java
 * @see NOLScheduleService.java
 */

interface NOLVintage {
  taxYear: number;
  originalAmount: number;
  previouslyUsed: number;
  expired: number;
  availableThisYear: number;
  usedThisYear: number;
  remainingForFuture: number;
  expirationDate: string | null;
  isCarriedBack: boolean;
  carrybackAmount: number;
}

interface NOLSchedule {
  id: string;
  businessId: string;
  returnId: string;
  taxYear: number;
  totalBeginningBalance: number;
  newNOLGenerated: number;
  totalAvailableNOL: number;
  nolDeduction: number;
  expiredNOL: number;
  totalEndingBalance: number;
  limitationPercentage: number;
  taxableIncomeBeforeNOL: number;
  taxableIncomeAfterNOL: number;
  vintages: NOLVintage[];
}

interface ExpirationAlert {
  id: string;
  nolId: string;
  taxYear: number;
  nolBalance: number;
  expirationDate: string;
  yearsUntilExpiration: number;
  severityLevel: 'CRITICAL' | 'WARNING' | 'INFO';
  alertMessage: string;
  dismissed: boolean;
}

interface NOLScheduleViewProps {
  businessId: string;
  taxYear: number;
  returnId?: string;
  onCarrybackElect?: (nolId: string) => void;
}

export const NOLScheduleView: React.FC<NOLScheduleViewProps> = ({
  businessId,
  taxYear,
  returnId,
  onCarrybackElect
}) => {
  const [schedule, setSchedule] = useState<NOLSchedule | null>(null);
  const [vintages, setVintages] = useState<NOLVintage[]>([]);
  const [alerts, setAlerts] = useState<ExpirationAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCarrybackModal, setShowCarrybackModal] = useState(false);
  const [selectedNOL, setSelectedNOL] = useState<string | null>(null);

  useEffect(() => {
    fetchNOLData();
  }, [businessId, taxYear]);

  const fetchNOLData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch NOL vintage breakdown
      const vintageResponse = await fetch(
        `/api/nol/schedule/${businessId}/vintages/${taxYear}`
      );
      if (vintageResponse.ok) {
        const vintageData = await vintageResponse.json();
        setVintages(vintageData);
      }

      // Fetch NOL schedule if return exists
      if (returnId) {
        const scheduleResponse = await fetch(`/api/nol/schedule/${returnId}`);
        if (scheduleResponse.ok) {
          const scheduleData = await scheduleResponse.json();
          setSchedule(scheduleData);
        }
      }

      // Fetch expiration alerts
      const alertsResponse = await fetch(`/api/nol/alerts/${businessId}`);
      if (alertsResponse.ok) {
        const alertsData = await alertsResponse.json();
        setAlerts(alertsData);
      }

      setLoading(false);
    } catch (err) {
      setError('Failed to load NOL data');
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const formatDate = (dateString: string | null): string => {
    if (!dateString) return 'No expiration';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const getSeverityColor = (severity: string): string => {
    switch (severity) {
      case 'CRITICAL':
        return 'text-red-600 bg-red-50 border-red-200';
      case 'WARNING':
        return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'INFO':
        return 'text-blue-600 bg-blue-50 border-blue-200';
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const handleCarrybackElection = (nolId: string) => {
    setSelectedNOL(nolId);
    setShowCarrybackModal(true);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <p className="text-red-800">{error}</p>
      </div>
    );
  }

  const totalAvailable = vintages.reduce((sum, v) => sum + v.availableThisYear, 0);
  const totalUsed = vintages.reduce((sum, v) => sum + v.usedThisYear, 0);
  const totalRemaining = vintages.reduce((sum, v) => sum + v.remainingForFuture, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">
              NOL Schedule - Tax Year {taxYear}
            </h2>
            <p className="text-sm text-gray-600 mt-1">
              Net Operating Loss Carryforward & Carryback Tracking
            </p>
          </div>
          <FileText className="h-8 w-8 text-blue-600" />
        </div>
      </div>

      {/* Expiration Alerts */}
      {alerts.length > 0 && !alerts.every(a => a.dismissed) && (
        <div className="space-y-2">
          {alerts
            .filter(alert => !alert.dismissed)
            .map(alert => (
              <div
                key={alert.id}
                className={`border rounded-lg p-4 ${getSeverityColor(alert.severityLevel)}`}
              >
                <div className="flex items-start">
                  <AlertTriangle className="h-5 w-5 mr-3 mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <p className="font-semibold">{alert.alertMessage}</p>
                    <div className="text-sm mt-2 space-y-1">
                      <div className="flex justify-between">
                        <span>Balance:</span>
                        <span className="font-medium">{formatCurrency(alert.nolBalance)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Expires:</span>
                        <span className="font-medium">{formatDate(alert.expirationDate)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Years until expiration:</span>
                        <span className="font-medium">{alert.yearsUntilExpiration}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Available NOL</p>
              <p className="text-2xl font-bold text-blue-600 mt-1">
                {formatCurrency(totalAvailable)}
              </p>
            </div>
            <DollarSign className="h-8 w-8 text-blue-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Used This Year</p>
              <p className="text-2xl font-bold text-green-600 mt-1">
                {formatCurrency(totalUsed)}
              </p>
            </div>
            <TrendingDown className="h-8 w-8 text-green-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Remaining Balance</p>
              <p className="text-2xl font-bold text-purple-600 mt-1">
                {formatCurrency(totalRemaining)}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-purple-600" />
          </div>
        </div>
      </div>

      {/* Current Year Calculation */}
      {schedule && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Current Year NOL Calculation
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">Taxable Income Before NOL:</span>
              <span className="font-semibold">{formatCurrency(schedule.taxableIncomeBeforeNOL)}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">Available NOL Balance:</span>
              <span className="font-semibold">{formatCurrency(schedule.totalAvailableNOL)}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">Maximum NOL Deduction ({schedule.limitationPercentage}%):</span>
              <span className="font-semibold">
                {formatCurrency(schedule.taxableIncomeBeforeNOL * schedule.limitationPercentage / 100)}
              </span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-200 bg-blue-50">
              <span className="text-gray-700 font-semibold">NOL Deduction Applied:</span>
              <span className="font-bold text-blue-600">{formatCurrency(schedule.nolDeduction)}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">Taxable Income After NOL:</span>
              <span className="font-semibold">{formatCurrency(schedule.taxableIncomeAfterNOL)}</span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-gray-700">Remaining NOL for Future Years:</span>
              <span className="font-semibold text-purple-600">{formatCurrency(schedule.totalEndingBalance)}</span>
            </div>
          </div>
        </div>
      )}

      {/* NOL Vintage Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">
            NOL Vintage Breakdown
          </h3>
          <p className="text-sm text-gray-600 mt-1">
            Multi-year tracking of NOL carryforwards (oldest first - FIFO ordering)
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Tax Year
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Original Amount
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Previously Used
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Expired
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Available This Year
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Used This Year
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Remaining
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Expiration
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {vintages.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-4 text-center text-gray-500">
                    <Info className="h-5 w-5 inline-block mr-2" />
                    No NOL carryforwards available
                  </td>
                </tr>
              ) : (
                vintages.map((vintage, index) => (
                  <tr key={index} className={index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {vintage.taxYear}
                      {vintage.isCarriedBack && (
                        <span className="ml-2 text-xs text-blue-600">(Carryback)</span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900">
                      {formatCurrency(vintage.originalAmount)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-600">
                      {formatCurrency(vintage.previouslyUsed)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-red-600">
                      {vintage.expired > 0 ? formatCurrency(vintage.expired) : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-blue-600 font-semibold">
                      {formatCurrency(vintage.availableThisYear)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-green-600">
                      {vintage.usedThisYear > 0 ? formatCurrency(vintage.usedThisYear) : '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-purple-600 font-semibold">
                      {formatCurrency(vintage.remainingForFuture)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      <div className="flex items-center">
                        <Calendar className="h-4 w-4 mr-1" />
                        {formatDate(vintage.expirationDate)}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
            {vintages.length > 0 && (
              <tfoot className="bg-gray-100 font-semibold">
                <tr>
                  <td className="px-6 py-4 text-sm text-gray-900">Total</td>
                  <td className="px-6 py-4 text-sm text-right text-gray-900">
                    {formatCurrency(vintages.reduce((sum, v) => sum + v.originalAmount, 0))}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-gray-900">
                    {formatCurrency(vintages.reduce((sum, v) => sum + v.previouslyUsed, 0))}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-gray-900">
                    {formatCurrency(vintages.reduce((sum, v) => sum + v.expired, 0))}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-blue-600">
                    {formatCurrency(totalAvailable)}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-green-600">
                    {formatCurrency(totalUsed)}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-purple-600">
                    {formatCurrency(totalRemaining)}
                  </td>
                  <td className="px-6 py-4"></td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>

      {/* CARES Act Carryback Information */}
      {taxYear >= 2018 && taxYear <= 2020 && vintages.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <div className="flex items-start">
            <Info className="h-6 w-6 text-blue-600 mr-3 mt-0.5 flex-shrink-0" />
            <div className="flex-1">
              <h4 className="text-lg font-semibold text-blue-900 mb-2">
                CARES Act NOL Carryback Available
              </h4>
              <p className="text-blue-800 mb-4">
                NOLs from tax years 2018-2020 are eligible for 5-year carryback under the CARES Act.
                You can carry this NOL back to prior years and claim a refund of taxes previously paid.
              </p>
              {vintages
                .filter(v => !v.isCarriedBack && v.taxYear >= 2018 && v.taxYear <= 2020 && v.availableThisYear > 0)
                .map((vintage, idx) => (
                  <button
                    key={idx}
                    onClick={() => onCarrybackElect && onCarrybackElect(`nol-${vintage.taxYear}`)}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors mr-2 mb-2"
                    disabled={!onCarrybackElect}
                  >
                    Elect Carryback for {vintage.taxYear} NOL ({formatCurrency(vintage.availableThisYear)})
                  </button>
                ))
              }
              {vintages.filter(v => !v.isCarriedBack && v.taxYear >= 2018 && v.taxYear <= 2020 && v.availableThisYear > 0).length === 0 && (
                <p className="text-sm text-blue-700 italic">
                  No eligible NOLs available for carryback. NOLs must not already be carried back and must have a remaining balance.
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Footer Info */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
        <p className="text-xs text-gray-600">
          <strong>Note:</strong> Post-2017 NOLs are limited to 80% of taxable income per TCJA rules.
          Pre-2018 NOLs can offset 100% of income but expire after 20 years.
          FIFO (First-In-First-Out) ordering is applied to use oldest NOLs first and prevent expiration.
        </p>
      </div>
    </div>
  );
};

export default NOLScheduleView;
