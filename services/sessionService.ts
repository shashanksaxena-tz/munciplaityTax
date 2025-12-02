
import { TaxReturnSession, TaxReturnStatus, TaxPayerProfile, TaxReturnSettings, BusinessProfile, FilingFrequency } from "../types";
import { api } from "./api";

// User context for API calls
interface UserContext {
  tenantId: string;
  userId: string;
}

// In-memory cache for sessions (for synchronous access during transitions)
let sessionCache: TaxReturnSession[] = [];
let cacheLoaded = false;

// Get the current user context from localStorage (set during login)
const getUserContext = (): UserContext | null => {
  const token = localStorage.getItem('auth_token');
  if (!token) return null;
  
  // Try to get user info from localStorage (set during login)
  const tenantId = localStorage.getItem('user_tenant_id') || 'dublin';
  const userId = localStorage.getItem('user_id');
  
  if (!userId) return null;
  return { tenantId, userId };
};

// Async function to fetch sessions from API
export const fetchSessions = async (): Promise<TaxReturnSession[]> => {
  const context = getUserContext();
  if (!context) {
    console.warn('No user context available for fetching sessions');
    return [];
  }
  
  try {
    const sessions = await api.sessions.getAll(context.tenantId, context.userId);
    sessionCache = sessions;
    cacheLoaded = true;
    return sessions;
  } catch (e) {
    console.error("Failed to fetch sessions from API", e);
    return sessionCache; // Return cached data on error
  }
};

// Synchronous access to cached sessions (for backward compatibility)
export const getSessions = (): TaxReturnSession[] => {
  return sessionCache;
};

// Check if cache needs loading
export const isCacheLoaded = (): boolean => {
  return cacheLoaded;
};

// Clear the cache (called on logout)
export const clearSessionCache = () => {
  sessionCache = [];
  cacheLoaded = false;
};

// Async function to save a session to API
export const saveSessionAsync = async (session: TaxReturnSession): Promise<TaxReturnSession> => {
  const context = getUserContext();
  if (!context) {
    throw new Error('No user context available for saving session');
  }
  
  const updatedSession = { ...session, lastModifiedDate: new Date().toISOString() };
  
  try {
    // Check if session exists in cache
    const existingIndex = sessionCache.findIndex(s => s.id === session.id);
    
    let savedSession: TaxReturnSession;
    if (existingIndex >= 0) {
      // Update existing session
      savedSession = await api.sessions.update(updatedSession, context.tenantId, context.userId);
      sessionCache[existingIndex] = savedSession;
    } else {
      // Create new session
      savedSession = await api.sessions.create(updatedSession, context.tenantId, context.userId);
      sessionCache.push(savedSession);
    }
    
    return savedSession;
  } catch (e) {
    console.error("Failed to save session to API", e);
    throw e;
  }
};

// Synchronous save for backward compatibility (updates cache and queues API call)
export const saveSession = (session: TaxReturnSession): TaxReturnSession => {
  const updatedSession = { ...session, lastModifiedDate: new Date().toISOString() };
  
  // Update local cache immediately
  const existingIndex = sessionCache.findIndex(s => s.id === session.id);
  if (existingIndex >= 0) {
    sessionCache[existingIndex] = updatedSession;
  } else {
    sessionCache.push(updatedSession);
  }
  
  // Queue API update (fire and forget for backward compatibility)
  saveSessionAsync(session).catch(e => {
    console.error("Background save failed", e);
  });
  
  return updatedSession;
};

// Async function to create a new session
export const createNewSessionAsync = async (
  initialProfile?: TaxPayerProfile | BusinessProfile,
  initialSettings?: TaxReturnSettings,
  type: 'INDIVIDUAL' | 'BUSINESS' = 'INDIVIDUAL'
): Promise<TaxReturnSession> => {
  const context = getUserContext();
  if (!context) {
    throw new Error('No user context available for creating session');
  }
  
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
  
  try {
    const savedSession = await api.sessions.create(newSession, context.tenantId, context.userId);
    sessionCache.push(savedSession);
    return savedSession;
  } catch (e) {
    console.error("Failed to create session via API", e);
    throw e;
  }
};

// Synchronous create for backward compatibility
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
  
  // Add to cache immediately
  sessionCache.push(newSession);
  
  // Queue API create (fire and forget for backward compatibility)
  const context = getUserContext();
  if (context) {
    api.sessions.create(newSession, context.tenantId, context.userId).catch(e => {
      console.error("Background create failed", e);
    });
  }
  
  return newSession;
};

// Async function to delete a session
export const deleteSessionAsync = async (id: string): Promise<void> => {
  try {
    await api.sessions.delete(id);
    sessionCache = sessionCache.filter(s => s.id !== id);
  } catch (e) {
    console.error("Failed to delete session via API", e);
    throw e;
  }
};

// Synchronous delete for backward compatibility
export const deleteSession = (id: string) => {
  // Update cache immediately
  sessionCache = sessionCache.filter(s => s.id !== id);
  
  // Queue API delete (fire and forget for backward compatibility)
  api.sessions.delete(id).catch(e => {
    console.error("Background delete failed", e);
  });
};

