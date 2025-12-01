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

interface AuthContextType {
    user: User | null;
    token: string | null;
    login: (email: string, password: string) => Promise<void>;
    logout: () => void;
    isAuthenticated: boolean;
    isLoading: boolean;
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

// Demo user for testing UI without backend
const DEMO_USER: User = {
    id: 'demo-user-1',
    email: 'admin@dublin.gov',
    firstName: 'Demo',
    lastName: 'Admin',
    roles: ['ROLE_ADMIN', 'ROLE_TAX_ADMINISTRATOR', 'ROLE_MANAGER', 'ROLE_AUDITOR', 'ROLE_SUPERVISOR'],
    tenantId: 'tenant-1'
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Load token from localStorage on mount
    useEffect(() => {
        const storedToken = localStorage.getItem('auth_token');
        
        // Check for demo mode
        if (localStorage.getItem('demo_mode') === 'true') {
            setUser(DEMO_USER);
            setToken('demo-token');
            setIsLoading(false);
            return;
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
                roles: userData.roles.split(','),
                tenantId: userData.tenantId
            });
            setToken(authToken);
        } catch (error) {
            console.error('Failed to fetch user info:', error);
            localStorage.removeItem('auth_token');
            setToken(null);
            setUser(null);
        } finally {
            setIsLoading(false);
        }
    };

    const login = async (email: string, password: string) => {
        try {
            console.log('Login attempt for:', email);
            
            // Check for demo mode login
            if (localStorage.getItem('demo_mode') === 'true' || email === 'demo@example.com') {
                console.log('Demo mode login');
                localStorage.setItem('demo_mode', 'true');
                setUser(DEMO_USER);
                setToken('demo-token');
                setIsLoading(false);
                return;
            }
            
            const data = await api.auth.login({ email, password });
            console.log('Login response:', data);

            if (data.token) {
                localStorage.setItem('auth_token', data.token);
                setToken(data.token);
                
                // Use login response data directly - no need to fetch again
                setUser({
                    id: data.userId,
                    email: data.email,
                    firstName: '',
                    lastName: '',
                    roles: data.roles ? data.roles.split(',') : [],
                    tenantId: ''
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
        setUser(null);
        setToken(null);
    };

    const value: AuthContextType = {
        user,
        token,
        login,
        logout,
        isAuthenticated: !!user && !!token,
        isLoading
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
