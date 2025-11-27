
import React, { useState } from 'react';
import { TaxCalculationResult, TaxBreakdownRule } from '../types';
import { RefreshCcw, ChevronLeft, Info, Printer, FileText, Table2, PieChart, Lock, Edit2, FileDown, CheckCircle, DollarSign } from 'lucide-react';
import { DiscrepancyView } from './DiscrepancyView';
import { api } from '../services/api';
import { PaymentGateway } from './PaymentGateway';

interface ResultsSectionProps {
  result: TaxCalculationResult;
  onReset: () => void;
  onBack: () => void;
  isSubmitted?: boolean;
  onSubmit: (data?: any) => void;
  onAmend: () => void;
}

type Tab = 'overview' | 'schedulex' | 'scheduley';

export const ResultsSection: React.FC<ResultsSectionProps> = ({ result, onReset, onBack, isSubmitted, onSubmit, onAmend }) => {
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [showPayment, setShowPayment] = useState(false);
  const [refundChoice, setRefundChoice] = useState<'REFUND' | 'CREDIT'>('REFUND');

  if (!result) return null;

  const handleDownloadPDF = async () => {
    try {
      const blob = await api.pdf.generateReturn(result);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Dublin_Tax_Return_${result.settings.taxYear}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (e) {
      console.error("PDF download failed", e);
      alert("Failed to generate PDF. Please try again.");
    }
  };

  const netTotal = result.municipalBalance;

  return (
    <div className="space-y-8 animate-fadeIn">
      {showPayment && (
        <PaymentGateway
          amount={Math.abs(netTotal)}
          recipient="City of Dublin Tax Division"
          onCancel={() => setShowPayment(false)}
          onSuccess={(record) => {
            setShowPayment(false);
            // In a real app, update session status to PAID here
            alert(`Payment Success! Confirmation: ${record.confirmationNumber}`);
            onSubmit(); // Auto submit after pay
          }}
        />
      )}

      {/* Header */}
      <div className="flex justify-between items-start print:hidden">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            Tax Return {result.settings.taxYear}
            {result.settings.isAmendment && <span className="bg-amber-100 text-amber-700 text-xs px-2 py-1 rounded font-bold uppercase">Amendment</span>}
            {isSubmitted && <span className="bg-green-100 text-green-700 text-xs px-2 py-1 rounded font-bold uppercase flex items-center gap-1"><CheckCircle className="w-3 h-3" /> Submitted</span>}
          </h2>
          <div className="text-sm text-slate-500 mt-1 font-medium">{result.profile.name} {result.profile.ssn && `• ***-**-${result.profile.ssn}`}</div>
        </div>
        <div className="flex gap-2">
          <button onClick={handleDownloadPDF} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-sm font-medium transition-colors">
            <FileDown className="w-4 h-4" /> Download Official PDF
          </button>
        </div>
      </div>

      {result.discrepancyReport?.hasDiscrepancies && !isSubmitted && (
        <div className="print:hidden">
          <DiscrepancyView report={result.discrepancyReport} />
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 border-b border-slate-200 print:hidden">
        <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} label="Return Overview" icon={<FileText className="w-4 h-4" />} />
        <TabButton active={activeTab === 'schedulex'} onClick={() => setActiveTab('schedulex')} label="Schedule X Details" icon={<Table2 className="w-4 h-4" />} count={result.scheduleX.entries.length} />
        <TabButton active={activeTab === 'scheduley'} onClick={() => setActiveTab('scheduley')} label="Schedule Y Credits" icon={<Table2 className="w-4 h-4" />} count={result.scheduleY.entries.length} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          {activeTab === 'overview' && (
            <>
              <div className="bg-indigo-900 rounded-2xl p-8 text-white shadow-xl relative overflow-hidden">
                <div className="relative z-10">
                  <div className="text-indigo-200 font-medium mb-1">Total Municipal Tax Due (Dublin)</div>
                  <div className="text-4xl font-bold mb-4">${result.municipalLiabilityAfterCredits.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                  <div className="flex gap-8 text-sm">
                    <div><span className="block text-indigo-300 text-xs uppercase">Taxable Income</span><span className="font-semibold text-lg">${result.totalTaxableIncome.toLocaleString()}</span></div>
                    <div><span className="block text-indigo-300 text-xs uppercase">Credits</span><span className="font-semibold text-lg text-green-300">-${result.scheduleY.totalCredit.toLocaleString()}</span></div>
                  </div>
                </div>
              </div>

              <BreakdownGroup title="Calculation Logic" rules={result.breakdown} total={result.municipalLiabilityAfterCredits} highlight />
            </>
          )}

          {activeTab === 'schedulex' && (
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
              <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                <h3 className="font-bold text-slate-800">Schedule X Sources</h3>
                <div className="text-right"><div className="text-xs text-slate-400 uppercase">Total Net</div><div className="font-bold text-indigo-600 text-lg">${result.scheduleX.totalNetProfit.toLocaleString()}</div></div>
              </div>
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-50 text-slate-500 font-medium border-b border-slate-200">
                  <tr><th className="px-6 py-3">Source</th><th className="px-6 py-3">Type</th><th className="px-6 py-3 text-right">Gross</th><th className="px-6 py-3 text-right">Expenses</th><th className="px-6 py-3 text-right">Net</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {result.scheduleX.entries.length === 0 ? <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-400 italic">No Schedule X income sources found.</td></tr> :
                    result.scheduleX.entries.map((entry, i) => (
                      <tr key={i} className="hover:bg-slate-50">
                        <td className="px-6 py-4 font-medium text-slate-700">{entry.source}</td>
                        <td className="px-6 py-4 text-xs"><span className="px-2 py-1 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100">{entry.type}</span></td>
                        <td className="px-6 py-4 text-right">${entry.gross.toLocaleString()}</td>
                        <td className="px-6 py-4 text-right text-red-400">{entry.expenses > 0 ? `(${entry.expenses.toLocaleString()})` : '-'}</td>
                        <td className="px-6 py-4 text-right font-bold">${entry.netProfit.toLocaleString()}</td>
                      </tr>
                    ))
                  }
                </tbody>
              </table>
            </div>
          )}

          {activeTab === 'scheduley' && (
            <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
              <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
                <h3 className="font-bold text-slate-800">Schedule Y Credits</h3>
                <div className="text-right"><div className="text-xs text-slate-400 uppercase">Total Credit</div><div className="font-bold text-green-600 text-lg">${result.scheduleY.totalCredit.toLocaleString()}</div></div>
              </div>
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-50 text-slate-500 font-medium border-b border-slate-200">
                  <tr><th className="px-6 py-3">City</th><th className="px-6 py-3 text-center">Rate</th><th className="px-6 py-3 text-right">Taxed Income</th><th className="px-6 py-3 text-right">Paid</th><th className="px-6 py-3 text-right">Allowed</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {result.scheduleY.entries.length === 0 ? <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-400 italic">No local tax credits found.</td></tr> :
                    result.scheduleY.entries.map((entry, i) => (
                      <tr key={i} className="hover:bg-slate-50">
                        <td className="px-6 py-4 font-medium text-indigo-600">{entry.locality}</td>
                        <td className="px-6 py-4 text-center text-xs font-mono bg-slate-50">{(entry.cityTaxRate * 100).toFixed(2)}%</td>
                        <td className="px-6 py-4 text-right">${entry.incomeTaxedByOtherCity.toLocaleString()}</td>
                        <td className="px-6 py-4 text-right">${entry.taxPaidToOtherCity.toLocaleString()}</td>
                        <td className="px-6 py-4 text-right font-bold text-green-600">${entry.creditAllowed.toLocaleString()}</td>
                      </tr>
                    ))
                  }
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm sticky top-24">
            <h3 className="font-bold text-lg mb-6 text-slate-800 pb-4 border-b border-slate-100">Final Summary</h3>
            <div className="space-y-5">
              <SummaryRow label="Total Taxable Income" amount={result.totalTaxableIncome} neutral />
              <SummaryRow label="Net Tax Liability (After Credits)" amount={result.municipalLiabilityAfterCredits} neutral />
              <SummaryRow label="Tax Withheld (W-2)" amount={result.totalLocalWithheld} neutral />

              <div className="pt-6 border-t border-slate-200">
                <div className="flex justify-between items-end mb-1">
                  <span className="font-bold text-slate-700">Net Total</span>
                  <span className="text-xs font-medium text-slate-400 uppercase">{netTotal > 0 ? 'Refund' : 'Due'}</span>
                </div>
                <div className={`text-3xl font-bold text-right ${netTotal > 0 ? 'text-green-600' : 'text-red-600'}`}>${Math.abs(netTotal).toLocaleString()}</div>
                <div className="text-xs text-right text-slate-400 mt-1 italic">
                  {netTotal > 0 ? "Refund amount to be disbursed" : "Amount due to City of Dublin"}
                </div>

                {netTotal > 0 && !isSubmitted && (
                  <div className="mt-4 p-4 bg-green-50 rounded-xl border border-green-100">
                    <h4 className="font-bold text-green-800 mb-2 text-xs uppercase">Overpayment Options</h4>
                    <div className="flex flex-col gap-2">
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input type="radio" name="refundChoice" checked={refundChoice === 'REFUND'} onChange={() => setRefundChoice('REFUND')} className="text-green-600 focus:ring-green-500" />
                        <span className="text-sm font-medium text-slate-700">Issue Refund Check</span>
                      </label>
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input type="radio" name="refundChoice" checked={refundChoice === 'CREDIT'} onChange={() => setRefundChoice('CREDIT')} className="text-green-600 focus:ring-green-500" />
                        <span className="text-sm font-medium text-slate-700">Credit to Next Year</span>
                      </label>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="mt-8 flex flex-col gap-3 print:hidden">
              {!isSubmitted ? (
                <>
                  {netTotal < 0 && (
                    <button onClick={() => setShowPayment(true)} className="flex items-center justify-center gap-2 w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-medium shadow-lg shadow-indigo-200 transition-all">
                      <DollarSign className="w-4 h-4" /> Pay Balance Now
                    </button>
                  )}
                  <button onClick={() => onSubmit({ refundChoice: netTotal > 0 ? refundChoice : null })} className="flex items-center justify-center gap-2 w-full py-3 bg-green-600 hover:bg-green-700 text-white rounded-xl font-medium shadow-md transition-all">
                    <Lock className="w-4 h-4" /> Submit & Finalize
                  </button>
                  <button onClick={onBack} className="flex items-center justify-center gap-2 w-full py-3 bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl font-medium transition-all">
                    <ChevronLeft className="w-4 h-4" /> Edit Data
                  </button>
                </>
              ) : (
                <button onClick={onAmend} className="flex items-center justify-center gap-2 w-full py-3 bg-amber-50 border border-amber-200 hover:bg-amber-100 text-amber-800 rounded-xl font-medium transition-all">
                  <Edit2 className="w-4 h-4" /> Amend / Re-open Return
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const TabButton = ({ active, onClick, label, icon, count }: any) => (
  <button onClick={onClick} className={`flex items-center gap-2 px-4 py-3 font-medium text-sm border-b-2 transition-colors ${active ? 'border-indigo-600 text-indigo-600 bg-indigo-50/50' : 'border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
    {icon} {label} {count > 0 && <span className="bg-slate-200 text-slate-600 text-xs px-1.5 py-0.5 rounded-full">{count}</span>}
  </button>
);

const BreakdownGroup = ({ title, rules, highlight }: any) => (
  <div className={`rounded-xl overflow-hidden border ${highlight ? 'border-indigo-200' : 'border-slate-200'}`}>
    <div className={`px-6 py-4 border-b flex justify-between items-center ${highlight ? 'bg-indigo-50/50 border-indigo-100' : 'bg-slate-50 border-slate-100'}`}><h3 className="font-bold text-slate-800">{title}</h3></div>
    <div className="p-6 space-y-5 bg-white">
      {rules.map((rule: TaxBreakdownRule, idx: number) => (
        <div key={idx} className="flex items-start gap-4">
          <div className="mt-1 p-1 rounded-full bg-slate-100 text-slate-400"><Info className="w-3.5 h-3.5" /></div>
          <div className="flex-1">
            <div className="flex justify-between mb-1"><span className="font-semibold text-slate-800 text-sm">{rule.ruleName}</span><span className="text-sm font-mono font-medium text-slate-700">{rule.amount !== 0 ? `$${Math.abs(rule.amount).toLocaleString()}` : '-'}</span></div>
            <p className="text-xs text-slate-500">{rule.description}</p>
          </div>
        </div>
      ))}
    </div>
  </div>
);

const SummaryRow = ({ label, amount, highlight, neutral }: any) => (
  <div className="flex justify-between items-center text-sm">
    <span className={`font-medium ${highlight ? 'text-indigo-900' : 'text-slate-600'}`}>{label}</span>
    <span className={`font-bold ${neutral ? 'text-slate-800' :
      amount > 0 ? 'text-green-600' :
        amount < 0 ? 'text-red-600' : 'text-slate-400'
      }`}>
      {amount === 0 ? '$0.00' : `${!neutral && amount > 0 ? '+' : ''}$${Math.abs(amount).toLocaleString()}`}
    </span>
  </div>
);
