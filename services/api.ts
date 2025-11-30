import { TaxFormData, TaxPayerProfile, TaxReturnSettings, TaxRulesConfig, TaxCalculationResult, NetProfitReturnData, BusinessFederalForm, BusinessTaxRulesConfig } from '../types';
import { apiConfig } from './apiConfig';

export const api = {
    auth: {
        login: async (credentials: any) => {
            const url = apiConfig.buildUrl('/auth', '/api/v1/auth/login');
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(credentials)
            });
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.message || 'Login failed');
            }
            return response.json();
        },
        getCurrentUser: async (token: string) => {
            const url = apiConfig.buildUrl('/auth', '/api/v1/auth/me');
            const response = await fetch(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to get user');
            return response.json();
        },
        validateToken: async (token: string) => {
            const url = apiConfig.buildUrl('/auth', '/api/v1/auth/validate');
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token })
            });
            return response.ok;
        }
    },

    taxEngine: {
        calculateIndividual: async (
            forms: TaxFormData[],
            profile: TaxPayerProfile,
            settings: TaxReturnSettings,
            rules: TaxRulesConfig
        ): Promise<TaxCalculationResult> => {
            const url = apiConfig.buildUrl('/tax-engine', '/api/v1/tax-engine/calculate/individual');
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
                },
                body: JSON.stringify({ forms, profile, settings, rules })
            });
            if (!response.ok) throw new Error('Calculation failed');
            return response.json();
        },

        calculateBusiness: async (
            year: number,
            estimates: number,
            priorCredit: number,
            schX: BusinessFederalForm['reconciliation'],
            schY: BusinessFederalForm['allocation'],
            nolCarryforward: number,
            rules: BusinessTaxRulesConfig
        ): Promise<NetProfitReturnData> => {
            const url = apiConfig.buildUrl('/tax-engine', '/api/v1/tax-engine/calculate/business');
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
                },
                body: JSON.stringify({
                    year, estimates, priorCredit, schX, schY, nolCarryforward, rules
                })
            });
            if (!response.ok) throw new Error('Business Calculation failed');
            return response.json();
        }
    },

    extraction: {
        uploadAndExtract: async (file: File, onProgress: (event: any) => void) => {
            const formData = new FormData();
            formData.append('file', file);

            const url = apiConfig.buildUrl('/extraction', '/api/v1/extraction/extract');
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
                },
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Extraction failed: ${response.status} ${errorText}`);
            }

            const reader = response.body?.getReader();
            const decoder = new TextDecoder();

            if (reader) {
                let buffer = '';
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;

                    const chunk = decoder.decode(value, { stream: true });
                    buffer += chunk;

                    const lines = buffer.split('\n\n');
                    buffer = lines.pop() || ''; // Keep the last incomplete chunk

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            try {
                                const jsonStr = line.substring(5).trim();
                                if (jsonStr) {
                                    const data = JSON.parse(jsonStr);
                                    onProgress(data);
                                }
                            } catch (e) {
                                console.warn('Failed to parse SSE data:', e);
                            }
                        }
                    }
                }
            }
        }
    },

    pdf: {
        generateReturn: async (result: TaxCalculationResult): Promise<Blob> => {
            const url = apiConfig.buildUrl('/pdf', '/api/v1/pdf/generate/tax-return');
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
                },
                body: JSON.stringify(result)
            });
            if (!response.ok) throw new Error('PDF generation failed');
            return response.blob();
        }
    },

    submission: {
        submitReturn: async (submission: any) => {
            const url = apiConfig.buildUrl('/submission', '/api/v1/submissions');
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
                },
                body: JSON.stringify(submission)
            });
            if (!response.ok) throw new Error('Submission failed');
            return response.json();
        }
    }
};
