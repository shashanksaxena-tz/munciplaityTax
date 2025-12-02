import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';

interface RegistrationFormData {
    email: string;
    password: string;
    confirmPassword: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    userRole: 'ROLE_INDIVIDUAL' | 'ROLE_BUSINESS' | 'ROLE_AUDITOR';
    profileType: 'INDIVIDUAL' | 'BUSINESS';
    ssnOrEin: string;
    businessName?: string;
    fiscalYearEnd?: string;
    street: string;
    city: string;
    state: string;
    zip: string;
    tenantId: string;
    agreeToTerms: boolean;
}

export const RegistrationForm: React.FC = () => {
    const navigate = useNavigate();
    const [step, setStep] = useState(1); // Multi-step form
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [formData, setFormData] = useState<RegistrationFormData>({
        email: '',
        password: '',
        confirmPassword: '',
        firstName: '',
        lastName: '',
        phoneNumber: '',
        userRole: 'ROLE_INDIVIDUAL',
        profileType: 'INDIVIDUAL',
        ssnOrEin: '',
        businessName: '',
        fiscalYearEnd: '',
        street: '',
        city: '',
        state: 'OH',
        zip: '',
        tenantId: 'dublin', // Default tenant
        agreeToTerms: false
    });

    const updateField = (field: keyof RegistrationFormData, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const getPasswordStrength = (password: string): { strength: number; label: string; color: string } => {
        let strength = 0;
        if (password.length >= 8) strength++;
        if (/[a-z]/.test(password)) strength++;
        if (/[A-Z]/.test(password)) strength++;
        if (/[0-9]/.test(password)) strength++;
        if (/[^a-zA-Z0-9]/.test(password)) strength++;

        const labels = ['Very Weak', 'Weak', 'Fair', 'Good', 'Strong'];
        const colors = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-blue-500', 'bg-green-500'];

        return { strength, label: labels[strength - 1] || 'Very Weak', color: colors[strength - 1] || 'bg-red-500' };
    };

    const validateStep1 = (): boolean => {
        if (!formData.email || !formData.password || !formData.confirmPassword) {
            setError('Please fill in all required fields');
            return false;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            setError('Please enter a valid email address');
            return false;
        }
        if (formData.password.length < 8) {
            setError('Password must be at least 8 characters long');
            return false;
        }
        if (formData.password !== formData.confirmPassword) {
            setError('Passwords do not match');
            return false;
        }
        return true;
    };

    const validateStep2 = (): boolean => {
        if (!formData.firstName || !formData.lastName || !formData.phoneNumber) {
            setError('Please fill in all required fields');
            return false;
        }
        if (!/^\d{10}$/.test(formData.phoneNumber.replace(/\D/g, ''))) {
            setError('Please enter a valid 10-digit phone number');
            return false;
        }
        return true;
    };

    const validateStep3 = (): boolean => {
        // Auditors don't need to provide SSN/address - they only need basic info
        if (formData.userRole === 'ROLE_AUDITOR') {
            if (!formData.agreeToTerms) {
                setError('You must agree to the terms and conditions');
                return false;
            }
            return true;
        }
        
        if (!formData.ssnOrEin) {
            setError('Please enter your SSN or EIN');
            return false;
        }
        if (formData.profileType === 'BUSINESS' && !formData.businessName) {
            setError('Please enter your business name');
            return false;
        }
        if (!formData.street || !formData.city || !formData.zip) {
            setError('Please fill in all address fields');
            return false;
        }
        if (!formData.agreeToTerms) {
            setError('You must agree to the terms and conditions');
            return false;
        }
        return true;
    };

    const handleNext = () => {
        setError('');
        if (step === 1 && validateStep1()) {
            setStep(2);
        } else if (step === 2 && validateStep2()) {
            setStep(3);
        }
    };

    const handleBack = () => {
        setError('');
        setStep(step - 1);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!validateStep3()) return;

        setIsLoading(true);

        try {
            const response = await fetch('/api/v1/users/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: formData.email,
                    password: formData.password,
                    firstName: formData.firstName,
                    lastName: formData.lastName,
                    phoneNumber: formData.phoneNumber,
                    userRole: formData.userRole,
                    profileType: formData.profileType,
                    ssnOrEin: formData.ssnOrEin,
                    businessName: formData.businessName,
                    fiscalYearEnd: formData.fiscalYearEnd,
                    address: {
                        street: formData.street,
                        city: formData.city,
                        state: formData.state,
                        zip: formData.zip,
                        country: 'USA'
                    },
                    tenantId: formData.tenantId
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                setStep(4); // Success step
            } else {
                setError(data.message || 'Registration failed. Please try again.');
            }
        } catch (err: any) {
            setError('An error occurred. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    if (step === 4) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-pink-100 py-12 px-4 sm:px-6 lg:px-8">
                <div className="max-w-md w-full bg-white p-10 rounded-2xl shadow-2xl text-center">
                    <div className="flex justify-center mb-6">
                        <div className="rounded-full bg-green-100 p-4">
                            <svg className="h-16 w-16 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                        </div>
                    </div>
                    <h2 className="text-2xl font-bold text-gray-900 mb-2">Account Created Successfully!</h2>
                    <p className="text-gray-600 mb-6">
                        Your account has been created with <strong>{formData.email}</strong>. You can now log in and start using MuniTax.
                    </p>
                    <Link
                        to="/login"
                        className="inline-block px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
                    >
                        Go to Login
                    </Link>
                </div>
            </div>
        );
    }

    const passwordStrength = getPasswordStrength(formData.password);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-pink-100 py-12 px-4 sm:px-6 lg:px-8">
            <div className="max-w-2xl w-full space-y-8 bg-white p-10 rounded-2xl shadow-2xl">
                {/* Header */}
                <div>
                    <h2 className="text-center text-4xl font-extrabold text-gray-900">
                        Create Your Account
                    </h2>
                    <p className="mt-2 text-center text-sm text-gray-600">
                        Join MuniTax and simplify your tax filing
                    </p>
                </div>

                {/* Progress Steps */}
                <div className="flex justify-between items-center mb-8">
                    {[1, 2, 3].map((s) => (
                        <div key={s} className="flex items-center flex-1">
                            <div className={`flex items-center justify-center w-10 h-10 rounded-full border-2 ${step >= s ? 'bg-indigo-600 border-indigo-600 text-white' : 'border-gray-300 text-gray-400'
                                } font-semibold`}>
                                {s}
                            </div>
                            {s < 3 && (
                                <div className={`flex-1 h-1 mx-2 ${step > s ? 'bg-indigo-600' : 'bg-gray-300'}`} />
                            )}
                        </div>
                    ))}
                </div>

                {/* Error Message */}
                {error && (
                    <div className="rounded-md bg-red-50 p-4 border border-red-200">
                        <p className="text-sm font-medium text-red-800">{error}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Step 1: Account Credentials */}
                    {step === 1 && (
                        <div className="space-y-4">
                            <h3 className="text-lg font-semibold text-gray-900">Account Credentials</h3>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Email Address *</label>
                                <input
                                    type="email"
                                    required
                                    value={formData.email}
                                    onChange={(e) => updateField('email', e.target.value)}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="you@example.com"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Password *</label>
                                <input
                                    type="password"
                                    required
                                    value={formData.password}
                                    onChange={(e) => updateField('password', e.target.value)}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="••••••••"
                                />
                                {formData.password && (
                                    <div className="mt-2">
                                        <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                                            <span>Password Strength:</span>
                                            <span className="font-semibold">{passwordStrength.label}</span>
                                        </div>
                                        <div className="w-full bg-gray-200 rounded-full h-2">
                                            <div
                                                className={`h-2 rounded-full transition-all ${passwordStrength.color}`}
                                                style={{ width: `${(passwordStrength.strength / 5) * 100}%` }}
                                            />
                                        </div>
                                    </div>
                                )}
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password *</label>
                                <input
                                    type="password"
                                    required
                                    value={formData.confirmPassword}
                                    onChange={(e) => updateField('confirmPassword', e.target.value)}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="••••••••"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Account Type *</label>
                                <select
                                    value={formData.userRole}
                                    onChange={(e) => {
                                        const role = e.target.value as any;
                                        updateField('userRole', role);
                                        updateField('profileType', role === 'ROLE_BUSINESS' ? 'BUSINESS' : 'INDIVIDUAL');
                                    }}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                >
                                    <option value="ROLE_INDIVIDUAL">Individual Filer</option>
                                    <option value="ROLE_BUSINESS">Business Filer</option>
                                    <option value="ROLE_AUDITOR">Auditor</option>
                                </select>
                            </div>
                        </div>
                    )}

                    {/* Step 2: Personal Information */}
                    {step === 2 && (
                        <div className="space-y-4">
                            <h3 className="text-lg font-semibold text-gray-900">Personal Information</h3>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">First Name *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.firstName}
                                        onChange={(e) => updateField('firstName', e.target.value)}
                                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Last Name *</label>
                                    <input
                                        type="text"
                                        required
                                        value={formData.lastName}
                                        onChange={(e) => updateField('lastName', e.target.value)}
                                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number *</label>
                                <input
                                    type="tel"
                                    required
                                    value={formData.phoneNumber}
                                    onChange={(e) => updateField('phoneNumber', e.target.value)}
                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                    placeholder="(555) 123-4567"
                                />
                            </div>
                        </div>
                    )}

                    {/* Step 3: Profile & Address */}
                    {step === 3 && (
                        <div className="space-y-4">
                            <h3 className="text-lg font-semibold text-gray-900">
                                {formData.userRole === 'ROLE_AUDITOR' ? 'Confirmation' : 'Profile & Address'}
                            </h3>

                            {/* Auditors don't need SSN/EIN or address */}
                            {formData.userRole === 'ROLE_AUDITOR' ? (
                                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                                    <div className="flex items-center">
                                        <svg className="h-5 w-5 text-blue-600 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        <p className="text-sm text-blue-800">
                                            As an auditor, you don't need to provide SSN or address information.
                                            Your account will be reviewed and approved by an administrator.
                                        </p>
                                    </div>
                                </div>
                            ) : (
                                <>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            {formData.profileType === 'BUSINESS' ? 'EIN' : 'SSN'} *
                                        </label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.ssnOrEin}
                                            onChange={(e) => updateField('ssnOrEin', e.target.value)}
                                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            placeholder={formData.profileType === 'BUSINESS' ? '12-3456789' : '123-45-6789'}
                                        />
                                    </div>

                                    {formData.profileType === 'BUSINESS' && (
                                        <>
                                            <div>
                                                <label className="block text-sm font-medium text-gray-700 mb-1">Business Name *</label>
                                                <input
                                                    type="text"
                                                    required
                                                    value={formData.businessName}
                                                    onChange={(e) => updateField('businessName', e.target.value)}
                                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-sm font-medium text-gray-700 mb-1">Fiscal Year End</label>
                                                <input
                                                    type="text"
                                                    value={formData.fiscalYearEnd}
                                                    onChange={(e) => updateField('fiscalYearEnd', e.target.value)}
                                                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                                    placeholder="12/31"
                                                />
                                            </div>
                                        </>
                                    )}

                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Street Address *</label>
                                        <input
                                            type="text"
                                            required
                                            value={formData.street}
                                            onChange={(e) => updateField('street', e.target.value)}
                                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
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
                                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1">State *</label>
                                            <select
                                                value={formData.state}
                                                onChange={(e) => updateField('state', e.target.value)}
                                                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
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
                                            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            placeholder="43016"
                                        />
                                    </div>
                                </>
                            )}

                            <div className="flex items-start">
                                <input
                                    type="checkbox"
                                    checked={formData.agreeToTerms}
                                    onChange={(e) => updateField('agreeToTerms', e.target.checked)}
                                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded mt-1"
                                />
                                <label className="ml-2 block text-sm text-gray-700">
                                    I agree to the <Link to="/terms" className="text-indigo-600 hover:text-indigo-500">Terms and Conditions</Link> and <Link to="/privacy" className="text-indigo-600 hover:text-indigo-500">Privacy Policy</Link> *
                                </label>
                            </div>
                        </div>
                    )}

                    {/* Navigation Buttons */}
                    <div className="flex justify-between pt-4">
                        {step > 1 && (
                            <button
                                type="button"
                                onClick={handleBack}
                                className="px-6 py-3 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition"
                            >
                                Back
                            </button>
                        )}

                        {step < 3 ? (
                            <button
                                type="button"
                                onClick={handleNext}
                                className="ml-auto px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
                            >
                                Next
                            </button>
                        ) : (
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="ml-auto px-6 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition flex items-center"
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        Creating Account...
                                    </>
                                ) : (
                                    'Create Account'
                                )}
                            </button>
                        )}
                    </div>
                </form>

                {/* Login Link */}
                <div className="text-center pt-4 border-t">
                    <p className="text-sm text-gray-600">
                        Already have an account?{' '}
                        <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500 transition">
                            Sign in
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
};
