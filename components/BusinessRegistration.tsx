
import React, { useState } from 'react';
import { BusinessProfile, FilingFrequency, Address } from '../types';
import { verifyLocalAddress } from '../utils/addressUtils';
import { Briefcase, ArrowRight, CheckCircle } from 'lucide-react';

interface BusinessRegistrationProps {
  onRegister: (profile: BusinessProfile) => void;
  onCancel: () => void;
}

export const BusinessRegistration: React.FC<BusinessRegistrationProps> = ({ onRegister, onCancel }) => {
  const [profile, setProfile] = useState<BusinessProfile>({
    businessName: '',
    fein: '',
    accountNumber: '',
    address: { street: '', city: '', state: 'OH', zip: '', verificationStatus: 'UNVERIFIED' },
    filingFrequency: FilingFrequency.QUARTERLY,
    fiscalYearEnd: '12-31'
  });

  const handleAddressChange = (field: keyof Address, value: string) => {
    setProfile({
      ...profile,
      address: { ...profile.address, [field]: value, verificationStatus: 'UNVERIFIED' }
    });
  };

  const handleVerify = () => {
    const verified = verifyLocalAddress(profile.address);
    setProfile({ ...profile, address: verified });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onRegister(profile);
  };

  return (
    <div className="max-w-2xl mx-auto py-12 animate-fadeIn">
      <div className="bg-white rounded-2xl shadow-lg border border-[#f0f0f0] overflow-hidden">
        <div className="bg-gradient-to-r from-[#970bed] to-[#469fe8] px-8 py-6 text-white">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-white/20 rounded-lg"><Briefcase className="w-6 h-6 text-white" /></div>
            <h2 className="text-2xl font-bold">Business Registration</h2>
          </div>
          <p className="text-white/90">Register your entity with the City of Dublin Tax Division.</p>
        </div>

        <form onSubmit={handleSubmit} className="p-8 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-sm font-bold text-[#102124]">Business Legal Name</label>
              <input
                required
                type="text"
                value={profile.businessName}
                onChange={e => setProfile({ ...profile, businessName: e.target.value })}
                className="w-full px-4 py-2.5 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                placeholder="Company LLC"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-bold text-[#102124]">FEIN</label>
              <input
                required
                type="text"
                value={profile.fein}
                onChange={e => setProfile({ ...profile, fein: e.target.value })}
                className="w-full px-4 py-2.5 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                placeholder="XX-XXXXXXX"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-bold text-[#102124]">Filing Frequency</label>
              <select
                value={profile.filingFrequency}
                onChange={e => setProfile({ ...profile, filingFrequency: e.target.value as FilingFrequency })}
                className="w-full px-4 py-2.5 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none bg-white"
              >
                {Object.values(FilingFrequency).map(f => <option key={f} value={f}>{f}</option>)}
              </select>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-bold text-[#102124]">Account Number (If known)</label>
              <input
                type="text"
                value={profile.accountNumber}
                onChange={e => setProfile({ ...profile, accountNumber: e.target.value })}
                className="w-full px-4 py-2.5 border border-[#dcdede] rounded-xl focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                placeholder="Optional"
              />
            </div>
          </div>

          <div className="bg-[#fbfbfb] p-4 rounded-xl border border-[#f0f0f0]">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-[#0f1012]">Business Address</h3>
              {profile.address.verificationStatus === 'VERIFIED_IN_DISTRICT' && (
                <span className="flex items-center gap-1 text-[#10b981] text-xs font-bold uppercase"><CheckCircle className="w-3 h-3" /> Verified In-District</span>
              )}
            </div>
            <div className="space-y-3">
              <input
                type="text" placeholder="Street Address" required
                value={profile.address.street} onChange={e => handleAddressChange('street', e.target.value)}
                className="w-full px-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
              />
              <div className="grid grid-cols-3 gap-3">
                <input
                  type="text" placeholder="City" required
                  value={profile.address.city} onChange={e => handleAddressChange('city', e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                />
                <input
                  type="text" placeholder="State" required
                  value={profile.address.state} onChange={e => handleAddressChange('state', e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                />
                <input
                  type="text" placeholder="Zip" required
                  value={profile.address.zip} onChange={e => handleAddressChange('zip', e.target.value)}
                  className="w-full px-3 py-2 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] outline-none"
                />
              </div>
              <div className="flex justify-end pt-2">
                <button type="button" onClick={handleVerify} className="text-sm text-[#970bed] hover:text-[#469fe8] hover:underline font-medium">Check Address</button>
              </div>
            </div>
          </div>

          <div className="flex gap-4 pt-4">
            <button type="button" onClick={onCancel} className="flex-1 py-3 border border-[#dcdede] rounded-xl text-[#5d6567] font-medium hover:bg-[#f0f0f0]">Cancel</button>
            <button type="submit" className="flex-[2] py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold shadow-lg flex items-center justify-center gap-2">
              Complete Registration <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
