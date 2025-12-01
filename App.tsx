import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import { LoginForm } from './components/auth/LoginForm';
import { RegistrationForm } from './components/auth/RegistrationForm';
import { ForgotPassword } from './components/auth/ForgotPassword';
import { ResetPassword } from './components/auth/ResetPassword';
import TaxFilingApp from './TaxFilingApp';
import { AuditorDashboard } from './components/AuditorDashboard';
import { ReturnReviewPanel } from './components/ReturnReviewPanel';
import { AdminDashboard } from './components/AdminDashboard';
import { AppStep } from './types';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated, isLoading, user } = useAuth();

    console.log('ProtectedRoute - isAuthenticated:', isAuthenticated, 'isLoading:', isLoading, 'user:', user);

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-slate-50">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    if (!isAuthenticated) {
        console.log('Not authenticated, redirecting to login');
        return <Navigate to="/login" />;
    }

    console.log('Authenticated, rendering protected content');
    return <>{children}</>;
};

const AdminRoute = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated, isLoading, user, isAdmin } = useAuth();
    
    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-slate-50">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        );
    }
    
    if (!isAuthenticated) {
        return <Navigate to="/login" />;
    }
    
    // Admin can access everything
    if (!isAdmin) {
        return <Navigate to="/" />;
    }
    
    return <>{children}</>;
};

const AuditorRoute = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated, user } = useAuth();
    
    if (!isAuthenticated) {
        return <Navigate to="/login" />;
    }
    
    const hasAuditorRole = user?.roles?.some(role => 
        ['ROLE_AUDITOR', 'ROLE_SENIOR_AUDITOR', 'ROLE_SUPERVISOR', 'ROLE_MANAGER', 'ROLE_ADMIN'].includes(role)
    );
    
    if (!hasAuditorRole) {
        return <Navigate to="/" />;
    }
    
    return <>{children}</>;
};

const AppContent = () => {
    const [reviewingReturnId, setReviewingReturnId] = React.useState<string | null>(null);
    const { user } = useAuth();
    
    return (
        <ToastProvider>
            <Router>
                <Routes>
                    <Route path="/login" element={<LoginForm />} />
                    <Route path="/register" element={<RegistrationForm />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/reset-password" element={<ResetPassword />} />
                    
                    {/* Admin Routes - Admin can manage tenants, users, and rules */}
                    <Route path="/admin" element={
                        <AdminRoute>
                            <AdminDashboard />
                        </AdminRoute>
                    } />
                    
                    {/* Auditor Routes */}
                    <Route path="/auditor" element={
                        <AuditorRoute>
                            {reviewingReturnId ? (
                                <ReturnReviewPanel 
                                    returnId={reviewingReturnId}
                                    userId={user?.id || ''}
                                    onBack={() => setReviewingReturnId(null)}
                                />
                            ) : (
                                <AuditorDashboard
                                    userId={user?.id || ''}
                                    onReviewReturn={(returnId) => setReviewingReturnId(returnId)}
                                />
                            )}
                        </AuditorRoute>
                    } />
                    
                    <Route path="/*" element={
                        <ProtectedRoute>
                            <TaxFilingApp />
                        </ProtectedRoute>
                    } />
                </Routes>
            </Router>
        </ToastProvider>
    );
};

export default function App() {
    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
}
