import { TaxFormData, TaxPayerProfile, TaxReturnSettings, TaxRulesConfig, TaxCalculationResult, NetProfitReturnData, BusinessFederalForm, BusinessTaxRulesConfig } from '../types';

const API_BASE_URL = '/api/v1';

export const api = {
    auth: {
        login: async (credentials: any) => {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
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
            const response = await fetch(`${API_BASE_URL}/auth/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!response.ok) throw new Error('Failed to get user');
            return response.json();
        },
        validateToken: async (token: string) => {
            const response = await fetch(`${API_BASE_URL}/auth/validate`, {
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
            const response = await fetch(`${API_BASE_URL}/tax-engine/calculate/individual`, {
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
            const response = await fetch(`${API_BASE_URL}/tax-engine/calculate/business`, {
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

            const response = await fetch(`${API_BASE_URL}/extraction/extract`, {
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
            const response = await fetch(`${API_BASE_URL}/pdf/generate/tax-return`, {
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
            const response = await fetch(`${API_BASE_URL}/submissions`, {
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
