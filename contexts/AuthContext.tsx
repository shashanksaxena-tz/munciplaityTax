import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '../services/api';
import { clearSessionCache } from '../services/sessionService';

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

// Helper to save user context for session service
const saveUserContext = (userId: string, tenantId: string) => {
    localStorage.setItem('user_id', userId);
    localStorage.setItem('user_tenant_id', tenantId || 'dublin');
};

// Helper to clear user context
const clearUserContext = () => {
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_tenant_id');
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [token, setToken] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // Load token from localStorage on mount
    useEffect(() => {
        const storedToken = localStorage.getItem('auth_token');
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
            const userObj = {
                id: userData.userId,
                email: userData.email,
                firstName: userData.firstName,
                lastName: userData.lastName,
                roles: userData.roles.split(','),
                tenantId: userData.tenantId
            };
            setUser(userObj);
            setToken(authToken);
            // Save user context for session service
            saveUserContext(userObj.id, userObj.tenantId);
        } catch (error) {
            console.error('Failed to fetch user info:', error);
            localStorage.removeItem('auth_token');
            clearUserContext();
            setToken(null);
            setUser(null);
        } finally {
            setIsLoading(false);
        }
    };

    const login = async (email: string, password: string) => {
        try {
            console.log('Login attempt for:', email);
            const data = await api.auth.login({ email, password });
            console.log('Login response:', data);

            if (data.token) {
                localStorage.setItem('auth_token', data.token);
                setToken(data.token);
                
                // Use login response data directly - no need to fetch again
                const userObj = {
                    id: data.userId,
                    email: data.email,
                    firstName: '',
                    lastName: '',
                    roles: data.roles ? data.roles.split(',') : [],
                    tenantId: data.tenantId || 'dublin'
                };
                setUser(userObj);
                
                // Save user context for session service
                saveUserContext(userObj.id, userObj.tenantId);
                
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
        clearUserContext();
        clearSessionCache(); // Clear session cache on logout
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
