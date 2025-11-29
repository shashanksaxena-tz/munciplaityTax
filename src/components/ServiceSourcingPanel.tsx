/**
 * ServiceSourcingPanel Component
 * Allows users to elect service sourcing method (market-based vs cost-of-performance)
 * Task: T099 [US3]
 */

import React from 'react';
import { MapPin, Briefcase, PieChart, Info } from 'lucide-react';
import type { ServiceSourcingMethod } from '../types/sourcing';

interface ServiceSourcingPanelProps {
  value: ServiceSourcingMethod;
  onChange: (method: ServiceSourcingMethod) => void;
  disabled?: boolean;
}

export function ServiceSourcingPanel({ value, onChange, disabled }: ServiceSourcingPanelProps) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Service Revenue Sourcing Method</h3>
      
      <div className="space-y-4">
        {/* Market-Based Sourcing */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'MARKET_BASED' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="service-sourcing-method"
            value="MARKET_BASED"
            checked={value === 'MARKET_BASED'}
            onChange={(e) => onChange(e.target.value as ServiceSourcingMethod)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <MapPin className="w-4 h-4 text-blue-600 mr-2" />
              <span className="font-semibold text-gray-900">
                Market-Based Sourcing (Recommended)
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Source service revenue to where the customer receives the benefit of the service. 
              This is the modern approach used by most states and reflects the economic reality 
              of where value is delivered.
            </div>
            <div className="mt-3 bg-white border border-blue-200 rounded p-3">
              <div className="text-xs font-medium text-gray-700 mb-2">
                <strong>Example:</strong> IT Consulting Firm
              </div>
              <div className="text-xs text-gray-600 space-y-1">
                <div>• Ohio office: 5 employees, 70% of payroll</div>
                <div>• California office: 2 employees, 30% of payroll</div>
                <div>• Project: $1M consulting for New York customer</div>
                <div className="border-t pt-1 mt-1">
                  <strong>Market-Based Result:</strong> 100% sourced to NY (customer location)
                </div>
                <div>• OH gets $0, CA gets $0, NY gets $1M</div>
              </div>
            </div>
            <div className="text-xs text-gray-500 mt-2">
              ✓ Aligns with modern economic nexus principles<br/>
              ✓ Reduces compliance burden (simpler than cost-of-performance)<br/>
              ✓ Adopted by majority of states since 2010
            </div>
          </div>
        </label>

        {/* Cost-of-Performance */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'COST_OF_PERFORMANCE' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="service-sourcing-method"
            value="COST_OF_PERFORMANCE"
            checked={value === 'COST_OF_PERFORMANCE'}
            onChange={(e) => onChange(e.target.value as ServiceSourcingMethod)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <Briefcase className="w-4 h-4 text-orange-600 mr-2" />
              <span className="font-semibold text-gray-900">
                Cost-of-Performance
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Source service revenue to where the work is performed, typically prorated based 
              on employee location and payroll. This is the traditional approach but requires 
              more detailed tracking.
            </div>
            <div className="mt-3 bg-white border border-orange-200 rounded p-3">
              <div className="text-xs font-medium text-gray-700 mb-2">
                <strong>Example:</strong> Same IT Consulting Firm
              </div>
              <div className="text-xs text-gray-600 space-y-1">
                <div>• Ohio office: 5 employees, 70% of payroll</div>
                <div>• California office: 2 employees, 30% of payroll</div>
                <div>• Project: $1M consulting for New York customer</div>
                <div className="border-t pt-1 mt-1">
                  <strong>Cost-of-Performance Result:</strong> Prorated by work location
                </div>
                <div>• OH gets $700K (70%), CA gets $300K (30%), NY gets $0</div>
              </div>
            </div>
            <div className="text-xs text-gray-500 mt-2">
              ✓ Traditional method (pre-2010)<br/>
              ✓ Tracks where income is earned<br/>
              ✓ May be required by some older municipal codes
            </div>
          </div>
        </label>

        {/* Pro-Rata */}
        <label className={`flex items-start p-4 border-2 rounded-lg cursor-pointer transition ${
          value === 'PRO_RATA' 
            ? 'border-blue-500 bg-blue-50' 
            : 'border-gray-200 hover:border-gray-300'
        }`}>
          <input
            type="radio"
            name="service-sourcing-method"
            value="PRO_RATA"
            checked={value === 'PRO_RATA'}
            onChange={(e) => onChange(e.target.value as ServiceSourcingMethod)}
            disabled={disabled}
            className="mt-1 mr-3"
          />
          <div className="flex-1">
            <div className="flex items-center">
              <PieChart className="w-4 h-4 text-purple-600 mr-2" />
              <span className="font-semibold text-gray-900">
                Pro-Rata
              </span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Source service revenue proportionally based on apportionment factors 
              (property, payroll, or sales factors). This is a fallback method when 
              customer location or cost-of-performance data is unavailable.
            </div>
            <div className="mt-3 bg-white border border-purple-200 rounded p-3">
              <div className="text-xs font-medium text-gray-700 mb-2">
                <strong>Example:</strong> Same IT Consulting Firm
              </div>
              <div className="text-xs text-gray-600 space-y-1">
                <div>• Property factor: 60% OH, 40% CA</div>
                <div>• Payroll factor: 70% OH, 30% CA</div>
                <div>• Overall average: 65% OH, 35% CA</div>
                <div className="border-t pt-1 mt-1">
                  <strong>Pro-Rata Result:</strong> Based on average factors
                </div>
                <div>• OH gets $650K (65%), CA gets $350K (35%)</div>
              </div>
            </div>
            <div className="text-xs text-gray-500 mt-2">
              ✓ Fallback method when other data unavailable<br/>
              ✓ Uses existing apportionment factor data<br/>
              ✓ Simplest calculation method
            </div>
          </div>
        </label>
      </div>

      {/* Info Box - Cascading Rules */}
      <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div className="flex items-start">
          <Info className="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
          <div className="text-sm text-blue-800">
            <strong>Cascading Sourcing Rules:</strong>
            <p className="mt-1">
              The system automatically applies cascading rules for service sourcing:
            </p>
            <ol className="mt-2 ml-4 list-decimal space-y-1">
              <li><strong>Try Market-Based:</strong> If customer location is known</li>
              <li><strong>Fallback to Cost-of-Performance:</strong> If employee/payroll data available</li>
              <li><strong>Fallback to Pro-Rata:</strong> If other methods cannot be determined</li>
            </ol>
            <p className="mt-2 text-xs">
              This ensures service revenue is always properly sourced even with incomplete data.
            </p>
          </div>
        </div>
      </div>

      {/* State Adoption Info */}
      <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg">
        <p className="text-sm text-green-800">
          <strong>💡 State Adoption:</strong>
        </p>
        <div className="text-xs text-green-700 mt-2 space-y-1">
          <p>
            • <strong>Market-Based States:</strong> Ohio (adopted 2014), California, New York, 
            and 30+ other states
          </p>
          <p>
            • <strong>Cost-of-Performance States:</strong> Some states still use this for 
            certain service types
          </p>
          <p>
            • <strong>Hybrid States:</strong> Some states use market-based for most services 
            but cost-of-performance for specific industries
          </p>
        </div>
      </div>

      {/* Important Notice */}
      <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
        <div className="flex items-start">
          <svg className="w-5 h-5 text-yellow-600 mt-0.5 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
          </svg>
          <div className="text-sm text-yellow-800">
            <strong>Important:</strong> Service sourcing elections can significantly impact your 
            apportionment percentage. Market-based sourcing typically results in lower Ohio 
            apportionment if most customers are located outside Ohio. Consult with your tax 
            advisor to determine the best method for your business.
          </div>
        </div>
      </div>
    </div>
  );
}
