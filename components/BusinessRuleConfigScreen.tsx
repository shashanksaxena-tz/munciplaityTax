
import React from 'react';
import { BusinessTaxRulesConfig } from '../types';
import { Settings, Save, RotateCcw, Percent, AlertTriangle } from 'lucide-react';
import { DEFAULT_BUSINESS_RULES } from '../constants';

interface Props {
  rules: BusinessTaxRulesConfig;
  onUpdate: (rules: BusinessTaxRulesConfig) => void;
  onClose: () => void;
}

export const BusinessRuleConfigScreen: React.FC<Props> = ({ rules, onUpdate, onClose }) => {
  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex justify-end animate-fadeIn">
      <div className="w-full max-w-lg bg-white h-full shadow-2xl flex flex-col p-6">
        <div className="flex justify-between items-center mb-8">
           <h2 className="text-xl font-bold flex items-center gap-2"><Settings className="w-5 h-5 text-indigo-600"/> Business Rules</h2>
           <button onClick={onClose} className="text-slate-500 hover:text-slate-800">Close</button>
        </div>

        <div className="space-y-8 flex-1 overflow-y-auto pr-2">
           
           {/* General Section */}
           <section className="space-y-4">
             <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider border-b pb-2">General Tax Settings</h3>
             <div className="space-y-3">
                <InputGroup label="Net Profits Tax Rate" value={rules.municipalRate} onChange={v => onUpdate({...rules, municipalRate: v})} step={0.001} tooltip="Base rate (e.g. 0.020)" />
                <InputGroup label="Minimum Tax ($)" value={rules.minimumTax} onChange={v => onUpdate({...rules, minimumTax: v})} tooltip="Min tax even if loss" />
             </div>
           </section>

           {/* Allocation Section */}
           <section className="space-y-4">
             <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider border-b pb-2">Allocation (Schedule Y)</h3>
             <div className="space-y-3">
                <div>
                  <label className="text-sm font-bold text-slate-700">Allocation Method</label>
                  <select value={rules.allocationMethod} onChange={e=>onUpdate({...rules, allocationMethod: e.target.value as any})} className="w-full border border-slate-300 p-2 rounded-lg mt-1 text-sm">
                     <option value="3_FACTOR">3-Factor Formula (Prop/Pay/Sales)</option>
                     <option value="GROSS_RECEIPTS_ONLY">Gross Receipts Only</option>
                  </select>
                </div>
                <InputGroup label="Sales Factor Weight" value={rules.allocationSalesFactorWeight} onChange={v => onUpdate({...rules, allocationSalesFactorWeight: v})} step={1} tooltip="1 = Standard, 2 = Double Weighted" />
             </div>
           </section>

           {/* Schedule X Section */}
           <section className="space-y-4">
             <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider border-b pb-2">Schedule X (Adjustments)</h3>
             <div className="space-y-3">
               <InputGroup label="Intangible Expense Rate" value={rules.intangibleExpenseRate} onChange={v => onUpdate({...rules, intangibleExpenseRate: v})} step={0.01} tooltip="Rate (5%) applied to non-taxable income to find add-back expense" />
             </div>
           </section>

           {/* NOL Section */}
           <section className="space-y-4">
             <h3 className="text-xs font-bold uppercase text-slate-400 tracking-wider border-b pb-2">Loss Carryforwards (NOL)</h3>
             <div className="flex items-center gap-2 mb-2">
               <input type="checkbox" checked={rules.enableNOL} onChange={e => onUpdate({...rules, enableNOL: e.target.checked})} className="w-4 h-4 text-indigo-600 rounded" />
               <span className="text-sm font-medium">Enable NOL Deduction</span>
             </div>
             {rules.enableNOL && (
               <InputGroup label="NOL Deduction Cap %" value={rules.nolOffsetCapPercent} onChange={v => onUpdate({...rules, nolOffsetCapPercent: v})} step={0.01} tooltip="Max % of income that can be offset (e.g. 0.50)" />
             )}
           </section>

           {/* Penalties Section */}
           <section className="space-y-4 bg-red-50 p-4 rounded-xl border border-red-100">
             <h3 className="text-xs font-bold uppercase text-red-800 tracking-wider flex items-center gap-2"><AlertTriangle className="w-3 h-3"/> Compliance & Penalties</h3>
             <div className="space-y-3">
                <InputGroup label="Safe Harbor %" value={rules.safeHarborPercent} onChange={v => onUpdate({...rules, safeHarborPercent: v})} step={0.01} tooltip="Required payment % to avoid penalty" />
                <InputGroup label="Underpayment Penalty Rate" value={rules.penaltyRateUnderpayment} onChange={v => onUpdate({...rules, penaltyRateUnderpayment: v})} step={0.01} tooltip="Penalty on unpaid tax" />
                <InputGroup label="Late Filing Flat Fee ($)" value={rules.penaltyRateLateFiling} onChange={v => onUpdate({...rules, penaltyRateLateFiling: v})} step={1} />
                <InputGroup label="Annual Interest Rate" value={rules.interestRateAnnual} onChange={v => onUpdate({...rules, interestRateAnnual: v})} step={0.001} />
             </div>
           </section>
        </div>
        
        <div className="mt-4 pt-4 border-t flex gap-2 bg-white sticky bottom-0">
           <button onClick={()=>onUpdate(DEFAULT_BUSINESS_RULES)} className="p-2 border rounded-lg hover:bg-slate-50 text-slate-500" title="Reset Defaults"><RotateCcw className="w-5 h-5"/></button>
           <button onClick={onClose} className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-2 rounded-lg transition-colors">Save Configuration</button>
        </div>
      </div>
    </div>
  );
};

const InputGroup = ({ label, value, onChange, step = 1, tooltip }: any) => (
  <div>
    <label className="text-sm font-bold text-slate-700 block mb-1 flex justify-between">
      {label}
      {tooltip && <span className="text-xs font-normal text-slate-400 italic">{tooltip}</span>}
    </label>
    <input 
      type="number" 
      step={step}
      value={value} 
      onChange={e=>onChange(parseFloat(e.target.value))} 
      className="w-full border border-slate-300 p-2 rounded-lg outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 text-sm" 
    />
  </div>
);
