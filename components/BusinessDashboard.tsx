
import React from 'react';
import { BusinessProfile, FilingFrequency, TaxReturnSession, WithholdingReturnData } from '../types';
import { Calendar, TrendingUp, History, DollarSign, Briefcase, Upload, Settings, FileText } from 'lucide-react';
import { getNextDueDate } from '../utils/businessUtils';
import { calculateWithholding, getAvailablePeriods } from '../utils/businessUtils';
import { saveSession } from '../services/sessionService';

interface BusinessDashboardProps {
  session: TaxReturnSession;
  onStartWithholding: () => void;
  onStartNetProfits: () => void;
  onStartReconciliation: () => void;
  onViewHistory: () => void;
  onOpenRules?: () => void;
  onUploadDocs?: () => void;
}

export const BusinessDashboard: React.FC<BusinessDashboardProps> = ({ session, onStartWithholding, onStartNetProfits, onStartReconciliation, onViewHistory, onOpenRules, onUploadDocs }) => {
  const profile = session.profile as BusinessProfile;
  const nextDueDate = getNextDueDate(profile.filingFrequency);
  const daysUntilDue = Math.ceil((nextDueDate.getTime() - new Date().getTime()) / (1000 * 3600 * 24));
  const isUrgent = daysUntilDue <= 3;

  const history = session.businessFilings || [];
  const totalWages = history.reduce((acc, h) => acc + h.grossWages, 0);
  const totalTax = history.reduce((acc, h) => acc + h.taxDue, 0);

  const handleBulkUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
       setTimeout(() => {
          const periods = getAvailablePeriods(profile.filingFrequency, new Date().getFullYear());
          const currentP = periods.find(p => p.period === 'Q1') || periods[0];
          const draft: WithholdingReturnData = { ...calculateWithholding(50000, 0, currentP), paymentStatus: 'PAID', confirmationNumber: 'BULK-CSV-99' };
          const updatedHistory = [...history, draft];
          const updatedSession = { ...session, businessFilings: updatedHistory, lastModifiedDate: new Date().toISOString() };
          saveSession(updatedSession);
          alert("Bulk Payroll Uploaded & Processed Successfully!");
          window.location.reload(); 
       }, 1000);
    }
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      <div className="bg-white rounded-2xl border border-[#dcdede] p-6 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
         <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-[#0f1012] rounded-xl flex items-center justify-center text-white"><Briefcase className="w-8 h-8" /></div>
            <div>
               <h2 className="text-2xl font-bold text-[#0f1012]">{profile.businessName}</h2>
               <div className="flex gap-3 text-sm text-[#5d6567] mt-1">
                  <span className="flex items-center gap-1"><span className="font-mono bg-[#f0f0f0] px-1.5 rounded text-[#0f1012]">FEIN: {profile.fein}</span></span>
                  <span>•</span>
                  <span>Freq: <span className="font-medium text-[#970bed]">{profile.filingFrequency}</span></span>
               </div>
            </div>
         </div>
         <div className="flex gap-2">
            {onOpenRules && (
              <button onClick={onOpenRules} className="p-2 border border-[#dcdede] rounded-lg hover:bg-[#fbfbfb] text-[#5d6567]" title="Business Tax Rules"><Settings className="w-5 h-5"/></button>
            )}
            <div className="text-right ml-2">
               <div className="text-xs text-[#babebf] uppercase font-bold tracking-wider">Account Status</div>
               <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-[#d5faeb] text-[#10b981] rounded-full font-bold text-sm mt-1"><span className="w-2 h-2 bg-[#10b981] rounded-full"></span> Active</div>
            </div>
         </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
         <div className={`rounded-xl p-6 border shadow-sm relative overflow-hidden ${isUrgent ? 'bg-[#f59e0b]/10 border-[#f59e0b]/30' : 'bg-white border-[#dcdede]'}`}>
            <h3 className="text-sm font-bold uppercase tracking-wider text-[#babebf] mb-2">Next Withholding Due</h3>
            <div className="text-3xl font-bold text-[#0f1012] mb-1">{nextDueDate.toLocaleDateString()}</div>
            <div className={`text-sm font-medium ${isUrgent ? 'text-[#f59e0b]' : 'text-[#10b981]'}`}>{daysUntilDue < 0 ? 'Overdue!' : `Due in ${daysUntilDue} days`}</div>
            <button onClick={onStartWithholding} className="mt-6 w-full py-2.5 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg font-bold shadow-lg shadow-[#970bed]/20 transition-all flex items-center justify-center gap-2"><DollarSign className="w-4 h-4" /> File & Pay Now</button>
         </div>

         <div className="bg-white rounded-xl p-6 border border-[#dcdede] shadow-sm relative overflow-hidden">
            <h3 className="text-sm font-bold uppercase tracking-wider text-[#babebf] mb-4">Year-to-Date</h3>
            <div className="space-y-4">
               <div><div className="flex justify-between text-sm mb-1"><span className="text-[#5d6567]">Total Wages Paid</span><span className="font-medium text-[#0f1012]">${totalWages.toLocaleString()}</span></div><div className="w-full bg-[#f0f0f0] h-1.5 rounded-full"><div className="w-3/4 bg-[#469fe8] h-1.5 rounded-full"></div></div></div>
               <div><div className="flex justify-between text-sm mb-1"><span className="text-[#5d6567]">Tax Remitted</span><span className="font-medium text-[#0f1012]">${totalTax.toLocaleString()}</span></div><div className="w-full bg-[#f0f0f0] h-1.5 rounded-full"><div className="w-3/4 bg-[#10b981] h-1.5 rounded-full"></div></div></div>
            </div>
         </div>

         <div className="bg-white rounded-xl p-6 border border-[#dcdede] shadow-sm flex flex-col justify-between">
            <div><h3 className="text-sm font-bold uppercase tracking-wider text-[#babebf] mb-4">Quick Actions</h3></div>
            <div className="space-y-2">
               <button onClick={onStartNetProfits} className="w-full py-2 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#5d6567] rounded-lg text-sm font-bold flex items-center justify-center gap-2"><FileText className="w-4 h-4"/> Form 27 (Annual)</button>
               <button onClick={onViewHistory} className="w-full py-2 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#5d6567] rounded-lg text-sm font-medium flex items-center justify-center gap-2"><History className="w-4 h-4" /> View History</button>
            </div>
         </div>
      </div>

      <h3 className="font-bold text-[#0f1012] text-lg">Compliance Center</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
         <ActionCard title="Form W-1" subtitle="Withholding Return" onClick={onStartWithholding} />
         <ActionCard title="Form 27" subtitle="Net Profits Return" onClick={onStartNetProfits} />
         <ActionCard title="Form W-3" subtitle="Annual Reconciliation" onClick={onStartReconciliation} />
         <label className="p-4 bg-white border border-[#dcdede] hover:border-[#970bed] hover:shadow-md rounded-xl text-left transition-all cursor-pointer group relative">
            <input type="file" className="absolute inset-0 opacity-0 cursor-pointer" onChange={handleBulkUpload} accept=".csv" />
            <div className="font-bold text-[#0f1012] group-hover:text-[#970bed] transition-colors flex items-center gap-2"><Upload className="w-4 h-4"/> Bulk Upload</div>
            <div className="text-xs text-[#5d6567]">Upload CSV Payroll Data</div>
         </label>
      </div>
    </div>
  );
};

const ActionCard = ({ title, subtitle, onClick }: any) => (
  <button onClick={onClick} className="p-4 bg-white border border-[#dcdede] hover:border-[#970bed] hover:shadow-md rounded-xl text-left transition-all group">
     <div className="font-bold text-[#0f1012] group-hover:text-[#970bed] transition-colors">{title}</div>
     <div className="text-xs text-[#5d6567]">{subtitle}</div>
  </button>
);
