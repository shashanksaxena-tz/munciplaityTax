
import { TaxReturnSession, TaxReturnStatus, TaxPayerProfile, TaxReturnSettings, BusinessProfile, FilingFrequency } from "../types";

const STORAGE_KEY = 'munitax_sessions';

export const getSessions = (): TaxReturnSession[] => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to load sessions", e);
    return [];
  }
};

// Get sessions filtered by userId
export const getSessionsByUserId = (userId: string): TaxReturnSession[] => {
  const allSessions = getSessions();
  return allSessions.filter(s => s.userId === userId);
};

// Get sessions filtered by userId and type
export const getSessionsByUserIdAndType = (userId: string, type: 'INDIVIDUAL' | 'BUSINESS'): TaxReturnSession[] => {
  const allSessions = getSessions();
  return allSessions.filter(s => s.userId === userId && s.type === type);
};

export const saveSession = (session: TaxReturnSession) => {
  const sessions = getSessions();
  const index = sessions.findIndex(s => s.id === session.id);
  
  const updatedSession = { ...session, lastModifiedDate: new Date().toISOString() };
  
  if (index >= 0) {
    sessions[index] = updatedSession;
  } else {
    sessions.push(updatedSession);
  }
  
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  return updatedSession;
};

export const createNewSession = (
  initialProfile?: TaxPayerProfile | BusinessProfile,
  initialSettings?: TaxReturnSettings,
  type: 'INDIVIDUAL' | 'BUSINESS' = 'INDIVIDUAL',
  userId?: string
): TaxReturnSession => {
  
  let defaultProfile: TaxPayerProfile | BusinessProfile;

  if (type === 'BUSINESS') {
    defaultProfile = {
      businessName: '',
      fein: '',
      accountNumber: '',
      address: { street: '', city: '', state: '', zip: '' },
      filingFrequency: FilingFrequency.QUARTERLY,
      fiscalYearEnd: '12-31'
    } as BusinessProfile;
  } else {
    defaultProfile = {
      name: '',
      address: { street: '', city: '', state: '', zip: '' }
    } as TaxPayerProfile;
  }

  const newSession: TaxReturnSession = {
    id: crypto.randomUUID(),
    userId: userId,
    createdDate: new Date().toISOString(),
    lastModifiedDate: new Date().toISOString(),
    status: TaxReturnStatus.DRAFT,
    type: type,
    profile: initialProfile || defaultProfile,
    settings: initialSettings || { taxYear: new Date().getFullYear() - 1, isAmendment: false },
    forms: []
  };
  
  saveSession(newSession);
  return newSession;
};

export const deleteSession = (id: string) => {
  const sessions = getSessions().filter(s => s.id !== id);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
};
