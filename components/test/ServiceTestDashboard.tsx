/**
 * Service Testing Dashboard
 * Main entry point for testing individual services
 */

import React from 'react';
import { Settings, BookOpen, FileSearch, FileText, Home, Shield } from 'lucide-react';
import { Link } from 'react-router-dom';

export const ServiceTestDashboard: React.FC = () => {
  const services = [
    {
      name: 'Auth Service',
      description: 'Test user authentication and authorization',
      icon: Shield,
      color: 'blue',
      path: '/test/auth',
      port: 8081,
      features: ['User Registration', 'Login/Logout', 'JWT Tokens', 'Protected Endpoints']
    },
    {
      name: 'Rule Service',
      description: 'Test tax rules configuration and management',
      icon: Settings,
      color: 'indigo',
      path: '/test/rules',
      port: 8084,
      features: ['Create/Update Rules', 'Approval Workflow', 'Temporal Rules', 'Multi-tenant']
    },
    {
      name: 'Ledger Service',
      description: 'Test accounting and ledger operations',
      icon: BookOpen,
      color: 'green',
      path: '/test/ledger',
      port: 8087,
      features: ['Journal Entries', 'Trial Balance', 'Payments', 'Reconciliation']
    },
    {
      name: 'Extraction Service',
      description: 'Test AI-powered document extraction',
      icon: FileSearch,
      color: 'purple',
      path: '/test/extraction',
      port: 8083,
      features: ['Document Upload', 'AI Extraction', 'W-2/1099 Processing', 'Confidence Scoring']
    },
    {
      name: 'PDF Service',
      description: 'Test PDF form generation and manipulation',
      icon: FileText,
      color: 'orange',
      path: '/test/pdf',
      port: 8086,
      features: ['Form Generation', 'PDF Templates', 'Watermarks', 'Filing Packages']
    }
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Header */}
      <div className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-slate-900">Service Testing Dashboard</h1>
              <p className="text-slate-600 mt-1">Test individual microservices in standalone mode</p>
            </div>
            <Link 
              to="/"
              className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
            >
              <Home className="w-5 h-5" />
              Back to Main App
            </Link>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        {/* Instructions */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-8">
          <h2 className="text-lg font-semibold text-blue-900 mb-3">🚀 Getting Started</h2>
          <div className="text-sm text-blue-800 space-y-2">
            <p><strong>1. Start Services in Standalone Mode:</strong></p>
            <div className="bg-white bg-opacity-50 rounded p-3 font-mono text-xs space-y-1">
              <div>cd backend/auth-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone</div>
              <div>cd backend/rule-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone</div>
              <div>cd backend/ledger-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone</div>
              <div>cd backend/extraction-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone</div>
              <div>cd backend/pdf-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone</div>
            </div>
            <p className="mt-3"><strong>2.</strong> Click on any service card below to open its test interface</p>
            <p><strong>3.</strong> Test endpoints, view responses, and validate functionality</p>
          </div>
        </div>

        {/* Service Cards */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {services.map((service) => {
            const Icon = service.icon;
            // Use explicit classes instead of dynamic generation for Tailwind
            const cardStyles = service.color === 'blue'
              ? { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-900', icon: 'text-blue-600', dot: 'bg-blue-500' }
              : service.color === 'indigo' 
              ? { bg: 'bg-indigo-50', border: 'border-indigo-200', text: 'text-indigo-900', icon: 'text-indigo-600', dot: 'bg-indigo-500' }
              : service.color === 'green'
              ? { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-900', icon: 'text-green-600', dot: 'bg-green-500' }
              : service.color === 'purple'
              ? { bg: 'bg-purple-50', border: 'border-purple-200', text: 'text-purple-900', icon: 'text-purple-600', dot: 'bg-purple-500' }
              : { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-900', icon: 'text-orange-600', dot: 'bg-orange-500' };

            return (
              <Link
                key={service.name}
                to={service.path}
                className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-slate-200 overflow-hidden group"
              >
                <div className={`${cardStyles.bg} border-b ${cardStyles.border} p-6`}>
                  <Icon className={`w-12 h-12 ${cardStyles.icon} mb-3`} />
                  <h3 className={`text-xl font-bold ${cardStyles.text} mb-2`}>{service.name}</h3>
                  <p className="text-sm text-slate-600">{service.description}</p>
                </div>

                <div className="p-6">
                  <div className="mb-4">
                    <span className="text-xs font-medium text-slate-500">PORT</span>
                    <p className="text-lg font-semibold text-slate-900">{service.port}</p>
                  </div>

                  <div>
                    <span className="text-xs font-medium text-slate-500 mb-2 block">FEATURES</span>
                    <div className="space-y-1">
                      {service.features.map((feature, idx) => (
                        <div key={idx} className="flex items-center gap-2 text-sm text-slate-700">
                          <div className={`w-1.5 h-1.5 rounded-full ${cardStyles.dot}`}></div>
                          {feature}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="mt-6 pt-4 border-t border-slate-200">
                    <span className="text-sm font-medium text-indigo-600 group-hover:text-indigo-700">
                      Open Test Interface →
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>

        {/* Info Section */}
        <div className="mt-12 bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold text-slate-900 mb-4">About Standalone Testing</h2>
          <div className="text-slate-700 space-y-3">
            <p>
              This testing interface allows you to test individual microservices independently without
              running the full application stack.
            </p>
            <div className="bg-slate-50 p-4 rounded-lg">
              <h3 className="font-semibold mb-2">Key Benefits:</h3>
              <ul className="list-disc list-inside space-y-1 text-sm">
                <li>Test services in isolation without dependencies</li>
                <li>Faster development and debugging cycles</li>
                <li>Direct access to service endpoints</li>
                <li>No interference with the main application</li>
                <li>Simplified troubleshooting and validation</li>
              </ul>
            </div>
            <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-lg">
              <h3 className="font-semibold text-yellow-900 mb-2">⚠️ Note:</h3>
              <p className="text-sm text-yellow-800">
                Services must be started with the <code className="bg-yellow-100 px-1 py-0.5 rounded">standalone</code> profile
                to disable Eureka discovery and enable CORS for local testing.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ServiceTestDashboard;
