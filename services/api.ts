import { TaxFormData, TaxPayerProfile, TaxReturnSettings, TaxRulesConfig, TaxCalculationResult, NetProfitReturnData, BusinessFederalForm, BusinessTaxRulesConfig, TaxReturnSession, TaxReturnStatus, BusinessProfile, RealTimeExtractionUpdate } from '../types';

const API_BASE_URL = '/api/v1';

// Helper to get auth token
const getAuthToken = () => localStorage.getItem('auth_token');

// Backend session DTO that matches the Java TaxReturnSession entity
interface BackendSession {
    id: string;
    tenantId: string;
    userId: string;
    type: 'INDIVIDUAL' | 'BUSINESS';
    status: 'DRAFT' | 'IN_PROGRESS' | 'CALCULATED' | 'SUBMITTED' | 'AMENDED';
    profileJson: string;
    settingsJson: string;
    formsJson: string;
    calculationResultJson?: string;
    businessFilingsJson?: string;
    netProfitFilingsJson?: string;
    reconciliationsJson?: string;
    createdDate: string;
    lastModifiedDate: string;
    submittedDate?: string;
    notes?: string;
}

// Map backend status to frontend TaxReturnStatus
const mapBackendStatusToFrontend = (status: BackendSession['status']): TaxReturnStatus => {
    const statusMap: Record<BackendSession['status'], TaxReturnStatus> = {
        'DRAFT': TaxReturnStatus.DRAFT,
        'IN_PROGRESS': TaxReturnStatus.DRAFT,
        'CALCULATED': TaxReturnStatus.DRAFT,
        'SUBMITTED': TaxReturnStatus.SUBMITTED,
        'AMENDED': TaxReturnStatus.AMENDED
    };
    return statusMap[status] || TaxReturnStatus.DRAFT;
};

// Map frontend TaxReturnStatus to backend status
const mapFrontendStatusToBackend = (status: TaxReturnStatus): BackendSession['status'] => {
    // Only map statuses that exist in the backend
    switch (status) {
        case TaxReturnStatus.SUBMITTED:
            return 'SUBMITTED';
        case TaxReturnStatus.AMENDED:
            return 'AMENDED';
        case TaxReturnStatus.IN_REVIEW:
        case TaxReturnStatus.AWAITING_DOCUMENTATION:
        case TaxReturnStatus.APPROVED:
        case TaxReturnStatus.REJECTED:
        case TaxReturnStatus.PAID:
        case TaxReturnStatus.LATE:
            return 'SUBMITTED'; // Map all these to SUBMITTED for backend
        case TaxReturnStatus.DRAFT:
        default:
            return 'DRAFT';
    }
};

// Convert backend session to frontend TaxReturnSession
const convertBackendToFrontendSession = (backend: BackendSession): TaxReturnSession => {
    return {
        id: backend.id,
        createdDate: backend.createdDate,
        lastModifiedDate: backend.lastModifiedDate,
        status: mapBackendStatusToFrontend(backend.status),
        type: backend.type,
        profile: backend.profileJson ? JSON.parse(backend.profileJson) : {},
        settings: backend.settingsJson ? JSON.parse(backend.settingsJson) : { taxYear: new Date().getFullYear() - 1, isAmendment: false },
        forms: backend.formsJson ? JSON.parse(backend.formsJson) : [],
        lastCalculationResult: backend.calculationResultJson ? JSON.parse(backend.calculationResultJson) : undefined,
        businessFilings: backend.businessFilingsJson ? JSON.parse(backend.businessFilingsJson) : undefined,
        netProfitFilings: backend.netProfitFilingsJson ? JSON.parse(backend.netProfitFilingsJson) : undefined,
        reconciliations: backend.reconciliationsJson ? JSON.parse(backend.reconciliationsJson) : undefined
    };
};

// Convert frontend TaxReturnSession to backend session DTO
const convertFrontendToBackendSession = (
    frontend: TaxReturnSession,
    tenantId: string,
    userId: string
): BackendSession => {
    return {
        id: frontend.id,
        tenantId,
        userId,
        type: frontend.type,
        status: mapFrontendStatusToBackend(frontend.status),
        profileJson: JSON.stringify(frontend.profile),
        settingsJson: JSON.stringify(frontend.settings),
        formsJson: JSON.stringify(frontend.forms),
        calculationResultJson: frontend.lastCalculationResult ? JSON.stringify(frontend.lastCalculationResult) : undefined,
        businessFilingsJson: frontend.businessFilings ? JSON.stringify(frontend.businessFilings) : undefined,
        netProfitFilingsJson: frontend.netProfitFilings ? JSON.stringify(frontend.netProfitFilings) : undefined,
        reconciliationsJson: frontend.reconciliations ? JSON.stringify(frontend.reconciliations) : undefined,
        createdDate: frontend.createdDate,
        lastModifiedDate: frontend.lastModifiedDate
    };
};

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
                    'Authorization': `Bearer ${getAuthToken()}`
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
                    'Authorization': `Bearer ${getAuthToken()}`
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
        /**
         * Upload and extract tax data from a document using Gemini AI.
         * 
         * @param file The tax document to process
         * @param onProgress Callback for real-time progress updates
         * @param options Optional extraction options including user API key
         */
        uploadAndExtract: async (
            file: File, 
            onProgress: (event: RealTimeExtractionUpdate) => void,
            options?: {
                geminiApiKey?: string;
                geminiModel?: string;
            }
        ) => {
            const formData = new FormData();
            formData.append('file', file);

            const headers: Record<string, string> = {
                'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
            };

            // Add user-provided API key if available
            if (options?.geminiApiKey) {
                headers['X-Gemini-Api-Key'] = options.geminiApiKey;
            }
            if (options?.geminiModel) {
                headers['X-Gemini-Model'] = options.geminiModel;
            }

            const response = await fetch(`${API_BASE_URL}/extraction/extract`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${getAuthToken()}`
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
                                    const data = JSON.parse(jsonStr) as RealTimeExtractionUpdate;
                                    onProgress(data);
                                }
                            } catch (e) {
                                console.warn('Failed to parse SSE data:', e);
                            }
                        }
                    }
                }
            }
        },

        /**
         * Batch upload and extract from multiple documents.
         */
        uploadAndExtractBatch: async (
            files: File[],
            onProgress: (event: RealTimeExtractionUpdate, fileIndex: number, fileName: string) => void,
            options?: {
                geminiApiKey?: string;
                geminiModel?: string;
            }
        ) => {
            const formData = new FormData();
            files.forEach(file => formData.append('files', file));

            const headers: Record<string, string> = {
                'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
            };

            if (options?.geminiApiKey) {
                headers['X-Gemini-Api-Key'] = options.geminiApiKey;
            }
            if (options?.geminiModel) {
                headers['X-Gemini-Model'] = options.geminiModel;
            }

            const response = await fetch(`${API_BASE_URL}/extraction/extract/batch`, {
                method: 'POST',
                headers,
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Batch extraction failed: ${response.status} ${errorText}`);
            }

            const reader = response.body?.getReader();
            const decoder = new TextDecoder();
            let currentFileIndex = 0;

            if (reader) {
                let buffer = '';
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;

                    const chunk = decoder.decode(value, { stream: true });
                    buffer += chunk;

                    const lines = buffer.split('\n\n');
                    buffer = lines.pop() || '';

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            try {
                                const jsonStr = line.substring(5).trim();
                                if (jsonStr) {
                                    const data = JSON.parse(jsonStr) as RealTimeExtractionUpdate;
                                    // Track which file we're processing
                                    if (data.status === 'COMPLETE' || data.status === 'ERROR') {
                                        onProgress(data, currentFileIndex, files[currentFileIndex]?.name || 'unknown');
                                        currentFileIndex++;
                                    } else {
                                        onProgress(data, currentFileIndex, files[currentFileIndex]?.name || 'unknown');
                                    }
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
                    'Authorization': `Bearer ${getAuthToken()}`
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
                    'Authorization': `Bearer ${getAuthToken()}`
                },
                body: JSON.stringify(submission)
            });
            if (!response.ok) throw new Error('Submission failed');
            return response.json();
        }
    },

    sessions: {
        // Get all sessions for a user
        getAll: async (tenantId: string, userId: string): Promise<TaxReturnSession[]> => {
            const params = new URLSearchParams({ tenantId, userId });
            const response = await fetch(`${API_BASE_URL}/sessions?${params}`, {
                headers: {
                    'Authorization': `Bearer ${getAuthToken()}`
                }
            });
            if (!response.ok) throw new Error('Failed to fetch sessions');
            const backendSessions: BackendSession[] = await response.json();
            return backendSessions.map(convertBackendToFrontendSession);
        },

        // Get a single session by ID
        get: async (sessionId: string): Promise<TaxReturnSession> => {
            const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}`, {
                headers: {
                    'Authorization': `Bearer ${getAuthToken()}`
                }
            });
            if (!response.ok) throw new Error('Failed to fetch session');
            const backendSession: BackendSession = await response.json();
            return convertBackendToFrontendSession(backendSession);
        },

        // Create a new session
        create: async (session: TaxReturnSession, tenantId: string, userId: string): Promise<TaxReturnSession> => {
            const backendSession = convertFrontendToBackendSession(session, tenantId, userId);
            const response = await fetch(`${API_BASE_URL}/sessions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getAuthToken()}`
                },
                body: JSON.stringify(backendSession)
            });
            if (!response.ok) throw new Error('Failed to create session');
            const createdSession: BackendSession = await response.json();
            return convertBackendToFrontendSession(createdSession);
        },

        // Update an existing session
        update: async (session: TaxReturnSession, tenantId: string, userId: string): Promise<TaxReturnSession> => {
            const backendSession = convertFrontendToBackendSession(session, tenantId, userId);
            const response = await fetch(`${API_BASE_URL}/sessions/${session.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getAuthToken()}`
                },
                body: JSON.stringify(backendSession)
            });
            if (!response.ok) throw new Error('Failed to update session');
            const updatedSession: BackendSession = await response.json();
            return convertBackendToFrontendSession(updatedSession);
        },

        // Delete a session
        delete: async (sessionId: string): Promise<void> => {
            const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${getAuthToken()}`
                }
            });
            if (!response.ok) throw new Error('Failed to delete session');
        }
    }
};
