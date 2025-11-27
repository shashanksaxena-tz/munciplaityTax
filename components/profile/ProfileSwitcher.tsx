import React, { useState, useRef, useEffect } from 'react';
import { useProfiles } from '../../contexts/ProfileContext';

export const ProfileSwitcher: React.FC = () => {
    const { profiles, activeProfile, setActiveProfile } = useProfiles();
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleProfileSwitch = (profile: any) => {
        setActiveProfile(profile);
        setIsOpen(false);
    };

    if (!activeProfile) return null;

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center space-x-3 px-4 py-2 rounded-lg hover:bg-gray-100 transition"
            >
                <div className={`flex-shrink-0 h-10 w-10 rounded-full flex items-center justify-center ${activeProfile.type === 'BUSINESS' ? 'bg-blue-100' : 'bg-purple-100'
                    }`}>
                    {activeProfile.type === 'BUSINESS' ? (
                        <svg className="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                        </svg>
                    ) : (
                        <svg className="h-6 w-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                        </svg>
                    )}
                </div>
                <div className="flex-1 text-left">
                    <p className="text-sm font-medium text-gray-900">{activeProfile.name}</p>
                    <p className="text-xs text-gray-500">
                        {activeProfile.type === 'BUSINESS' ? activeProfile.businessName : 'Individual'}
                    </p>
                </div>
                <svg className={`h-5 w-5 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
            </button>

            {isOpen && (
                <div className="absolute left-0 mt-2 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50">
                    <div className="px-4 py-2 border-b border-gray-200">
                        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Switch Profile</p>
                    </div>
                    <div className="max-h-64 overflow-y-auto">
                        {profiles.map((profile) => (
                            <button
                                key={profile.id}
                                onClick={() => handleProfileSwitch(profile)}
                                className={`w-full px-4 py-3 flex items-center space-x-3 hover:bg-gray-50 transition ${activeProfile.id === profile.id ? 'bg-indigo-50' : ''
                                    }`}
                            >
                                <div className={`flex-shrink-0 h-10 w-10 rounded-full flex items-center justify-center ${profile.type === 'BUSINESS' ? 'bg-blue-100' : 'bg-purple-100'
                                    }`}>
                                    {profile.type === 'BUSINESS' ? (
                                        <svg className="h-5 w-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                                        </svg>
                                    ) : (
                                        <svg className="h-5 w-5 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                        </svg>
                                    )}
                                </div>
                                <div className="flex-1 text-left">
                                    <p className="text-sm font-medium text-gray-900">{profile.name}</p>
                                    <p className="text-xs text-gray-500">
                                        {profile.type === 'BUSINESS' ? profile.businessName : 'Individual'}
                                    </p>
                                </div>
                                {activeProfile.id === profile.id && (
                                    <svg className="h-5 w-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                    </svg>
                                )}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};
