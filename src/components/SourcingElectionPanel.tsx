/**
 * SourcingElectionPanel Component
 * Allows users to elect between Finnigan and Joyce sourcing methods
 */

import React from 'react';
import type { SourcingMethodElection } from '../types/sourcing';

interface SourcingElectionPanelProps {
  value: SourcingMethodElection;
  onChange: (method: SourcingMethodElection) => void;
  disabled?: boolean;
}

export function SourcingElectionPanel({ value, onChange, disabled }: SourcingElectionPanelProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Sales Factor Sourcing Method</h3>
      
      <div className="space-y-4">
        {/* Finnigan Method */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'FINNIGAN' 
            ? 'border-[#469fe8] bg-[#ebf4ff]' 
            : 'border-[#dcdede] hover:border-[#babebf]'
        }`}>
          <input
            type="radio"
            name="sourcing-method"
            value="FINNIGAN"
            checked={value === 'FINNIGAN'}
            onChange={(e) => onChange(e.target.value as SourcingMethodElection)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="font-semibold text-[#0f1012]">
              Finnigan Method (Recommended)
            </div>
            <div className="text-sm text-[#5d6567] mt-1">
              Includes sales from all affiliated entities, even if they lack Ohio nexus. 
              Used by the majority of states. This is the default method.
            </div>
            <div className="text-xs text-[#babebf] mt-2">
              ✓ Includes all group sales in denominator<br/>
              ✓ Majority rule across US states<br/>
              ✓ Simpler calculation
            </div>
          </div>
        </label>

        {/* Joyce Method */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'JOYCE' 
            ? 'border-[#469fe8] bg-[#ebf4ff]' 
            : 'border-[#dcdede] hover:border-[#babebf]'
        }`}>
          <input
            type="radio"
            name="sourcing-method"
            value="JOYCE"
            checked={value === 'JOYCE'}
            onChange={(e) => onChange(e.target.value as SourcingMethodElection)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="font-semibold text-[#0f1012]">
              Joyce Method
            </div>
            <div className="text-sm text-[#5d6567] mt-1">
              Includes only sales from entities with Ohio nexus. 
              Use this method only if explicitly required by the municipality.
            </div>
            <div className="text-xs text-[#babebf] mt-2">
              ✓ Only nexus entities in denominator<br/>
              ✓ Minority rule (few states use this)<br/>
              ✓ More complex nexus tracking required
            </div>
          </div>
        </label>
      </div>

      {/* Info Box */}
      <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div className="flex items-start">
          <svg className="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="text-sm text-blue-800">
            <strong>Impact:</strong> This election can result in 10-30% differences in your 
            taxable income allocation. The Finnigan method typically includes more sales in 
            the denominator, which may reduce your Ohio apportionment percentage.
          </div>
        </div>
      </div>
    </div>
  );
}
