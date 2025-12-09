import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const UserMenu: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
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

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    if (!user) return null;

    const initials = `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() || user.email[0].toUpperCase();

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center space-x-3 focus:outline-none"
            >
                <div className="h-10 w-10 rounded-full bg-gradient-to-br from-[#970bed] to-[#469fe8] flex items-center justify-center text-white font-semibold shadow-lg">
                    {initials}
                </div>
                <div className="hidden md:block text-left">
                    <p className="text-sm font-medium text-[#0f1012]">{user.firstName} {user.lastName}</p>
                    <p className="text-xs text-[#5d6567]">{user.email}</p>
                </div>
                <svg className={`h-5 w-5 text-[#babebf] transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
            </button>

            {isOpen && (
                <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-[#f0f0f0] py-2 z-50">
                    {/* User Info */}
                    <div className="px-4 py-3 border-b border-[#f0f0f0]">
                        <p className="text-sm font-medium text-[#0f1012]">{user.firstName} {user.lastName}</p>
                        <p className="text-xs text-[#5d6567] truncate">{user.email}</p>
                        <div className="mt-2 flex flex-wrap gap-1">
                            {user.roles.map((role) => (
                                <span key={role} className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-[#970bed]/10 text-[#970bed]">
                                    {role.replace('ROLE_', '')}
                                </span>
                            ))}
                        </div>
                    </div>

                    {/* Menu Items */}
                    <div className="py-1">
                        <Link
                            to="/profiles"
                            onClick={() => setIsOpen(false)}
                            className="flex items-center px-4 py-2 text-sm text-[#5d6567] hover:bg-[#f0f0f0] hover:text-[#970bed] transition"
                        >
                            <svg className="h-5 w-5 mr-3 text-[#babebf]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                            Manage Profiles
                        </Link>

                        <Link
                            to="/settings"
                            onClick={() => setIsOpen(false)}
                            className="flex items-center px-4 py-2 text-sm text-[#5d6567] hover:bg-[#f0f0f0] hover:text-[#970bed] transition"
                        >
                            <svg className="h-5 w-5 mr-3 text-[#babebf]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            </svg>
                            Settings
                        </Link>

                        <Link
                            to="/help"
                            onClick={() => setIsOpen(false)}
                            className="flex items-center px-4 py-2 text-sm text-[#5d6567] hover:bg-[#f0f0f0] hover:text-[#970bed] transition"
                        >
                            <svg className="h-5 w-5 mr-3 text-[#babebf]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            Help & Support
                        </Link>
                    </div>

                    {/* Logout */}
                    <div className="border-t border-[#f0f0f0] pt-1">
                        <button
                            onClick={handleLogout}
                            className="flex items-center w-full px-4 py-2 text-sm text-[#ec1656] hover:bg-[#ec1656]/10 transition"
                        >
                            <svg className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                            </svg>
                            Sign Out
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};
