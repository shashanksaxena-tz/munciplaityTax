import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { 
    Building2, 
    Users, 
    Settings, 
    Plus, 
    Edit, 
    Trash2, 
    Check, 
    X, 
    Search,
    Shield,
    Home,
    FileText,
    ChevronRight,
    AlertCircle
} from 'lucide-react';

// Tenant interface matching backend
interface Tenant {
    tenantId: string;
    name: string;
    schemaName: string;
    dbUrl?: string;
    dbUsername?: string;
    dbPassword?: string;
}

// Mock data for tenants - in production this would come from the API
const mockTenants: Tenant[] = [
    { tenantId: 'dublin', name: 'Dublin Municipality', schemaName: 'tenant_dublin' },
    { tenantId: 'columbus', name: 'Columbus City', schemaName: 'tenant_columbus' },
    { tenantId: 'westerville', name: 'Westerville Township', schemaName: 'tenant_westerville' },
];

export const AdminDashboard: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<'tenants' | 'users' | 'rules'>('tenants');
    const [tenants, setTenants] = useState<Tenant[]>(mockTenants);
    const [showAddTenant, setShowAddTenant] = useState(false);
    const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedTenant, setSelectedTenant] = useState<string | null>(null);

    // Form state for new/edit tenant
    const [formData, setFormData] = useState({
        tenantId: '',
        name: '',
        schemaName: '',
        dbUrl: '',
        dbUsername: '',
        dbPassword: ''
    });

    const isAdmin = user?.roles?.includes('ROLE_ADMIN');

    useEffect(() => {
        // In production, fetch tenants from API
        // fetchTenants();
    }, []);

    const resetForm = () => {
        setFormData({
            tenantId: '',
            name: '',
            schemaName: '',
            dbUrl: '',
            dbUsername: '',
            dbPassword: ''
        });
    };

    const handleAddTenant = () => {
        if (!formData.name.trim()) return;
        
        const newTenant: Tenant = {
            tenantId: formData.tenantId || formData.name.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
            name: formData.name,
            schemaName: formData.schemaName || `tenant_${formData.name.toLowerCase().replace(/[^a-z0-9]+/g, '_')}`,
            dbUrl: formData.dbUrl || undefined,
            dbUsername: formData.dbUsername || undefined,
            dbPassword: formData.dbPassword || undefined
        };
        
        setTenants([...tenants, newTenant]);
        setShowAddTenant(false);
        resetForm();
    };

    const handleEditTenant = () => {
        if (!editingTenant || !formData.name.trim()) return;
        
        setTenants(tenants.map(t => 
            t.tenantId === editingTenant.tenantId 
                ? { ...t, name: formData.name, dbUrl: formData.dbUrl, dbUsername: formData.dbUsername }
                : t
        ));
        setEditingTenant(null);
        resetForm();
    };

    const handleDeleteTenant = (tenantId: string) => {
        if (window.confirm('Are you sure you want to delete this tenant? This action cannot be undone.')) {
            setTenants(tenants.filter(t => t.tenantId !== tenantId));
        }
    };

    const startEdit = (tenant: Tenant) => {
        setEditingTenant(tenant);
        setFormData({
            tenantId: tenant.tenantId,
            name: tenant.name,
            schemaName: tenant.schemaName,
            dbUrl: tenant.dbUrl || '',
            dbUsername: tenant.dbUsername || '',
            dbPassword: ''
        });
    };

    const filteredTenants = tenants.filter(t => 
        t.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        t.tenantId.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (!isAdmin) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-slate-50">
                <div className="bg-white p-8 rounded-xl shadow-lg text-center">
                    <AlertCircle className="w-16 h-16 text-red-500 mx-auto mb-4" />
                    <h2 className="text-2xl font-bold text-slate-900 mb-2">Access Denied</h2>
                    <p className="text-slate-600 mb-6">You need admin privileges to access this page.</p>
                    <button 
                        onClick={() => navigate('/')}
                        className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                    >
                        Return to Dashboard
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50">
            {/* Header */}
            <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
                <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <button onClick={() => navigate('/')} className="p-2 hover:bg-slate-100 rounded-lg text-slate-500">
                            <Home className="w-5 h-5" />
                        </button>
                        <div className="h-6 w-px bg-slate-200"></div>
                        <div className="flex items-center gap-2">
                            <Shield className="w-6 h-6 text-indigo-600" />
                            <h1 className="text-xl font-bold text-slate-900">Admin Console</h1>
                        </div>
                    </div>
                    <div className="flex items-center gap-4">
                        <span className="text-sm text-slate-600">
                            Logged in as <span className="font-semibold">{user?.email}</span>
                        </span>
                        <button 
                            onClick={logout}
                            className="px-4 py-2 text-sm text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg"
                        >
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            <div className="max-w-7xl mx-auto px-4 py-8">
                {/* Tabs */}
                <div className="flex gap-2 mb-8 border-b border-slate-200">
                    <button 
                        onClick={() => setActiveTab('tenants')}
                        className={`px-6 py-3 font-medium text-sm flex items-center gap-2 border-b-2 transition-colors ${
                            activeTab === 'tenants' 
                                ? 'border-indigo-600 text-indigo-600' 
                                : 'border-transparent text-slate-500 hover:text-slate-700'
                        }`}
                    >
                        <Building2 className="w-4 h-4" />
                        Tenant Management
                    </button>
                    <button 
                        onClick={() => setActiveTab('users')}
                        className={`px-6 py-3 font-medium text-sm flex items-center gap-2 border-b-2 transition-colors ${
                            activeTab === 'users' 
                                ? 'border-indigo-600 text-indigo-600' 
                                : 'border-transparent text-slate-500 hover:text-slate-700'
                        }`}
                    >
                        <Users className="w-4 h-4" />
                        User Management
                    </button>
                    <button 
                        onClick={() => setActiveTab('rules')}
                        className={`px-6 py-3 font-medium text-sm flex items-center gap-2 border-b-2 transition-colors ${
                            activeTab === 'rules' 
                                ? 'border-indigo-600 text-indigo-600' 
                                : 'border-transparent text-slate-500 hover:text-slate-700'
                        }`}
                    >
                        <Settings className="w-4 h-4" />
                        Rule Configuration
                    </button>
                </div>

                {/* Tenant Management Tab */}
                {activeTab === 'tenants' && (
                    <div className="space-y-6">
                        {/* Actions bar */}
                        <div className="flex justify-between items-center">
                            <div className="relative w-80">
                                <Search className="absolute left-3 top-2.5 w-5 h-5 text-slate-400" />
                                <input
                                    type="text"
                                    placeholder="Search tenants..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                />
                            </div>
                            <button
                                onClick={() => setShowAddTenant(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium"
                            >
                                <Plus className="w-4 h-4" />
                                Onboard New Tenant
                            </button>
                        </div>

                        {/* Add/Edit Tenant Modal */}
                        {(showAddTenant || editingTenant) && (
                            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                                <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6">
                                    <h3 className="text-xl font-bold text-slate-900 mb-6">
                                        {editingTenant ? 'Edit Tenant' : 'Onboard New Tenant'}
                                    </h3>
                                    
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Tenant Name *
                                            </label>
                                            <input
                                                type="text"
                                                value={formData.name}
                                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                                placeholder="e.g., Dublin Municipality"
                                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            />
                                        </div>

                                        {!editingTenant && (
                                            <div>
                                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                                    Tenant ID (auto-generated if empty)
                                                </label>
                                                <input
                                                    type="text"
                                                    value={formData.tenantId}
                                                    onChange={(e) => setFormData({ ...formData, tenantId: e.target.value })}
                                                    placeholder="e.g., dublin"
                                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                                />
                                            </div>
                                        )}

                                        <div>
                                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                                Database URL (optional)
                                            </label>
                                            <input
                                                type="text"
                                                value={formData.dbUrl}
                                                onChange={(e) => setFormData({ ...formData, dbUrl: e.target.value })}
                                                placeholder="jdbc:postgresql://localhost:5432/tenant_db"
                                                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                            />
                                        </div>

                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                                    DB Username
                                                </label>
                                                <input
                                                    type="text"
                                                    value={formData.dbUsername}
                                                    onChange={(e) => setFormData({ ...formData, dbUsername: e.target.value })}
                                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                                    DB Password
                                                </label>
                                                <input
                                                    type="password"
                                                    value={formData.dbPassword}
                                                    onChange={(e) => setFormData({ ...formData, dbPassword: e.target.value })}
                                                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    <div className="flex justify-end gap-3 mt-8">
                                        <button
                                            onClick={() => {
                                                setShowAddTenant(false);
                                                setEditingTenant(null);
                                                resetForm();
                                            }}
                                            className="px-4 py-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg"
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            onClick={editingTenant ? handleEditTenant : handleAddTenant}
                                            className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium"
                                        >
                                            {editingTenant ? 'Save Changes' : 'Create Tenant'}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Tenants List */}
                        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
                            <table className="w-full">
                                <thead className="bg-slate-50 border-b border-slate-200">
                                    <tr>
                                        <th className="text-left px-6 py-4 text-sm font-semibold text-slate-700">Tenant</th>
                                        <th className="text-left px-6 py-4 text-sm font-semibold text-slate-700">Tenant ID</th>
                                        <th className="text-left px-6 py-4 text-sm font-semibold text-slate-700">Schema</th>
                                        <th className="text-left px-6 py-4 text-sm font-semibold text-slate-700">Database</th>
                                        <th className="text-right px-6 py-4 text-sm font-semibold text-slate-700">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                    {filteredTenants.map((tenant) => (
                                        <tr key={tenant.tenantId} className="hover:bg-slate-50">
                                            <td className="px-6 py-4">
                                                <div className="flex items-center gap-3">
                                                    <div className="w-10 h-10 bg-indigo-100 text-indigo-600 rounded-lg flex items-center justify-center font-bold">
                                                        {tenant.name.charAt(0).toUpperCase()}
                                                    </div>
                                                    <span className="font-medium text-slate-900">{tenant.name}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 text-slate-600 font-mono text-sm">{tenant.tenantId}</td>
                                            <td className="px-6 py-4 text-slate-600 font-mono text-sm">{tenant.schemaName}</td>
                                            <td className="px-6 py-4 text-slate-500 text-sm">
                                                {tenant.dbUrl ? 'Custom DB' : 'Shared Schema'}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <div className="flex items-center justify-end gap-2">
                                                    <button
                                                        onClick={() => {
                                                            setSelectedTenant(tenant.tenantId);
                                                            setActiveTab('rules');
                                                        }}
                                                        className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg"
                                                        title="Configure Rules"
                                                    >
                                                        <Settings className="w-4 h-4" />
                                                    </button>
                                                    <button
                                                        onClick={() => startEdit(tenant)}
                                                        className="p-2 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg"
                                                        title="Edit Tenant"
                                                    >
                                                        <Edit className="w-4 h-4" />
                                                    </button>
                                                    <button
                                                        onClick={() => handleDeleteTenant(tenant.tenantId)}
                                                        className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg"
                                                        title="Delete Tenant"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            
                            {filteredTenants.length === 0 && (
                                <div className="text-center py-12 text-slate-500">
                                    <Building2 className="w-12 h-12 mx-auto mb-4 text-slate-300" />
                                    <p>No tenants found</p>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* User Management Tab */}
                {activeTab === 'users' && (
                    <div className="bg-white border border-slate-200 rounded-xl p-8 text-center">
                        <Users className="w-16 h-16 mx-auto mb-4 text-slate-300" />
                        <h3 className="text-lg font-semibold text-slate-700 mb-2">User Management</h3>
                        <p className="text-slate-500 mb-6">
                            Manage users across all tenants. Create, update, or deactivate user accounts.
                        </p>
                        <p className="text-sm text-slate-400">Coming soon...</p>
                    </div>
                )}

                {/* Rule Configuration Tab */}
                {activeTab === 'rules' && (
                    <div className="space-y-6">
                        <div className="bg-white border border-slate-200 rounded-xl p-6">
                            <h3 className="text-lg font-semibold text-slate-900 mb-4">
                                Select Tenant to Configure Rules
                            </h3>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                {tenants.map((tenant) => (
                                    <button
                                        key={tenant.tenantId}
                                        onClick={() => setSelectedTenant(tenant.tenantId)}
                                        className={`p-4 border rounded-xl text-left transition-all ${
                                            selectedTenant === tenant.tenantId
                                                ? 'border-indigo-600 bg-indigo-50'
                                                : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                                        }`}
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className={`w-10 h-10 rounded-lg flex items-center justify-center font-bold ${
                                                selectedTenant === tenant.tenantId
                                                    ? 'bg-indigo-600 text-white'
                                                    : 'bg-slate-100 text-slate-600'
                                            }`}>
                                                {tenant.name.charAt(0).toUpperCase()}
                                            </div>
                                            <div>
                                                <div className="font-medium text-slate-900">{tenant.name}</div>
                                                <div className="text-xs text-slate-500">{tenant.tenantId}</div>
                                            </div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>

                        {selectedTenant && (
                            <div className="bg-white border border-slate-200 rounded-xl p-6">
                                <div className="flex items-center justify-between mb-6">
                                    <h3 className="text-lg font-semibold text-slate-900">
                                        Rules for: {tenants.find(t => t.tenantId === selectedTenant)?.name}
                                    </h3>
                                    <button className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium flex items-center gap-2">
                                        <Settings className="w-4 h-4" />
                                        Open Rule Editor
                                    </button>
                                </div>
                                
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                                        <div className="text-sm text-slate-500 mb-1">Municipal Tax Rate</div>
                                        <div className="text-2xl font-bold text-slate-900">2.5%</div>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                                        <div className="text-sm text-slate-500 mb-1">Credit Limit Rate</div>
                                        <div className="text-2xl font-bold text-slate-900">2.0%</div>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                                        <div className="text-sm text-slate-500 mb-1">Withholding Frequency</div>
                                        <div className="text-2xl font-bold text-slate-900">Monthly</div>
                                    </div>
                                    <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                                        <div className="text-sm text-slate-500 mb-1">Active Rules</div>
                                        <div className="text-2xl font-bold text-slate-900">12</div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};
