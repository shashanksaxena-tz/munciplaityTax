
import React from 'react';
import { ChevronLeft, CheckCircle, AlertCircle, Briefcase } from 'lucide-react';
import { BusinessProfile, WithholdingReturnData } from '../types';

interface BusinessHistoryProps {
  profile: BusinessProfile;
  history?: WithholdingReturnData[];
  onBack: () => void;
}

export const BusinessHistory: React.FC<BusinessHistoryProps> = ({ profile, history = [], onBack }) => {
  return (
    <div className="max-w-5xl mx-auto py-8 animate-fadeIn">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={onBack} className="p-2 hover:bg-slate-100 rounded-full"><ChevronLeft className="w-5 h-5 text-slate-500"/></button>
        <div>
           <h2 className="text-xl font-bold text-slate-900">Filing History</h2>
           <p className="text-sm text-slate-500">{profile.businessName} (FEIN: {profile.fein})</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden min-h-[400px]">
        {history.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 text-slate-400">
             <Briefcase className="w-12 h-12 mb-3 opacity-20" />
             <p>No filings found for this business.</p>
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider">
               <tr>
                 <th className="px-6 py-4">Period</th>
                 <th className="px-6 py-4">Date Filed</th>
                 <th className="px-6 py-4 text-right">Gross Wages</th>
                 <th className="px-6 py-4 text-right">Tax Paid</th>
                 <th className="px-6 py-4">Status</th>
                 <th className="px-6 py-4 text-right">Ref #</th>
               </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
               {history.map((item) => (
                 <tr key={item.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 font-medium text-indigo-600">{item.period.period} {item.period.year}</td>
                    <td className="px-6 py-4 text-slate-600">{new Date(item.dateFiled).toLocaleDateString()}</td>
                    <td className="px-6 py-4 text-right text-slate-600">${item.grossWages.toLocaleString()}</td>
                    <td className="px-6 py-4 text-right font-mono font-bold text-slate-800">${item.totalAmountDue.toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                    <td className="px-6 py-4">
                       <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase ${
                         item.paymentStatus === 'PAID' ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                       }`}>
                         {item.paymentStatus === 'PAID' ? <CheckCircle className="w-3 h-3" /> : <AlertCircle className="w-3 h-3" />}
                         {item.paymentStatus}
                       </span>
                    </td>
                    <td className="px-6 py-4 text-right text-xs font-mono text-slate-400">
                       {item.confirmationNumber || '-'}
                    </td>
                 </tr>
               ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
