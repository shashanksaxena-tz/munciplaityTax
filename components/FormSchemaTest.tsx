import React, { useEffect, useState } from 'react';
import { getDisplayFields } from '../services/formSchemaService';

/**
 * Test component to verify form schema loading for all form types
 */
export function FormSchemaTest() {
  const [results, setResults] = useState<Record<string, { success: boolean; fieldCount: number; error?: string }>>({});
  
  const formTypes = [
    'W-2',
    'Federal 1040',
    'Schedule C',
    '1099-NEC',
    '1099-MISC',
    'W-2G',
    'Schedule E'
  ];

  useEffect(() => {
    async function testAllForms() {
      const testResults: Record<string, { success: boolean; fieldCount: number; error?: string }> = {};
      
      for (const formType of formTypes) {
        try {
          console.log(`Testing schema load for: ${formType}`);
          const fields = await getDisplayFields(formType);
          testResults[formType] = {
            success: fields.length > 0,
            fieldCount: fields.length,
          };
          console.log(`✓ ${formType}: ${fields.length} fields`);
        } catch (error) {
          testResults[formType] = {
            success: false,
            fieldCount: 0,
            error: error instanceof Error ? error.message : String(error),
          };
          console.error(`✗ ${formType}:`, error);
        }
      }
      
      setResults(testResults);
    }
    
    testAllForms();
  }, []);

  return (
    <div className="max-w-4xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Form Schema Loading Test</h1>
      
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Form Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Fields Loaded
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Error
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {formTypes.map(formType => {
              const result = results[formType];
              if (!result) {
                return (
                  <tr key={formType}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {formType}
                    </td>
                    <td colSpan={3} className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      Loading...
                    </td>
                  </tr>
                );
              }
              
              return (
                <tr key={formType} className={result.success ? 'bg-green-50' : 'bg-red-50'}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {formType}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {result.success ? (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                        ✓ Success
                      </span>
                    ) : (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                        ✗ Failed
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {result.fieldCount}
                  </td>
                  <td className="px-6 py-4 text-sm text-red-600">
                    {result.error || '—'}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      
      <div className="mt-6 p-4 bg-blue-50 rounded-lg">
        <p className="text-sm text-blue-800">
          <strong>Check the browser console for detailed logging.</strong>
          <br />
          Each form type should load its schema and display the configured fields.
        </p>
      </div>
    </div>
  );
}
