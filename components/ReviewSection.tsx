
import React, { useState, useMemo, useEffect } from 'react';
import { TaxFormData, TaxFormType, W2Form, Form1099, W2GForm, ScheduleC, ScheduleE, ScheduleF, LocalTaxForm, TaxPayerProfile, TaxReturnSettings, TaxRulesConfig, TaxCalculationResult, Address, FilingStatus, FederalTaxForm, PartnershipEntity } from '../types';
import { Trash2, DollarSign, Plus, ChevronDown, User, Calendar, Building, Ticket, FileText, Home, Wheat, MapPin, Hash, Briefcase, AlertTriangle, Calculator, Activity, PieChart, BarChart, CheckCircle, XCircle, ShieldAlert, FileSearch, Sparkles, AlertOctagon, Users, FileCheck } from 'lucide-react';
import { DiscrepancyView } from './DiscrepancyView';
import { api } from '../services/api';
import { verifyLocalAddress } from '../utils/addressUtils';
import { Tooltip } from './Tooltip';

interface ReviewSectionProps {
  forms: TaxFormData[];
  profile: TaxPayerProfile;
  settings: TaxReturnSettings;
  onUpdateProfile: (p: TaxPayerProfile) => void;
  onUpdateSettings: (s: TaxReturnSettings) => void;
  onUpdateForm: (form: TaxFormData) => void;
  onDeleteForm: (id: string) => void;
  onAddManualForm: (type: TaxFormType) => void;
  onCalculate: () => void;
  onBack: () => void;
  rules: TaxRulesConfig;
  isSubmitted?: boolean;
}

export const ReviewSection: React.FC<ReviewSectionProps> = ({
  forms, profile, settings, onUpdateProfile, onUpdateSettings, onUpdateForm, onDeleteForm, onAddManualForm, onCalculate, onBack, rules, isSubmitted
}) => {
  const [expandedProfile, setExpandedProfile] = useState(!profile.name);
  const [showAddMenu, setShowAddMenu] = useState(false);
  const [liveResult, setLiveResult] = useState<TaxCalculationResult | null>(null);

  useEffect(() => {
    const timer = setTimeout(async () => {
      if (forms.length > 0) {
        try {
          const res = await api.taxEngine.calculateIndividual(forms, profile, settings, rules);
          setLiveResult(res);
        } catch (e) {
          setLiveResult(null);
        }
      } else {
        setLiveResult(null);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [forms, profile, settings, rules]);

  const netBalance = liveResult ? (liveResult.municipalBalance) : 0;
  const isMFJ = profile.filingStatus === FilingStatus.MARRIED_FILING_JOINTLY;

  const handleVerifyProfileAddress = () => {
    if (profile.address) {
      const verified = verifyLocalAddress(profile.address);
      onUpdateProfile({ ...profile, address: verified });
    }
  };

  const INDIVIDUAL_FORMS = [
    TaxFormType.W2,
    TaxFormType.W2G,
    TaxFormType.FORM_1099_NEC,
    TaxFormType.FORM_1099_MISC,
    TaxFormType.SCHEDULE_C,
    TaxFormType.SCHEDULE_E,
    TaxFormType.SCHEDULE_F,
    TaxFormType.LOCAL_1040,
    TaxFormType.LOCAL_1040_EZ,
    TaxFormType.FORM_R,
    TaxFormType.FEDERAL_1040
  ];

  if (isSubmitted) {
    return (
      <div className="max-w-2xl mx-auto py-12 text-center space-y-6">
        <div className="w-16 h-16 bg-[#d5faeb] rounded-full flex items-center justify-center mx-auto">
          <CheckCircle className="w-8 h-8 text-[#10b981]" />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-[#0f1012]">Return Submitted</h2>
          <p className="text-[#5d6567] mt-2">This return has been finalized and locked for editing.</p>
        </div>
        <div className="flex justify-center gap-4">
          <button onClick={onCalculate} className="px-6 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg font-medium">View Final Report</button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-fadeIn pb-24">

      {liveResult && forms.length > 0 && <LiveTaxVisualizer result={liveResult} />}

      {/* Settings & Profile */}
      <div className="bg-white rounded-xl border border-[#dcdede] shadow-sm overflow-hidden">
        <div
          className="bg-[#fbfbfb] px-6 py-4 border-b border-[#dcdede] flex justify-between items-center cursor-pointer hover:bg-[#f0f0f0] transition-colors"
          onClick={() => setExpandedProfile(!expandedProfile)}
        >
          <div className="flex items-center gap-3">
            <div className="bg-[#970bed]/10 p-2 rounded-lg text-[#970bed]"><User className="w-5 h-5" /></div>
            <div>
              <h3 className="font-bold text-[#0f1012]">Return Settings & Taxpayer Profile</h3>
              <p className="text-xs text-[#5d6567]">Tax Year: {settings.taxYear} • {settings.isAmendment ? 'Amendment' : 'Standard'}</p>
            </div>
          </div>
          <div className="text-[#babebf]">{expandedProfile ? 'Hide' : 'Edit'}</div>
        </div>

        {expandedProfile && (
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-[#babebf] uppercase tracking-wider mb-2">Settings</h4>
              <div className="grid grid-cols-2 gap-4">
                <InputGroup label="Tax Year" value={settings.taxYear} onChange={(v: any) => onUpdateSettings({ ...settings, taxYear: v })} isText tooltip="The calendar year for which you are filing taxes." taxRelevant />
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-[#102124] uppercase">Filing Status</label>
                  <select
                    value={profile.filingStatus || FilingStatus.SINGLE}
                    onChange={(e) => onUpdateProfile({ ...profile, filingStatus: e.target.value as FilingStatus })}
                    className="w-full py-1.5 px-2 bg-white border border-[#dcdede] rounded text-sm font-medium outline-none focus:ring-2 focus:ring-[#970bed]/20"
                  >
                    <option value={FilingStatus.SINGLE}>Single</option>
                    <option value={FilingStatus.MARRIED_FILING_JOINTLY}>Married Filing Jointly</option>
                    <option value={FilingStatus.MARRIED_FILING_SEPARATELY}>Married Filing Separately</option>
                    <option value={FilingStatus.HEAD_OF_HOUSEHOLD}>Head of Household</option>
                  </select>
                </div>
                <div className="flex items-center mt-2 col-span-2">
                  <input type="checkbox" checked={settings.isAmendment} onChange={(e) => onUpdateSettings({ ...settings, isAmendment: e.target.checked })} className="mr-2" />
                  <span className="text-sm">This is an Amended Return</span>
                </div>
                {settings.isAmendment && (
                  <div className="col-span-2">
                    <InputGroup label="Reason for Amendment" value={settings.amendmentReason || ''} onChange={(v: any) => onUpdateSettings({ ...settings, amendmentReason: v })} isText />
                  </div>
                )}
              </div>
            </div>
            <div className="space-y-4">
              <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Taxpayer Profile</h4>
              <InputGroup label="Primary Name" value={profile.name} onChange={(v: any) => onUpdateProfile({ ...profile, name: v })} isText tooltip="Primary taxpayer's legal name." />
              <InputGroup label="Primary SSN" value={profile.ssn || ''} onChange={(v: any) => onUpdateProfile({ ...profile, ssn: v })} isText tooltip="Last 4 digits of SSN." />

              {isMFJ && (
                <div className="p-3 bg-[#ebf4ff] rounded-lg border border-[#469fe8]/20 space-y-3 mt-2">
                  <div className="flex items-center gap-2 text-[#469fe8] font-bold text-xs uppercase"><Users className="w-3.5 h-3.5" /> Spouse Details</div>
                  <InputGroup label="Spouse Name" value={profile.spouse?.name || ''} onChange={(v: any) => onUpdateProfile({ ...profile, spouse: { ...profile.spouse, name: v } as any })} isText />
                  <InputGroup label="Spouse SSN" value={profile.spouse?.ssn || ''} onChange={(v: any) => onUpdateProfile({ ...profile, spouse: { ...profile.spouse, ssn: v } as any })} isText />
                </div>
              )}

              {/* Address Block */}
              <div className="bg-[#fbfbfb] p-3 rounded-lg border border-[#dcdede] mt-4">
                <div className="flex justify-between items-center mb-2">
                  <label className="text-[10px] font-bold text-[#102124] uppercase">Address</label>
                  <AddressStatusBadge status={profile.address?.verificationStatus} onVerify={handleVerifyProfileAddress} />
                </div>
                <InputGroup
                  label="Street"
                  value={profile.address?.street || ''}
                  onChange={(v: any) => onUpdateProfile({ ...profile, address: { ...profile.address!, street: v, verificationStatus: 'UNVERIFIED' } })}
                  isText noLabel
                />
                <div className="grid grid-cols-3 gap-2 mt-2">
                  <InputGroup
                    label="City"
                    value={profile.address?.city || ''}
                    onChange={(v: any) => onUpdateProfile({ ...profile, address: { ...profile.address!, city: v, verificationStatus: 'UNVERIFIED' } })}
                    isText placeholder="City" noLabel
                  />
                  <InputGroup
                    label="State"
                    value={profile.address?.state || ''}
                    onChange={(v: any) => onUpdateProfile({ ...profile, address: { ...profile.address!, state: v, verificationStatus: 'UNVERIFIED' } })}
                    isText placeholder="State" noLabel
                  />
                  <InputGroup
                    label="Zip"
                    value={profile.address?.zip || ''}
                    onChange={(v: any) => onUpdateProfile({ ...profile, address: { ...profile.address!, zip: v, verificationStatus: 'UNVERIFIED' } })}
                    isText placeholder="Zip" noLabel
                  />
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {liveResult?.discrepancyReport && <DiscrepancyView report={liveResult.discrepancyReport} />}

      <div className="space-y-4">
        <h2 className="text-xl font-bold text-[#0f1012]">Tax Documents</h2>
        {forms.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-xl border-2 border-dashed border-[#dcdede]"><p className="text-[#5d6567]">No forms added.</p></div>
        ) : (
          <div className="grid gap-6">
            {forms.map((form) => {
              const props = { key: form.id, form: form as any, onUpdate: onUpdateForm, onDelete: onDeleteForm };
              if (form.formType === TaxFormType.W2) return <W2Card {...props} />;
              if (form.formType === TaxFormType.W2G) return <W2GCard {...props} />;
              if (form.formType === TaxFormType.SCHEDULE_C) return <ScheduleCCard {...props} />;
              if (form.formType === TaxFormType.SCHEDULE_E) return <ScheduleECard {...props} />;
              if (form.formType === TaxFormType.SCHEDULE_F) return <ScheduleFCard {...props} />;
              if (form.formType === TaxFormType.FEDERAL_1040) return <Federal1040Card {...props} />;
              if ([TaxFormType.LOCAL_1040, TaxFormType.LOCAL_1040_EZ, TaxFormType.FORM_R].includes(form.formType))
                return <LocalFormCard {...props} />;
              return <Form1099Card {...props} />;
            })}
          </div>
        )}
      </div>

      <div className="flex flex-col sm:flex-row justify-between gap-4 pt-6">
        <div className="flex gap-2 relative">
          <button onClick={onBack} className="px-4 py-2 border border-[#dcdede] rounded-lg hover:bg-[#fbfbfb] font-medium text-[#5d6567]">Upload More</button>
          <div className="relative">
            <button onClick={() => setShowAddMenu(!showAddMenu)} className="px-4 py-2 bg-[#0f1012] text-white rounded-lg font-medium flex items-center gap-2 hover:bg-[#102124]">
              <Plus className="w-4 h-4" /> Add Manual Form <ChevronDown className="w-4 h-4" />
            </button>
            {showAddMenu && (
              <div className="absolute bottom-full left-0 mb-2 w-56 bg-white rounded-xl shadow-xl border border-[#dcdede] overflow-hidden z-20">
                {INDIVIDUAL_FORMS.map(t => <MenuOption key={t} label={t} onClick={() => { onAddManualForm(t); setShowAddMenu(false); }} />)}
              </div>
            )}
          </div>
        </div>
        <button onClick={onCalculate} disabled={forms.length === 0} className="px-8 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg font-semibold shadow-md disabled:opacity-50">View Final Report</button>
      </div>

      {liveResult && (
        <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-md border-t border-[#dcdede] shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)] z-40 animate-slideUp">
          <div className="max-w-6xl mx-auto px-4 py-4 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-[#970bed]/10 rounded-full text-[#970bed] animate-pulse"><Activity className="w-5 h-5" /></div>
              <div>
                <h4 className="text-xs font-bold text-[#babebf] uppercase tracking-wider">Live Estimate</h4>
                <div className="text-sm font-medium text-[#5d6567]">Updating in real-time</div>
              </div>
            </div>
            <div className="flex gap-8 text-sm">
              <div>
                <span className="block text-[#babebf] text-xs uppercase mb-1">Taxable Income</span>
                <span className="font-bold text-[#0f1012] text-lg">${liveResult.totalTaxableIncome.toLocaleString()}</span>
              </div>
              <div>
                <span className="block text-[#babebf] text-xs uppercase mb-1">Est. Liability</span>
                <span className="font-bold text-[#970bed] text-lg">${liveResult.municipalLiabilityAfterCredits.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="pl-6 border-l border-[#dcdede]">
                <span className="block text-[#babebf] text-xs uppercase mb-1">Balance Due</span>
                <span className={`font-bold text-xl ${netBalance > 0 ? 'text-[#10b981]' : 'text-[#ec1656]'}`}>
                  {netBalance > 0 ? `+${Math.abs(netBalance).toLocaleString()}` : `${Math.abs(netBalance).toLocaleString()}`}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const AddressStatusBadge = ({ status, onVerify }: { status?: string, onVerify: () => void }) => {
  if (!status || status === 'UNVERIFIED') {
    return (
      <button onClick={onVerify} className="flex items-center gap-1 px-2 py-0.5 bg-[#f0f0f0] hover:bg-[#dcdede] text-[#5d6567] text-[10px] font-bold uppercase rounded border border-[#dcdede] transition-colors">
        Verify Address
      </button>
    );
  }
  if (status === 'VERIFIED_IN_DISTRICT') {
    return <span className="flex items-center gap-1 px-2 py-0.5 bg-[#d5faeb] text-[#10b981] text-[10px] font-bold uppercase rounded border border-[#10b981]/20"><CheckCircle className="w-3 h-3" /> Inside Dublin</span>;
  }
  if (status === 'JEDD') {
    return <span className="flex items-center gap-1 px-2 py-0.5 bg-[#ebf4ff] text-[#469fe8] text-[10px] font-bold uppercase rounded border border-[#469fe8]/20"><Briefcase className="w-3 h-3" /> JEDD District</span>;
  }
  return <span className="flex items-center gap-1 px-2 py-0.5 bg-[#ec1656]/10 text-[#ec1656] text-[10px] font-bold uppercase rounded border border-[#ec1656]/20"><XCircle className="w-3 h-3" /> Out of District</span>;
};

const LiveTaxVisualizer: React.FC<{ result: TaxCalculationResult }> = ({ result }) => {
  const totalIncome = Math.max(1, result.totalTaxableIncome);
  const w2Pct = (result.w2TaxableIncome / totalIncome) * 100;
  const schXPct = (result.scheduleX.totalNetProfit / totalIncome) * 100;

  const totalLiability = result.municipalLiability;
  const liabilityMax = Math.max(1, totalLiability);
  const creditsPct = Math.min(100, (result.scheduleY.totalCredit / liabilityMax) * 100);
  const withheldPct = Math.min(100, (result.totalLocalWithheld / liabilityMax) * 100);

  return (
    <div className="bg-white rounded-xl border border-[#dcdede] shadow-sm p-6 mb-4">
      <div className="flex items-center gap-2 mb-4">
        <PieChart className="w-5 h-5 text-[#970bed]" />
        <h3 className="font-bold text-[#0f1012] text-sm uppercase tracking-wider">Tax Breakdown Preview</h3>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="space-y-2">
          <div className="flex justify-between text-xs font-semibold text-[#5d6567]">
            <span>Taxable Income Sources</span>
            <span>${result.totalTaxableIncome.toLocaleString()}</span>
          </div>
          <div className="h-4 w-full bg-[#f0f0f0] rounded-full overflow-hidden flex">
            {w2Pct > 0 && <div style={{ width: `${w2Pct}%` }} className="bg-[#469fe8] relative"></div>}
            {schXPct > 0 && <div style={{ width: `${schXPct}%` }} className="bg-[#970bed] relative"></div>}
          </div>
          <div className="flex gap-4 text-[10px] text-[#5d6567]">
            {w2Pct > 0 && <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-[#469fe8]"></div> W-2</div>}
            {schXPct > 0 && <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-[#970bed]"></div> Schedule X</div>}
          </div>
        </div>

        <div className="space-y-2">
          <div className="flex justify-between text-xs font-semibold text-[#5d6567]">
            <span>Estimated Liability Coverage</span>
            <span>${totalLiability.toLocaleString()}</span>
          </div>
          <div className="h-4 w-full bg-[#f0f0f0] rounded-full overflow-hidden flex relative">
            {totalLiability === 0 && <div className="w-full bg-[#dcdede]"></div>}
            {creditsPct > 0 && <div style={{ width: `${creditsPct}%` }} className="bg-[#10b981]"></div>}
            {withheldPct > 0 && <div style={{ width: `${withheldPct}%` }} className="bg-[#10b981]/70"></div>}
            {result.municipalLiabilityAfterCredits > (result.scheduleY.totalCredit + result.totalLocalWithheld) &&
              <div className="flex-1 bg-[#ec1656]"></div>
            }
          </div>
        </div>
      </div>
    </div>
  );
};

const MenuOption = ({ label, onClick }: any) => (
  <button onClick={onClick} className="w-full text-left px-3 py-2 text-sm text-[#5d6567] hover:bg-[#ebf4ff] hover:text-[#970bed]">{label}</button>
);

const InputGroup = ({ label, value, onChange, isText, placeholder, tooltip, taxRelevant, noLabel, confidence }: any) => {
  const isHighConfidence = confidence !== undefined && confidence >= 0.90;
  const isMedConfidence = confidence !== undefined && confidence >= 0.75 && confidence < 0.90;
  const isLowConfidence = confidence !== undefined && confidence < 0.75;
  const confPercent = confidence !== undefined ? Math.round(confidence * 100) : null;

  return (
    <div className="w-full relative group">
      {!noLabel && (
        <div className="flex items-center gap-1 mb-1">
          <label className={`text-[10px] font-bold uppercase tracking-wider truncate flex items-center gap-1 ${taxRelevant ? 'text-[#10b981]' : 'text-[#102124]'}`}>
            {label}
            {taxRelevant && <Calculator className="w-3 h-3 text-[#10b981]" />}
          </label>
          {tooltip && <Tooltip content={taxRelevant ? `${tooltip} (Calculated Field)` : tooltip} />}
          {confPercent !== null && (
            <Tooltip content={`Extraction Confidence: ${confPercent}%`}>
              {isLowConfidence ? <AlertOctagon className="w-3 h-3 text-[#ec1656] animate-pulse" /> :
                isMedConfidence ? <AlertTriangle className="w-3 h-3 text-[#f59e0b]" /> :
                  <CheckCircle className="w-3 h-3 text-[#10b981]/50" />
              }
            </Tooltip>
          )}
        </div>
      )}
      <div className="relative">
        {!isText && <DollarSign className={`absolute left-2 top-2 w-3.5 h-3.5 ${taxRelevant ? 'text-[#10b981]' : 'text-[#babebf]'}`} />}
        <input
          type={isText ? 'text' : 'number'}
          value={value} placeholder={placeholder}
          onChange={(e) => isText ? onChange(e.target.value) : onChange(parseFloat(e.target.value) || 0)}
          className={`w-full ${!isText ? 'pl-7' : 'pl-3'} pr-2 py-1.5 bg-white border rounded text-sm font-medium outline-none transition-all
            ${isLowConfidence ? 'border-[#ec1656]/50 ring-1 ring-[#ec1656]/20 bg-[#ec1656]/5' : ''}
            ${isMedConfidence ? 'border-[#f59e0b]/50 ring-1 ring-[#f59e0b]/20 bg-[#f59e0b]/5' : ''}
            ${isHighConfidence ? 'border-[#dcdede] hover:border-[#10b981]/50 focus:border-[#10b981]' : ''}
            ${taxRelevant && !isLowConfidence && !isMedConfidence
              ? 'border-[#10b981]/30 focus:border-[#10b981] focus:ring-2 focus:ring-[#10b981]/20 text-[#0f1012] bg-[#d5faeb]/10'
              : 'focus:ring-2 focus:ring-[#970bed]/20'}
          `}
        />
      </div>
    </div>
  );
};

const ExtractionMetaBar = ({ form }: { form: TaxFormData }) => {
  // Only show extraction metadata if the form was AI-extracted
  if (!form.isAiExtracted) {
    return null;
  }

  return (
    <div className="bg-[#ebf4ff] px-6 py-2 border-b border-[#469fe8]/20 flex items-center gap-6 text-xs text-[#469fe8]">
      <div className="flex items-center gap-1.5" title="Click to view PDF source">
        <FileSearch className="w-3.5 h-3.5 text-[#469fe8]" />
        <span className="font-semibold">Source: Page {form.sourcePage || 1}</span>
      </div>
      <div className="flex items-center gap-1.5">
        <Sparkles className="w-3.5 h-3.5 text-[#970bed]" />
        <span className="font-semibold">Reason:</span> {form.extractionReason || "AI Identified Form"}
      </div>
      {form.confidenceScore !== undefined && (
        <div className="flex items-center gap-1.5 ml-auto">
          <span className="font-semibold text-[#5d6567]">Form Confidence:</span>
          <div className="flex items-center gap-1">
            <div className={`w-16 h-1.5 rounded-full bg-[#f0f0f0] overflow-hidden`}>
              <div style={{ width: `${form.confidenceScore * 100}%` }} className={`h-full ${form.confidenceScore > 0.85 ? 'bg-[#10b981]' : 'bg-[#f59e0b]'}`}></div>
            </div>
            <span className="font-mono">{Math.round(form.confidenceScore * 100)}%</span>
          </div>
        </div>
      )}
      {form.sourceDocumentUrl && (
        <a 
          href={form.sourceDocumentUrl} 
          target="_blank" 
          rel="noopener noreferrer"
          className="flex items-center gap-1.5 px-2 py-0.5 bg-[#469fe8]/10 hover:bg-[#469fe8]/20 text-[#469fe8] rounded transition-colors"
        >
          <FileSearch className="w-3 h-3" />
          <span>View PDF</span>
        </a>
      )}
    </div>
  );
};

const OwnerToggle = ({ value, onChange }: { value?: 'PRIMARY' | 'SPOUSE', onChange: (v: 'PRIMARY' | 'SPOUSE') => void }) => (
  <div className="flex items-center gap-2 bg-[#f0f0f0] p-1 rounded-lg">
    <button onClick={() => onChange('PRIMARY')} className={`px-2 py-0.5 text-xs font-bold rounded ${value !== 'SPOUSE' ? 'bg-white shadow text-[#970bed]' : 'text-[#5d6567]'}`}>Primary</button>
    <button onClick={() => onChange('SPOUSE')} className={`px-2 py-0.5 text-xs font-bold rounded ${value === 'SPOUSE' ? 'bg-white shadow text-[#970bed]' : 'text-[#5d6567]'}`}>Spouse</button>
  </div>
);

const CardContainer = ({ title, icon, onDelete, children, meta, headerAction }: any) => (
  <div className="bg-white rounded-xl border border-[#dcdede] shadow-sm transition-all hover:shadow-md overflow-hidden">
    <div className="bg-[#fbfbfb] px-6 py-3 border-b border-[#dcdede] flex items-center justify-between">
      <div className="flex items-center gap-3 font-bold text-[#0f1012]">{icon} {title}</div>
      <div className="flex items-center gap-3">
        {headerAction}
        <button onClick={onDelete} className="text-[#babebf] hover:text-[#ec1656] transition-colors"><Trash2 className="w-4 h-4" /></button>
      </div>
    </div>
    {meta && meta}
    <div className="p-6">
      {children}
    </div>
  </div>
);

// --- FORM CARDS ---

const W2Card: React.FC<{ form: W2Form, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: keyof W2Form, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};

  console.log('[W2Card] Rendering W2 form:', form);

  return (
    <CardContainer title={`W-2: ${form.employer || 'Unknown Employer'}`} icon={<Building className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => update('owner', v)} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3 p-3 bg-[#fbfbfb] rounded-lg">
          <h5 className="font-bold text-xs text-[#babebf] uppercase">Employer Info</h5>
          <InputGroup label="Name" value={form.employer} onChange={(v: any) => update('employer', v)} isText confidence={conf.employer} />
          <InputGroup label="EIN" value={form.employerEin} onChange={(v: any) => update('employerEin', v)} isText confidence={conf.employerEin} />
        </div>
        <div className="space-y-3 p-3 bg-[#ebf4ff] rounded-lg">
          <h5 className="font-bold text-xs text-[#469fe8] uppercase">Wages & Tax</h5>
          <InputGroup label="Box 1: Fed Wages" value={form.federalWages} onChange={(v: any) => update('federalWages', v)} taxRelevant confidence={conf.federalWages} />
          <InputGroup label="Box 5: Medicare Wages" value={form.medicareWages} onChange={(v: any) => update('medicareWages', v)} taxRelevant confidence={conf.medicareWages} />
          <InputGroup label="Box 18: Local Wages" value={form.localWages} onChange={(v: any) => update('localWages', v)} taxRelevant confidence={conf.localWages} />
          <div className="grid grid-cols-2 gap-2">
            <InputGroup label="Box 19 Tax" value={form.localWithheld} onChange={(v: any) => update('localWithheld', v)} taxRelevant confidence={conf.localWithheld} />
            <InputGroup label="Box 20 Locality" value={form.locality} onChange={(v: any) => update('locality', v)} isText taxRelevant confidence={conf.locality} />
          </div>
        </div>
      </div>
    </CardContainer>
  );
};

const W2GCard: React.FC<{ form: W2GForm, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: keyof W2GForm, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`W-2G: ${form.payer}`} icon={<Ticket className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => update('owner', v)} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3">
          <InputGroup label="Payer Name" value={form.payer} onChange={(v: any) => update('payer', v)} isText confidence={conf.payer} />
          <InputGroup label="Winner TIN" value={form.recipientTin} onChange={(v: any) => update('recipientTin', v)} isText />
        </div>
        <div className="space-y-3">
          <InputGroup label="Gross Winnings" value={form.grossWinnings} onChange={(v: any) => update('grossWinnings', v)} taxRelevant confidence={conf.grossWinnings} />
          <InputGroup label="Local Tax" value={form.localWithheld} onChange={(v: any) => update('localWithheld', v)} taxRelevant confidence={conf.localWithheld} />
          <InputGroup label="Locality" value={form.locality} onChange={(v: any) => update('locality', v)} isText taxRelevant confidence={conf.locality} />
        </div>
      </div>
    </CardContainer>
  );
};

const ScheduleCCard: React.FC<{ form: ScheduleC, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: keyof ScheduleC, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`Schedule C: ${form.businessName}`} icon={<Briefcase className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => update('owner', v)} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3">
          <InputGroup label="Business Name" value={form.businessName} onChange={(v: any) => update('businessName', v)} isText confidence={conf.businessName} />
          <InputGroup label="EIN" value={form.businessEin} onChange={(v: any) => update('businessEin', v)} isText confidence={conf.businessEin} />
        </div>
        <div className="space-y-3">
          <InputGroup label="Gross Receipts" value={form.grossReceipts} onChange={(v: any) => update('grossReceipts', v)} taxRelevant confidence={conf.grossReceipts} />
          <InputGroup label="Net Profit (Line 31)" value={form.netProfit} onChange={(v: any) => update('netProfit', v)} taxRelevant confidence={conf.netProfit} />
        </div>
      </div>
    </CardContainer>
  );
};

const ScheduleFCard: React.FC<{ form: ScheduleF, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: keyof ScheduleF, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`Schedule F: ${form.businessName}`} icon={<Wheat className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => update('owner', v)} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3"><InputGroup label="Proprietor" value={form.businessName} onChange={(v: any) => update('businessName', v)} isText confidence={conf.businessName} /></div>
        <div className="space-y-3"><InputGroup label="Net Farm Profit" value={form.netFarmProfit} onChange={(v: any) => update('netFarmProfit', v)} taxRelevant confidence={conf.netFarmProfit} /></div>
      </div>
    </CardContainer>
  );
};

const ScheduleECard: React.FC<{ form: ScheduleE, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const rentals = form.rentals || [];
  const partnerships = form.partnerships || [];

  const addRental = () => {
    const newRental = { id: crypto.randomUUID(), streetAddress: '', city: '', state: '', zip: '', rentalType: 'Residential', line21_FairRentalDays_or_Income: 0, line22_DeductibleLoss: 0, calculatedNetIncome: 0 };
    onUpdate({ ...form, rentals: [...rentals, newRental] });
  };

  const updateRental = (idx: number, k: string, v: any) => {
    const newRentals = [...rentals];
    newRentals[idx] = { ...newRentals[idx], [k]: v };
    onUpdate({ ...form, rentals: newRentals });
  };

  const removeRental = (idx: number) => {
    onUpdate({ ...form, rentals: rentals.filter((_, i) => i !== idx) });
  };

  const addPartnership = () => {
    const newP = { id: crypto.randomUUID(), name: '', ein: '', netProfit: 0 };
    onUpdate({ ...form, partnerships: [...partnerships, newP] });
  };

  const updatePartnership = (idx: number, k: string, v: any) => {
    const newPs = [...partnerships];
    newPs[idx] = { ...newPs[idx], [k]: v };
    onUpdate({ ...form, partnerships: newPs });
  };

  const removePartnership = (idx: number) => {
    onUpdate({ ...form, partnerships: partnerships.filter((_, i) => i !== idx) });
  };

  return (
    <CardContainer title="Schedule E (Rentals & Partnerships)" icon={<Home className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => onUpdate({ ...form, owner: v })} />}>
      <div className="space-y-8">

        {/* Part I */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h5 className="font-bold text-xs text-[#babebf] uppercase flex items-center gap-2">
              Part I: Rental Properties
              <span className="bg-[#f0f0f0] text-[#5d6567] px-1.5 rounded-full">{rentals.length}</span>
            </h5>
            <button type="button" onClick={addRental} className="px-3 py-1.5 bg-[#ebf4ff] hover:bg-[#469fe8]/20 text-[#469fe8] rounded text-xs font-bold flex items-center gap-1 transition-colors">
              <Plus className="w-3 h-3" /> Add Property
            </button>
          </div>

          {rentals.length === 0 ? (
            <div className="text-center py-4 bg-[#fbfbfb] rounded-lg border border-dashed border-[#dcdede] text-[#babebf] text-xs italic">No Part I rental properties.</div>
          ) : (
            <div className="space-y-3">
              {rentals.map((r, i) => (
                <div key={r.id || i} className="bg-white p-4 rounded-lg border border-[#dcdede] shadow-sm relative group">
                  <div className="absolute top-2 right-2 flex gap-2">
                    <span className="text-xs font-mono bg-[#f0f0f0] px-2 py-0.5 rounded text-[#5d6567]">#{i + 1}</span>
                    <button onClick={() => removeRental(i)} className="text-[#babebf] hover:text-[#ec1656] transition-colors p-1"><Trash2 className="w-3.5 h-3.5" /></button>
                  </div>
                  <div className="mb-4 pr-12"><InputGroup label="Property Address" value={r.streetAddress} onChange={(v: any) => updateRental(i, 'streetAddress', v)} isText placeholder="e.g. 123 Rental Ave" /></div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <InputGroup label="Rents Received" value={r.line21_FairRentalDays_or_Income} onChange={(v: any) => updateRental(i, 'line21_FairRentalDays_or_Income', v)} taxRelevant />
                    <InputGroup label="Total Expenses" value={r.line22_DeductibleLoss} onChange={(v: any) => updateRental(i, 'line22_DeductibleLoss', v)} taxRelevant />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Part II */}
        <div>
          <div className="flex justify-between items-center mb-4 pt-4 border-t border-[#f0f0f0]">
            <h5 className="font-bold text-xs text-[#babebf] uppercase flex items-center gap-2">
              Part II: Partnerships & S-Corps
              <span className="bg-[#f0f0f0] text-[#5d6567] px-1.5 rounded-full">{partnerships.length}</span>
            </h5>
            <button type="button" onClick={addPartnership} className="px-3 py-1.5 bg-[#ebf4ff] hover:bg-[#469fe8]/20 text-[#469fe8] rounded text-xs font-bold flex items-center gap-1 transition-colors">
              <Plus className="w-3 h-3" /> Add Entity
            </button>
          </div>

          {partnerships.length === 0 ? (
            <div className="text-center py-4 bg-[#fbfbfb] rounded-lg border border-dashed border-[#dcdede] text-[#babebf] text-xs italic">No Part II entities found.</div>
          ) : (
            <div className="space-y-3">
              {partnerships.map((p, i) => (
                <div key={p.id || i} className="bg-white p-4 rounded-lg border border-[#dcdede] shadow-sm relative group flex gap-4 items-end">
                  <div className="flex-1">
                    <InputGroup label="Entity Name" value={p.name} onChange={(v: any) => updatePartnership(i, 'name', v)} isText placeholder="LLC Name" />
                  </div>
                  <div className="w-32">
                    <InputGroup label="EIN" value={p.ein} onChange={(v: any) => updatePartnership(i, 'ein', v)} isText placeholder="XX-XXXXXXX" />
                  </div>
                  <div className="w-32">
                    <InputGroup label="Net Income" value={p.netProfit} onChange={(v: any) => updatePartnership(i, 'netProfit', v)} taxRelevant />
                  </div>
                  <button onClick={() => removePartnership(i)} className="text-[#babebf] hover:text-[#ec1656] transition-colors p-2"><Trash2 className="w-4 h-4" /></button>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </CardContainer>
  );
};

const Form1099Card: React.FC<{ form: Form1099, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: keyof Form1099, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`${form.formType}`} icon={<FileText className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />} headerAction={<OwnerToggle value={form.owner} onChange={(v) => update('owner', v)} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <InputGroup label="Payer" value={form.payer} onChange={(v: any) => update('payer', v)} isText confidence={conf.payer} />
        <InputGroup label="Income Amount" value={form.incomeAmount} onChange={(v: any) => update('incomeAmount', v)} taxRelevant confidence={conf.incomeAmount} />
      </div>
    </CardContainer>
  );
}

const LocalFormCard: React.FC<{ form: LocalTaxForm, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: string, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`Verification: ${form.formType}`} icon={<FileCheck className="w-4 h-4 text-[#970bed]" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3 p-3 bg-[#fbfbfb] rounded-xl">
          <h5 className="text-xs font-bold text-[#babebf] uppercase border-b pb-1 mb-2">Income</h5>
          <InputGroup label="Qualifying Wages (Line 1)" value={form.qualifyingWages} onChange={(v: any) => update('qualifyingWages', v)} />
          <InputGroup label="Other Income (Line 2)" value={form.otherIncome} onChange={(v: any) => update('otherIncome', v)} />
          <div className="pt-2 border-t border-[#dcdede]">
            <InputGroup label="Total Taxable (Line 5)" value={form.reportedTaxableIncome} onChange={(v: any) => update('reportedTaxableIncome', v)} confidence={conf.reportedTaxableIncome} />
          </div>
        </div>
        <div className="space-y-3 p-3 bg-[#ebf4ff] rounded-xl">
          <h5 className="text-xs font-bold text-[#469fe8] uppercase border-b border-[#469fe8]/20 pb-1 mb-2">Tax & Credits</h5>
          <InputGroup label="Credits (Line 7)" value={form.credits} onChange={(v: any) => update('credits', v)} />
          <InputGroup label="Tax Due (Line 19/Balance)" value={form.taxDue} onChange={(v: any) => update('taxDue', v)} confidence={conf.taxDue} />
          <InputGroup label="Overpayment" value={form.overpayment} onChange={(v: any) => update('overpayment', v)} />
        </div>
      </div>
    </CardContainer>
  );
};

const Federal1040Card: React.FC<{ form: FederalTaxForm, onUpdate: any, onDelete: any }> = ({ form, onUpdate, onDelete }) => {
  const update = (k: string, v: any) => onUpdate({ ...form, [k]: v });
  const conf = form.fieldConfidence || {};
  return (
    <CardContainer title={`Federal 1040 Summary`} icon={<FileText className="w-4 h-4" />} onDelete={() => onDelete(form.id)} meta={<ExtractionMetaBar form={form} />}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-3">
          <InputGroup label="Wages (1z)" value={form.wages} onChange={(v: any) => update('wages', v)} />
          <InputGroup label="Qual. Dividends (3a)" value={form.qualifiedDividends} onChange={(v: any) => update('qualifiedDividends', v)} />
          <InputGroup label="Capital Gains (7)" value={form.capitalGains} onChange={(v: any) => update('capitalGains', v)} />
        </div>
        <div className="space-y-3">
          <InputGroup label="Pensions (5b)" value={form.pensions} onChange={(v: any) => update('pensions', v)} />
          <InputGroup label="Social Security (6b)" value={form.socialSecurity} onChange={(v: any) => update('socialSecurity', v)} />
          <InputGroup label="Total Income (9)" value={form.totalIncome} onChange={(v: any) => update('totalIncome', v)} taxRelevant />
        </div>
      </div>
    </CardContainer>
  );
};
