import React, { useState } from 'react';
import { Link } from 'react-router-dom';

export const ForgotPassword: React.FC = () => {
    const [email, setEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const response = await fetch('/api/v1/users/forgot-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email })
            });

            const data = await response.json();

            if (response.ok) {
                setIsSubmitted(true);
            } else {
                setError(data.message || 'Failed to send reset email');
            }
        } catch (err) {
            setError('An error occurred. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    if (isSubmitted) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#ebf4ff] to-white py-12 px-4">
                <div className="max-w-md w-full bg-white p-10 rounded-2xl shadow-2xl text-center border border-[#f0f0f0]">
                    <div className="flex justify-center mb-6">
                        <div className="rounded-full bg-[#d5faeb] p-4">
                            <svg className="h-16 w-16 text-[#10b981]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                            </svg>
                        </div>
                    </div>
                    <h2 className="text-2xl font-bold text-[#0f1012] mb-2">Check Your Email</h2>
                    <p className="text-[#5d6567] mb-6">
                        If an account exists with <strong>{email}</strong>, you will receive a password reset link shortly.
                    </p>
                    <Link
                        to="/login"
                        className="inline-block px-6 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-lg transition"
                    >
                        Back to Login
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#ebf4ff] to-white py-12 px-4">
            <div className="max-w-md w-full bg-white p-10 rounded-2xl shadow-2xl border border-[#f0f0f0]">
                <div className="text-center mb-8">
                    <h2 className="text-3xl font-extrabold text-[#0f1012]">Forgot Password?</h2>
                    <p className="mt-2 text-sm text-[#5d6567]">
                        Enter your email and we'll send you a reset link
                    </p>
                </div>

                {error && (
                    <div className="rounded-md bg-[#ec1656]/10 p-4 border border-[#ec1656]/30 mb-6">
                        <p className="text-sm font-medium text-[#ec1656]">{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label htmlFor="email" className="block text-sm font-medium text-[#102124] mb-1">
                            Email Address
                        </label>
                        <input
                            id="email"
                            type="email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full px-4 py-3 border border-[#dcdede] rounded-lg focus:ring-2 focus:ring-[#970bed] focus:border-[#970bed]"
                            placeholder="you@example.com"
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
                                Sending...
                            </>
                        ) : (
                            'Send Reset Link'
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
