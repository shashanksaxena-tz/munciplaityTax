
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
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex justify-end animate-fadeIn">
      <div className="w-full max-w-2xl bg-white h-full shadow-2xl flex flex-col animate-slideLeft">

        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-slate-50">
          <div className="flex items-center gap-2 text-slate-800">
            <Settings className="w-5 h-5 text-indigo-600" />
            <h2 className="font-bold text-lg">Tax Rule Engine</h2>
          </div>
          <div className="flex items-center gap-2">
            {/* Sync from Backend Button */}
            <button 
              onClick={() => handleSyncFromBackend(false)} 
              disabled={syncing}
              className="flex items-center gap-1.5 px-3 py-2 text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors text-sm font-medium disabled:opacity-50" 
              title="Sync rules from Rule Management Dashboard"
            >
              <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
              {syncing ? 'Syncing...' : 'Sync from Backend'}
            </button>
            <button onClick={handleReset} className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors" title="Reset to Defaults">
              <RotateCcw className="w-4 h-4" />
            </button>
            <button onClick={onClose} className="px-4 py-2 bg-slate-800 text-white rounded-lg hover:bg-slate-900 font-medium text-sm">
              Done
            </button>
          </div>
        </div>

        {/* Sync Status Banner */}
        {syncStatus !== 'none' && (
          <div className={`px-6 py-2 flex items-center gap-2 text-sm ${
            syncStatus === 'success' ? 'bg-green-50 text-green-700' : 'bg-orange-50 text-orange-700'
          }`}>
            {syncStatus === 'success' ? <Cloud className="w-4 h-4" /> : <CloudOff className="w-4 h-4" />}
            {syncMessage}
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-6 space-y-8">

          {/* Logic Rules */}
          <section>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <Sliders className="w-4 h-4" /> Calculation Logic
            </h3>
            <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-6">
              {/* W-2 Rule */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700 block">W-2 Qualifying Wages Rule</label>
                <select
                  value={rules.w2QualifyingWagesRule}
                  onChange={(e) => onUpdateRules({ ...rules, w2QualifyingWagesRule: e.target.value as W2QualifyingWagesRule })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white outline-none focus:border-indigo-500 text-sm"
                >
                  <option value={W2QualifyingWagesRule.HIGHEST_OF_ALL}>Highest of Box 1, 5, or 18 (Default)</option>
                  <option value={W2QualifyingWagesRule.BOX_5_MEDICARE}>Always Use Box 5 (Medicare)</option>
                  <option value={W2QualifyingWagesRule.BOX_18_LOCAL}>Always Use Box 18 (Local)</option>
                  <option value={W2QualifyingWagesRule.BOX_1_FEDERAL}>Always Use Box 1 (Federal)</option>
                </select>
                <p className="text-xs text-slate-500">Determines which W-2 box determines the municipal tax base.</p>
              </div>

              <div className="h-px bg-slate-100"></div>

              {/* Inclusion Toggles */}
              <div>
                <label className="text-sm font-medium text-slate-700 block mb-3">Taxable Income Inclusions</label>
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
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <Hash className="w-4 h-4" /> Rounding & Precision
            </h3>
            <div className="bg-white border border-slate-200 rounded-xl p-5">
              <ToggleOption
                label="Round Tax Due to Whole Dollars"
                checked={rules.enableRounding}
                onChange={() => onUpdateRules({ ...rules, enableRounding: !rules.enableRounding })}
              />
              <p className="text-xs text-slate-500 mt-2">When enabled, the final municipal tax liability and balance due will be rounded to the nearest whole dollar (e.g. $10.50 &rarr; $11).</p>
            </div>
          </section>

          {/* Global Rates */}
          <section>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">Municipal Rates & Limits</h3>
            <div className="grid grid-cols-2 gap-6 bg-slate-50/50 p-5 rounded-xl border border-slate-200">
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">Municipal Tax Rate (Dublin)</label>
                <div className="relative">
                  <input
                    type="number" step="0.001"
                    value={rules.municipalRate}
                    onChange={(e) => handleRateChange('municipalRate', e.target.value)}
                    className="w-full pl-3 pr-12 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                  <span className="absolute right-3 top-2 text-slate-400 text-sm">Rate</span>
                </div>
                <p className="text-xs text-slate-500">The base tax rate applied to taxable income.</p>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">Credit Limit Rate</label>
                <div className="relative">
                  <input
                    type="number" step="0.001"
                    value={rules.municipalCreditLimitRate}
                    onChange={(e) => handleRateChange('municipalCreditLimitRate', e.target.value)}
                    className="w-full pl-3 pr-12 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                  <span className="absolute right-3 top-2 text-slate-400 text-sm">Cap</span>
                </div>
                <p className="text-xs text-slate-500">Max credit rate for taxes paid to other cities.</p>
              </div>
            </div>
          </section>

          {/* Municipality Map */}
          <section className="border-t border-slate-200 pt-8">
            <div className="flex justify-between items-end mb-4">
              <div>
                <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider">Reciprocity Map</h3>
                <p className="text-xs text-slate-500 mt-1">Rates for other municipalities used in Schedule Y credits.</p>
              </div>
              <div className="relative w-48">
                <Search className="absolute left-2.5 top-2.5 w-4 h-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search cities..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            <div className="bg-slate-50 border border-slate-200 rounded-xl overflow-hidden">
              <div className="max-h-60 overflow-y-auto divide-y divide-slate-200">
                {filteredCities.map(([city, rate]) => (
                  <div key={city} className="flex items-center justify-between px-4 py-3 hover:bg-white transition-colors group">
                    <span className="capitalize font-medium text-slate-700">{city}</span>
                    <div className="flex items-center gap-3">
                      <input
                        type="number"
                        value={rate}
                        step="0.001"
                        onChange={(e) => handleCityRateChange(city, e.target.value)}
                        className="w-20 text-right px-2 py-1 border border-slate-200 rounded text-sm bg-white"
                      />
                      <button onClick={() => handleDeleteCity(city)} className="text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
                {filteredCities.length === 0 && <div className="p-4 text-center text-slate-500 text-sm">No cities found matching "{searchTerm}"</div>}
              </div>

              {/* Add New */}
              <div className="border-t border-slate-200 p-3 bg-white flex gap-2">
                <input
                  type="text" placeholder="New City Name"
                  value={newCityName} onChange={(e) => setNewCityName(e.target.value)}
                  className="flex-1 px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-indigo-500"
                />
                <input
                  type="number" placeholder="Rate (e.g. 0.025)" step="0.001"
                  value={newCityRate} onChange={(e) => setNewCityRate(e.target.value)}
                  className="w-32 px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none focus:border-indigo-500"
                />
                <button
                  onClick={handleAddCity}
                  disabled={!newCityName || !newCityRate}
                  className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white p-2 rounded-lg transition-colors"
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
      ${checked ? 'bg-indigo-50 border-indigo-200' : 'bg-white border-slate-200 hover:border-slate-300'}
    `}
  >
    <span className={`text-sm font-medium ${checked ? 'text-indigo-700' : 'text-slate-600'}`}>{label}</span>
    {checked ? <ToggleRight className="w-6 h-6 text-indigo-600" /> : <ToggleLeft className="w-6 h-6 text-slate-400" />}
  </div>
);
