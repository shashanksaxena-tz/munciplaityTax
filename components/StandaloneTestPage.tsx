import React, { useState } from 'react';
import { ApiConfigPanel } from './ApiConfigPanel';
import { apiConfig } from '../services/apiConfig';

export const StandaloneTestPage: React.FC = () => {
  const [activeService, setActiveService] = useState<string>('');
  const config = apiConfig.getConfig();

  const services = [
    { name: 'Auth Service', key: 'authService', port: '8081', color: 'blue' },
    { name: 'Rule Service', key: 'ruleService', port: '8084', color: 'green' },
    { name: 'Tax Engine', key: 'taxEngineService', port: '8085', color: 'purple' },
    { name: 'Extraction Service', key: 'extractionService', port: '8083', color: 'orange' },
    { name: 'Ledger Service', key: 'ledgerService', port: '8087', color: 'red' },
    { name: 'Submission Service', key: 'submissionService', port: '8089', color: 'indigo' },
    { name: 'PDF Service', key: 'pdfService', port: '8086', color: 'pink' }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-800">Standalone Service Testing</h1>
              <p className="text-gray-600 mt-2">
                Test individual microservices with configurable endpoints
              </p>
            </div>
            <ApiConfigPanel />
          </div>
        </div>

        {/* Current Configuration Display */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
          <h2 className="text-xl font-bold text-gray-800 mb-4">Current Configuration</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <span className="text-sm font-semibold text-gray-700">Mode:</span>
              <span className={`ml-2 px-3 py-1 rounded-full text-sm font-medium ${
                config.mode === 'standalone' 
                  ? 'bg-blue-100 text-blue-800' 
                  : 'bg-green-100 text-green-800'
              }`}>
                {config.mode}
              </span>
            </div>
            {config.mode === 'production' && (
              <div>
                <span className="text-sm font-semibold text-gray-700">API Gateway:</span>
                <code className="ml-2 text-sm bg-gray-100 px-2 py-1 rounded">
                  {config.production.apiGateway}
                </code>
              </div>
            )}
          </div>
        </div>

        {/* Service Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {services.map((service) => {
            const url = config.mode === 'standalone' 
              ? config.standalone[service.key as keyof typeof config.standalone]
              : config.production.apiGateway;
            
            return (
              <div
                key={service.key}
                className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-shadow cursor-pointer"
                onClick={() => setActiveService(service.key)}
              >
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-bold text-gray-800">{service.name}</h3>
                  <div className={`w-3 h-3 rounded-full bg-${service.color}-500`}></div>
                </div>
                
                <div className="space-y-2">
                  <div>
                    <span className="text-xs font-semibold text-gray-600">Default Port:</span>
                    <code className="ml-2 text-sm bg-gray-100 px-2 py-1 rounded">{service.port}</code>
                  </div>
                  <div>
                    <span className="text-xs font-semibold text-gray-600">Current URL:</span>
                    <code className="block mt-1 text-xs bg-gray-100 px-2 py-1 rounded truncate">
                      {url}
                    </code>
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    window.location.href = `?test=${service.key}`;
                  }}
                  className={`mt-4 w-full px-4 py-2 bg-${service.color}-600 text-white rounded-lg hover:bg-${service.color}-700 transition-colors font-medium`}
                >
                  Test Service
                </button>
              </div>
            );
          })}
        </div>

        {/* Instructions */}
        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-xl p-6">
          <h3 className="text-lg font-bold text-blue-900 mb-3">Quick Start Guide</h3>
          <ol className="space-y-2 text-sm text-blue-800">
            <li className="flex items-start">
              <span className="font-bold mr-2">1.</span>
              <span>Click <strong>"API Configuration"</strong> to set service URLs and ports</span>
            </li>
            <li className="flex items-start">
              <span className="font-bold mr-2">2.</span>
              <span>Start the services you want to test with <code className="bg-blue-100 px-1 rounded">-Dspring-boot.run.profiles=standalone</code></span>
            </li>
            <li className="flex items-start">
              <span className="font-bold mr-2">3.</span>
              <span>Click <strong>"Test Service"</strong> on any service card to open its test UI</span>
            </li>
            <li className="flex items-start">
              <span className="font-bold mr-2">4.</span>
              <span>Configuration persists during your browser session</span>
            </li>
          </ol>
        </div>
      </div>
    </div>
  );
};

export default StandaloneTestPage;
