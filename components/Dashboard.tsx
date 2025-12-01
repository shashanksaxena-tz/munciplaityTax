
import React, { useEffect, useState } from 'react';
import { TaxReturnSession, TaxReturnStatus, BusinessProfile, TaxPayerProfile } from '../types';
import { getSessions, createNewSession, deleteSession, fetchSessions, isCacheLoaded } from '../services/sessionService';
import { Plus, User, FileText, Calendar, Trash2, ArrowRight, Briefcase, Loader2 } from 'lucide-react';

interface DashboardProps {
  onSelectSession: (session: TaxReturnSession) => void;
  onRegisterBusiness: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectSession, onRegisterBusiness }) => {
  const [sessions, setSessions] = useState<TaxReturnSession[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadSessions = async () => {
      setIsLoading(true);
      try {
        // Fetch sessions from API if cache is not loaded
        if (!isCacheLoaded()) {
          const fetchedSessions = await fetchSessions();
          setSessions(fetchedSessions);
        } else {
          setSessions(getSessions());
        }
      } catch (error) {
        console.error('Failed to load sessions:', error);
        // Fallback to cached data
        setSessions(getSessions());
      } finally {
        setIsLoading(false);
      }
    };
    
    loadSessions();
  }, []);

  const handleCreateIndividual = () => {
    const newSession = createNewSession(undefined, undefined, 'INDIVIDUAL');
    onSelectSession(newSession);
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
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

  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-8 animate-fadeIn">
        <div className="flex flex-col items-center justify-center py-16">
          <Loader2 className="w-12 h-12 text-indigo-600 animate-spin mb-4" />
          <p className="text-slate-500">Loading your tax returns...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 animate-fadeIn">
      <div className="flex flex-col md:flex-row justify-between items-center mb-8 gap-4">
        <div>
           <h2 className="text-2xl font-bold text-slate-900">Tax Returns Dashboard</h2>
           <p className="text-slate-500">Manage multiple taxpayer profiles and filings.</p>
        </div>
        <div className="flex gap-3">
          <button 
            onClick={onRegisterBusiness}
            className="flex items-center gap-2 px-6 py-3 bg-slate-800 hover:bg-slate-900 text-white rounded-xl shadow-lg transition-all font-medium"
          >
            <Briefcase className="w-5 h-5" /> Register Business
          </button>
          <button 
            onClick={handleCreateIndividual}
            className="flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-lg shadow-indigo-200 transition-all font-medium"
          >
            <Plus className="w-5 h-5" /> Start Individual Return
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {sessions.length === 0 ? (
          <div className="col-span-full py-16 text-center border-2 border-dashed border-slate-300 rounded-2xl bg-slate-50">
            <div className="w-16 h-16 bg-slate-200 rounded-full flex items-center justify-center mx-auto mb-4">
              <FileText className="w-8 h-8 text-slate-400" />
            </div>
            <h3 className="text-lg font-medium text-slate-700">No Returns Found</h3>
            <p className="text-slate-500 mb-6">Create a new return to get started.</p>
          </div>
        ) : (
          sessions.map(session => (
            <div 
              key={session.id}
              onClick={() => onSelectSession(session)}
              className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:shadow-md hover:border-indigo-200 transition-all cursor-pointer group relative"
            >
               <div className="flex justify-between items-start mb-4">
                  <div className={`px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wide border ${
                    session.status === TaxReturnStatus.SUBMITTED ? 'bg-green-50 text-green-700 border-green-100' :
                    session.status === TaxReturnStatus.AMENDED ? 'bg-amber-50 text-amber-700 border-amber-100' :
                    'bg-slate-100 text-slate-600 border-slate-200'
                  }`}>
                    {session.status}
                  </div>
                  <button onClick={(e) => handleDelete(session.id, e)} className="p-1.5 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors">
                    <Trash2 className="w-4 h-4" />
                  </button>
               </div>

               <div className="space-y-3 mb-6">
                  <div className="flex items-center gap-3">
                     <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${session.type === 'BUSINESS' ? 'bg-slate-100 text-slate-700' : 'bg-indigo-50 text-indigo-600'}`}>
                       {session.type === 'BUSINESS' ? <Briefcase className="w-5 h-5" /> : <User className="w-5 h-5" />}
                     </div>
                     <div>
                        <div className="font-bold text-slate-900 line-clamp-1">{getDisplayName(session)}</div>
                        <div className="text-xs text-slate-500">{getDisplayId(session)}</div>
                     </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-2 text-sm">
                     <div className="bg-slate-50 p-2 rounded border border-slate-100">
                        <span className="block text-xs text-slate-400">Tax Year</span>
                        <span className="font-medium text-slate-700 flex items-center gap-1"><Calendar className="w-3 h-3"/> {session.settings.taxYear}</span>
                     </div>
                     <div className="bg-slate-50 p-2 rounded border border-slate-100">
                        <span className="block text-xs text-slate-400">Type</span>
                        <span className="font-medium text-slate-700 flex items-center gap-1">
                          {session.type === 'BUSINESS' ? 'Business' : 'Individual'}
                        </span>
                     </div>
                  </div>
               </div>

               <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-sm">
                  <span className="text-slate-400 text-xs">Modified: {new Date(session.lastModifiedDate).toLocaleDateString()}</span>
                  <div className="flex items-center gap-1 text-indigo-600 font-medium group-hover:translate-x-1 transition-transform">
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
