
import React, { useState, useEffect } from 'react';
import { BusinessProfile, ReconciliationReturnData, WithholdingReturnData } from '../types';
import { reconcileW3 } from '../utils/businessUtils';
import { ArrowRight, AlertOctagon, CheckCircle, ChevronLeft } from 'lucide-react';

interface ReconciliationWizardProps {
  profile: BusinessProfile;
  filings: WithholdingReturnData[];
  onBack: () => void;
  onComplete: (data: ReconciliationReturnData) => void;
}

export const ReconciliationWizard: React.FC<ReconciliationWizardProps> = ({ profile, filings, onBack, onComplete }) => {
  const [w2Tax, setW2Tax] = useState(0);
  const [result, setResult] = useState<ReconciliationReturnData | null>(null);

  const currentYear = new Date().getFullYear() - 1;
  const totalW1 = filings.filter(f => f.period.year === currentYear).reduce((acc, f) => acc + f.taxDue, 0);

  const handleReconcile = () => {
     setResult(reconcileW3(totalW1, w2Tax, currentYear));
  };

  return (
    <div className="max-w-3xl mx-auto py-8 animate-fadeIn">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={onBack} className="p-2 hover:bg-slate-100 rounded-full"><ChevronLeft className="w-5 h-5 text-slate-500"/></button>
        <div>
           <h2 className="text-xl font-bold text-slate-900">Form W-3: Annual Reconciliation</h2>
           <p className="text-sm text-slate-500">{profile.businessName} • Tax Year {currentYear}</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        {!result ? (
          <div className="space-y-6">
             <div className="bg-indigo-50 p-4 rounded-xl">
                <div className="text-sm font-bold text-indigo-800 uppercase mb-1">Total W-1 Tax Remitted</div>
                <div className="text-3xl font-bold text-indigo-900">${totalW1.toLocaleString(undefined, {minimumFractionDigits: 2})}</div>
                <div className="text-xs text-indigo-600 mt-1">Calculated from {filings.length} filings this year.</div>
             </div>
             <div>
                <label className="block text-sm font-bold text-slate-700 mb-2">Total Local Tax Withheld on W-2s</label>
                <input 
                  type="number" 
                  value={w2Tax} 
                  onChange={e=>setW2Tax(parseFloat(e.target.value))} 
                  className="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none text-lg"
                  placeholder="0.00"
                />
                <p className="text-xs text-slate-500 mt-2">Enter the Sum of Box 19 from all employee W-2s.</p>
             </div>
             <button onClick={handleReconcile} className="w-full py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700">Run Reconciliation</button>
          </div>
        ) : (
          <div className="text-center space-y-6 animate-slideLeft">
             {result.status === 'BALANCED' ? (
               <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto"><CheckCircle className="w-8 h-8 text-green-600"/></div>
             ) : (
               <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto"><AlertOctagon className="w-8 h-8 text-red-600"/></div>
             )}
             
             <h3 className="text-2xl font-bold text-slate-900">{result.status === 'BALANCED' ? 'Account Balanced' : 'Discrepancy Found'}</h3>
             <p className="text-slate-600">Difference: <span className="font-mono font-bold">${result.discrepancy.toFixed(2)}</span></p>
             
             <button onClick={() => onComplete(result)} className="w-full py-3 bg-slate-900 text-white rounded-xl font-bold">Submit W-3</button>
          </div>
        )}
      </div>
    </div>
  );
};
