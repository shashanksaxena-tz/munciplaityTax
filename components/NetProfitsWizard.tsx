import React, { useState } from 'react';
import { BusinessProfile, NetProfitReturnData, BusinessScheduleXDetails, BusinessAllocation, TaxFormData, BusinessFederalForm } from '../types';
import { DEFAULT_BUSINESS_RULES } from '../constants';
import { api } from '../services/api';
import { UploadSection } from './UploadSection';
import { PaymentGateway } from './PaymentGateway';
import { ScheduleXAccordion } from '../src/components/business/ScheduleXAccordion';
import { createEmptyScheduleXDetails, recalculateTotals } from '../src/types/scheduleX';
import { ArrowRight, DollarSign, ChevronLeft, Upload, Table2, TrendingDown, Calculator } from 'lucide-react';

interface Props {
  profile: BusinessProfile;
  onBack: () => void;
  onComplete: (data: NetProfitReturnData) => void;
}

export const NetProfitsWizard: React.FC<Props> = ({ profile, onBack, onComplete }) => {
  const [step, setStep] = useState(1);
  const [taxYear, setTaxYear] = useState(new Date().getFullYear() - 1);

  // Data State - Use new 27-field Schedule X structure
  const [schX, setSchX] = useState<BusinessScheduleXDetails>(createEmptyScheduleXDetails(0));

  const [schY, setSchY] = useState<BusinessAllocation>({
    property: { dublin: 0, everywhere: 0, pct: 0 },
    payroll: { dublin: 0, everywhere: 0, pct: 0 },
    sales: { dublin: 0, everywhere: 0, pct: 0 },
    totalPct: 0, averagePct: 0
  });

  const [estimates, setEstimates] = useState(0);
  const [priorCredit, setPriorCredit] = useState(0);
  const [nolCarryforward, setNolCarryforward] = useState(0);
  const [result, setResult] = useState<NetProfitReturnData | null>(null);
  const [showPayment, setShowPayment] = useState(false);

  const handleExtraction = (forms: TaxFormData[]) => {
    // Auto-fill from extracted Business Federal Forms
    const bizForm = forms.find(f => ["Federal 1120", "Federal 1065", "Form 27"].some(t => f.formType.includes(t))) as BusinessFederalForm;
    if (bizForm) {
      if (bizForm.reconciliation) setSchX(recalculateTotals(bizForm.reconciliation));
      if (bizForm.allocation) setSchY(bizForm.allocation);
      alert("Extracted Schedule X & Y data from " + bizForm.formType);
    }
    setStep(2);
  };

  const handleCalculate = async () => {
    try {
      const res = await api.taxEngine.calculateBusiness(
        taxYear, estimates, priorCredit, schX, schY, nolCarryforward, DEFAULT_BUSINESS_RULES
      );
      setResult(res);
      setStep(4);
    } catch (e) {
      console.error(e);
      alert("Calculation failed");
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-8 animate-fadeIn">
      {showPayment && result && (
        <PaymentGateway amount={result.balanceDue} recipient="City of Dublin" onCancel={() => setShowPayment(false)} onSuccess={(rec) => onComplete({ ...result, paymentStatus: 'PAID', confirmationNumber: rec.confirmationNumber })} />
      )}

      <div className="flex items-center gap-4 mb-8">
        <button onClick={onBack} className="p-2 hover:bg-slate-100 rounded-full"><ChevronLeft className="w-5 h-5 text-slate-500" /></button>
        <h2 className="text-xl font-bold">Form 27: Net Profits Return (Smart Wizard)</h2>
      </div>

      <div className="flex gap-2 mb-6">
        {[1, 2, 3, 4].map(s => <div key={s} className={`h-2 flex-1 rounded-full ${s <= step ? 'bg-indigo-600' : 'bg-slate-200'}`}></div>)}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">

        {/* Step 1: Upload */}
        {step === 1 && (
          <div className="space-y-6 text-center">
            <h3 className="text-lg font-bold">1. Upload Federal Return</h3>
            <p className="text-slate-500">Upload Form 1120 (Corp) or 1065 (Partnership) to auto-fill Schedule X & Y.</p>
            <div className="max-w-xl mx-auto border-2 border-dashed rounded-xl p-8">
              <UploadSection onDataExtracted={handleExtraction} />
            </div>
            <button onClick={() => setStep(2)} className="text-indigo-600 font-bold hover:underline">Skip Upload (Manual Entry)</button>
          </div>
        )}

        {/* Step 2: Schedule X Reconciliation (EXPANDED: 27 fields) */}
        {step === 2 && (
          <div className="space-y-6 animate-slideLeft">
            <h3 className="font-bold text-lg flex items-center gap-2">
              <Table2 className="w-5 h-5 text-indigo-600" /> 
              2. Schedule X Reconciliation (Federal to Municipal)
            </h3>
            <p className="text-sm text-slate-600">
              Reconcile federal taxable income with municipal taxable income using comprehensive M-1 adjustments.
            </p>
            
            {/* Federal Taxable Income Input */}
            <div className="bg-blue-50 p-5 rounded-xl border border-blue-200">
              <label className="block text-sm font-bold text-slate-700 mb-2">
                Federal Taxable Income (Form 1120 Line 30 / Form 1065 Line 22)
              </label>
              <input 
                type="number" 
                value={schX.fedTaxableIncome} 
                onChange={e => {
                  const updated = { ...schX, fedTaxableIncome: parseFloat(e.target.value) || 0 };
                  setSchX(recalculateTotals(updated));
                }}
                className="w-full border p-3 rounded-xl text-lg font-mono font-bold" 
                placeholder="0.00"
              />
              <p className="text-xs text-slate-500 mt-1">
                Enter net income from your federal return before municipal adjustments
              </p>
            </div>

            {/* Schedule X Accordion with all 27 fields */}
            <ScheduleXAccordion
              scheduleX={schX}
              onUpdate={(updated) => setSchX(recalculateTotals(updated))}
              entityType={profile.entityType as 'C-CORP' | 'PARTNERSHIP' | 'S-CORP'}
              className="mt-6"
            />

            <div className="flex justify-end gap-2">
              <button onClick={() => setStep(1)} className="px-4 py-2 border rounded-lg">
                Back
              </button>
              <button 
                onClick={() => setStep(3)} 
                className="bg-indigo-600 text-white px-6 py-2 rounded-lg font-bold"
              >
                Next: Allocation & Credits
              </button>
            </div>
          </div>
        )}

        {/* Step 3: Schedule Y & Credits */}
        {step === 3 && (
          <div className="space-y-6 animate-slideLeft">
            <h3 className="font-bold text-lg">3. Allocation & Credits</h3>
            <table className="w-full text-sm text-left">
              <thead><tr className="bg-slate-100"><th className="p-2">Factor</th><th className="p-2">Dublin Amount</th><th className="p-2">Everywhere Amount</th></tr></thead>
              <tbody>
                <tr>
                  <td className="p-2 font-medium">1. Property (Cost)</td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.property.dublin} onChange={e => setSchY({ ...schY, property: { ...schY.property, dublin: parseFloat(e.target.value) } })} /></td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.property.everywhere} onChange={e => setSchY({ ...schY, property: { ...schY.property, everywhere: parseFloat(e.target.value) } })} /></td>
                </tr>
                <tr>
                  <td className="p-2 font-medium">2. Payroll (Wages)</td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.payroll.dublin} onChange={e => setSchY({ ...schY, payroll: { ...schY.payroll, dublin: parseFloat(e.target.value) } })} /></td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.payroll.everywhere} onChange={e => setSchY({ ...schY, payroll: { ...schY.payroll, everywhere: parseFloat(e.target.value) } })} /></td>
                </tr>
                <tr>
                  <td className="p-2 font-medium">3. Sales (Gross Receipts)</td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.sales.dublin} onChange={e => setSchY({ ...schY, sales: { ...schY.sales, dublin: parseFloat(e.target.value) } })} /></td>
                  <td className="p-2"><input type="number" className="border w-full p-1" value={schY.sales.everywhere} onChange={e => setSchY({ ...schY, sales: { ...schY.sales, everywhere: parseFloat(e.target.value) } })} /></td>
                </tr>
              </tbody>
            </table>

            <div className="bg-amber-50 p-4 rounded-xl border border-amber-200">
              <h4 className="font-bold text-amber-800 text-sm mb-2 flex items-center gap-2"><TrendingDown className="w-4 h-4" /> Net Operating Loss (NOL)</h4>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-amber-700 uppercase">Available Carryforward</label>
                  <input type="number" value={nolCarryforward} onChange={e => setNolCarryforward(parseFloat(e.target.value))} className="w-full border border-amber-300 p-2 rounded" placeholder="0.00" />
                </div>
                <div className="text-xs text-amber-600 flex items-center">
                  Prior year losses can offset up to 50% of current income.
                </div>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="flex-1"><label className="text-sm font-bold">Estimates Paid</label><input type="number" value={estimates} onChange={e => setEstimates(parseFloat(e.target.value))} className="w-full border p-2 rounded" /></div>
              <div className="flex-1"><label className="text-sm font-bold">Prior Credits</label><input type="number" value={priorCredit} onChange={e => setPriorCredit(parseFloat(e.target.value))} className="w-full border p-2 rounded" /></div>
            </div>
            <div className="flex justify-end gap-2">
              <button onClick={() => setStep(2)} className="px-4 py-2 border rounded">Back</button>
              <button onClick={handleCalculate} className="bg-indigo-600 text-white px-6 py-2 rounded-lg font-bold">Calculate Tax</button>
            </div>
          </div>
        )}

        {/* Step 4: Result */}
        {step === 4 && result && (
          <div className="space-y-6 animate-slideLeft text-center">
            <h3 className="text-2xl font-bold">Tax Calculation Complete</h3>
            <div className="bg-slate-50 p-6 rounded-xl inline-block text-left min-w-[300px] space-y-2">
              <div className="flex justify-between"><span>Adj. Fed Income:</span> <b>${result.adjustedFedTaxableIncome.toLocaleString()}</b></div>
              <div className="flex justify-between"><span>Allocation %:</span> <b>{(result.allocation.averagePct * 100).toFixed(4)}%</b></div>
              <div className="flex justify-between border-b pb-2"><span>Allocated Income:</span> <b>${result.allocatedTaxableIncome.toLocaleString()}</b></div>

              {result.nolApplied > 0 && (
                <div className="flex justify-between text-amber-600"><span>NOL Applied:</span> <b>-${result.nolApplied.toLocaleString()}</b></div>
              )}
              <div className="flex justify-between font-medium"><span>Net Taxable Income:</span> <b>${result.taxableIncomeAfterNOL.toLocaleString()}</b></div>

              <div className="border-t pt-2 mt-2 flex justify-between text-lg"><span>Tax Due (2%):</span> <b>${result.taxDue.toLocaleString()}</b></div>
              <div className="flex justify-between text-green-600"><span>Credits:</span> <b>-${(result.estimatedPayments + result.priorYearCredit).toLocaleString()}</b></div>

              {(result.penaltyUnderpayment > 0 || result.interest > 0) && (
                <div className="bg-red-50 p-2 rounded mt-2 text-xs text-red-700">
                  {result.penaltyUnderpayment > 0 && <div className="flex justify-between"><span>Penalty:</span> <b>+${result.penaltyUnderpayment.toLocaleString()}</b></div>}
                  {result.interest > 0 && <div className="flex justify-between"><span>Interest:</span> <b>+${result.interest.toLocaleString()}</b></div>}
                </div>
              )}

              <div className="border-t pt-2 mt-2 flex justify-between text-xl font-bold text-indigo-900"><span>Balance Due:</span> <b>${result.balanceDue.toLocaleString()}</b></div>
            </div>
            <div className="flex justify-center gap-4">
              <button onClick={() => setStep(3)} className="px-6 py-3 border rounded-xl">Edit</button>
              {result.balanceDue > 0 ? (
                <button onClick={() => setShowPayment(true)} className="px-6 py-3 bg-green-600 text-white font-bold rounded-xl flex items-center gap-2"><DollarSign className="w-4 h-4" /> Pay Now</button>
              ) : (
                <button onClick={() => onComplete({ ...result, paymentStatus: 'PAID' })} className="px-6 py-3 bg-indigo-600 text-white font-bold rounded-xl">Submit</button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const Input = ({ label, val, set }: any) => (
  <div>
    <label className="text-xs font-bold text-slate-500 uppercase block mb-1">{label}</label>
    <input type="number" value={val} onChange={e => set(parseFloat(e.target.value))} className="w-full border border-slate-300 rounded p-1.5" />
  </div>
);
