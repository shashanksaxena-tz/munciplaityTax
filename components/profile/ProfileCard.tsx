import React, { useState } from 'react';
import { useProfiles } from '../../contexts/ProfileContext';
import { EditProfileModal } from './EditProfileModal';

interface Profile {
    id: string;
    userId: string;
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
        country: string;
    };
    relationshipToUser?: string;
    isPrimary: boolean;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

interface ProfileCardProps {
    profile: Profile;
}

export const ProfileCard: React.FC<ProfileCardProps> = ({ profile }) => {
    const { activeProfile, setActiveProfile, deleteProfile } = useProfiles();
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    const isActive = activeProfile?.id === profile.id;

    const handleSetActive = () => {
        setActiveProfile(profile);
    };

    const handleDelete = async () => {
        if (profile.isPrimary) {
            alert('Cannot delete primary profile');
            return;
        }

        setIsDeleting(true);
        try {
            await deleteProfile(profile.id);
            setShowDeleteConfirm(false);
        } catch (error) {
            alert('Failed to delete profile');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <>
            <div className={`bg-white rounded-lg shadow-md overflow-hidden transition-all duration-200 hover:shadow-xl ${isActive ? 'ring-2 ring-indigo-600' : ''
                }`}>
                {/* Card Header */}
                <div className={`px-6 py-4 ${profile.type === 'BUSINESS' ? 'bg-gradient-to-r from-blue-500 to-blue-600' : 'bg-gradient-to-r from-purple-500 to-purple-600'
                    }`}>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                            <div className="flex-shrink-0">
                                {profile.type === 'BUSINESS' ? (
                                    <svg className="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                    </svg>
                                ) : (
                                    <svg className="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                    </svg>
                                )}
                            </div>
                            <div>
                                <h3 className="text-lg font-semibold text-white">{profile.name}</h3>
                                <p className="text-xs text-white opacity-90">
                                    {profile.type === 'BUSINESS' ? 'Business Profile' : 'Individual Profile'}
                                </p>
                            </div>
                        </div>
                        {profile.isPrimary && (
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-white text-indigo-600">
                                Primary
                            </span>
                        )}
                    </div>
                </div>

                {/* Card Body */}
                <div className="px-6 py-4 space-y-3">
                    {profile.type === 'BUSINESS' && profile.businessName && (
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wide">Business Name</p>
                            <p className="text-sm font-medium text-gray-900">{profile.businessName}</p>
                        </div>
                    )}

                    <div>
                        <p className="text-xs text-gray-500 uppercase tracking-wide">
                            {profile.type === 'BUSINESS' ? 'EIN' : 'SSN'}
                        </p>
                        <p className="text-sm font-medium text-gray-900">
                            {profile.ssnOrEin.replace(/./g, '•').slice(0, -4) + profile.ssnOrEin.slice(-4)}
                        </p>
                    </div>

                    {profile.relationshipToUser && (
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wide">Relationship</p>
                            <p className="text-sm font-medium text-gray-900">{profile.relationshipToUser}</p>
                        </div>
                    )}

                    <div>
                        <p className="text-xs text-gray-500 uppercase tracking-wide">Address</p>
                        <p className="text-sm text-gray-900">
                            {profile.address.street}<br />
                            {profile.address.city}, {profile.address.state} {profile.address.zip}
                        </p>
                    </div>
                </div>

                {/* Card Footer */}
                <div className="px-6 py-4 bg-[#f8f9fa] border-t border-[#dcdede] flex items-center justify-between">
                    {!isActive ? (
                        <button
                            onClick={handleSetActive}
                            className="text-sm font-medium text-[#469fe8] hover:text-[#970bed] transition"
                        >
                            Set as Active
                        </button>
                    ) : (
                        <span className="inline-flex items-center text-sm font-medium text-[#10b981]">
                            <svg className="h-4 w-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                            </svg>
                            Active
                        </span>
                    )}

                    <div className="flex space-x-2">
                        <button
                            onClick={() => setIsEditModalOpen(true)}
                            className="p-2 text-[#babebf] hover:text-[#5d6567] transition"
                            title="Edit"
                        >
                            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                        </button>

                        {!profile.isPrimary && (
                            <button
                                onClick={() => setShowDeleteConfirm(true)}
                                className="p-2 text-[#babebf] hover:text-[#ec1656] transition"
                                title="Delete"
                            >
                                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Edit Modal */}
            <EditProfileModal
                isOpen={isEditModalOpen}
                onClose={() => setIsEditModalOpen(false)}
                profile={profile}
            />

            {/* Delete Confirmation Modal */}
            {showDeleteConfirm && (
                <div className="fixed inset-0 z-50 overflow-y-auto">
                    <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
                        <div className="fixed inset-0 transition-opacity bg-black/50 backdrop-blur-sm" onClick={() => setShowDeleteConfirm(false)} />

                        <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full border border-[#dcdede]">
                            <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                                <div className="sm:flex sm:items-start">
                                    <div className="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-[#fff5f5] sm:mx-0 sm:h-10 sm:w-10">
                                        <svg className="h-6 w-6 text-[#ec1656]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                        </svg>
                                    </div>
                                    <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                                        <h3 className="text-lg leading-6 font-medium text-[#0f1012]">Delete Profile</h3>
                                        <div className="mt-2">
                                            <p className="text-sm text-[#5d6567]">
                                                Are you sure you want to delete the profile for <strong>{profile.name}</strong>? This action cannot be undone.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                                <button
                                    onClick={handleDelete}
                                    disabled={isDeleting}
                                    className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50"
                                >
                                    {isDeleting ? 'Deleting...' : 'Delete'}
                                </button>
                                <button
                                    onClick={() => setShowDeleteConfirm(false)}
                                    className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};
