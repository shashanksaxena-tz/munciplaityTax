import React, { useState, useEffect } from 'react';
import { getFormSchema, getAllFields, FormFieldSchema } from '../services/formSchemaService';
import { FileText, Settings, CheckCircle2, AlertCircle } from 'lucide-react';

/**
 * FormSchemaViewer - Interactive UI to browse and test form schemas
 * Shows all available form types and their field definitions
 */
export const FormSchemaViewer: React.FC = () => {
  const [selectedFormType, setSelectedFormType] = useState<string>('W-2');
  const [fields, setFields] = useState<FormFieldSchema[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    loadFormFields();
  }, [selectedFormType]);

  const loadFormFields = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const schema = await getFormSchema(selectedFormType);
      if (schema) {
        const allFields = await getAllFields(selectedFormType);
        setFields(allFields);
      } else {
        setError(`Schema not found for ${selectedFormType}`);
        setFields([]);
      }
    } catch (err) {
      setError(`Error loading schema: ${err}`);
      setFields([]);
    } finally {
      setIsLoading(false);
    }
  };

  const getWeightColor = (weight: string) => {
    switch (weight) {
      case 'CRITICAL': return 'bg-[#ec1656]/10 text-[#ec1656] border-[#ec1656]/20';
      case 'HIGH': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'MEDIUM': return 'bg-[#ebf4ff] text-[#469fe8] border-[#469fe8]/20';
      case 'LOW': return 'bg-[#f0f0f0] text-[#102124] border-[#dcdede]';
      default: return 'bg-[#f0f0f0] text-[#102124] border-[#dcdede]';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'currency': return '💵';
      case 'percentage': return '%';
      case 'date': return '📅';
      case 'number': return '#';
      default: return 'T';
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-6">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <Settings className="w-8 h-8 text-[#970bed]" />
          <h1 className="text-3xl font-bold text-[#0f1012]">Form Schema Configuration</h1>
        </div>
        <p className="text-[#5d6567]">
          Single source of truth for form field definitions across UI and extraction service
        </p>
      </div>

      {/* Form Type Selector */}
      <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-6 mb-6">
        <label className="block text-sm font-medium text-[#102124] mb-3">
          Select Form Type
        </label>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
          {formTypes.map(formType => (
            <button
              key={formType}
              onClick={() => setSelectedFormType(formType)}
              className={`px-4 py-3 rounded-lg font-medium transition-all ${
                selectedFormType === formType
                  ? 'bg-gradient-to-r from-[#970bed] to-[#469fe8] text-white shadow-md'
                  : 'bg-[#f8f9fa] text-[#102124] hover:bg-[#f0f0f0] border border-[#dcdede]'
              }`}
            >
              {formType}
            </button>
          ))}
        </div>
      </div>

      {/* Stats Summary */}
      {!isLoading && fields.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-4">
            <div className="text-sm text-[#5d6567] mb-1">Total Fields</div>
            <div className="text-2xl font-bold text-[#0f1012]">{fields.length}</div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-4">
            <div className="text-sm text-[#5d6567] mb-1">Display in UI</div>
            <div className="text-2xl font-bold text-[#970bed]">
              {fields.filter(f => f.displayInUI).length}
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-4">
            <div className="text-sm text-[#5d6567] mb-1">Required Fields</div>
            <div className="text-2xl font-bold text-[#ec1656]">
              {fields.filter(f => f.required).length}
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-4">
            <div className="text-sm text-[#5d6567] mb-1">Critical Weight</div>
            <div className="text-2xl font-bold text-orange-600">
              {fields.filter(f => f.weight === 'CRITICAL').length}
            </div>
          </div>
        </div>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] p-12 text-center">
          <div className="animate-pulse text-[#babebf] mb-2">Loading schema...</div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-[#ec1656]/10 border border-[#ec1656]/20 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-[#ec1656] flex-shrink-0" />
          <div className="text-[#ec1656]">{error}</div>
        </div>
      )}

      {/* Fields Table */}
      {!isLoading && !error && fields.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border border-[#dcdede] overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-[#f8f9fa] border-b border-[#dcdede]">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Order
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Field ID
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Label
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Box/Line
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Weight
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Required
                  </th>
                  <th className="px-4 py-3 text-center text-xs font-medium text-[#102124] uppercase tracking-wider">
                    Display
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {fields.map((field, index) => (
                  <tr
                    key={field.id}
                    className={index % 2 === 0 ? 'bg-white' : 'bg-[#f8f9fa]'}
                  >
                    <td className="px-4 py-3 text-sm text-[#5d6567]">
                      {field.displayOrder}
                    </td>
                    <td className="px-4 py-3">
                      <code className="text-sm font-mono text-[#970bed] bg-[#ebf4ff] px-2 py-1 rounded">
                        {field.id}
                      </code>
                    </td>
                    <td className="px-4 py-3 text-sm font-medium text-[#0f1012]">
                      {field.label}
                    </td>
                    <td className="px-4 py-3 text-sm text-[#5d6567]">
                      {field.boxNumber || field.lineNumber || '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center gap-1 text-sm text-[#102124]">
                        <span className="text-base">{getTypeIcon(field.type)}</span>
                        {field.type}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block px-2 py-1 text-xs font-medium rounded-full border ${getWeightColor(field.weight)}`}>
                        {field.weight}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      {field.required ? (
                        <CheckCircle2 className="w-5 h-5 text-[#10b981] mx-auto" />
                      ) : (
                        <span className="text-gray-300">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center">
                      {field.displayInUI ? (
                        <CheckCircle2 className="w-5 h-5 text-[#469fe8] mx-auto" />
                      ) : (
                        <span className="text-gray-300">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Legend */}
      {!isLoading && fields.length > 0 && (
        <div className="mt-6 bg-[#f8f9fa] rounded-lg border border-[#dcdede] p-4">
          <div className="text-sm font-medium text-[#102124] mb-3">Legend</div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-[#5d6567]">
            <div>
              <span className="font-medium">Display in UI:</span> Field is shown in extraction review panel
            </div>
            <div>
              <span className="font-medium">Required:</span> Field must have a value
            </div>
            <div>
              <span className="font-medium">Weight:</span> Importance for extraction confidence scoring
            </div>
            <div>
              <span className="font-medium">Type:</span> Data type and formatting rules
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FormSchemaViewer;
