
import React from 'react';
import { AlertTriangle, CheckCircle, AlertOctagon } from 'lucide-react';
import { DiscrepancyReport } from '../types';

interface DiscrepancyViewProps {
  report: DiscrepancyReport;
}

export const DiscrepancyView: React.FC<DiscrepancyViewProps> = ({ report }) => {
  if (!report.hasDiscrepancies) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-4 flex items-start gap-3">
        <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
        <div>
          <h4 className="font-bold text-green-800">No Discrepancies Found</h4>
          <p className="text-sm text-green-700">Your uploaded Local 1040 matches the calculated values from your source documents.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-red-50 border border-red-200 rounded-xl p-5 space-y-4">
      <div className="flex items-center gap-3 border-b border-red-200 pb-3">
        <div className="p-2 bg-red-100 rounded-full">
          <AlertOctagon className="w-6 h-6 text-red-600" />
        </div>
        <div>
          <h4 className="font-bold text-red-900 text-lg">Discrepancy Detected</h4>
          <p className="text-sm text-red-700">The data calculated from your W-2s/1099s does not match your uploaded Local Return.</p>
        </div>
      </div>

      <div className="space-y-3">
        {report.issues.map((issue, idx) => (
          <div key={idx} className="bg-white p-4 rounded-lg border border-red-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
               <span className="text-xs font-bold uppercase tracking-wider text-slate-500">{issue.field}</span>
               <div className="text-red-700 font-medium mt-1">{issue.message}</div>
            </div>
            <div className="flex items-center gap-6 text-sm">
               <div className="text-center">
                 <div className="text-slate-500 text-xs mb-1">Calculated</div>
                 <div className="font-mono font-bold text-slate-800">${issue.sourceValue.toLocaleString()}</div>
               </div>
               <div className="text-slate-300">vs</div>
               <div className="text-center">
                 <div className="text-slate-500 text-xs mb-1">Reported (Form)</div>
                 <div className="font-mono font-bold text-slate-800">${issue.formValue.toLocaleString()}</div>
               </div>
               <div className="bg-red-100 text-red-700 font-bold px-3 py-1 rounded text-xs">
                  Diff: ${Math.abs(issue.difference).toLocaleString()}
               </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
