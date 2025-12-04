
import { TaxReturnSession, TaxReturnStatus, TaxPayerProfile, TaxReturnSettings, BusinessProfile, FilingFrequency } from "../types";
import { safeLocalStorage } from '../utils/safeStorage';

const STORAGE_KEY = 'munitax_sessions';

export const getSessions = (): TaxReturnSession[] => {
  try {
    const raw = safeLocalStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to load sessions", e);
    return [];
  }
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
  
  safeLocalStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
  return updatedSession;
};

export const createNewSession = (
  initialProfile?: TaxPayerProfile | BusinessProfile,
  initialSettings?: TaxReturnSettings,
  type: 'INDIVIDUAL' | 'BUSINESS' = 'INDIVIDUAL'
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
  safeLocalStorage.setItem(STORAGE_KEY, JSON.stringify(sessions));
};
