import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';

export const ResetPassword: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [showPassword, setShowPassword] = useState(false);

    const token = searchParams.get('token');

    const getPasswordStrength = (pwd: string): { strength: number; label: string; color: string } => {
        let strength = 0;
        if (pwd.length >= 8) strength++;
        if (/[a-z]/.test(pwd)) strength++;
        if (/[A-Z]/.test(pwd)) strength++;
        if (/[0-9]/.test(pwd)) strength++;
        if (/[^a-zA-Z0-9]/.test(pwd)) strength++;

        const labels = ['Very Weak', 'Weak', 'Fair', 'Good', 'Strong'];
        const colors = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-blue-500', 'bg-green-500'];

        return { strength, label: labels[strength - 1] || 'Very Weak', color: colors[strength - 1] || 'bg-red-500' };
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!token) {
            setError('Invalid reset link');
            return;
        }

        if (password.length < 8) {
            setError('Password must be at least 8 characters long');
            return;
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetch('/api/v1/users/reset-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ token, newPassword: password })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                // Show success and redirect to login
                navigate('/login', { state: { message: 'Password reset successful. Please log in.' } });
            } else {
                setError(data.message || 'Failed to reset password');
            }
        } catch (err) {
            setError('An error occurred. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    const passwordStrength = getPasswordStrength(password);

    if (!token) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#ebf4ff] to-white py-12 px-4">
                <div className="max-w-md w-full bg-white p-10 rounded-2xl shadow-2xl text-center border border-[#f0f0f0]">
                    <div className="flex justify-center mb-6">
                        <div className="rounded-full bg-[#ec1656]/10 p-4">
                            <svg className="h-16 w-16 text-[#ec1656]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </div>
                    </div>
                    <h2 className="text-2xl font-bold text-[#0f1012] mb-2">Invalid Reset Link</h2>
                    <p className="text-[#5d6567] mb-6">This password reset link is invalid or has expired.</p>
                    <Link
                        to="/forgot-password"
                        className="inline-block px-6 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg transition"
                    >
                        Request New Link
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#ebf4ff] to-white py-12 px-4">
            <div className="max-w-md w-full bg-white p-10 rounded-2xl shadow-2xl border border-[#f0f0f0]">
                <div className="text-center mb-8">
                    <h2 className="text-3xl font-extrabold text-[#0f1012]">Reset Password</h2>
                    <p className="mt-2 text-sm text-[#5d6567]">
                        Enter your new password below
                    </p>
                </div>

                {error && (
                    <div className="rounded-md bg-[#ec1656]/10 p-4 border border-[#ec1656]/30 mb-6">
                        <p className="text-sm font-medium text-[#ec1656]">{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label htmlFor="password" className="block text-sm font-medium text-[#102124] mb-1">
                            New Password
                        </label>
                        <div className="relative">
                            <input
                                id="password"
                                type={showPassword ? 'text' : 'password'}
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full px-4 py-3 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed] pr-10"
                                placeholder="••••••••"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                            >
                                {showPassword ? (
                                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                                    </svg>
                                ) : (
                                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                    </svg>
                                )}
                            </button>
                        </div>
                        {password && (
                            <div className="mt-2">
                                <div className="flex items-center justify-between text-xs text-[#5d6567] mb-1">
                                    <span>Password Strength:</span>
                                    <span className="font-semibold">{passwordStrength.label}</span>
                                </div>
                                <div className="w-full bg-[#f0f0f0] rounded-full h-2">
                                    <div
                                        className={`h-2 rounded-full transition-all ${passwordStrength.color}`}
                                        style={{ width: `${(passwordStrength.strength / 5) * 100}%` }}
                                    />
                                </div>
                            </div>
                        )}
                    </div>

                    <div>
                        <label htmlFor="confirmPassword" className="block text-sm font-medium text-[#102124] mb-1">
                            Confirm New Password
                        </label>
                        <input
                            id="confirmPassword"
                            type="password"
                            required
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="w-full px-4 py-3 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed]"
                            placeholder="••••••••"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg text-white bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#970bed] disabled:opacity-50 transition"
                    >
                        {isLoading ? (
                            <>
                                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                Resetting...
                            </>
                        ) : (
                            'Reset Password'
                        )}
                    </button>

                    <div className="text-center">
                        <Link to="/login" className="text-sm font-medium text-indigo-600 hover:text-indigo-500 transition">
                            Back to Login
                        </Link>
                    </div>
                </form>
            </div>
        </div>
    );
};
