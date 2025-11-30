import React, { useState, useEffect } from 'react';
import { Settings, RotateCcw, Save, Server } from 'lucide-react';
import { apiConfig } from '../services/apiConfig';

export const ApiConfigPanel: React.FC<{ className?: string }> = ({ className = '' }) => {
  const [config, setConfig] = useState(apiConfig.getConfig());
  const [showPanel, setShowPanel] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setConfig(apiConfig.getConfig());
  }, []);

  const handleSave = () => {
    apiConfig.saveConfig(config);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const handleReset = () => {
    apiConfig.resetConfig();
    setConfig(apiConfig.getDefaults());
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const updateStandaloneUrl = (service: keyof typeof config.standalone, value: string) => {
    setConfig(prev => ({
      ...prev,
      standalone: {
        ...prev.standalone,
        [service]: value
      }
    }));
  };

  return (
    <div className={`${className}`}>
      {/* Toggle Button */}
      <button
        onClick={() => setShowPanel(!showPanel)}
        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
      >
        <Settings className="w-4 h-4" />
        API Configuration
      </button>

      {/* Configuration Panel */}
      {showPanel && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Server className="w-6 h-6 text-blue-600" />
                <h2 className="text-xl font-bold">API Configuration</h2>
              </div>
              <button
                onClick={() => setShowPanel(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl"
              >
                ×
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Mode Selection */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Mode
                </label>
                <select
                  value={config.mode}
                  onChange={(e) => setConfig({ ...config, mode: e.target.value as 'standalone' | 'production' })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="standalone">Standalone (Direct Service Ports)</option>
                  <option value="production">Production (API Gateway)</option>
                </select>
                <p className="mt-1 text-xs text-gray-500">
                  {config.mode === 'standalone' 
                    ? 'Calls services directly on their ports (8081, 8084, etc.)'
                    : 'All calls go through API Gateway with service discovery'}
                </p>
              </div>

              {/* Standalone Configuration */}
              {config.mode === 'standalone' && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-gray-800 flex items-center gap-2">
                    <Server className="w-5 h-5" />
                    Service Endpoints
                  </h3>
                  
                  {Object.entries(config.standalone).map(([key, value]) => (
                    <div key={key}>
                      <label className="block text-sm font-medium text-gray-700 mb-1 capitalize">
                        {key.replace('Service', ' Service')}
                      </label>
                      <input
                        type="text"
                        value={value}
                        onChange={(e) => updateStandaloneUrl(key as keyof typeof config.standalone, e.target.value)}
                        placeholder="http://localhost:XXXX"
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono text-sm"
                      />
                    </div>
                  ))}
                </div>
              )}

              {/* Production Configuration */}
              {config.mode === 'production' && (
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    API Gateway URL
                  </label>
                  <input
                    type="text"
                    value={config.production.apiGateway}
                    onChange={(e) => setConfig({
                      ...config,
                      production: { apiGateway: e.target.value }
                    })}
                    placeholder="http://localhost:8080"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    Gateway will route to services via Eureka service discovery
                  </p>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex gap-3 pt-4 border-t">
                <button
                  onClick={handleSave}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
                >
                  <Save className="w-4 h-4" />
                  {saved ? 'Saved!' : 'Save Configuration'}
                </button>
                <button
                  onClick={handleReset}
                  className="flex items-center gap-2 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors font-medium"
                >
                  <RotateCcw className="w-4 h-4" />
                  Reset
                </button>
              </div>

              {/* Info Box */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <p className="text-sm text-blue-800">
                  <strong>💡 Tip:</strong> Configuration is saved to session storage and persists during your browser session. 
                  Changes take effect immediately for new API calls.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApiConfigPanel;
