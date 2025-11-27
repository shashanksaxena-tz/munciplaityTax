import React, { useState } from 'react';
import { useProfiles } from '../../contexts/ProfileContext';

interface CreateProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const CreateProfileModal: React.FC<CreateProfileModalProps> = ({ isOpen, onClose }) => {
    const { createProfile } = useProfiles();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [formData, setFormData] = useState({
        type: 'INDIVIDUAL' as 'INDIVIDUAL' | 'BUSINESS',
        name: '',
        ssnOrEin: '',
        businessName: '',
        fiscalYearEnd: '',
        relationshipToUser: 'Self',
        street: '',
        city: '',
        state: 'OH',
        zip: ''
    });

    const updateField = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await createProfile({
                type: formData.type,
                name: formData.name,
                ssnOrEin: formData.ssnOrEin,
                businessName: formData.type === 'BUSINESS' ? formData.businessName : undefined,
                fiscalYearEnd: formData.type === 'BUSINESS' ? formData.fiscalYearEnd : undefined,
                relationshipToUser: formData.relationshipToUser,
                address: {
                    street: formData.street,
                    city: formData.city,
                    state: formData.state,
                    zip: formData.zip,
                    country: 'USA'
                }
            });

            // Reset form and close
            setFormData({
                type: 'INDIVIDUAL',
                name: '',
                ssnOrEin: '',
                businessName: '',
                fiscalYearEnd: '',
                relationshipToUser: 'Self',
                street: '',
                city: '',
                state: 'OH',
                zip: ''
            });
            onClose();
        } catch (err: any) {
            setError(err.message || 'Failed to create profile');
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto">
            <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
                {/* Backdrop */}
                <div className="fixed inset-0 transition-opacity bg-gray-500 bg-opacity-75" onClick={onClose} />

                {/* Modal */}
                <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
                    <form onSubmit={handleSubmit}>
                        {/* Header */}
                        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-4">
                            <h3 className="text-lg font-semibold text-white">Create New Profile</h3>
                        </div>

                        {/* Body */}
                        <div className="bg-white px-6 py-4 space-y-4 max-h-[70vh] overflow-y-auto">
                            {error && (
                                <div className="rounded-md bg-red-50 p-4 border border-red-200">
                                    <p className="text-sm font-medium text-red-800">{error}</p>
                                </div>
                            )}

                            {/* Profile Type */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Profile Type *</label>
                                <div className="grid grid-cols-2 gap-4">
                                    <button
                                        type="button"
                                        onClick={() => updateField('type', 'INDIVIDUAL')}
                                        className={`p-4 border-2 rounded-lg transition ${formData.type === 'INDIVIDUAL'
                                                ? 'border-indigo-600 bg-indigo-50'
                                                : 'border-gray-300 hover:border-gray-400'
                                            }`}
                                    >
                                        <svg className={`h-8 w-8 mx-auto mb-2 ${formData.type === 'INDIVIDUAL' ? 'text-indigo-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                        </svg>
                                        <p className={`text-sm font-medium ${formData.type === 'INDIVIDUAL' ? 'text-indigo-900' : 'text-gray-700'}`}>Individual</p>
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => updateField('type', 'BUSINESS')}
                                        className={`p-4 border-2 rounded-lg transition ${formData.type === 'BUSINESS'
                                                ? 'border-indigo-600 bg-indigo-50'
                                                : 'border-gray-300 hover:border-gray-400'
                                            }`}
                                    >
                                        <svg className={`h-8 w-8 mx-auto mb-2 ${formData.type === 'BUSINESS' ? 'text-indigo-600' : 'text-gray-400'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                        </svg>
                                        <p className={`text-sm font-medium ${formData.type === 'BUSINESS' ? 'text-indigo-900' : 'text-gray-700'}`}>Business</p>
                                    </button>
                                </div>
                            </div>

                            {/* Name */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {formData.type === 'BUSINESS' ? 'Contact Name' : 'Full Name'} *
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.name}
                                    onChange={(e) => updateField('name', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="John Doe"
                                />
                            </div>

                            {/* SSN/EIN */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {formData.type === 'BUSINESS' ? 'EIN' : 'SSN'} *
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.ssnOrEin}
                                    onChange={(e) => updateField('ssnOrEin', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder={formData.type === 'BUSINESS' ? '12-3456789' : '123-45-6789'}
                                />
                            </div>

                            {/* Business Fields */}
                            {formData.type === 'BUSINESS' && (
                                <>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Business Name *</label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.businessName}
                                            onChange={(e) => updateField('businessName', e.target.value)}
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            placeholder="Acme Corporation"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Fiscal Year End</label>
                                        <input
                                            type="text"
                                            value={formData.fiscalYearEnd}
                                            onChange={(e) => updateField('fiscalYearEnd', e.target.value)}
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            placeholder="12/31"
                                        />
                                    </div>
                                </>
                            )}

                            {/* Relationship */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Relationship *</label>
                                <select
                                    value={formData.relationshipToUser}
                                    onChange={(e) => updateField('relationshipToUser', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                >
                                    <option value="Self">Self</option>
                                    <option value="Spouse">Spouse</option>
                                    <option value="Dependent">Dependent</option>
                                    <option value="Employee">Employee</option>
                                    <option value="Other">Other</option>
                                </select>
                            </div>

                            {/* Address */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Street Address *</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.street}
                                    onChange={(e) => updateField('street', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="123 Main St"
                                />
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">City *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.city}
                                        onChange={(e) => updateField('city', e.target.value)}
                                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                        placeholder="Dublin"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">State *</label>
                                    <select
                                        value={formData.state}
                                        onChange={(e) => updateField('state', e.target.value)}
                                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    >
                                        <option value="OH">OH</option>
                                    </select>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">ZIP Code *</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.zip}
                                    onChange={(e) => updateField('zip', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="43016"
                                />
                            </div>
                        </div>

                        {/* Footer */}
                        <div className="bg-gray-50 px-6 py-4 flex justify-end space-x-3">
                            <button
                                type="button"
                                onClick={onClose}
                                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition flex items-center"
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        Creating...
                                    </>
                                ) : (
                                    'Create Profile'
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};
