import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

export const LoginForm: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await login(email, password);

            // Redirect based on user role (will be implemented in AuthContext)
            // For now, redirect to main app
            navigate('/');
        } catch (err: any) {
            setError(err.message || 'Invalid email or password');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex bg-white">
            {/* Left Panel - Branding */}
            <div className="hidden lg:flex lg:w-[648px] bg-gradient-to-br from-[#ebf4ff] to-white p-[120px] flex-col gap-8">
                {/* Logo */}
                <div className="flex items-center gap-2">
                    <div className="w-[88px] h-[80px] bg-gradient-to-br from-[#970bed] to-[#469fe8] rounded-lg flex items-center justify-center shadow-md">
                        <span className="text-3xl font-bold text-white">MT</span>
                    </div>
                </div>

                {/* Feature Card */}
                <div className="w-[312.67px] h-[350px] bg-[#dfedff] rounded-[15.56px] relative overflow-hidden">
                    {/* Background circle decoration */}
                    <div className="absolute -left-[68.44px] top-[217px] w-[136.11px] h-[136.11px] bg-gradient-to-br from-blue-200/50 to-blue-300/50 rounded-full blur-sm" />
                    
                    {/* Phone mockup illustration placeholder */}
                    <div className="absolute left-[56px] top-[79.33px] w-[196.78px] h-[340.67px] bg-white/60 rounded-lg shadow-sm" />
                    
                    {/* Feature badge - centered and animated */}
                    <div className="absolute left-1/2 top-[179px] -translate-x-1/2 bg-white border border-gray-200 rounded-[9.33px] shadow-lg px-4 py-3 flex items-center gap-2 w-[231.78px]">
                        <div className="w-[31.11px] h-[31.11px] bg-green-100 rounded-full flex items-center justify-center shrink-0">
                            <span className="text-lg">⚡</span>
                        </div>
                        <div className="flex-1">
                            <p className="font-bold text-[12.45px] text-gray-900 mb-0.5">Quick & Easy Filing</p>
                            <p className="text-[9.33px] text-gray-600">File your taxes in minutes, not hours</p>
                        </div>
                    </div>
                    
                    {/* Progress dots */}
                    <div className="absolute left-1/2 bottom-[38.89px] -translate-x-1/2 flex gap-[3.11px]">
                        <div className="w-[12.44px] h-[6.22px] bg-gray-400 rounded-[1.56px]" />
                        <div className="w-[6.22px] h-[6.22px] bg-gray-300 rounded-[1.56px]" />
                        <div className="w-[6.22px] h-[6.22px] bg-gray-300 rounded-[1.56px]" />
                        <div className="w-[6.22px] h-[6.22px] bg-gray-300 rounded-[1.56px]" />
                    </div>
                </div>

                {/* Heading */}
                <div className="space-y-2">
                    <h1 className="text-[36px] leading-[42px] font-bold text-[#0f1012]">
                        Let's make filing your<br />taxes easier than ever
                    </h1>
                    <p className="text-[16px] leading-[24px] text-[#5d6567] font-medium">
                        Experience the easiest way to file your taxes online. Our platform guides you through every step, ensuring accuracy and maximizing your refunds.
                    </p>
                </div>

                {/* Footer */}
                <div className="flex items-center gap-2 text-sm text-[#5d6567]">
                    <span className="font-medium">Powered By:</span>
                    <div className="flex items-center gap-1">
                        <div className="w-4 h-4 bg-[#970bed] rounded" />
                        <span className="font-semibold text-[#0f1012]">MuniTax</span>
                    </div>
                </div>
            </div>

            {/* Right Panel - Form */}
            <div className="flex-1 flex items-center justify-center p-8 bg-gradient-to-br from-transparent via-[rgba(235,244,255,0.2)] to-[rgba(235,244,255,0.2)]">
                <div className="w-full max-w-[500px] space-y-8">
                    {/* Login Form Card */}
                    <div className="bg-white border border-[#f0f0f0] rounded-[24px] shadow-[0px_20px_40px_0px_rgba(151,11,237,0.03),-10px_0px_40px_0px_rgba(70,159,232,0.03)] p-[52px]">
                        {/* Header */}
                        <div className="space-y-2 mb-8">
                            <h2 className="text-[28px] font-bold text-[#0f1012] leading-normal">
                                Welcome Back!
                            </h2>
                            <p className="text-[16px] leading-[22px] text-[#5d6567] font-medium">
                                Sign in to your MuniTax account to continue
                            </p>
                        </div>

                {/* Error Message */}
                {error && (
                    <div className="rounded-lg bg-[#fff5f8] p-4 border border-[#ec1656]/20 mb-6">
                        <div className="flex items-center gap-3">
                            <div className="flex-shrink-0">
                                <svg className="h-5 w-5 text-[#ec1656]" viewBox="0 0 20 20" fill="currentColor">
                                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                                </svg>
                            </div>
                            <p className="text-sm font-medium text-[#ec1656]">{error}</p>
                        </div>
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Email Input */}
                    <div className="space-y-2">
                        <label htmlFor="email" className="flex gap-1 text-[14px] leading-[20px] font-medium text-[#102124]">
                            <span>Email Address</span>
                            <span className="text-[#ec1656]">*</span>
                        </label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            autoComplete="email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full h-[48px] px-3 py-3 bg-white border border-[#dcdede] rounded-[8px] text-[14px] leading-[20px] font-medium text-[#0f1012] placeholder-[#5d6567] focus:outline-none focus:ring-2 focus:ring-[#970bed] focus:border-transparent transition duration-150"
                            placeholder="you@example.com"
                        />
                    </div>

                    {/* Password Input */}
                    <div className="space-y-2">
                        <label htmlFor="password" className="flex gap-1 text-[14px] leading-[20px] font-medium text-[#102124]">
                            <span>Password</span>
                            <span className="text-[#ec1656]">*</span>
                        </label>
                        <div className="relative">
                            <input
                                id="password"
                                name="password"
                                type={showPassword ? 'text' : 'password'}
                                autoComplete="current-password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full h-[48px] px-3 py-3 bg-white border border-[#dcdede] rounded-[8px] text-[14px] leading-[20px] font-medium text-[#0f1012] placeholder-[#5d6567] focus:outline-none focus:ring-2 focus:ring-[#970bed] focus:border-transparent transition duration-150 pr-10"
                                placeholder="••••••••"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute inset-y-0 right-0 pr-3 flex items-center text-[#5d6567] hover:text-[#0f1012] transition"
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
                    </div>

                    {/* Remember Me & Forgot Password */}
                    <div className="flex items-center justify-between pt-2">
                        <div className="flex items-center">
                            <input
                                id="remember-me"
                                name="remember-me"
                                type="checkbox"
                                checked={rememberMe}
                                onChange={(e) => setRememberMe(e.target.checked)}
                                className="h-4 w-4 text-[#970bed] focus:ring-[#970bed] border-[#dcdede] rounded cursor-pointer"
                            />
                            <label htmlFor="remember-me" className="ml-2 block text-[14px] text-[#5d6567] cursor-pointer font-medium">
                                Remember me
                            </label>
                        </div>

                        <div className="text-sm">
                            <Link to="/forgot-password" className="font-semibold text-[14px] text-[#970bed] hover:text-[#469fe8] transition duration-150">
                                Forgot password?
                            </Link>
                        </div>
                    </div>

                    {/* Submit Button */}
                    <div className="pt-4">
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full h-[48px] bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white font-bold text-[14px] leading-[20px] rounded-[12px] px-4 flex items-center justify-center transition duration-150 disabled:opacity-50 disabled:cursor-not-allowed transform hover:scale-[1.01] active:scale-[0.99]"
                        >
                            {isLoading ? (
                                <>
                                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    Signing in...
                                </>
                            ) : (
                                'Sign in'
                            )}
                        </button>
                    </div>

                    {/* Security Badges */}
                    <div className="flex items-center justify-center gap-3 text-[14px] leading-[24px] text-[#5d6567] pt-4">
                        <div className="flex items-center gap-1">
                            <span>🔐</span>
                            <span className="font-semibold">Secure</span>
                        </div>
                        <div className="w-2 h-2 rounded-full bg-[#dcdede]" />
                        <div className="flex items-center gap-1">
                            <span>🛡️</span>
                            <span className="font-semibold">Encrypted</span>
                        </div>
                        <div className="w-2 h-2 rounded-full bg-[#dcdede]" />
                        <div className="flex items-center gap-1">
                            <span>✅</span>
                            <span className="font-semibold">Trusted</span>
                        </div>
                    </div>

                    {/* Demo Credentials Hint */}
                    <div className="bg-[#ebf4ff] border border-[#dcdede] rounded-lg p-4 mt-6">
                        <p className="text-xs text-[#0f1012] font-semibold mb-2">Demo Credentials:</p>
                        <div className="grid grid-cols-2 gap-2 text-xs text-[#5d6567]">
                            <div>
                                <span className="font-medium">Admin:</span> admin@example.com / admin
                            </div>
                            <div>
                                <span className="font-medium">Auditor:</span> auditor@example.com / auditor
                            </div>
                            <div>
                                <span className="font-medium">Filer:</span> filer@example.com / filer
                            </div>
                            <div>
                                <span className="font-medium">Business:</span> business@example.com / business
                            </div>
                        </div>
                    </div>
                </form>
            </div>

            {/* Register Link */}
            <Link to="/register" className="block">
                <button
                    type="button"
                    className="w-full h-[52px] border border-[#f0f0f0] bg-white hover:bg-[#fbfbfb] text-[#0f1012] font-bold text-[14px] leading-[20px] rounded-[12px] px-4 transition duration-150 transform hover:scale-[1.01] active:scale-[0.99]"
                >
                    I'm new here, help me setup my account
                </button>
            </Link>
        </div>
    </div>
</div>
    );
};
