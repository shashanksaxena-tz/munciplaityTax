import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '../services/api';

interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    roles: string[];
    tenantId: string;
}

// Tenant information
interface Tenant {
    id: string;
    name: string;
}

interface AuthContextType {
    user: User | null;
    token: string | null;
    currentTenant: Tenant | null;
    login: (email: string, password: string, tenantId?: string) => Promise<void>;
    logout: () => void;
    setCurrentTenant: (tenant: Tenant) => void;
    isAuthenticated: boolean;
    isLoading: boolean;
    isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [currentTenant, setCurrentTenant] = useState<Tenant | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Load token and tenant from localStorage on mount
    useEffect(() => {
        const storedToken = localStorage.getItem('auth_token');
        const storedTenant = localStorage.getItem('current_tenant');
        
        if (storedTenant) {
            try {
                setCurrentTenant(JSON.parse(storedTenant));
            } catch (e) {
                console.error('Failed to parse stored tenant:', e);
            }
        }
        
        if (storedToken) {
            // Validate token and load user info
            fetchUserInfo(storedToken);
        } else {
            setIsLoading(false);
        }
    }, []);

    const fetchUserInfo = async (authToken: string) => {
        try {
            const userData = await api.auth.getCurrentUser(authToken);
            setUser({
                id: userData.userId,
                email: userData.email,
                firstName: userData.firstName,
                lastName: userData.lastName,
                roles: userData.roles ? userData.roles.split(',') : [],
                tenantId: userData.tenantId || ''
            });
            setToken(authToken);
        } catch (error) {
            console.error('Failed to fetch user info:', error);
            localStorage.removeItem('auth_token');
            localStorage.removeItem('current_tenant');
            setToken(null);
            setUser(null);
            setCurrentTenant(null);
        } finally {
            setIsLoading(false);
        }
    };

    const login = async (email: string, password: string, tenantId?: string) => {
        try {
            console.log('Login attempt for:', email, 'tenant:', tenantId);
            const data = await api.auth.login({ email, password });
            console.log('Login response:', data);

            if (data.token) {
                localStorage.setItem('auth_token', data.token);
                setToken(data.token);
                
                const roles = data.roles ? data.roles.split(',') : [];
                const isAdminUser = roles.includes('ROLE_ADMIN');
                
                // Set tenant context
                if (tenantId && !isAdminUser) {
                    const tenant = { id: tenantId, name: getTenantName(tenantId) };
                    setCurrentTenant(tenant);
                    localStorage.setItem('current_tenant', JSON.stringify(tenant));
                } else if (isAdminUser) {
                    // Admin users don't have a specific tenant
                    setCurrentTenant(null);
                    localStorage.removeItem('current_tenant');
                }
                
                // Use login response data directly - no need to fetch again
                setUser({
                    id: data.userId,
                    email: data.email,
                    firstName: '',
                    lastName: '',
                    roles: roles,
                    tenantId: tenantId || ''
                });
                
                console.log('Login successful, user state updated');
                setIsLoading(false);
            } else {
                throw new Error(data.message || 'Login failed');
            }
        } catch (error) {
            console.error('Login error:', error);
            setIsLoading(false);
            throw error;
        }
    };

    const logout = () => {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('current_tenant');
        setUser(null);
        setToken(null);
        setCurrentTenant(null);
    };
    
    const handleSetCurrentTenant = (tenant: Tenant) => {
        setCurrentTenant(tenant);
        localStorage.setItem('current_tenant', JSON.stringify(tenant));
    };

    // Helper function to get tenant name
    const getTenantName = (tenantId: string): string => {
        const tenantNames: Record<string, string> = {
            'dublin': 'Dublin Municipality',
            'columbus': 'Columbus City',
            'westerville': 'Westerville Township'
        };
        return tenantNames[tenantId] || tenantId;
    };

    const isAdmin = user?.roles?.includes('ROLE_ADMIN') || false;

    const value: AuthContextType = {
        user,
        token,
        currentTenant,
        login,
        logout,
        setCurrentTenant: handleSetCurrentTenant,
        isAuthenticated: !!user && !!token,
        isLoading,
        isAdmin
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
