
import React, { useState, useEffect, useMemo } from 'react';
import { BusinessProfile, WithholdingPeriod, WithholdingReturnData, FilingFrequency, ReconciliationIssue } from '../types';
import { calculateWithholding, reconcilePayroll, getAvailablePeriods, getDailyPeriod, reconcileW1Filings } from '../utils/businessUtils';
import { ArrowRight, Upload, DollarSign, ChevronLeft, FileCheck } from 'lucide-react';
import { DiscrepancyView } from './DiscrepancyView';
import { PaymentGateway } from './PaymentGateway';
import { ReconciliationIssuesList } from './ReconciliationIssuesList';
import { PeriodHistoryTable } from './PeriodHistoryTable';
import { CumulativeTotalsPanel } from './CumulativeTotalsPanel';

interface WithholdingWizardProps {
  profile: BusinessProfile;
  onBack: () => void;
  onComplete: (data: WithholdingReturnData) => void;
}

export const WithholdingWizard: React.FC<WithholdingWizardProps> = ({ profile, onBack, onComplete }) => {
  const [step, setStep] = useState(1);
  
  // Step 1
  const [selectedPeriod, setSelectedPeriod] = useState<WithholdingPeriod | null>(null);
  const [dailyDate, setDailyDate] = useState<string>(new Date().toISOString().split('T')[0]);

  // Step 2
  const [wages, setWages] = useState<number>(0);
  const [adjustments, setAdjustments] = useState<number>(0);
  const [uploadedWages, setUploadedWages] = useState<number | null>(null);
  
  const [calculation, setCalculation] = useState<WithholdingReturnData | null>(null);
  const [showPayment, setShowPayment] = useState(false);
  
  // Step 4 - Reconciliation
  const [reconciliationIssues, setReconciliationIssues] = useState<ReconciliationIssue[]>([]);
  const [loadingReconciliation, setLoadingReconciliation] = useState(false);
  const [showReconciliation, setShowReconciliation] = useState(false);
  
  // Mock filings data for demo (in production, this would come from backend/session)
  // This represents past W-1 filings for the year
  const mockFilings: WithholdingReturnData[] = useMemo(() => {
    const currentYear = new Date().getFullYear();
    
    // Generate sample quarterly filings for demonstration
    if (profile.filingFrequency === FilingFrequency.QUARTERLY) {
      return [
        {
          id: '1',
          dateFiled: `${currentYear}-04-15`,
          period: { year: currentYear, period: 'Q1', startDate: `${currentYear}-01-01`, endDate: `${currentYear}-03-31`, dueDate: `${currentYear}-04-30` },
          grossWages: 125000,
          adjustments: 0,
          taxDue: 2500,
          penalty: 0,
          interest: 0,
          totalAmountDue: 2500,
          isReconciled: true,
          paymentStatus: 'PAID',
          confirmationNumber: 'W1-Q1-2024-001'
        },
        {
          id: '2',
          dateFiled: `${currentYear}-07-20`,
          period: { year: currentYear, period: 'Q2', startDate: `${currentYear}-04-01`, endDate: `${currentYear}-06-30`, dueDate: `${currentYear}-07-31` },
          grossWages: 132000,
          adjustments: -1000,
          taxDue: 2620,
          penalty: 0,
          interest: 0,
          totalAmountDue: 2620,
          isReconciled: true,
          paymentStatus: 'PAID',
          confirmationNumber: 'W1-Q2-2024-002'
        }
      ];
    }
    
    return [];
  }, [profile.filingFrequency]);

  const availablePeriods = useMemo(() => {
     if (profile.filingFrequency === FilingFrequency.DAILY) return [];
     return getAvailablePeriods(profile.filingFrequency, new Date().getFullYear());
  }, [profile.filingFrequency]);

  useEffect(() => {
    if (wages > 0 && selectedPeriod) {
      setCalculation(calculateWithholding(wages, adjustments, selectedPeriod));
    }
  }, [wages, adjustments, selectedPeriod]);

  const handleDailyDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setDailyDate(val);
    setSelectedPeriod(getDailyPeriod(val));
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTimeout(() => setUploadedWages(wages + 150.00), 800);
  };

  const discrepancy = (uploadedWages !== null && calculation) 
    ? reconcilePayroll(calculation.grossWages, uploadedWages) 
    : null;

  // Function to load reconciliation data
  const loadReconciliation = async () => {
    setLoadingReconciliation(true);
    try {
      // Use FEIN as employer ID (Federal Employer Identification Number)
      const employerId = profile.fein;
      if (!employerId) {
        console.warn('No FEIN available for reconciliation');
        setReconciliationIssues([]);
        return;
      }
      const taxYear = new Date().getFullYear();
      const issues = await reconcileW1Filings(employerId, taxYear);
      setReconciliationIssues(issues);
    } catch (error) {
      console.error('Failed to load reconciliation:', error);
    } finally {
      setLoadingReconciliation(false);
    }
  };

  // Load reconciliation when showing reconciliation view
  useEffect(() => {
    if (showReconciliation) {
      loadReconciliation();
    }
  }, [showReconciliation]);

  const handleResolveIssue = (issueId: string, note: string) => {
    // Update the issue locally (in production, would call backend API)
    setReconciliationIssues(prev => 
      prev.map(issue => 
        issue.id === issueId 
          ? { ...issue, resolved: true, resolutionNote: note, resolvedDate: new Date().toISOString().split('T')[0] }
          : issue
      )
    );
  };

  return (
    <div className="max-w-3xl mx-auto py-8 animate-fadeIn">
      {showPayment && calculation && (
        <PaymentGateway 
          amount={calculation.totalAmountDue} 
          recipient="City of Dublin Tax Division"
          onCancel={() => setShowPayment(false)}
          onSuccess={(record) => {
             setShowPayment(false);
             const finalData: WithholdingReturnData = {
               ...calculation,
               paymentStatus: 'PAID',
               confirmationNumber: record.confirmationNumber
             };
             onComplete(finalData);
          }}
        />
      )}

      <div className="flex items-center gap-4 mb-8">
        <button onClick={onBack} className="p-2 hover:bg-[#fbfbfb] rounded-full"><ChevronLeft className="w-5 h-5 text-[#5d6567]"/></button>
        <div className="flex-1">
           <h2 className="text-xl font-bold text-[#0f1012]">File Withholding (Form W-1)</h2>
           <p className="text-sm text-[#5d6567]">Frequency: {profile.filingFrequency}</p>
        </div>
        <button
          onClick={() => setShowReconciliation(true)}
          className="px-4 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-medium shadow-sm flex items-center gap-2"
        >
          <FileCheck className="w-4 h-4" />
          View Reconciliation
        </button>
      </div>

      <div className="flex gap-2 mb-6">
         {[1, 2, 3].map(s => (
            <div key={s} className={`h-2 flex-1 rounded-full transition-all ${s <= step ? 'bg-gradient-to-r from-[#970bed] to-[#469fe8]' : 'bg-[#dcdede]'}`}></div>
         ))}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-[#dcdede] overflow-hidden">
        
        {step === 1 && (
           <div className="p-8">
              <h3 className="font-bold text-[#0f1012] mb-4">1. Select Filing Period</h3>
              
              {profile.filingFrequency === FilingFrequency.DAILY ? (
                 <div className="space-y-4">
                    <label className="block text-sm font-medium text-[#102124]">Select Date of Wages</label>
                    <input 
                      type="date" 
                      value={dailyDate}
                      onChange={handleDailyDateChange}
                      className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none"
                    />
                    <div className="p-3 bg-[#ebf4ff] border border-[#469fe8]/30 rounded-lg text-xs text-[#469fe8]">
                       Daily filers must remit taxes by the next banking day.
                    </div>
                 </div>
              ) : (
                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-80 overflow-y-auto pr-2">
                    {availablePeriods.map((p) => {
                       const isSelected = selectedPeriod?.period === p.period;
                       return (
                         <button 
                           key={p.period}
                           onClick={() => setSelectedPeriod(p)}
                           className={`text-left p-4 rounded-xl border transition-all ${isSelected ? 'border-[#970bed] bg-[#970bed]/10 ring-1 ring-[#970bed]' : 'border-[#dcdede] hover:border-[#469fe8]'}`}
                         >
                            <div className={`font-bold ${isSelected ? 'text-[#970bed]' : 'text-[#0f1012]'}`}>{p.period}</div>
                            <div className="text-xs text-[#5d6567] mt-1">Due: {p.dueDate}</div>
                         </button>
                       )
                    })}
                 </div>
              )}

              <div className="flex justify-end pt-8">
                <button 
                  onClick={() => setStep(2)}
                  disabled={!selectedPeriod}
                  className="px-6 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold shadow-lg shadow-[#970bed]/20 disabled:opacity-50 transition-all flex items-center gap-2"
                >
                  Next: Enter Data <ArrowRight className="w-4 h-4" />
                </button>
             </div>
           </div>
        )}
        
        {step === 2 && (
          <div className="p-8 space-y-6 animate-slideLeft">
             <div className="bg-[#fbfbfb] border border-[#dcdede] p-4 rounded-xl">
               <div className="flex justify-between items-center mb-4">
                  <h3 className="font-bold text-[#0f1012]">2. Enter Payroll Data</h3>
                  <span className="text-xs font-mono bg-white border border-[#dcdede] px-2 py-1 rounded text-[#5d6567]">{selectedPeriod?.period}</span>
               </div>
               <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold uppercase text-[#5d6567] mb-1">Total Gross Wages</label>
                    <div className="relative">
                       <DollarSign className="absolute left-3 top-2.5 w-4 h-4 text-[#babebf]" />
                       <input 
                         type="number" 
                         value={wages || ''} onChange={e => setWages(parseFloat(e.target.value))}
                         className="w-full pl-9 pr-3 py-2 border border-[#dcdede] rounded-lg outline-none focus:ring-2 focus:ring-[#970bed] font-mono text-lg"
                         placeholder="0.00"
                       />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold uppercase text-[#5d6567] mb-1">Adjustments (+/-)</label>
                    <input 
                         type="number" 
                         value={adjustments || ''} onChange={e => setAdjustments(parseFloat(e.target.value))}
                         className="w-full px-3 py-2 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-lg outline-none"
                         placeholder="0.00"
                       />
                  </div>
               </div>
             </div>
             <div className="border-t border-[#dcdede] pt-6">
               <h3 className="font-bold text-[#0f1012] mb-4 flex items-center gap-2"><Upload className="w-4 h-4 text-[#469fe8]" /> Reconcile (Optional)</h3>
               <div className="border-2 border-dashed border-[#dcdede] rounded-xl p-6 text-center hover:bg-[#fbfbfb] transition-colors relative">
                  <input type="file" onChange={handleFileUpload} className="absolute inset-0 opacity-0 cursor-pointer" />
                  <p className="text-sm text-[#5d6567] font-medium">Drop Payroll File</p>
               </div>
               {discrepancy && discrepancy.hasDiscrepancies && <div className="mt-4"><DiscrepancyView report={discrepancy} /></div>}
             </div>
             <div className="flex justify-between pt-4">
                <button onClick={() => setStep(1)} className="text-[#5d6567] font-medium hover:text-[#0f1012]">Change Period</button>
                <button onClick={() => setStep(3)} disabled={!wages} className="px-6 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold shadow-lg shadow-[#970bed]/20 disabled:opacity-50 transition-all flex items-center gap-2">
                  Calculate & Review <ArrowRight className="w-4 h-4" />
                </button>
             </div>
          </div>
        )}

        {step === 3 && calculation && (
           <div className="p-8 animate-slideLeft">
              <div className="bg-[#ebf4ff] border border-[#469fe8]/30 rounded-xl p-6 mb-6 text-center">
                 <h3 className="text-sm font-bold uppercase text-[#5d6567] tracking-wider mb-2">Total Amount Due</h3>
                 <div className="text-4xl font-bold text-[#970bed] mb-4">${calculation.totalAmountDue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                 <div className="inline-block text-left space-y-1 text-sm text-[#5d6567] bg-white/50 p-4 rounded-lg">
                    <div className="flex justify-between gap-8"><span>Gross Wages:</span><span className="font-mono text-[#0f1012]">${calculation.grossWages.toLocaleString()}</span></div>
                    <div className="flex justify-between gap-8"><span>Tax Due (2.0%):</span><span className="font-mono text-[#0f1012]">${calculation.taxDue.toLocaleString()}</span></div>
                    {calculation.penalty > 0 && <div className="flex justify-between gap-8 text-[#ec1656]"><span>Penalty:</span><span className="font-mono">+${calculation.penalty.toLocaleString()}</span></div>}
                    {calculation.interest > 0 && <div className="flex justify-between gap-8 text-[#ec1656]"><span>Interest:</span><span className="font-mono">+${calculation.interest.toLocaleString()}</span></div>}
                 </div>
              </div>
              <div className="flex gap-4">
                 <button onClick={() => setStep(2)} className="flex-1 py-3 border border-[#dcdede] rounded-xl font-medium text-[#5d6567] hover:bg-[#fbfbfb]">Back to Edit</button>
                 <button onClick={() => setShowPayment(true)} className="flex-[2] py-3 bg-gradient-to-r from-[#10b981] to-[#10b981] hover:from-[#059669] hover:to-[#059669] text-white rounded-xl font-bold shadow-lg shadow-[#10b981]/20 flex items-center justify-center gap-2">
                    <DollarSign className="w-4 h-4" /> Pay Now
                 </button>
              </div>
           </div>
        )}
      </div>

      {/* Reconciliation Modal/Overlay */}
      {showReconciliation && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-start justify-center overflow-y-auto p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-7xl w-full my-8 animate-slideUp">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] p-6 rounded-t-2xl">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-2xl font-bold text-white flex items-center gap-2">
                    <FileCheck className="w-6 h-6" />
                    W-1 Filing Reconciliation
                  </h2>
                  <p className="text-white/80 text-sm mt-1">
                    {profile.businessName} - Tax Year {new Date().getFullYear()}
                  </p>
                </div>
                <button
                  onClick={() => setShowReconciliation(false)}
                  className="p-2 hover:bg-white/20 rounded-full transition-colors"
                >
                  <ChevronLeft className="w-6 h-6 text-white" />
                </button>
              </div>
            </div>

            {/* Modal Content */}
            <div className="p-6 space-y-6 max-h-[calc(100vh-200px)] overflow-y-auto">
              {loadingReconciliation ? (
                <div className="text-center py-12">
                  <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-gray-200 border-t-[#970bed]"></div>
                  <p className="text-gray-600 mt-4">Loading reconciliation data...</p>
                </div>
              ) : (
                <>
                  {/* Cumulative Totals Panel */}
                  <CumulativeTotalsPanel 
                    filings={mockFilings}
                    taxYear={new Date().getFullYear()}
                  />

                  {/* Reconciliation Issues */}
                  <div>
                    <ReconciliationIssuesList 
                      issues={reconciliationIssues}
                      onResolveIssue={handleResolveIssue}
                    />
                  </div>

                  {/* Period History Table */}
                  <div>
                    <PeriodHistoryTable 
                      filings={mockFilings}
                      onSelectPeriod={(filing) => {
                        console.log('Selected filing:', filing);
                      }}
                    />
                  </div>
                </>
              )}
            </div>

            {/* Modal Footer */}
            <div className="bg-gray-50 border-t border-gray-200 p-4 rounded-b-2xl flex justify-between items-center">
              <div className="text-sm text-gray-600">
                Last updated: {new Date().toLocaleString()}
              </div>
              <button
                onClick={() => setShowReconciliation(false)}
                className="px-6 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded-xl font-medium transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
