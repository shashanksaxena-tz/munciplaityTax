/**
 * Extraction Service Standalone Test UI
 * Tests extraction-service endpoints independently
 */

import React, { useState, useEffect } from 'react';
import { FileSearch, Upload, RefreshCw, CheckCircle, XCircle, Loader } from 'lucide-react';

const API_BASE = 'http://localhost:8083/api/v1/extraction';

interface ExtractionResult {
  documentType: string;
  extractedData: Record<string, any>;
  confidence: number;
  processingTime: number;
}

export const ExtractionServiceTestUI: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [testResult, setTestResult] = useState<string>('');
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('testing');
  const [extractionResult, setExtractionResult] = useState<ExtractionResult | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');

  useEffect(() => {
    testConnection();
  }, []);

  const testConnection = async () => {
    setConnectionStatus('testing');
    try {
      const response = await fetch(`${API_BASE}/health`, {
        method: 'GET',
      });
      
      // Even if health endpoint doesn't exist, if we get a response, service is up
      if (response.status < 500) {
        setConnectionStatus('connected');
        setTestResult('✅ Successfully connected to Extraction Service');
      } else {
        setConnectionStatus('disconnected');
        setTestResult(`❌ Connection failed: ${response.statusText}`);
      }
    } catch (err) {
      setConnectionStatus('disconnected');
      setTestResult(`❌ Connection failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError('');
      
      // Create preview for images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          setPreviewUrl(e.target?.result as string);
        };
        reader.readAsDataURL(file);
      } else if (file.type === 'application/pdf') {
        setPreviewUrl('pdf');
      }
    }
  };

  const extractDocument = async () => {
    if (!selectedFile) {
      setError('Please select a file first');
      return;
    }

    setLoading(true);
    setError('');
    setExtractionResult(null);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await fetch(`${API_BASE}/extract`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      const data = await response.json();
      setExtractionResult(data);
      setTestResult('✅ Document extracted successfully');
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Extraction failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  const testWithSampleData = async () => {
    setLoading(true);
    setError('');
    setExtractionResult(null);

    try {
      // Test the streaming endpoint with sample data
      const response = await fetch(`${API_BASE}/stream?fileName=sample-w2.pdf`, {
        method: 'GET',
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      // Read the stream
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let result = '';
      
      if (reader) {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          result += decoder.decode(value, { stream: true });
        }
      }

      // Parse the last data line which should have the complete result
      const lines = result.trim().split('\n').filter(line => line.startsWith('data:'));
      if (lines.length > 0) {
        const lastLine = lines[lines.length - 1].substring(5); // Remove 'data:' prefix
        const data = JSON.parse(lastLine);
        setExtractionResult({
          documentType: data.detectedForms?.join(', ') || 'Unknown',
          extractedData: data.result || {},
          confidence: data.confidence || 0,
          processingTime: 0
        });
        setTestResult(`✅ Test extraction completed: ${data.status} (${Math.round(data.confidence * 100)}% confidence)`);
      } else {
        setTestResult('✅ Test extraction completed successfully');
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Unknown error';
      setError(errorMsg);
      setTestResult(`❌ Test extraction failed: ${errorMsg}`);
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
              <FileSearch className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-2xl font-bold text-slate-900">Extraction Service Test UI</h1>
                <p className="text-sm text-slate-600">Standalone testing interface for extraction-service</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
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
                onClick={testConnection}
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
            <p><strong>Service URL:</strong> {API_BASE}</p>
            <p><strong>Port:</strong> 8083</p>
            <p><strong>Profile:</strong> standalone</p>
            <p><strong>Features:</strong> AI-powered document extraction using Gemini</p>
            <p><strong>Supported Docs:</strong> W-2, 1099, Business Forms, Tax Returns</p>
          </div>
        </div>

        {/* Upload Section */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <Upload className="w-5 h-5" />
            Upload Document
          </h2>
          
          <div className="border-2 border-dashed border-slate-300 rounded-lg p-8 text-center">
            <input
              type="file"
              onChange={handleFileSelect}
              accept=".pdf,.png,.jpg,.jpeg"
              className="hidden"
              id="file-upload"
            />
            <label htmlFor="file-upload" className="cursor-pointer">
              <Upload className="w-12 h-12 text-slate-400 mx-auto mb-4" />
              <p className="text-slate-600 mb-2">
                {selectedFile ? selectedFile.name : 'Click to upload or drag and drop'}
              </p>
              <p className="text-sm text-slate-500">PDF, PNG, JPG up to 10MB</p>
            </label>
          </div>

          {previewUrl && previewUrl !== 'pdf' && (
            <div className="mt-4">
              <img src={previewUrl} alt="Preview" className="max-w-full h-auto max-h-64 mx-auto rounded-lg" />
            </div>
          )}

          <div className="flex gap-4 mt-4">
            <button
              onClick={extractDocument}
              disabled={!selectedFile || loading}
              className="flex-1 bg-indigo-600 text-white px-6 py-3 rounded-md hover:bg-indigo-700 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader className="w-5 h-5 animate-spin" />
                  Extracting...
                </>
              ) : (
                <>
                  <FileSearch className="w-5 h-5" />
                  Extract Document
                </>
              )}
            </button>
            <button
              onClick={testWithSampleData}
              disabled={loading}
              className="flex-1 bg-slate-600 text-white px-6 py-3 rounded-md hover:bg-slate-700 disabled:opacity-50"
            >
              Test with Sample Data
            </button>
          </div>
        </div>

        {/* Extraction Results */}
        {extractionResult && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <CheckCircle className="w-5 h-5 text-green-600" />
              Extraction Results
            </h2>

            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div className="bg-slate-50 p-4 rounded-lg">
                  <p className="text-sm text-slate-600 mb-1">Document Type</p>
                  <p className="text-lg font-semibold text-slate-900">{extractionResult.documentType}</p>
                </div>
                <div className="bg-slate-50 p-4 rounded-lg">
                  <p className="text-sm text-slate-600 mb-1">Confidence</p>
                  <p className="text-lg font-semibold text-slate-900">
                    {(extractionResult.confidence * 100).toFixed(1)}%
                  </p>
                </div>
                <div className="bg-slate-50 p-4 rounded-lg">
                  <p className="text-sm text-slate-600 mb-1">Processing Time</p>
                  <p className="text-lg font-semibold text-slate-900">
                    {extractionResult.processingTime}ms
                  </p>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded-lg">
                <p className="text-sm font-medium text-slate-700 mb-2">Extracted Data</p>
                <pre className="text-xs text-slate-900 overflow-x-auto">
                  {JSON.stringify(extractionResult.extractedData, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ExtractionServiceTestUI;
