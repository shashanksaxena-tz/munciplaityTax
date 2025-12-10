
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TaxReturnSession, TaxReturnStatus, BusinessProfile, TaxPayerProfile } from '../types';
import { getSessions, createNewSession, deleteSession } from '../services/sessionService';
import { Plus, User, FileText, Calendar, Trash2, ArrowRight, Briefcase, Settings, DollarSign, Shield, BookOpen } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

interface DashboardProps {
  onSelectSession: (session: TaxReturnSession) => void;
  onRegisterBusiness: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectSession, onRegisterBusiness }) => {
  const [sessions, setSessions] = useState<TaxReturnSession[]>([]);
  const navigate = useNavigate();
  const { user } = useAuth();

  // Check if user has admin roles
  const isAdmin = user?.roles?.some(role => 
    ['ROLE_TAX_ADMINISTRATOR', 'ROLE_MANAGER', 'ROLE_ADMIN'].includes(role)
  );
  
  const isAuditor = user?.roles?.some(role => 
    ['ROLE_AUDITOR', 'ROLE_SENIOR_AUDITOR', 'ROLE_SUPERVISOR', 'ROLE_MANAGER', 'ROLE_ADMIN'].includes(role)
  );

  useEffect(() => {
    setSessions(getSessions());
  }, []);

  const handleCreateIndividual = () => {
    const newSession = createNewSession(undefined, undefined, 'INDIVIDUAL');
    onSelectSession(newSession);
  };

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm("Are you sure you want to delete this return? This cannot be undone.")) {
      deleteSession(id);
      setSessions(getSessions());
    }
  };

  const getDisplayName = (session: TaxReturnSession) => {
    if (session.type === 'BUSINESS') {
      return (session.profile as BusinessProfile).businessName || 'Unnamed Business';
    }
    return (session.profile as TaxPayerProfile).name || 'Unnamed Taxpayer';
  };

  const getDisplayId = (session: TaxReturnSession) => {
    if (session.type === 'BUSINESS') {
      return `FEIN: ${(session.profile as BusinessProfile).fein || 'N/A'}`;
    }
    return (session.profile as TaxPayerProfile).ssn ? `***-**-${(session.profile as TaxPayerProfile).ssn}` : 'No SSN';
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 animate-fadeIn">
      <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
        <div>
           <h2 className="text-2xl font-bold text-[#0f1012]">Tax Returns Dashboard</h2>
           <p className="text-[#5d6567]">Manage multiple taxpayer profiles and filings.</p>
        </div>
        <div className="flex gap-3">
          <button 
            onClick={onRegisterBusiness}
            className="flex items-center gap-2 px-6 py-3 bg-[#0f1012] hover:bg-[#0f1012] text-white rounded-xl shadow-lg transition-all font-medium"
          >
            <Briefcase className="w-5 h-5" /> Register Business
          </button>
          <button 
            onClick={handleCreateIndividual}
            className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl shadow-lg transition-all font-medium"
          >
            <Plus className="w-5 h-5" /> Start Individual Return
          </button>
        </div>
      </div>

      {/* Quick Access Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        {/* Ledger Dashboard - Available to all users */}
        <button
          onClick={() => navigate('/ledger')}
          className="flex items-center gap-4 p-4 bg-white border border-[#f0f0f0] rounded-xl hover:border-[#10b981] hover:shadow-md transition-all text-left group"
        >
          <div className="p-3 bg-[#d5faeb] rounded-lg group-hover:bg-[#10b981]/20 transition-colors">
            <DollarSign className="w-6 h-6 text-[#10b981]" />
          </div>
          <div>
            <h3 className="font-semibold text-[#0f1012]">Ledger & Payments</h3>
            <p className="text-sm text-[#5d6567]">View account, payments, and reports</p>
          </div>
          <ArrowRight className="w-5 h-5 text-[#babebf] ml-auto group-hover:text-[#10b981] group-hover:translate-x-1 transition-all" />
        </button>

        {/* Auditor Dashboard - For auditors */}
        {isAuditor && (
          <button
            onClick={() => navigate('/auditor')}
            className="flex items-center gap-4 p-4 bg-white border border-[#f0f0f0] rounded-xl hover:border-[#469fe8] hover:shadow-md transition-all text-left group"
          >
            <div className="p-3 bg-[#ebf4ff] rounded-lg group-hover:bg-[#469fe8]/20 transition-colors">
              <Shield className="w-6 h-6 text-[#469fe8]" />
            </div>
            <div>
              <h3 className="font-semibold text-[#0f1012]">Auditor Dashboard</h3>
              <p className="text-sm text-[#5d6567]">Review and approve returns</p>
            </div>
            <ArrowRight className="w-5 h-5 text-[#babebf] ml-auto group-hover:text-[#469fe8] group-hover:translate-x-1 transition-all" />
          </button>
        )}

        {/* Rule Management - For admins */}
        {isAdmin && (
          <button
            onClick={() => navigate('/admin/rules')}
            className="flex items-center gap-4 p-4 bg-white border border-[#f0f0f0] rounded-xl hover:border-[#970bed] hover:shadow-md transition-all text-left group"
          >
            <div className="p-3 bg-[#970bed]/10 rounded-lg group-hover:bg-[#970bed]/20 transition-colors">
              <Settings className="w-6 h-6 text-[#970bed]" />
            </div>
            <div>
              <h3 className="font-semibold text-[#0f1012]">Admin Dashboard</h3>
              <p className="text-sm text-[#5d6567]">Manage system and users</p>
            </div>
            <ArrowRight className="w-5 h-5 text-[#babebf] ml-auto group-hover:text-[#970bed] group-hover:translate-x-1 transition-all" />
          </button>
        )}
      </div>

      <h3 className="text-lg font-semibold text-[#0f1012] mb-4">Your Tax Returns</h3>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {sessions.length === 0 ? (
          <div className="col-span-full py-16 text-center border-2 border-dashed border-[#dcdede] rounded-2xl bg-[#f8f9fa]">
            <div className="w-16 h-16 bg-[#dcdede] rounded-full flex items-center justify-center mx-auto mb-4">
              <FileText className="w-8 h-8 text-[#babebf]" />
            </div>
            <h3 className="text-lg font-medium text-[#102124]">No Returns Found</h3>
            <p className="text-[#5d6567] mb-6">Create a new return to get started.</p>
          </div>
        ) : (
          sessions.map(session => (
            <div 
              key={session.id}
              onClick={() => onSelectSession(session)}
              className="bg-white border border-[#f0f0f0] rounded-xl p-5 shadow-sm hover:shadow-md hover:border-[#970bed] transition-all cursor-pointer group relative"
            >
               <div className="flex justify-between items-start mb-4">
                  <div className={`px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wide border ${
                    session.status === TaxReturnStatus.SUBMITTED ? 'bg-[#d5faeb] text-[#10b981] border-green-100' :
                    session.status === TaxReturnStatus.AMENDED ? 'bg-[#f59e0b]/10 text-[#f59e0b] border-amber-100' :
                    'bg-[#f0f0f0] text-[#5d6567] border-[#dcdede]'
                  }`}>
                    {session.status}
                  </div>
                  <button onClick={(e) => handleDelete(session.id, e)} className="p-1.5 text-[#babebf] hover:text-[#ec1656] hover:bg-[#ec1656]/10 rounded-lg transition-colors">
                    <Trash2 className="w-4 h-4" />
                  </button>
               </div>

               <div className="space-y-3 mb-6">
                  <div className="flex items-center gap-3">
                     <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${session.type === 'BUSINESS' ? 'bg-[#f0f0f0] text-[#5d6567]' : 'bg-[#ebf4ff] text-[#970bed]'}`}>
                       {session.type === 'BUSINESS' ? <Briefcase className="w-5 h-5" /> : <User className="w-5 h-5" />}
                     </div>
                     <div>
                        <div className="font-bold text-[#0f1012] line-clamp-1">{getDisplayName(session)}</div>
                        <div className="text-xs text-[#5d6567]">{getDisplayId(session)}</div>
                     </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-2 text-sm">
                     <div className="bg-[#f8f9fa] p-2 rounded border border-[#dcdede]">
                        <span className="block text-xs text-[#babebf]">Tax Year</span>
                        <span className="font-medium text-[#102124] flex items-center gap-1"><Calendar className="w-3 h-3"/> {session.settings.taxYear}</span>
                     </div>
                     <div className="bg-[#f8f9fa] p-2 rounded border border-[#dcdede]">
                        <span className="block text-xs text-[#babebf]">Type</span>
                        <span className="font-medium text-[#102124] flex items-center gap-1">
                          {session.type === 'BUSINESS' ? 'Business' : 'Individual'}
                        </span>
                     </div>
                  </div>
               </div>

               <div className="pt-4 border-t border-[#dcdede] flex items-center justify-between text-sm">
                  <span className="text-[#babebf] text-xs">Modified: {new Date(session.lastModifiedDate).toLocaleDateString()}</span>
                  <div className="flex items-center gap-1 text-[#970bed] font-medium group-hover:translate-x-1 transition-transform">
                    Continue <ArrowRight className="w-4 h-4" />
                  </div>
               </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
