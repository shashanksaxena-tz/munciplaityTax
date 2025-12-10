import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
    children: React.ReactNode;
    requiredRoles?: string[];
    redirectTo?: string;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
    children,
    requiredRoles = [],
    redirectTo = '/login'
}) => {
    const { isAuthenticated, isLoading, user } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-[#f8f9fa]">
                <div className="text-center">
                    <svg className="animate-spin h-12 w-12 text-[#970bed] mx-auto" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="mt-4 text-[#5d6567]">Loading...</p>
                </div>
            </div>
        );
    }

    if (!isAuthenticated) {
        // Redirect to login but save the attempted location
        return <Navigate to={redirectTo} state={{ from: location }} replace />;
    }

    // Check if user has required roles
    if (requiredRoles.length > 0 && user) {
        const hasRequiredRole = requiredRoles.some(role => user.roles.includes(role));
        if (!hasRequiredRole) {
            // User doesn't have required role, redirect to unauthorized page
            return <Navigate to="/unauthorized" replace />;
        }
    }

    return <>{children}</>;
};
