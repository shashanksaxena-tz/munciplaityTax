import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import { LoginForm } from './components/auth/LoginForm';
import { RegistrationForm } from './components/auth/RegistrationForm';
import { ForgotPassword } from './components/auth/ForgotPassword';
import { ResetPassword } from './components/auth/ResetPassword';
import TaxFilingApp from './TaxFilingApp';
import { AuditorDashboard } from './components/AuditorDashboard';
import { ReturnReviewPanel } from './components/ReturnReviewPanel';
import { RuleManagementDashboard } from './components/RuleManagementDashboard';
import LedgerDashboard from './components/LedgerDashboard';
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

const AdminRoute = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated, user } = useAuth();
    
    if (!isAuthenticated) {
        return <Navigate to="/login" />;
    }
    
    const hasAdminRole = user?.roles?.some(role => 
        ['ROLE_TAX_ADMINISTRATOR', 'ROLE_MANAGER', 'ROLE_ADMIN'].includes(role)
    );
    
    if (!hasAdminRole) {
        return <Navigate to="/" />;
    }
    
    return <>{children}</>;
};

// Wrapper component for Rule Management to handle navigation
const RuleManagementWrapper = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    
    return (
        <div className="min-h-screen bg-slate-50 p-6">
            <RuleManagementDashboard
                userId={user?.id || ''}
                tenantId={user?.tenantId || 'tenant-1'}
                onBack={() => navigate('/')}
            />
        </div>
    );
};

// Wrapper component for Ledger Dashboard to handle navigation
const LedgerDashboardWrapper = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    
    // Determine user role for ledger
    const getLedgerRole = (): 'filer' | 'municipality' | 'admin' => {
        if (user?.roles?.some(role => ['ROLE_ADMIN', 'ROLE_MANAGER'].includes(role))) {
            return 'admin';
        }
        if (user?.roles?.some(role => ['ROLE_TAX_ADMINISTRATOR', 'ROLE_AUDITOR', 'ROLE_SENIOR_AUDITOR', 'ROLE_SUPERVISOR'].includes(role))) {
            return 'municipality';
        }
        return 'filer';
    };
    
    return (
        <div className="min-h-screen bg-slate-50 p-6">
            <div className="max-w-7xl mx-auto">
                <div className="mb-4">
                    <button
                        onClick={() => navigate('/')}
                        className="flex items-center gap-2 text-slate-600 hover:text-slate-900"
                    >
                        ← Back to Dashboard
                    </button>
                </div>
                <LedgerDashboard
                    userRole={getLedgerRole()}
                    tenantId={user?.tenantId || 'tenant-1'}
                    filerId={user?.id}
                    municipalityId={user?.tenantId} // Use tenantId as municipalityId for now
                />
            </div>
        </div>
    );
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
                    
                    {/* Admin Routes - Rule Management */}
                    <Route path="/admin/rules" element={
                        <AdminRoute>
                            <RuleManagementWrapper />
                        </AdminRoute>
                    } />
                    
                    {/* Ledger Dashboard - Available to all authenticated users */}
                    <Route path="/ledger" element={
                        <ProtectedRoute>
                            <LedgerDashboardWrapper />
                        </ProtectedRoute>
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
