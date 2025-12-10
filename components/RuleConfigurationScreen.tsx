
import React, { useState, useEffect, useCallback } from 'react';
import { TaxRulesConfig, TaxRule, W2QualifyingWagesRule } from '../types';
import { Settings, Save, RotateCcw, Plus, Trash2, Search, Sliders, ToggleLeft, ToggleRight, Hash, RefreshCw, Cloud, CloudOff, AlertCircle } from 'lucide-react';
import { DEFAULT_TAX_RULES } from '../constants';
import { ruleService, transformRulesToConfig } from '../services/ruleService';

interface RuleConfigurationScreenProps {
  rules: TaxRulesConfig;
  onUpdateRules: (rules: TaxRulesConfig) => void;
  onClose: () => void;
  tenantId?: string;
}

export const RuleConfigurationScreen: React.FC<RuleConfigurationScreenProps> = ({ rules, onUpdateRules, onClose, tenantId = 'dublin' }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [newCityName, setNewCityName] = useState('');
  const [newCityRate, setNewCityRate] = useState('');
  const [syncing, setSyncing] = useState(false);
  const [syncStatus, setSyncStatus] = useState<'none' | 'success' | 'error'>('none');
  const [syncMessage, setSyncMessage] = useState('');
  const [backendRules, setBackendRules] = useState<TaxRule[]>([]);

  const handleSyncFromBackend = useCallback(async (silent: boolean = false) => {
    if (!silent) {
      setSyncing(true);
      setSyncStatus('none');
    }
    
    try {
      const activeRules = await ruleService.getActiveRules({
        tenantId,
        taxYear: new Date().getFullYear()
      });
      
      setBackendRules(activeRules);
      
      if (activeRules.length > 0) {
        const { taxRules } = transformRulesToConfig(activeRules);
        onUpdateRules(taxRules);
        
        if (!silent) {
          setSyncStatus('success');
          setSyncMessage(`Synced ${activeRules.length} rules from backend`);
        }
      } else if (!silent) {
        setSyncStatus('error');
        setSyncMessage('No rules found in backend');
      }
    } catch (err) {
      console.warn('Failed to sync from backend, using local rules:', err);
      if (!silent) {
        setSyncStatus('error');
        setSyncMessage('Backend unavailable. Using local rules.');
      }
    } finally {
      if (!silent) {
        setSyncing(false);
        // Clear status after 3 seconds
        setTimeout(() => setSyncStatus('none'), 3000);
      }
    }
  }, [tenantId, onUpdateRules]);

  // Attempt to sync rules from backend on first load
  useEffect(() => {
    handleSyncFromBackend(true);
  }, [handleSyncFromBackend]);

  const handleRateChange = (key: keyof TaxRulesConfig, value: string) => {
    const num = parseFloat(value);
    if (!isNaN(num)) {
      onUpdateRules({ ...rules, [key]: num });
    }
  };

  const handleCityRateChange = (city: string, value: string) => {
    const num = parseFloat(value);
    if (!isNaN(num)) {
      onUpdateRules({
        ...rules,
        municipalRates: { ...rules.municipalRates, [city]: num }
      });
    }
  };

  const handleAddCity = () => {
    if (newCityName && newCityRate) {
      const rate = parseFloat(newCityRate);
      if (!isNaN(rate)) {
        onUpdateRules({
          ...rules,
          municipalRates: { ...rules.municipalRates, [newCityName.toLowerCase()]: rate }
        });
        setNewCityName('');
        setNewCityRate('');
      }
    }
  };

  const handleDeleteCity = (city: string) => {
    const newRates = { ...rules.municipalRates };
    delete newRates[city];
    onUpdateRules({ ...rules, municipalRates: newRates });
  };

  const handleReset = () => {
    if (window.confirm("Reset all rules to system defaults?")) {
      onUpdateRules(DEFAULT_TAX_RULES);
    }
  };

  const toggleIncomeRule = (key: keyof typeof rules.incomeInclusion) => {
    onUpdateRules({
      ...rules,
      incomeInclusion: {
        ...rules.incomeInclusion,
        [key]: !rules.incomeInclusion[key]
      }
    });
  };

  const filteredCities = Object.entries(rules.municipalRates).filter(([city]) =>
    city.includes(searchTerm.toLowerCase())
  ).sort((a, b) => a[0].localeCompare(b[0]));

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex justify-end animate-fadeIn">
      <div className="w-full max-w-2xl bg-white h-full shadow-2xl flex flex-col animate-slideLeft">

        {/* Header */}
        <div className="px-6 py-4 border-b border-[#dcdede] flex justify-between items-center bg-[#f8f9fa]">
          <div className="flex items-center gap-2 text-[#0f1012]">
            <Settings className="w-5 h-5 text-[#469fe8]" />
            <h2 className="font-bold text-lg">Tax Rule Engine</h2>
          </div>
          <div className="flex items-center gap-2">
            {/* Sync from Backend Button */}
            <button 
              onClick={() => handleSyncFromBackend(false)} 
              disabled={syncing}
              className="flex items-center gap-1.5 px-3 py-2 text-[#469fe8] hover:bg-[#ebf4ff] rounded-lg transition-colors text-sm font-medium disabled:opacity-50" 
              title="Sync rules from Rule Management Dashboard"
            >
              <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
              {syncing ? 'Syncing...' : 'Sync from Backend'}
            </button>
            <button onClick={handleReset} className="p-2 text-[#5d6567] hover:text-[#ec1656] hover:bg-[#fff5f5] rounded-lg transition-colors" title="Reset to Defaults">
              <RotateCcw className="w-4 h-4" />
            </button>
            <button onClick={onClose} className="px-4 py-2 bg-gradient-to-r from-[#970bed] to-[#469fe8] text-white rounded-lg hover:from-[#7f09c5] hover:to-[#3a8bd4] font-medium text-sm">
              Done
            </button>
          </div>
        </div>

        {/* Sync Status Banner */}
        {syncStatus !== 'none' && (
          <div className={`px-6 py-2 flex items-center gap-2 text-sm ${
            syncStatus === 'success' ? 'bg-[#d5faeb] text-[#10b981]' : 'bg-[#fff5e6] text-[#f59e0b]'
          }`}>
            {syncStatus === 'success' ? <Cloud className="w-4 h-4" /> : <CloudOff className="w-4 h-4" />}
            {syncMessage}
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-6 space-y-8">

          {/* Logic Rules */}
          <section>
            <h3 className="text-sm font-bold text-[#5d6567] uppercase tracking-wider mb-4 flex items-center gap-2">
              <Sliders className="w-4 h-4" /> Calculation Logic
            </h3>
            <div className="bg-white border border-[#dcdede] rounded-xl p-5 space-y-6">
              {/* W-2 Rule */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-[#102124] block">W-2 Qualifying Wages Rule</label>
                <select
                  value={rules.w2QualifyingWagesRule}
                  onChange={(e) => onUpdateRules({ ...rules, w2QualifyingWagesRule: e.target.value as W2QualifyingWagesRule })}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg bg-white outline-none focus:border-[#970bed] focus:ring-2 focus:ring-[#970bed] text-sm"
                >
                  <option value={W2QualifyingWagesRule.HIGHEST_OF_ALL}>Highest of Box 1, 5, or 18 (Default)</option>
                  <option value={W2QualifyingWagesRule.BOX_5_MEDICARE}>Always Use Box 5 (Medicare)</option>
                  <option value={W2QualifyingWagesRule.BOX_18_LOCAL}>Always Use Box 18 (Local)</option>
                  <option value={W2QualifyingWagesRule.BOX_1_FEDERAL}>Always Use Box 1 (Federal)</option>
                </select>
                <p className="text-xs text-[#5d6567]">Determines which W-2 box determines the municipal tax base.</p>
              </div>

              <div className="h-px bg-[#f0f0f0]"></div>

              {/* Inclusion Toggles */}
              <div>
                <label className="text-sm font-medium text-[#102124] block mb-3">Taxable Income Inclusions</label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <ToggleOption label="Schedule C (Business)" checked={rules.incomeInclusion.scheduleC} onChange={() => toggleIncomeRule('scheduleC')} />
                  <ToggleOption label="Schedule E (Rentals)" checked={rules.incomeInclusion.scheduleE} onChange={() => toggleIncomeRule('scheduleE')} />
                  <ToggleOption label="Schedule F (Farm)" checked={rules.incomeInclusion.scheduleF} onChange={() => toggleIncomeRule('scheduleF')} />
                  <ToggleOption label="W-2G (Gambling)" checked={rules.incomeInclusion.w2g} onChange={() => toggleIncomeRule('w2g')} />
                  <ToggleOption label="1099-MISC/NEC" checked={rules.incomeInclusion.form1099} onChange={() => toggleIncomeRule('form1099')} />
                </div>
              </div>
            </div>
          </section>

          {/* Rounding & Precision */}
          <section>
            <h3 className="text-sm font-bold text-[#babebf] uppercase tracking-wider mb-4 flex items-center gap-2">
              <Hash className="w-4 h-4" /> Rounding & Precision
            </h3>
            <div className="bg-white border border-[#dcdede] rounded-xl p-5">
              <ToggleOption
                label="Round Tax Due to Whole Dollars"
                checked={rules.enableRounding}
                onChange={() => onUpdateRules({ ...rules, enableRounding: !rules.enableRounding })}
              />
              <p className="text-xs text-[#5d6567] mt-2">When enabled, the final municipal tax liability and balance due will be rounded to the nearest whole dollar (e.g. $10.50 &rarr; $11).</p>
            </div>
          </section>

          {/* Global Rates */}
          <section>
            <h3 className="text-sm font-bold text-[#5d6567] uppercase tracking-wider mb-4">Municipal Rates & Limits</h3>
            <div className="grid grid-cols-2 gap-6 bg-[#f8f9fa] p-5 rounded-xl border border-[#dcdede]">
              <div className="space-y-2">
                <label className="text-sm font-medium text-[#102124]">Municipal Tax Rate (Dublin)</label>
                <div className="relative">
                  <input
                    type="number" step="0.001"
                    value={rules.municipalRate}
                    onChange={(e) => handleRateChange('municipalRate', e.target.value)}
                    className="w-full pl-3 pr-12 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-transparent outline-none"
                  />
                  <span className="absolute right-3 top-2 text-[#babebf] text-sm">Rate</span>
                </div>
                <p className="text-xs text-[#5d6567]">The base tax rate applied to taxable income.</p>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-[#102124]">Credit Limit Rate</label>
                <div className="relative">
                  <input
                    type="number" step="0.001"
                    value={rules.municipalCreditLimitRate}
                    onChange={(e) => handleRateChange('municipalCreditLimitRate', e.target.value)}
                    className="w-full pl-3 pr-12 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-transparent outline-none"
                  />
                  <span className="absolute right-3 top-2 text-[#babebf] text-sm">Cap</span>
                </div>
                <p className="text-xs text-[#5d6567]">Max credit rate for taxes paid to other cities.</p>
              </div>
            </div>
          </section>

          {/* Municipality Map */}
          <section className="border-t border-[#dcdede] pt-8">
            <div className="flex justify-between items-end mb-4">
              <div>
                <h3 className="text-sm font-bold text-[#5d6567] uppercase tracking-wider">Reciprocity Map</h3>
                <p className="text-xs text-[#5d6567] mt-1">Rates for other municipalities used in Schedule Y credits.</p>
              </div>
              <div className="relative w-48">
                <Search className="absolute left-2.5 top-2.5 w-4 h-4 text-[#babebf]" />
                <input
                  type="text"
                  placeholder="Search cities..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-[#dcdede] rounded-lg outline-none focus:border-[#970bed] focus:ring-2 focus:ring-[#970bed]"
                />
              </div>
            </div>

            <div className="bg-[#f8f9fa] border border-[#dcdede] rounded-xl overflow-hidden">
              <div className="max-h-60 overflow-y-auto divide-y divide-[#dcdede]">
                {filteredCities.map(([city, rate]) => (
                  <div key={city} className="flex items-center justify-between px-4 py-3 hover:bg-white transition-colors group">
                    <span className="capitalize font-medium text-[#0f1012]">{city}</span>
                    <div className="flex items-center gap-3">
                      <input
                        type="number"
                        value={rate}
                        step="0.001"
                        onChange={(e) => handleCityRateChange(city, e.target.value)}
                        className="w-20 text-right px-2 py-1 border border-[#dcdede] rounded text-sm bg-white"
                      />
                      <button onClick={() => handleDeleteCity(city)} className="text-[#dcdede] hover:text-[#ec1656] opacity-0 group-hover:opacity-100 transition-all">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
                {filteredCities.length === 0 && <div className="p-4 text-center text-[#5d6567] text-sm">No cities found matching "{searchTerm}"</div>}
              </div>

              {/* Add New */}
              <div className="border-t border-[#dcdede] p-3 bg-white flex gap-2">
                <input
                  type="text" placeholder="New City Name"
                  value={newCityName} onChange={(e) => setNewCityName(e.target.value)}
                  className="flex-1 px-3 py-2 border border-[#dcdede] rounded-lg text-sm outline-none focus:border-indigo-500"
                />
                <input
                  type="number" placeholder="Rate (e.g. 0.025)" step="0.001"
                  value={newCityRate} onChange={(e) => setNewCityRate(e.target.value)}
                  className="w-32 px-3 py-2 border border-[#dcdede] rounded-lg text-sm outline-none focus:border-indigo-500"
                />
                <button
                  onClick={handleAddCity}
                  disabled={!newCityName || !newCityRate}
                  className="bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] disabled:bg-slate-300 text-white p-2 rounded-lg transition-colors"
                >
                  <Plus className="w-5 h-5" />
                </button>
              </div>
            </div>
          </section>

        </div>
      </div>
    </div>
  );
};

const ToggleOption = ({ label, checked, onChange }: any) => (
  <div
    onClick={onChange}
    className={`
      flex items-center justify-between p-3 rounded-lg border cursor-pointer transition-all
      ${checked ? 'bg-[#ebf4ff] border-[#970bed]/20' : 'bg-white border-[#dcdede] hover:border-[#dcdede]'}
    `}
  >
    <span className={`text-sm font-medium ${checked ? 'text-[#970bed]' : 'text-[#5d6567]'}`}>{label}</span>
    {checked ? <ToggleRight className="w-6 h-6 text-[#970bed]" /> : <ToggleLeft className="w-6 h-6 text-[#babebf]" />}
  </div>
);
