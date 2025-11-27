import React, { useState, useEffect } from 'react';
import { useProfiles } from '../../contexts/ProfileContext';

interface Profile {
    id: string;
    type: 'INDIVIDUAL' | 'BUSINESS';
    name: string;
    ssnOrEin: string;
    businessName?: string;
    fiscalYearEnd?: string;
    address: {
        street: string;
        city: string;
        state: string;
        zip: string;
    };
}

interface EditProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
    profile: Profile;
}

export const EditProfileModal: React.FC<EditProfileModalProps> = ({ isOpen, onClose, profile }) => {
    const { updateProfile } = useProfiles();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [formData, setFormData] = useState({
        name: profile.name,
        ssnOrEin: profile.ssnOrEin,
        businessName: profile.businessName || '',
        fiscalYearEnd: profile.fiscalYearEnd || '',
        street: profile.address.street,
        city: profile.address.city,
        state: profile.address.state,
        zip: profile.address.zip
    });

    useEffect(() => {
        setFormData({
            name: profile.name,
            ssnOrEin: profile.ssnOrEin,
            businessName: profile.businessName || '',
            fiscalYearEnd: profile.fiscalYearEnd || '',
            street: profile.address.street,
            city: profile.address.city,
            state: profile.address.state,
            zip: profile.address.zip
        });
    }, [profile]);

    const updateField = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await updateProfile(profile.id, {
                name: formData.name,
                ssnOrEin: formData.ssnOrEin,
                businessName: profile.type === 'BUSINESS' ? formData.businessName : undefined,
                fiscalYearEnd: profile.type === 'BUSINESS' ? formData.fiscalYearEnd : undefined,
                address: {
                    street: formData.street,
                    city: formData.city,
                    state: formData.state,
                    zip: formData.zip,
                    country: 'USA'
                }
            });

            onClose();
        } catch (err: any) {
            setError(err.message || 'Failed to update profile');
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
                        <div className={`px-6 py-4 ${profile.type === 'BUSINESS'
                                ? 'bg-gradient-to-r from-blue-600 to-blue-700'
                                : 'bg-gradient-to-r from-purple-600 to-purple-700'
                            }`}>
                            <h3 className="text-lg font-semibold text-white">Edit Profile</h3>
                            <p className="text-sm text-white opacity-90 mt-1">
                                {profile.type === 'BUSINESS' ? 'Business Profile' : 'Individual Profile'}
                            </p>
                        </div>

                        {/* Body */}
                        <div className="bg-white px-6 py-4 space-y-4 max-h-[70vh] overflow-y-auto">
                            {error && (
                                <div className="rounded-md bg-red-50 p-4 border border-red-200">
                                    <p className="text-sm font-medium text-red-800">{error}</p>
                                </div>
                            )}

                            {/* Name */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {profile.type === 'BUSINESS' ? 'Contact Name' : 'Full Name'} *
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.name}
                                    onChange={(e) => updateField('name', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                />
                            </div>

                            {/* SSN/EIN */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    {profile.type === 'BUSINESS' ? 'EIN' : 'SSN'} *
                                </label>
                                <input
                                    type="text"
                                    required
                                    value={formData.ssnOrEin}
                                    onChange={(e) => updateField('ssnOrEin', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                />
                            </div>

                            {/* Business Fields */}
                            {profile.type === 'BUSINESS' && (
                                <>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Business Name *</label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.businessName}
                                            onChange={(e) => updateField('businessName', e.target.value)}
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
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

                            {/* Address */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Street Address *</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.street}
                                    onChange={(e) => updateField('street', e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
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
                                        Updating...
                                    </>
                                ) : (
                                    'Update Profile'
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};
