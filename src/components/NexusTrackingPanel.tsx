/**
 * NexusTrackingPanel Component
 * Displays nexus status by state with reasons and management capabilities
 * Task: T085 [US2]
 */

import React, { useState } from 'react';
import { MapPin, CheckCircle, XCircle, AlertCircle, RefreshCw } from 'lucide-react';
import type { NexusStatusSummary, NexusReason } from '../types/nexus';
import { NEXUS_REASON_DESCRIPTIONS, STATE_NAMES } from '../types/nexus';

interface NexusTrackingPanelProps {
  nexusStatus: NexusStatusSummary | null;
  onRefresh?: () => void;
  onUpdateNexus?: (state: string, hasNexus: boolean, reason: NexusReason) => void;
  loading?: boolean;
}

export function NexusTrackingPanel({ 
  nexusStatus, 
  onRefresh, 
  onUpdateNexus,
  loading 
}: NexusTrackingPanelProps) {
  const [expandedState, setExpandedState] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<'all' | 'nexus' | 'no-nexus'>('all');

  if (!nexusStatus) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="text-center py-8">
          <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-3" />
          <p className="text-gray-600">No nexus data available</p>
          {onRefresh && (
            <button
              onClick={onRefresh}
              className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Load Nexus Status
            </button>
          )}
        </div>
      </div>
    );
  }

  const getFilteredStates = () => {
    const allStates = Object.keys(nexusStatus.nexusByState || {});
    if (filterType === 'nexus') {
      return allStates.filter(state => nexusStatus.nexusByState[state]);
    } else if (filterType === 'no-nexus') {
      return allStates.filter(state => !nexusStatus.nexusByState[state]);
    }
    return allStates;
  };

  const filteredStates = getFilteredStates();

  const getNexusIcon = (hasNexus: boolean) => {
    return hasNexus 
      ? <CheckCircle className="w-5 h-5 text-green-600" />
      : <XCircle className="w-5 h-5 text-red-600" />;
  };

  const getNexusReasonInfo = (reason: string) => {
    const reasonKey = reason as NexusReason;
    return NEXUS_REASON_DESCRIPTIONS[reasonKey] || {
      label: reason,
      description: 'Unknown reason',
      icon: '❓',
      color: 'gray'
    };
  };

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <MapPin className="w-6 h-6 text-blue-600 mr-2" />
            <h3 className="text-lg font-semibold text-gray-900">Nexus Status by State</h3>
          </div>
          {onRefresh && (
            <button
              onClick={onRefresh}
              disabled={loading}
              className="flex items-center px-3 py-1.5 text-sm border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          )}
        </div>

        {/* Summary Stats */}
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-green-50 border border-green-200 rounded-lg p-3">
            <div className="text-sm text-green-600 font-medium">Has Nexus</div>
            <div className="text-2xl font-bold text-green-700">{nexusStatus.nexusStateCount}</div>
          </div>
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <div className="text-sm text-red-600 font-medium">No Nexus</div>
            <div className="text-2xl font-bold text-red-700">
              {nexusStatus.nonNexusStates?.length || 0}
            </div>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <div className="text-sm text-blue-600 font-medium">Total States</div>
            <div className="text-2xl font-bold text-blue-700">
              {Object.keys(nexusStatus.nexusByState || {}).length}
            </div>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-2 mt-4">
          <button
            onClick={() => setFilterType('all')}
            className={`px-3 py-1.5 text-sm rounded ${
              filterType === 'all' 
                ? 'bg-blue-600 text-white' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            All States ({Object.keys(nexusStatus.nexusByState || {}).length})
          </button>
          <button
            onClick={() => setFilterType('nexus')}
            className={`px-3 py-1.5 text-sm rounded ${
              filterType === 'nexus' 
                ? 'bg-green-600 text-white' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Has Nexus ({nexusStatus.nexusStateCount})
          </button>
          <button
            onClick={() => setFilterType('no-nexus')}
            className={`px-3 py-1.5 text-sm rounded ${
              filterType === 'no-nexus' 
                ? 'bg-red-600 text-white' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            No Nexus ({nexusStatus.nonNexusStates?.length || 0})
          </button>
        </div>
      </div>

      {/* State List */}
      <div className="max-h-96 overflow-y-auto">
        {filteredStates.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            No states match the selected filter
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {filteredStates.map((state) => {
              const hasNexus = nexusStatus.nexusByState[state];
              const reason = nexusStatus.nexusReasonByState?.[state] || 'NO_NEXUS';
              const reasonInfo = getNexusReasonInfo(reason);
              const determinationDate = nexusStatus.nexusDeterminationDateByState?.[state];
              const isExpanded = expandedState === state;

              return (
                <div key={state} className="p-4 hover:bg-gray-50">
                  <div 
                    className="flex items-center justify-between cursor-pointer"
                    onClick={() => setExpandedState(isExpanded ? null : state)}
                  >
                    <div className="flex items-center flex-1">
                      {getNexusIcon(hasNexus)}
                      <div className="ml-3">
                        <div className="flex items-center">
                          <span className="font-semibold text-gray-900">
                            {STATE_NAMES[state] || state}
                          </span>
                          <span className="ml-2 text-sm text-gray-500">({state})</span>
                        </div>
                        <div className="text-sm text-gray-600 flex items-center mt-0.5">
                          <span className="mr-1">{reasonInfo.icon}</span>
                          <span>{reasonInfo.label}</span>
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className={`px-3 py-1 rounded-full text-xs font-medium ${
                        hasNexus 
                          ? 'bg-green-100 text-green-800' 
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {hasNexus ? 'HAS NEXUS' : 'NO NEXUS'}
                      </div>
                      {determinationDate && (
                        <div className="text-xs text-gray-500 mt-1">
                          {new Date(determinationDate).toLocaleDateString()}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Expanded Details */}
                  {isExpanded && (
                    <div className="mt-3 ml-8 p-3 bg-gray-50 rounded border border-gray-200">
                      <p className="text-sm text-gray-700 mb-2">
                        <strong>Reason:</strong> {reasonInfo.description}
                      </p>
                      {nexusStatus.nexusDetails?.[state] && (
                        <p className="text-sm text-gray-700 mb-2">
                          <strong>Details:</strong> {nexusStatus.nexusDetails[state]}
                        </p>
                      )}
                      {determinationDate && (
                        <p className="text-sm text-gray-700 mb-2">
                          <strong>Last Determined:</strong>{' '}
                          {new Date(determinationDate).toLocaleString()}
                        </p>
                      )}
                      
                      {/* Action Buttons (if handler provided) */}
                      {onUpdateNexus && (
                        <div className="mt-3 flex gap-2">
                          <button
                            onClick={() => onUpdateNexus(state, true, 'PHYSICAL_PRESENCE' as NexusReason)}
                            className="px-3 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
                          >
                            Mark Has Nexus
                          </button>
                          <button
                            onClick={() => onUpdateNexus(state, false, 'NO_NEXUS' as NexusReason)}
                            className="px-3 py-1 text-xs bg-red-600 text-white rounded hover:bg-red-700"
                          >
                            Mark No Nexus
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Footer Info */}
      <div className="p-4 bg-gray-50 border-t border-gray-200">
        <div className="flex items-start">
          <AlertCircle className="w-4 h-4 text-blue-600 mt-0.5 mr-2 flex-shrink-0" />
          <div className="text-xs text-gray-700">
            <p>
              <strong>Nexus Status Impact:</strong> States where you have no nexus may trigger 
              throwback rules, affecting your Ohio apportionment calculation. Keep nexus status 
              up to date for accurate apportionment.
            </p>
            <p className="mt-1">
              <strong>Economic Nexus:</strong> Most states have $500K sales or 200 transaction thresholds. 
              The system automatically tracks these thresholds.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
