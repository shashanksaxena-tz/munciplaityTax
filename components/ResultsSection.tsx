
import React, { useState } from 'react';
import { TaxCalculationResult, TaxBreakdownRule } from '../types';
import { RefreshCcw, ChevronLeft, Info, Printer, FileText, Table2, PieChart, Lock, Edit2, FileDown, CheckCircle, DollarSign } from 'lucide-react';
import { DiscrepancyView } from './DiscrepancyView';
import { api } from '../services/api';
import { PaymentGateway } from './PaymentGateway';
import { colors } from '../config/design-system';

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
          <h2 className="text-2xl font-bold text-[#0f1012] flex items-center gap-2">
            Tax Return {result.settings.taxYear}
            {result.settings.isAmendment && <span className="bg-[#f59e0b]/10 text-[#f59e0b] text-xs px-2 py-1 rounded font-bold uppercase">Amendment</span>}
            {isSubmitted && <span className="bg-[#d5faeb] text-[#10b981] text-xs px-2 py-1 rounded font-bold uppercase flex items-center gap-1"><CheckCircle className="w-3 h-3" /> Submitted</span>}
          </h2>
          <div className="text-sm text-[#5d6567] mt-1 font-medium">{result.profile.name} {result.profile.ssn && `• ***-**-${result.profile.ssn}`}</div>
        </div>
        <div className="flex gap-2">
          <button onClick={handleDownloadPDF} className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl shadow-lg font-medium transition-all">
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
      <div className="flex gap-1 border-b border-[#dcdede] print:hidden">
        <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')} label="Return Overview" icon={<FileText className="w-4 h-4" />} />
        <TabButton active={activeTab === 'schedulex'} onClick={() => setActiveTab('schedulex')} label="Schedule X Details" icon={<Table2 className="w-4 h-4" />} count={result.scheduleX.entries.length} />
        <TabButton active={activeTab === 'scheduley'} onClick={() => setActiveTab('scheduley')} label="Schedule Y Credits" icon={<Table2 className="w-4 h-4" />} count={result.scheduleY.entries.length} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          {activeTab === 'overview' && (
            <>
              <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] rounded-2xl p-8 text-white shadow-xl relative overflow-hidden">
                <div className="relative z-10">
                  <div className="text-white/80 font-medium mb-1">Total Municipal Tax Due (Dublin)</div>
                  <div className="text-4xl font-bold mb-4">${result.municipalLiabilityAfterCredits.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                  <div className="flex gap-8 text-sm">
                    <div><span className="block text-white/70 text-xs uppercase">Taxable Income</span><span className="font-semibold text-lg">${result.totalTaxableIncome.toLocaleString()}</span></div>
                    <div><span className="block text-white/70 text-xs uppercase">Credits</span><span className="font-semibold text-lg text-green-300">-${result.scheduleY.totalCredit.toLocaleString()}</span></div>
                  </div>
                </div>
              </div>

              <BreakdownGroup title="Calculation Logic" rules={result.breakdown} total={result.municipalLiabilityAfterCredits} highlight />
            </>
          )}

          {activeTab === 'schedulex' && (
            <div className="space-y-6">
              {/* Professional Header */}
              <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] text-white rounded-xl p-6 shadow-lg">
                <h3 className="text-2xl font-bold mb-2">Schedule X - Supplemental Income</h3>
                <p className="text-white/80 text-sm">Report all rental real estate, royalties, partnerships, S corporations, trusts, etc.</p>
                <div className="mt-4 flex items-end justify-between">
                  <div className="text-white/70 text-xs uppercase tracking-wide">Total Net Income</div>
                  <div className="text-3xl font-bold">${result.scheduleX.totalNetProfit.toLocaleString()}</div>
                </div>
              </div>

              {(() => {
                const rentals = result.scheduleX.entries.filter(e => e.type.toLowerCase().includes('rental'));
                const partnerships = result.scheduleX.entries.filter(e => e.type.toLowerCase().includes('partnership') || e.type.toLowerCase().includes('s-corp') || e.type.toLowerCase().includes('k-1'));
                const other = result.scheduleX.entries.filter(e => !rentals.includes(e) && !partnerships.includes(e));
                
                return (
                  <>
                    {/* Rental Income Section */}
                    {rentals.length > 0 && (
                      <div className="bg-white border border-[#dcdede] rounded-xl overflow-hidden shadow-sm">
                        <div className="px-6 py-4 border-b border-[#dcdede] bg-[#ebf4ff] flex justify-between items-center">
                          <div>
                            <h4 className="font-bold text-[#0f1012] text-lg">Rental Real Estate Income</h4>
                            <p className="text-xs text-[#5d6567] mt-1">Properties generating rental income</p>
                          </div>
                          <div className="text-right">
                            <div className="text-xs text-[#babebf] uppercase">Subtotal</div>
                            <div className="font-bold text-[#469fe8] text-xl">${rentals.reduce((sum, e) => sum + e.netProfit, 0).toLocaleString()}</div>
                          </div>
                        </div>
                        <table className="w-full text-sm">
                          <thead className="bg-[#f8f9fa] text-[#5d6567] font-semibold border-b border-[#dcdede]">
                            <tr>
                              <th className="px-6 py-3 text-left">Property/Source</th>
                              <th className="px-6 py-3 text-left">Type</th>
                              <th className="px-6 py-3 text-right">Gross Income</th>
                              <th className="px-6 py-3 text-right">Expenses</th>
                              <th className="px-6 py-3 text-right">Net Income</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-[#dcdede]">
                            {rentals.map((entry, i) => (
                              <tr key={i} className="hover:bg-[#f8f9fa] transition">
                                <td className="px-6 py-4 font-semibold text-[#0f1012]">{entry.source}</td>
                                <td className="px-6 py-4">
                                  <span className="px-2.5 py-1 rounded-md bg-[#ebf4ff] text-[#469fe8] border border-[#469fe8]/20 text-xs font-medium">{entry.type}</span>
                                </td>
                                <td className="px-6 py-4 text-right font-mono">${entry.gross.toLocaleString()}</td>
                                <td className="px-6 py-4 text-right font-mono text-[#ec1656]">{entry.expenses > 0 ? `($${entry.expenses.toLocaleString()})` : '$0'}</td>
                                <td className="px-6 py-4 text-right font-bold font-mono text-[#0f1012]">${entry.netProfit.toLocaleString()}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {/* Partnership/S-Corp Income Section */}
                    {partnerships.length > 0 && (
                      <div className="bg-white border border-[#dcdede] rounded-xl overflow-hidden shadow-sm">
                        <div className="px-6 py-4 border-b border-[#dcdede] bg-[#970bed]/10 flex justify-between items-center">
                          <div>
                            <h4 className="font-bold text-[#0f1012] text-lg">Partnership & S Corporation Income</h4>
                            <p className="text-xs text-[#5d6567] mt-1">Pass-through entities (Schedule K-1)</p>
                          </div>
                          <div className="text-right">
                            <div className="text-xs text-[#babebf] uppercase">Subtotal</div>
                            <div className="font-bold text-[#970bed] text-xl">${partnerships.reduce((sum, e) => sum + e.netProfit, 0).toLocaleString()}</div>
                          </div>
                        </div>
                        <table className="w-full text-sm">
                          <thead className="bg-[#f8f9fa] text-[#5d6567] font-semibold border-b border-[#dcdede]">
                            <tr>
                              <th className="px-6 py-3 text-left">Entity Name</th>
                              <th className="px-6 py-3 text-left">Type</th>
                              <th className="px-6 py-3 text-right">Gross Income</th>
                              <th className="px-6 py-3 text-right">Expenses</th>
                              <th className="px-6 py-3 text-right">Net Income</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-[#dcdede]">
                            {partnerships.map((entry, i) => (
                              <tr key={i} className="hover:bg-[#f8f9fa] transition">
                                <td className="px-6 py-4 font-semibold text-[#0f1012]">{entry.source}</td>
                                <td className="px-6 py-4">
                                  <span className="px-2.5 py-1 rounded-md bg-[#970bed]/10 text-[#970bed] border border-[#970bed]/20 text-xs font-medium">{entry.type}</span>
                                </td>
                                <td className="px-6 py-4 text-right font-mono">${entry.gross.toLocaleString()}</td>
                                <td className="px-6 py-4 text-right font-mono text-[#ec1656]">{entry.expenses > 0 ? `($${entry.expenses.toLocaleString()})` : '$0'}</td>
                                <td className="px-6 py-4 text-right font-bold font-mono text-[#0f1012]">${entry.netProfit.toLocaleString()}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {/* Other Income Section */}
                    {other.length > 0 && (
                      <div className="bg-white border border-[#dcdede] rounded-xl overflow-hidden shadow-sm">
                        <div className="px-6 py-4 border-b border-[#dcdede] bg-[#f8f9fa] flex justify-between items-center">
                          <div>
                            <h4 className="font-bold text-[#0f1012] text-lg">Other Schedule X Income</h4>
                            <p className="text-xs text-[#5d6567] mt-1">Additional supplemental income sources</p>
                          </div>
                          <div className="text-right">
                            <div className="text-xs text-[#babebf] uppercase">Subtotal</div>
                            <div className="font-bold text-[#5d6567] text-xl">${other.reduce((sum, e) => sum + e.netProfit, 0).toLocaleString()}</div>
                          </div>
                        </div>
                        <table className="w-full text-sm">
                          <thead className="bg-[#f8f9fa] text-[#5d6567] font-semibold border-b border-[#dcdede]">
                            <tr>
                              <th className="px-6 py-3 text-left">Source</th>
                              <th className="px-6 py-3 text-left">Type</th>
                              <th className="px-6 py-3 text-right">Gross Income</th>
                              <th className="px-6 py-3 text-right">Expenses</th>
                              <th className="px-6 py-3 text-right">Net Income</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-[#dcdede]">
                            {other.map((entry, i) => (
                              <tr key={i} className="hover:bg-[#f8f9fa] transition">
                                <td className="px-6 py-4 font-semibold text-[#102124]">{entry.source}</td>
                                <td className="px-6 py-4">
                                  <span className="px-2.5 py-1 rounded-md bg-[#f0f0f0] text-[#102124] border border-[#dcdede] text-xs font-medium">{entry.type}</span>
                                </td>
                                <td className="px-6 py-4 text-right font-mono">${entry.gross.toLocaleString()}</td>
                                <td className="px-6 py-4 text-right font-mono text-[#ec1656]">{entry.expenses > 0 ? `($${entry.expenses.toLocaleString()})` : '$0'}</td>
                                <td className="px-6 py-4 text-right font-bold font-mono text-[#0f1012]">${entry.netProfit.toLocaleString()}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {/* No entries message */}
                    {result.scheduleX.entries.length === 0 && (
                      <div className="bg-white border border-[#dcdede] rounded-xl p-12 text-center shadow-sm">
                        <div className="text-[#babebf] mb-2">
                          <svg className="w-16 h-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                        </div>
                        <p className="text-[#5d6567] font-medium">No Schedule X income sources reported</p>
                        <p className="text-[#babebf] text-sm mt-1">This section includes rental real estate, partnerships, S corporations, and trusts</p>
                      </div>
                    )}
                  </>
                );
              })()}
            </div>
          )}

          {activeTab === 'scheduley' && (
            <div className="space-y-6">
              {/* Professional Header */}
              <div className="bg-gradient-to-r from-[#10b981] to-[#059669] text-white rounded-xl p-6 shadow-lg">
                <h3 className="text-2xl font-bold mb-2">Schedule Y - Local Tax Credits</h3>
                <p className="text-white/80 text-sm">Credit for taxes paid to other Ohio municipalities</p>
                <div className="mt-4 flex items-end justify-between">
                  <div className="text-white/70 text-xs uppercase tracking-wide">Total Allowed Credit</div>
                  <div className="text-3xl font-bold">${result.scheduleY.totalCredit.toLocaleString()}</div>
                </div>
              </div>

              {/* Credits Table */}
              {result.scheduleY.entries.length > 0 ? (
                <div className="bg-white border border-[#dcdede] rounded-xl overflow-hidden shadow-sm">
                  <div className="px-6 py-4 border-b border-[#dcdede] bg-[#d5faeb]">
                    <h4 className="font-bold text-[#0f1012] text-lg">Municipal Tax Credit Details</h4>
                    <p className="text-xs text-[#5d6567] mt-1">Credits reduce your Dublin tax liability dollar-for-dollar</p>
                  </div>
                  <table className="w-full text-sm">
                    <thead className="bg-[#f8f9fa] text-[#5d6567] font-semibold border-b border-[#dcdede]">
                      <tr>
                        <th className="px-6 py-3 text-left">Municipality</th>
                        <th className="px-6 py-3 text-center">Tax Rate</th>
                        <th className="px-6 py-3 text-right">Income Taxed</th>
                        <th className="px-6 py-3 text-right">Tax Paid</th>
                        <th className="px-6 py-3 text-right">Credit Allowed</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#dcdede]">
                      {result.scheduleY.entries.map((entry, i) => (
                        <tr key={i} className="hover:bg-[#d5faeb]/30 transition">
                          <td className="px-6 py-4 font-semibold text-[#102124]">{entry.locality}</td>
                          <td className="px-6 py-4 text-center">
                            <span className="px-3 py-1 rounded-md bg-[#f0f0f0] text-[#102124] font-mono text-xs font-bold">{(entry.cityTaxRate * 100).toFixed(2)}%</span>
                          </td>
                          <td className="px-6 py-4 text-right font-mono">${entry.incomeTaxedByOtherCity.toLocaleString()}</td>
                          <td className="px-6 py-4 text-right font-mono">${entry.taxPaidToOtherCity.toLocaleString()}</td>
                          <td className="px-6 py-4 text-right font-bold font-mono text-[#10b981]">${entry.creditAllowed.toLocaleString()}</td>
                        </tr>
                      ))}
                      <tr className="bg-[#d5faeb] font-bold">
                        <td colSpan={4} className="px-6 py-4 text-right text-[#102124]">Total Credits Applied:</td>
                        <td className="px-6 py-4 text-right font-mono text-[#10b981] text-lg">${result.scheduleY.totalCredit.toLocaleString()}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="bg-white border border-[#dcdede] rounded-xl p-12 text-center shadow-sm">
                  <div className="text-[#babebf] mb-2">
                    <svg className="w-16 h-16 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <p className="text-[#5d6567] font-medium">No local tax credits claimed</p>
                  <p className="text-[#babebf] text-sm mt-1">You may claim credits for taxes paid to other Ohio municipalities</p>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="bg-white p-6 rounded-xl border border-[#dcdede] shadow-sm sticky top-24">
            <h3 className="font-bold text-lg mb-6 text-[#0f1012] pb-4 border-b border-[#dcdede]">Final Summary</h3>
            <div className="space-y-5">
              <SummaryRow label="Total Taxable Income" amount={result.totalTaxableIncome} neutral />
              <SummaryRow label="Net Tax Liability (After Credits)" amount={result.municipalLiabilityAfterCredits} neutral />
              <SummaryRow label="Tax Withheld (W-2)" amount={result.totalLocalWithheld} neutral />

              <div className="pt-6 border-t border-[#dcdede]">
                <div className="flex justify-between items-end mb-1">
                  <span className="font-bold text-[#102124]">Net Total</span>
                  <span className="text-xs font-medium text-[#babebf] uppercase">{netTotal > 0 ? 'Refund' : 'Due'}</span>
                </div>
                <div className={`text-3xl font-bold text-right ${netTotal > 0 ? 'text-[#10b981]' : 'text-[#ec1656]'}`}>${Math.abs(netTotal).toLocaleString()}</div>
                <div className="text-xs text-right text-[#babebf] mt-1 italic">
                  {netTotal > 0 ? "Refund amount to be disbursed" : "Amount due to City of Dublin"}
                </div>

                {netTotal > 0 && !isSubmitted && (
                  <div className="mt-4 p-4 bg-[#d5faeb] rounded-xl border border-green-100">
                    <h4 className="font-bold text-[#10b981] mb-2 text-xs uppercase">Overpayment Options</h4>
                    <div className="flex flex-col gap-2">
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input type="radio" name="refundChoice" checked={refundChoice === 'REFUND'} onChange={() => setRefundChoice('REFUND')} className="text-[#10b981] focus:ring-green-500" />
                        <span className="text-sm font-medium text-[#102124]">Issue Refund Check</span>
                      </label>
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input type="radio" name="refundChoice" checked={refundChoice === 'CREDIT'} onChange={() => setRefundChoice('CREDIT')} className="text-[#10b981] focus:ring-green-500" />
                        <span className="text-sm font-medium text-[#102124]">Credit to Next Year</span>
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
                    <button onClick={() => setShowPayment(true)} className="flex items-center justify-center gap-2 w-full py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-medium shadow-lg shadow-[#970bed]/20 transition-all">
                      <DollarSign className="w-4 h-4" /> Pay Balance Now
                    </button>
                  )}
                  <button onClick={() => onSubmit({ refundChoice: netTotal > 0 ? refundChoice : null })} className="flex items-center justify-center gap-2 w-full py-3 bg-gradient-to-r from-[#10b981] to-[#059669] hover:from-[#059669] hover:to-[#047857] text-white rounded-xl font-medium shadow-lg shadow-[#10b981]/20 transition-all">
                    <Lock className="w-4 h-4" /> Submit & Finalize
                  </button>
                  <button onClick={onBack} className="flex items-center justify-center gap-2 w-full py-3 bg-white border border-[#dcdede] hover:bg-[#f8f9fa] text-[#102124] rounded-xl font-medium transition-all">
                    <ChevronLeft className="w-4 h-4" /> Edit Data
                  </button>
                </>
              ) : (
                <button onClick={onAmend} className="flex items-center justify-center gap-2 w-full py-3 bg-[#f59e0b]/10 border border-[#f59e0b]/20 hover:bg-[#f59e0b]/20 text-[#f59e0b] rounded-xl font-medium transition-all">
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
  <button onClick={onClick} className={`flex items-center gap-2 px-4 py-3 font-medium text-sm border-b-2 transition-colors ${active ? 'border-[#970bed] text-[#970bed] bg-[#ebf4ff]/50' : 'border-transparent text-[#5d6567] hover:text-[#0f1012] hover:bg-[#f8f9fa]'}`}>
    {icon} {label} {count > 0 && <span className="bg-[#dcdede] text-[#5d6567] text-xs px-1.5 py-0.5 rounded-full">{count}</span>}
  </button>
);

const BreakdownGroup = ({ title, rules, highlight }: any) => (
  <div className={`rounded-xl overflow-hidden border ${highlight ? 'border-[#970bed]/20' : 'border-[#dcdede]'}`}>
    <div className={`px-6 py-4 border-b flex justify-between items-center ${highlight ? 'bg-[#ebf4ff]/50 border-[#970bed]/10' : 'bg-[#f8f9fa] border-[#dcdede]'}`}><h3 className="font-bold text-[#0f1012]">{title}</h3></div>
    <div className="p-6 space-y-5 bg-white">
      {rules.map((rule: TaxBreakdownRule, idx: number) => (
        <div key={idx} className="flex items-start gap-4">
          <div className="mt-1 p-1 rounded-full bg-[#f0f0f0] text-[#babebf]"><Info className="w-3.5 h-3.5" /></div>
          <div className="flex-1">
            <div className="flex justify-between mb-1"><span className="font-semibold text-[#0f1012] text-sm">{rule.ruleName}</span><span className="text-sm font-mono font-medium text-[#102124]">{rule.amount !== 0 ? `$${Math.abs(rule.amount).toLocaleString()}` : '-'}</span></div>
            <p className="text-xs text-[#5d6567]">{rule.description}</p>
          </div>
        </div>
      ))}
    </div>
  </div>
);

const SummaryRow = ({ label, amount, highlight, neutral }: any) => (
  <div className="flex justify-between items-center text-sm">
    <span className={`font-medium ${highlight ? 'text-[#970bed]' : 'text-[#5d6567]'}`}>{label}</span>
    <span className={`font-bold ${neutral ? 'text-[#0f1012]' :
      amount > 0 ? 'text-[#10b981]' :
        amount < 0 ? 'text-[#ec1656]' : 'text-[#babebf]'
      }`}>
      {amount === 0 ? '$0.00' : `${!neutral && amount > 0 ? '+' : ''}$${Math.abs(amount).toLocaleString()}`}
    </span>
  </div>
);
