/**
 * ThrowbackElectionPanel Component
 * Allows users to elect throwback vs throwout treatment for no-nexus state sales
 * Task: T084 [US2]
 */

import React from 'react';
import { ArrowLeft, Ban, Info } from 'lucide-react';
import type { ThrowbackElection } from '../types/sourcing';

interface ThrowbackElectionPanelProps {
  value: ThrowbackElection;
  onChange: (election: ThrowbackElection) => void;
  disabled?: boolean;
}

export function ThrowbackElectionPanel({ value, onChange, disabled }: ThrowbackElectionPanelProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Throwback Rule Election</h3>
      
      <div className="space-y-4">
        {/* Throwback Option */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'THROWBACK' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="throwback-election"
            value="THROWBACK"
            checked={value === 'THROWBACK'}
            onChange={(e) => onChange(e.target.value as ThrowbackElection)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <ArrowLeft className="w-4 h-4 text-blue-600 mr-2" />
              <span className="font-semibold text-gray-900">
                Throwback (Recommended)
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Sales to states where you lack nexus are "thrown back" to the origin state (Ohio). 
              This increases your Ohio numerator while keeping the denominator unchanged.
            </div>
            <div className="text-xs text-gray-500 mt-2">
              <strong>Example:</strong> Ship $100K goods from OH to CA (no CA nexus)<br/>
              → Add $100K to OH sales numerator<br/>
              → Include $100K in total sales denominator<br/>
              → <strong>Effect:</strong> Increases OH apportionment percentage
            </div>
          </div>
        </label>

        {/* Throwout Option */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'THROWOUT' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="throwback-election"
            value="THROWOUT"
            checked={value === 'THROWOUT'}
            onChange={(e) => onChange(e.target.value as ThrowbackElection)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <Ban className="w-4 h-4 text-orange-600 mr-2" />
              <span className="font-semibold text-gray-900">
                Throwout
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Sales to states where you lack nexus are excluded from the denominator entirely. 
              This reduces your total sales denominator, potentially decreasing your Ohio apportionment.
            </div>
            <div className="text-xs text-gray-500 mt-2">
              <strong>Example:</strong> Ship $100K goods from OH to CA (no CA nexus)<br/>
              → $0 added to OH sales numerator<br/>
              → Remove $100K from total sales denominator<br/>
              → <strong>Effect:</strong> May increase or decrease OH apportionment depending on other factors
            </div>
          </div>
        </label>

        {/* None Option */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'NONE' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="throwback-election"
            value="NONE"
            checked={value === 'NONE'}
            onChange={(e) => onChange(e.target.value as ThrowbackElection)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <Info className="w-4 h-4 text-gray-600 mr-2" />
              <span className="font-semibold text-gray-900">
                None (No Special Treatment)
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Sales to states where you lack nexus are included in the denominator only, 
              without any throwback or throwout adjustment. This is the most conservative approach.
            </div>
            <div className="text-xs text-gray-500 mt-2">
              <strong>Example:</strong> Ship $100K goods from OH to CA (no CA nexus)<br/>
              → $0 added to OH sales numerator<br/>
              → Include $100K in total sales denominator<br/>
              → <strong>Effect:</strong> Decreases OH apportionment percentage (nowhere income)
            </div>
          </div>
        </label>
      </div>

      {/* Info Box */}
      <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
        <div className="flex items-start">
          <svg className="w-5 h-5 text-yellow-600 mt-0.5 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="text-sm text-yellow-800">
            <strong>Important:</strong> Ohio generally follows the throwback rule. This election 
            determines how sales to states where you lack nexus are treated. Most Ohio municipalities 
            require throwback treatment unless you elect otherwise. Consult with your tax advisor 
            to determine the best election for your business.
          </div>
        </div>
      </div>

      {/* State-Specific Guidance */}
      <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p className="text-sm text-blue-800">
          <strong>💡 State Requirements:</strong>
        </p>
        <ul className="text-xs text-blue-700 mt-2 space-y-1 ml-4 list-disc">
          <li><strong>Throwback states:</strong> Ohio, Alabama, Arkansas, California (limited), and others - require throwing back sales to origin state</li>
          <li><strong>Throwout states:</strong> Illinois, Pennsylvania - exclude no-nexus sales from denominator</li>
          <li><strong>No throwback/throwout:</strong> Most states - creates "nowhere income" not taxed anywhere</li>
        </ul>
      </div>
    </div>
  );
}
