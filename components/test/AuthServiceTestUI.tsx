/**
 * Auth Service Standalone Test UI
 * Tests auth-service endpoints with actual login/registration flows
 */

import React, { useState, useEffect } from 'react';
import { Shield, UserPlus, LogIn, RefreshCw, CheckCircle, Users } from 'lucide-react';
import { ApiConfigPanel } from '../ApiConfigPanel';
import { apiConfig } from '../../services/apiConfig';

interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  createdAt: string;
}

export const AuthServiceTestUI: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [testResult, setTestResult] = useState<string>('');
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('testing');
  const [authToken, setAuthToken] = useState<string>('');
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [serviceUrl, setServiceUrl] = useState('');

  const [loginForm, setLoginForm] = useState({
    username: 'testuser@example.com',
    password: 'Test123!'
  });

  const [registerForm, setRegisterForm] = useState({
    email: 'newuser@example.com',
    password: 'Test123!',
    firstName: 'Test',
    lastName: 'User',
    phoneNumber: '+15551234567',
    userRole: 'ROLE_BUSINESS',
    profileType: 'BUSINESS',
    ssnOrEin: '12-3456789',
    businessName: 'Test Business LLC',
    fiscalYearEnd: '12-31',
    address: {
      street: '123 Main St',
      city: 'Dublin',
      state: 'OH',
      zip: '43016'
    }
  });

  useEffect(() => {
    // Get the configured service URL
    const url = apiConfig.getServiceUrl('/auth');
    setServiceUrl(url);
    testConnection(url);
  }, []);

  const testConnection = async (url: string = serviceUrl) => {
    setConnectionStatus('testing');
    try {
      const response = await fetch(`${url}/actuator/health`, {
        method: 'GET',
      });
      
      if (response.ok) {
        setConnectionStatus('connected');
        setTestResult(`✅ Successfully connected to Auth Service at ${url}`);
      } else {
        setConnectionStatus('disconnected');
        setTestResult(`❌ Connection failed: ${response.statusText}`);
      }
    } catch (err) {
      setConnectionStatus('disconnected');
      setTestResult(`❌ Connection failed: ${err instanceof Error ? err.message : 'Unknown error'}. Make sure service is running at ${url}`);
    }
  };

  const handleLogin = async () => {
    setLoading(true);
    setError('');
    try {
      const url = apiConfig.buildUrl('/auth', '/api/v1/auth/login');
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(loginForm),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      const data = await response.json();
      setAuthToken(data.token || data.accessToken);
      setCurrentUser(data.user);
      setTestResult(`✅ Login successful! Token: ${(data.token || data.accessToken).substring(0, 20)}...`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Login failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    setLoading(true);
    setError('');
    try {
      const url = apiConfig.buildUrl('/auth', '/api/v1/users/register');
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(registerForm),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      const data = await response.json();
      setTestResult(`✅ Registration successful! User ID: ${data.id || data.userId}`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Registration failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const testProtectedEndpoint = async () => {
    if (!authToken) {
      setError('Please login first to get a token');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const url = apiConfig.buildUrl('/auth', '/api/v1/auth/me');
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      setTestResult(`✅ Protected endpoint accessed! User: ${data.username}`);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Protected endpoint failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Shield className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Auth Service Test UI</h1>
                <p className="text-sm text-slate-600">Standalone testing interface for auth-service</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <ApiConfigPanel />
              <div className={`px-3 py-1 rounded-full text-sm font-medium ${
                connectionStatus === 'connected' ? 'bg-green-100 text-green-800' :
                connectionStatus === 'disconnected' ? 'bg-red-100 text-red-800' :
                'bg-yellow-100 text-yellow-800'
              }`}>
                {connectionStatus === 'connected' ? '● Connected' :
                 connectionStatus === 'disconnected' ? '● Disconnected' :
                 '● Testing...'}
              </div>
              <button
                onClick={() => testConnection()}
                className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
                title="Test connection"
              >
                <RefreshCw className="w-5 h-5 text-slate-600" />
              </button>
            </div>
          </div>
        </div>

        {/* Test Result Banner */}
        {testResult && (
          <div className={`rounded-lg p-4 mb-6 ${
            testResult.startsWith('✅') ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
          }`}>
            <p className="font-medium">{testResult}</p>
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p className="text-red-800 font-medium">Error: {error}</p>
          </div>
        )}

        {/* Connection Info */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <h3 className="font-semibold text-blue-900 mb-2">Service Information</h3>
          <div className="text-sm text-blue-800 space-y-1">
            <p><strong>Configured URL:</strong> <code className="bg-blue-100 px-2 py-1 rounded">{serviceUrl}</code></p>
            <p><strong>Default Port:</strong> 8081</p>
            <p><strong>Profile:</strong> standalone</p>
            <p><strong>Features:</strong> User registration, Login, JWT token generation, Protected endpoints</p>
            <p className="mt-2 text-xs">💡 Click "API Configuration" to change the service URL</p>
          </div>
        </div>

        {/* Current Session */}
        {authToken && (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
            <h3 className="font-semibold text-green-900 mb-2 flex items-center gap-2">
              <CheckCircle className="w-5 h-5" />
              Active Session
            </h3>
            <div className="text-sm text-green-800 space-y-1">
              <p><strong>Token:</strong> {authToken.substring(0, 40)}...</p>
              {currentUser && (
                <>
                  <p><strong>User:</strong> {currentUser.username}</p>
                  <p><strong>Roles:</strong> {currentUser.roles?.join(', ')}</p>
                </>
              )}
            </div>
          </div>
        )}

        <div className="grid md:grid-cols-2 gap-6">
          {/* Login Form */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <LogIn className="w-5 h-5" />
              Login Test
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Username/Email</label>
                <input
                  type="text"
                  value={loginForm.username}
                  onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
                <input
                  type="password"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md"
                />
              </div>
              <button
                onClick={handleLogin}
                disabled={loading}
                className="w-full bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 disabled:opacity-50"
              >
                {loading ? 'Logging in...' : 'Test Login'}
              </button>
            </div>
          </div>

          {/* Register Form */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <UserPlus className="w-5 h-5" />
              Registration Test
            </h2>
            <div className="space-y-3 max-h-[600px] overflow-y-auto">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Email *</label>
                <input
                  type="email"
                  value={registerForm.email}
                  onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="user@example.com"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">First Name *</label>
                  <input
                    type="text"
                    value={registerForm.firstName}
                    onChange={(e) => setRegisterForm({ ...registerForm, firstName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Last Name *</label>
                  <input
                    type="text"
                    value={registerForm.lastName}
                    onChange={(e) => setRegisterForm({ ...registerForm, lastName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Password *</label>
                <input
                  type="password"
                  value={registerForm.password}
                  onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="Min 8 characters"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Phone Number *</label>
                <input
                  type="tel"
                  value={registerForm.phoneNumber}
                  onChange={(e) => setRegisterForm({ ...registerForm, phoneNumber: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="+15551234567"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">User Role *</label>
                <select
                  value={registerForm.userRole}
                  onChange={(e) => setRegisterForm({ ...registerForm, userRole: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value="ROLE_INDIVIDUAL">Individual Filer</option>
                  <option value="ROLE_BUSINESS">Business Filer</option>
                  <option value="ROLE_AUDITOR">Auditor</option>
                  <option value="ROLE_ADMIN">Admin</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Profile Type *</label>
                <select
                  value={registerForm.profileType}
                  onChange={(e) => setRegisterForm({ ...registerForm, profileType: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value="INDIVIDUAL">Individual</option>
                  <option value="BUSINESS">Business</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">SSN/EIN *</label>
                <input
                  type="text"
                  value={registerForm.ssnOrEin}
                  onChange={(e) => setRegisterForm({ ...registerForm, ssnOrEin: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="12-3456789"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Business Name</label>
                <input
                  type="text"
                  value={registerForm.businessName}
                  onChange={(e) => setRegisterForm({ ...registerForm, businessName: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="Company LLC"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Fiscal Year End</label>
                <input
                  type="text"
                  value={registerForm.fiscalYearEnd}
                  onChange={(e) => setRegisterForm({ ...registerForm, fiscalYearEnd: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  placeholder="12-31"
                />
              </div>
              <div className="border-t pt-3">
                <label className="block text-sm font-semibold text-slate-700 mb-2">Address *</label>
                <div className="space-y-2">
                  <input
                    type="text"
                    value={registerForm.address.street}
                    onChange={(e) => setRegisterForm({ ...registerForm, address: { ...registerForm.address, street: e.target.value } })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                    placeholder="Street Address"
                  />
                  <div className="grid grid-cols-3 gap-2">
                    <input
                      type="text"
                      value={registerForm.address.city}
                      onChange={(e) => setRegisterForm({ ...registerForm, address: { ...registerForm.address, city: e.target.value } })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                      placeholder="City"
                    />
                    <input
                      type="text"
                      value={registerForm.address.state}
                      onChange={(e) => setRegisterForm({ ...registerForm, address: { ...registerForm.address, state: e.target.value } })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                      placeholder="State"
                    />
                    <input
                      type="text"
                      value={registerForm.address.zip}
                      onChange={(e) => setRegisterForm({ ...registerForm, address: { ...registerForm.address, zip: e.target.value } })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                      placeholder="Zip"
                    />
                  </div>
                </div>
              </div>
              <button
                onClick={handleRegister}
                disabled={loading}
                className="w-full bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:opacity-50 mt-4"
              >
                {loading ? 'Registering...' : 'Test Registration'}
              </button>
            </div>
          </div>
        </div>

        {/* Protected Endpoint Test */}
        <div className="mt-6 bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Users className="w-5 h-5" />
            Protected Endpoint Test
          </h2>
          <p className="text-sm text-slate-600 mb-4">
            Test accessing a protected endpoint with JWT token. Login first to get a token.
          </p>
          <button
            onClick={testProtectedEndpoint}
            disabled={loading || !authToken}
            className="bg-purple-600 text-white px-6 py-2 rounded-md hover:bg-purple-700 disabled:opacity-50"
          >
            {loading ? 'Testing...' : 'Test Protected Endpoint'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default AuthServiceTestUI;
