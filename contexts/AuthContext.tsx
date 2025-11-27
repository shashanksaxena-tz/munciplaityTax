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
            const data = await api.auth.login({ email, password });

            if (data.token) {
                localStorage.setItem('auth_token', data.token);
                setToken(data.token);
                // Initial user set (will be updated by fetchUserInfo)
                setUser({
                    id: data.userId,
                    email: data.email,
                    firstName: '',
                    lastName: '',
                    roles: data.roles ? data.roles.split(',') : [],
                    tenantId: ''
                });

                // Fetch complete user info
                await fetchUserInfo(data.token);
            } else {
                throw new Error(data.message || 'Login failed');
            }
        } catch (error) {
            console.error('Login error:', error);
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
