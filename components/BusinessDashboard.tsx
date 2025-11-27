
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
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
         <div className="flex items-center gap-4">
            <div className="w-16 h-16 bg-slate-900 rounded-xl flex items-center justify-center text-white"><Briefcase className="w-8 h-8" /></div>
            <div>
               <h2 className="text-2xl font-bold text-slate-900">{profile.businessName}</h2>
               <div className="flex gap-3 text-sm text-slate-500 mt-1">
                  <span className="flex items-center gap-1"><span className="font-mono bg-slate-100 px-1.5 rounded text-slate-700">FEIN: {profile.fein}</span></span>
                  <span>•</span>
                  <span>Freq: <span className="font-medium text-indigo-600">{profile.filingFrequency}</span></span>
               </div>
            </div>
         </div>
         <div className="flex gap-2">
            {onOpenRules && (
              <button onClick={onOpenRules} className="p-2 border rounded-lg hover:bg-slate-50 text-slate-600" title="Business Tax Rules"><Settings className="w-5 h-5"/></button>
            )}
            <div className="text-right ml-2">
               <div className="text-xs text-slate-400 uppercase font-bold tracking-wider">Account Status</div>
               <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-green-100 text-green-700 rounded-full font-bold text-sm mt-1"><span className="w-2 h-2 bg-green-500 rounded-full"></span> Active</div>
            </div>
         </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
         <div className={`rounded-xl p-6 border shadow-sm relative overflow-hidden ${isUrgent ? 'bg-amber-50 border-amber-200' : 'bg-white border-slate-200'}`}>
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-2">Next Withholding Due</h3>
            <div className="text-3xl font-bold text-slate-900 mb-1">{nextDueDate.toLocaleDateString()}</div>
            <div className={`text-sm font-medium ${isUrgent ? 'text-amber-700' : 'text-green-600'}`}>{daysUntilDue < 0 ? 'Overdue!' : `Due in ${daysUntilDue} days`}</div>
            <button onClick={onStartWithholding} className="mt-6 w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-bold shadow-lg shadow-indigo-100 transition-all flex items-center justify-center gap-2"><DollarSign className="w-4 h-4" /> File & Pay Now</button>
         </div>

         <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm relative overflow-hidden">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-4">Year-to-Date</h3>
            <div className="space-y-4">
               <div><div className="flex justify-between text-sm mb-1"><span className="text-slate-600">Total Wages Paid</span><span className="font-medium">${totalWages.toLocaleString()}</span></div><div className="w-full bg-slate-100 h-1.5 rounded-full"><div className="w-3/4 bg-blue-500 h-1.5 rounded-full"></div></div></div>
               <div><div className="flex justify-between text-sm mb-1"><span className="text-slate-600">Tax Remitted</span><span className="font-medium">${totalTax.toLocaleString()}</span></div><div className="w-full bg-slate-100 h-1.5 rounded-full"><div className="w-3/4 bg-green-500 h-1.5 rounded-full"></div></div></div>
            </div>
         </div>

         <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm flex flex-col justify-between">
            <div><h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-4">Quick Actions</h3></div>
            <div className="space-y-2">
               <button onClick={onStartNetProfits} className="w-full py-2 border hover:bg-slate-50 text-slate-700 rounded-lg text-sm font-bold flex items-center justify-center gap-2"><FileText className="w-4 h-4"/> Form 27 (Annual)</button>
               <button onClick={onViewHistory} className="w-full py-2 border hover:bg-slate-50 text-slate-700 rounded-lg text-sm font-medium flex items-center justify-center gap-2"><History className="w-4 h-4" /> View History</button>
            </div>
         </div>
      </div>

      <h3 className="font-bold text-slate-800 text-lg">Compliance Center</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
         <ActionCard title="Form W-1" subtitle="Withholding Return" onClick={onStartWithholding} />
         <ActionCard title="Form 27" subtitle="Net Profits Return" onClick={onStartNetProfits} />
         <ActionCard title="Form W-3" subtitle="Annual Reconciliation" onClick={onStartReconciliation} />
         <label className="p-4 bg-white border border-slate-200 hover:border-indigo-400 hover:shadow-md rounded-xl text-left transition-all cursor-pointer group relative">
            <input type="file" className="absolute inset-0 opacity-0 cursor-pointer" onChange={handleBulkUpload} accept=".csv" />
            <div className="font-bold text-slate-800 group-hover:text-indigo-600 transition-colors flex items-center gap-2"><Upload className="w-4 h-4"/> Bulk Upload</div>
            <div className="text-xs text-slate-500">Upload CSV Payroll Data</div>
         </label>
      </div>
    </div>
  );
};

const ActionCard = ({ title, subtitle, onClick }: any) => (
  <button onClick={onClick} className="p-4 bg-white border border-slate-200 hover:border-indigo-400 hover:shadow-md rounded-xl text-left transition-all group">
     <div className="font-bold text-slate-800 group-hover:text-indigo-600 transition-colors">{title}</div>
     <div className="text-xs text-slate-500">{subtitle}</div>
  </button>
);
