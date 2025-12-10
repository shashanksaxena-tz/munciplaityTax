
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
        <button onClick={onBack} className="p-2 hover:bg-[#f0f0f0] rounded-full"><ChevronLeft className="w-5 h-5 text-[#5d6567]"/></button>
        <div>
           <h2 className="text-xl font-bold text-[#0f1012]">Filing History</h2>
           <p className="text-sm text-[#5d6567]">{profile.businessName} (FEIN: {profile.fein})</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-[#dcdede] overflow-hidden min-h-[400px]">
        {history.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 text-[#babebf]">
             <Briefcase className="w-12 h-12 mb-3 opacity-20" />
             <p>No filings found for this business.</p>
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-[#f8f9fa] border-b border-[#dcdede] text-[#5d6567] font-bold uppercase tracking-wider">
               <tr>
                 <th className="px-6 py-4">Period</th>
                 <th className="px-6 py-4">Date Filed</th>
                 <th className="px-6 py-4 text-right">Gross Wages</th>
                 <th className="px-6 py-4 text-right">Tax Paid</th>
                 <th className="px-6 py-4">Status</th>
                 <th className="px-6 py-4 text-right">Ref #</th>
               </tr>
            </thead>
            <tbody className="divide-y divide-[#dcdede]">
               {history.map((item) => (
                 <tr key={item.id} className="hover:bg-[#f8f9fa] transition-colors">
                    <td className="px-6 py-4 font-medium text-[#970bed]">{item.period.period} {item.period.year}</td>
                    <td className="px-6 py-4 text-[#5d6567]">{new Date(item.dateFiled).toLocaleDateString()}</td>
                    <td className="px-6 py-4 text-right text-[#5d6567]">${item.grossWages.toLocaleString()}</td>
                    <td className="px-6 py-4 text-right font-mono font-bold text-[#0f1012]">${item.totalAmountDue.toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                    <td className="px-6 py-4">
                       <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase ${
                         item.paymentStatus === 'PAID' ? 'bg-[#d5faeb] text-[#10b981]' : 'bg-[#f59e0b]/10 text-[#f59e0b]'
                       }`}>
                         {item.paymentStatus === 'PAID' ? <CheckCircle className="w-3 h-3" /> : <AlertCircle className="w-3 h-3" />}
                         {item.paymentStatus}
                       </span>
                    </td>
                    <td className="px-6 py-4 text-right text-xs font-mono text-[#babebf]">
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
