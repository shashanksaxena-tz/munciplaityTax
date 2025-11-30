import React, { useState, useEffect } from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { CheckCircle2, XCircle, AlertTriangle, RefreshCw, ChevronDown, ChevronRight } from 'lucide-react';

interface ReconciliationData {
  reportDate: string;
  municipalityAR: number;
  filerLiabilities: number;
  arVariance: number;
  municipalityCash: number;
  filerPayments: number;
  cashVariance: number;
  status: 'RECONCILED' | 'DISCREPANCY';
  discrepancies: DiscrepancyDetail[];
}

interface DiscrepancyDetail {
  filerId?: string;
  filerName?: string;
  transactionType: string;
  transactionDate: string;
  filerAmount: number;
  municipalityAmount: number;
  variance: number;
  description: string;
}

interface ReconciliationReportProps {
  tenantId: string;
  municipalityId: string;
}

export default function ReconciliationReport({ tenantId, municipalityId }: ReconciliationReportProps) {
  const [data, setData] = useState<ReconciliationData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedDiscrepancies, setExpandedDiscrepancies] = useState<Set<number>>(new Set());

  const fetchReconciliationReport = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/v1/reconciliation/report/${tenantId}/${municipalityId}`
      );

      if (!response.ok) {
        throw new Error('Failed to fetch reconciliation report');
      }

      const reportData = await response.json();
      setData(reportData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReconciliationReport();
  }, [tenantId, municipalityId]);

  const toggleDiscrepancy = (index: number) => {
    const newExpanded = new Set(expandedDiscrepancies);
    if (newExpanded.has(index)) {
      newExpanded.delete(index);
    } else {
      newExpanded.add(index);
    }
    setExpandedDiscrepancies(newExpanded);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  if (loading) {
    return (
      <Card className="w-full">
        <CardContent className="pt-6">
          <div className="flex items-center justify-center">
            <RefreshCw className="h-6 w-6 animate-spin mr-2" />
            <span>Loading reconciliation report...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertTriangle className="h-4 w-4" />
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!data) {
    return null;
  }

  const isReconciled = data.status === 'RECONCILED';
  const hasDiscrepancies = data.discrepancies && data.discrepancies.length > 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Two-Way Ledger Reconciliation</CardTitle>
              <CardDescription>
                Report Date: {formatDate(data.reportDate)}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              {isReconciled ? (
                <Badge variant="success" className="bg-green-500">
                  <CheckCircle2 className="h-4 w-4 mr-1" />
                  RECONCILED
                </Badge>
              ) : (
                <Badge variant="destructive">
                  <XCircle className="h-4 w-4 mr-1" />
                  DISCREPANCY
                </Badge>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={fetchReconciliationReport}
                disabled={loading}
              >
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Reconciliation Summary */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Accounts Receivable Reconciliation */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Accounts Receivable</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Municipality AR:</span>
              <span className="font-semibold">{formatCurrency(data.municipalityAR)}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Filer Liabilities:</span>
              <span className="font-semibold">{formatCurrency(data.filerLiabilities)}</span>
            </div>
            <div className="border-t pt-4">
              <div className="flex justify-between items-center">
                <span className="text-sm font-semibold">Variance:</span>
                <span
                  className={`font-bold ${
                    data.arVariance === 0
                      ? 'text-green-600'
                      : 'text-red-600'
                  }`}
                >
                  {formatCurrency(data.arVariance)}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Cash Reconciliation */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Cash Receipts</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Municipality Cash:</span>
              <span className="font-semibold">{formatCurrency(data.municipalityCash)}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Filer Payments:</span>
              <span className="font-semibold">{formatCurrency(data.filerPayments)}</span>
            </div>
            <div className="border-t pt-4">
              <div className="flex justify-between items-center">
                <span className="text-sm font-semibold">Variance:</span>
                <span
                  className={`font-bold ${
                    data.cashVariance === 0
                      ? 'text-green-600'
                      : 'text-red-600'
                  }`}
                >
                  {formatCurrency(data.cashVariance)}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Discrepancies Section */}
      {hasDiscrepancies && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg flex items-center">
              <AlertTriangle className="h-5 w-5 mr-2 text-yellow-600" />
              Discrepancies Detected ({data.discrepancies.length})
            </CardTitle>
            <CardDescription>
              The following discrepancies were found during reconciliation:
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12"></TableHead>
                  <TableHead>Transaction Type</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Filer Amount</TableHead>
                  <TableHead className="text-right">Municipality Amount</TableHead>
                  <TableHead className="text-right">Variance</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.discrepancies.map((discrepancy, index) => (
                  <React.Fragment key={index}>
                    <TableRow
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => toggleDiscrepancy(index)}
                    >
                      <TableCell>
                        {expandedDiscrepancies.has(index) ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </TableCell>
                      <TableCell className="font-medium">
                        {discrepancy.transactionType}
                      </TableCell>
                      <TableCell>{formatDate(discrepancy.transactionDate)}</TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(discrepancy.filerAmount)}
                      </TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(discrepancy.municipalityAmount)}
                      </TableCell>
                      <TableCell className="text-right font-semibold text-red-600">
                        {formatCurrency(discrepancy.variance)}
                      </TableCell>
                    </TableRow>
                    {expandedDiscrepancies.has(index) && (
                      <TableRow>
                        <TableCell colSpan={6} className="bg-gray-50">
                          <div className="p-4 space-y-2">
                            <div className="text-sm">
                              <span className="font-semibold">Description:</span>{' '}
                              {discrepancy.description}
                            </div>
                            {discrepancy.filerId && (
                              <div className="text-sm">
                                <span className="font-semibold">Filer:</span>{' '}
                                {discrepancy.filerName || discrepancy.filerId}
                              </div>
                            )}
                            <div className="flex gap-2 mt-3">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  // TODO: Navigate to filer detail
                                }}
                              >
                                View Filer Details
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  // TODO: Navigate to transaction history
                                }}
                              >
                                View Transactions
                              </Button>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {/* Success Message */}
      {isReconciled && !hasDiscrepancies && (
        <Alert className="bg-green-50 border-green-200">
          <CheckCircle2 className="h-4 w-4 text-green-600" />
          <AlertDescription className="text-green-800">
            Perfect reconciliation! All filer liabilities match municipality accounts receivable,
            and all payments are properly recorded on both sides.
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
}
