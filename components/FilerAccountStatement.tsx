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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { 
  RefreshCw, 
  Download, 
  Calendar, 
  FileText,
  DollarSign,
  TrendingUp,
  TrendingDown
} from 'lucide-react';

interface StatementTransaction {
  transactionDate: string;
  transactionType: string;
  description: string;
  debitAmount: number;
  creditAmount: number;
  runningBalance: number;
  entryNumber: string;
}

interface AccountStatementData {
  accountName: string;
  statementDate: string;
  beginningBalance: number;
  endingBalance: number;
  totalDebits: number;
  totalCredits: number;
  transactions: StatementTransaction[];
}

interface FilerAccountStatementProps {
  tenantId: string;
  filerId: string;
}

export default function FilerAccountStatement({ tenantId, filerId }: FilerAccountStatementProps) {
  const [data, setData] = useState<AccountStatementData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // T039: Date range filter state
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [filterApplied, setFilterApplied] = useState(false);

  const fetchStatement = async (start?: string, end?: string) => {
    setLoading(true);
    setError(null);

    try {
      let url = `/api/v1/statements/filer/${tenantId}/${filerId}`;
      
      // Add date range parameters if provided
      const params = new URLSearchParams();
      if (start) params.append('startDate', start);
      if (end) params.append('endDate', end);
      
      if (params.toString()) {
        url += '?' + params.toString();
      }

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error('Failed to fetch account statement');
      }

      const statementData = await response.json();
      setData(statementData);
      setFilterApplied(!!start || !!end);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatement();
  }, [tenantId, filerId]);

  // T039: Apply date range filter
  const handleApplyFilter = () => {
    fetchStatement(startDate, endDate);
  };

  const handleClearFilter = () => {
    setStartDate('');
    setEndDate('');
    fetchStatement();
  };

  // T040: Export to PDF
  const handleExportPdf = async () => {
    try {
      const response = await fetch(`/api/v1/statements/filer/${tenantId}/${filerId}/pdf`);
      if (!response.ok) {
        throw new Error('Failed to export PDF');
      }
      
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `statement-${filerId}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setError('Failed to export PDF');
    }
  };

  // T040: Export to CSV
  const handleExportCsv = async () => {
    try {
      const response = await fetch(`/api/v1/statements/filer/${tenantId}/${filerId}/csv`);
      if (!response.ok) {
        throw new Error('Failed to export CSV');
      }
      
      const csvContent = await response.text();
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `statement-${filerId}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      setError('Failed to export CSV');
    }
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
      month: 'short',
      day: 'numeric',
    });
  };

  const getTransactionTypeColor = (type: string) => {
    switch (type) {
      case 'TAX_ASSESSMENT':
        return 'bg-blue-100 text-blue-800';
      case 'PAYMENT':
        return 'bg-green-100 text-green-800';
      case 'PENALTY':
        return 'bg-orange-100 text-orange-800';
      case 'INTEREST':
        return 'bg-yellow-100 text-yellow-800';
      case 'REFUND':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading && !data) {
    return (
      <Card className="w-full">
        <CardContent className="pt-6">
          <div className="flex items-center justify-center">
            <RefreshCw className="h-6 w-6 animate-spin mr-2" />
            <span>Loading account statement...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!data) {
    return null;
  }

  const balanceIsPositive = data.endingBalance.compareTo(0) > 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Account Statement</CardTitle>
              <CardDescription>
                {data.accountName} - Statement Date: {formatDate(data.statementDate)}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => fetchStatement(startDate, endDate)}
                disabled={loading}
              >
                <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* T039: Date Range Filter */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center">
            <Calendar className="h-5 w-5 mr-2" />
            Filter by Date Range
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <Label htmlFor="startDate">Start Date</Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="endDate">End Date</Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
            <div className="flex items-end gap-2">
              <Button onClick={handleApplyFilter} disabled={loading}>
                Apply Filter
              </Button>
              {filterApplied && (
                <Button variant="outline" onClick={handleClearFilter}>
                  Clear
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Beginning Balance</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatCurrency(data.beginningBalance)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Total Charges</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {formatCurrency(data.totalCredits)}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Total Payments</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {formatCurrency(data.totalDebits)}
            </div>
          </CardContent>
        </Card>

        <Card className={balanceIsPositive ? 'border-red-200 bg-red-50' : 'border-green-200 bg-green-50'}>
          <CardHeader className="pb-2">
            <CardDescription>Current Balance</CardDescription>
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold flex items-center ${balanceIsPositive ? 'text-red-600' : 'text-green-600'}`}>
              {balanceIsPositive ? (
                <TrendingUp className="h-5 w-5 mr-2" />
              ) : (
                <TrendingDown className="h-5 w-5 mr-2" />
              )}
              {formatCurrency(data.endingBalance)}
            </div>
            {balanceIsPositive && (
              <p className="text-sm text-red-600 mt-1">Amount Owed</p>
            )}
            {!balanceIsPositive && data.endingBalance === 0 && (
              <p className="text-sm text-green-600 mt-1">Paid in Full</p>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Transactions Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg">Transaction History</CardTitle>
              <CardDescription>
                {data.transactions.length} transaction{data.transactions.length !== 1 ? 's' : ''}
                {filterApplied && ' (filtered)'}
              </CardDescription>
            </div>
            {/* T040: Export Buttons */}
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={handleExportPdf}>
                <Download className="h-4 w-4 mr-2" />
                Export PDF
              </Button>
              <Button variant="outline" size="sm" onClick={handleExportCsv}>
                <FileText className="h-4 w-4 mr-2" />
                Export CSV
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {data.transactions.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No transactions found for the selected period.
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Charges</TableHead>
                  <TableHead className="text-right">Payments</TableHead>
                  <TableHead className="text-right">Balance</TableHead>
                  <TableHead>Entry #</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.transactions.map((transaction, index) => (
                  <TableRow key={index}>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(transaction.transactionDate)}
                    </TableCell>
                    <TableCell>
                      <Badge className={getTransactionTypeColor(transaction.transactionType)}>
                        {transaction.transactionType.replace('_', ' ')}
                      </Badge>
                    </TableCell>
                    <TableCell className="max-w-xs truncate">
                      {transaction.description}
                    </TableCell>
                    <TableCell className="text-right text-red-600 font-medium">
                      {transaction.creditAmount > 0 
                        ? formatCurrency(transaction.creditAmount)
                        : '-'}
                    </TableCell>
                    <TableCell className="text-right text-green-600 font-medium">
                      {transaction.debitAmount > 0 
                        ? formatCurrency(transaction.debitAmount)
                        : '-'}
                    </TableCell>
                    <TableCell className="text-right font-semibold">
                      {formatCurrency(transaction.runningBalance)}
                    </TableCell>
                    <TableCell className="text-sm text-gray-500">
                      {transaction.entryNumber}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Balance Notice */}
      {balanceIsPositive && (
        <Alert className="bg-yellow-50 border-yellow-200">
          <DollarSign className="h-4 w-4 text-yellow-600" />
          <AlertDescription className="text-yellow-800">
            You have an outstanding balance of <strong>{formatCurrency(data.endingBalance)}</strong>.
            Please make a payment to avoid penalties and interest charges.
          </AlertDescription>
        </Alert>
      )}
    </div>
  );
}
