import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Palette } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { ProfileSwitcher } from './profile/ProfileSwitcher';
import { UserMenu } from './UserMenu';
import { DesignConfigurator } from './DesignConfigurator';

export const Header: React.FC = () => {
    const { isAuthenticated, user, logout } = useAuth();
    const [showDesignConfigurator, setShowDesignConfigurator] = useState(false);

    return (
        <header className="bg-white shadow-sm border-b border-[#f0f0f0]">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center h-16">
                    {/* Logo */}
                    <div className="flex items-center">
                        <Link to="/" className="flex items-center space-x-3">
                            <div className="h-10 w-10 bg-gradient-to-br from-[#970bed] to-[#469fe8] rounded-lg flex items-center justify-center">
                                <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                            </div>
                            <div>
                                <h1 className="text-xl font-bold text-[#0f1012]">MuniTax</h1>
                                <p className="text-xs text-[#5d6567]">Dublin Tax Filing</p>
                            </div>
                        </Link>
                    </div>

                    {/* Navigation */}
                    {isAuthenticated && user && (
                        <nav className="hidden md:flex items-center space-x-8">
                            {user.roles.includes('ROLE_INDIVIDUAL') && (
                                <Link to="/filer" className="text-sm font-medium text-[#5d6567] hover:text-[#970bed] transition">
                                    File Return
                                </Link>
                            )}
                            {user.roles.includes('ROLE_BUSINESS') && (
                                <Link to="/business" className="text-sm font-medium text-[#5d6567] hover:text-[#970bed] transition">
                                    Business Filing
                                </Link>
                            )}
                            {user.roles.includes('ROLE_AUDITOR') && (
                                <Link to="/auditor" className="text-sm font-medium text-[#5d6567] hover:text-[#970bed] transition">
                                    Review Queue
                                </Link>
                            )}
                            {user.roles.includes('ROLE_ADMIN') && (
                                <Link to="/admin/rules" className="text-sm font-medium text-[#5d6567] hover:text-[#970bed] transition">
                                    Rule Management
                                </Link>
                            )}
                        </nav>
                    )}

                    {/* Right Side */}
                    <div className="flex items-center space-x-4">
                        {isAuthenticated ? (
                            <>
                                {/* Design Configurator Button - visible to all logged in users */}
                                <button
                                    onClick={() => setShowDesignConfigurator(true)}
                                    className="p-2 text-[#469fe8] hover:text-[#970bed] hover:bg-[#ebf4ff] rounded-lg transition-colors"
                                    title="Customize Theme"
                                >
                                    <Palette className="h-5 w-5" />
                                </button>

                                {/* Profile Switcher */}
                                <ProfileSwitcher />

                                {/* Notifications */}
                                <button className="relative p-2 text-[#babebf] hover:text-[#5d6567] transition">
                                    <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                                    </svg>
                                    <span className="absolute top-1 right-1 h-2 w-2 bg-[#ec1656] rounded-full"></span>
                                </button>

                                {/* User Menu */}
                                <UserMenu />
                                
                                {/* Logout Button */}
                                <button
                                    onClick={() => {
                                        if (window.confirm('Are you sure you want to logout?')) {
                                            const { logout } = require('../contexts/AuthContext');
                                            logout();
                                            window.location.href = '/login';
                                        }
                                    }}
                                    className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-red-600 hover:bg-red-50 rounded-lg transition flex items-center gap-2"
                                >
                                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                    </svg>
                                    Logout
                                </button>
                            </>
                        ) : (
                            <div className="flex items-center space-x-4">
                                <Link
                                    to="/login"
                                    className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition"
                                >
                                    Sign In
                                </Link>
                                <Link
                                    to="/register"
                                    className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition"
                                >
                                    Register
                                </Link>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Design Configurator Modal */}
            {showDesignConfigurator && (
                <DesignConfigurator onClose={() => setShowDesignConfigurator(false)} />
            )}
        </header>
    );
};
