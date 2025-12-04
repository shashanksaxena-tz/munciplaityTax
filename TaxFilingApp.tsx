import React, { useState, useEffect } from 'react';
import { api } from './services/api';
import { UploadSection } from './components/UploadSection';
import { ReviewSection } from './components/ReviewSection';
import { ResultsSection } from './components/ResultsSection';
import { RuleConfigurationScreen } from './components/RuleConfigurationScreen';
import { Dashboard } from './components/Dashboard';
import { ExtractionSummary } from './components/ExtractionSummary';
import { BusinessDashboard } from './components/BusinessDashboard';
import { BusinessHistory } from './components/BusinessHistory';
import { WithholdingWizard } from './components/WithholdingWizard';
import { BusinessRegistration } from './components/BusinessRegistration';
import { NetProfitsWizard } from './components/NetProfitsWizard';
import { ReconciliationWizard } from './components/ReconciliationWizard';
import { BusinessRuleConfigScreen } from './components/BusinessRuleConfigScreen';
import { TaxFormData, AppStep, TaxCalculationResult, TaxFormType, W2Form, Form1099, W2GForm, ScheduleC, ScheduleE, ScheduleF, LocalTaxForm, TaxPayerProfile, TaxReturnSettings, TaxRulesConfig, TaxReturnSession, TaxReturnStatus, BusinessProfile, FilingFrequency, WithholdingReturnData, NetProfitReturnData, ReconciliationReturnData, BusinessTaxRulesConfig } from './types';
import { DEFAULT_TAX_RULES, DEFAULT_BUSINESS_RULES } from './constants';
import { saveSession, createNewSession } from './services/sessionService';
import { Calculator, Settings, Briefcase, User, Home, Save, CheckCircle } from 'lucide-react';

import { useAuth } from './contexts/AuthContext';

export default function TaxFilingApp() {
  const { user } = useAuth();
  const [currentSession, setCurrentSession] = useState<TaxReturnSession | null>(null);
  const [step, setStep] = useState<AppStep>(AppStep.DASHBOARD);

  // State
  const [forms, setForms] = useState<TaxFormData[]>([]);
  const [individualForms, setIndividualForms] = useState<TaxFormData[]>([]);
  const [businessForms, setBusinessForms] = useState<TaxFormData[]>([]);
  const [pendingForms, setPendingForms] = useState<TaxFormData[]>([]);
  const [pendingProfile, setPendingProfile] = useState<TaxPayerProfile | undefined>();
  const [pendingSettings, setPendingSettings] = useState<any | undefined>();
  const [pendingPdfData, setPendingPdfData] = useState<string | undefined>(undefined);
  const [pendingFormProvenances, setPendingFormProvenances] = useState<any[] | undefined>(undefined);
  const [pendingSummary, setPendingSummary] = useState<any | undefined>(undefined);
  const [sessionPdfData, setSessionPdfData] = useState<string | undefined>(undefined);
  const [sessionFormProvenances, setSessionFormProvenances] = useState<any[] | undefined>(undefined);
  const [calculationResult, setCalculationResult] = useState<TaxCalculationResult | null>(null);
  const [taxPayerProfile, setTaxPayerProfile] = useState<TaxPayerProfile>({ name: '', address: { street: '', city: '', state: '', zip: '' } });
  const [returnSettings, setReturnSettings] = useState<TaxReturnSettings>({ taxYear: new Date().getFullYear() - 1, isAmendment: false });
  const [savedNotify, setSavedNotify] = useState(false);

  // Configs
  const [showRulesEngine, setShowRulesEngine] = useState(false);
  const [taxRules, setTaxRules] = useState<TaxRulesConfig>(DEFAULT_TAX_RULES);
  const [businessRules, setBusinessRules] = useState<BusinessTaxRulesConfig>(DEFAULT_BUSINESS_RULES);
  const [showBizRules, setShowBizRules] = useState(false);

  // Sync Session on Load
  useEffect(() => {
    if (currentSession) {
      if (currentSession.type === 'BUSINESS') {
        setBusinessForms(currentSession.forms);
        if (![AppStep.BUSINESS_DASHBOARD, AppStep.WITHHOLDING_WIZARD, AppStep.NET_PROFITS_WIZARD, AppStep.RECONCILIATION_WIZARD, AppStep.BUSINESS_HISTORY, AppStep.BUSINESS_RULES].includes(step)) {
          setStep(AppStep.BUSINESS_DASHBOARD);
        }
      } else {
        setIndividualForms(currentSession.forms);
        setTaxPayerProfile(currentSession.profile as TaxPayerProfile);
        setReturnSettings(currentSession.settings);
        setCalculationResult(currentSession.lastCalculationResult as TaxCalculationResult || null);

        if (currentSession.status === TaxReturnStatus.SUBMITTED && currentSession.lastCalculationResult) {
          setStep(AppStep.RESULTS);
        } else if (currentSession.forms.length > 0 && step === AppStep.DASHBOARD) {
          setStep(AppStep.REVIEW);
        } else if (step === AppStep.DASHBOARD) {
          setStep(AppStep.UPLOAD);
        }
      }
    } else {
      if (step !== AppStep.REGISTER_BUSINESS) {
        setStep(AppStep.DASHBOARD);
      }
    }
  }, [currentSession]);

  const handleSave = () => {
    if (currentSession) {
      const updated = {
        ...currentSession,
        profile: currentSession.type === 'INDIVIDUAL' ? taxPayerProfile : currentSession.profile,
        settings: returnSettings,
        forms: currentSession.type === 'INDIVIDUAL' ? individualForms : businessForms,
        lastCalculationResult: calculationResult || undefined,
        lastModifiedDate: new Date().toISOString()
      };
      saveSession(updated);
      setCurrentSession(updated);
      setSavedNotify(true);
      setTimeout(() => setSavedNotify(false), 2000);
    }
  };

  const handleExtraction = (result: any) => {
    // Result can be array or object with extractedProfile
    if (Array.isArray(result)) {
      setPendingForms(result);
      setPendingPdfData(undefined);
      setPendingFormProvenances(undefined);
      setPendingSummary(undefined);
    } else {
      setPendingForms(result.forms || []);
      setPendingProfile(result.extractedProfile);
      setPendingSettings(result.extractedSettings);
      setPendingPdfData(result.pdfData);
      setPendingFormProvenances(result.formProvenances);
      setPendingSummary(result.summary);
    }
    setStep(AppStep.SUMMARY);
  };

  const handleConfirmExtraction = () => {
    if (currentSession?.type === 'BUSINESS') {
      setBusinessForms([...businessForms, ...pendingForms]);
      if (currentSession) {
        const updated = { ...currentSession, forms: [...businessForms, ...pendingForms] };
        saveSession(updated);
        setCurrentSession(updated);
      }
      setStep(AppStep.BUSINESS_DASHBOARD);
    } else {
      const updatedForms = [...individualForms, ...pendingForms];
      setIndividualForms(updatedForms);

      // Store PDF data and provenances for this session
      if (pendingPdfData) {
        setSessionPdfData(pendingPdfData);
      }
      if (pendingFormProvenances) {
        setSessionFormProvenances(pendingFormProvenances);
      }

      let newProfile = { ...taxPayerProfile };

      // Merge with pending extracted profile
      if (pendingProfile) {
        newProfile = { ...newProfile, ...pendingProfile };
      }

      // Fallback to form data if profile incomplete
      if (!newProfile.name && pendingForms.length > 0) {
        const first = pendingForms[0] as any;
        if (first.employeeInfo) newProfile = { ...newProfile, ...first.employeeInfo };
        else if (first.employee) newProfile = { ...newProfile, name: first.employee };
        else if (first.recipient) newProfile = { ...newProfile, name: first.recipient };

        if (first.employeeInfo?.address) newProfile.address = first.employeeInfo.address;
      }
      setTaxPayerProfile(newProfile);

      // Update settings
      if (pendingSettings) {
        setReturnSettings({ ...returnSettings, ...pendingSettings });
      }

      if (currentSession) {
        const updated = { ...currentSession, forms: updatedForms, profile: newProfile, settings: pendingSettings ? { ...returnSettings, ...pendingSettings } : returnSettings };
        saveSession(updated);
        setCurrentSession(updated);
      }
      setStep(AppStep.REVIEW);
    }
    setPendingForms([]);
    setPendingProfile(undefined);
    setPendingPdfData(undefined);
    setPendingFormProvenances(undefined);
    setPendingSummary(undefined);
  };

  const handleAddManualForm = (type: TaxFormType) => {
    const base = { id: crypto.randomUUID(), fileName: 'Manual Entry', taxYear: returnSettings.taxYear, formType: type, confidenceScore: 1.0, owner: 'PRIMARY' };
    let newForm: TaxFormData;

    switch (type) {
      case TaxFormType.W2:
        newForm = { ...base, employer: '', employerEin: '', employerAddress: { street: '', city: '', state: '', zip: '' }, employee: '', federalWages: 0, medicareWages: 0, localWages: 0, localWithheld: 0, locality: '' } as W2Form;
        break;
      case TaxFormType.SCHEDULE_C:
        newForm = { ...base, businessName: '', businessEin: '', principalBusiness: '', businessCode: '', businessAddress: { street: '', city: '', state: '', zip: '' }, grossReceipts: 0, totalExpenses: 0, netProfit: 0 } as ScheduleC;
        break;
      case TaxFormType.SCHEDULE_E:
        newForm = { ...base, rentals: [], partnerships: [], totalNetIncome: 0 } as ScheduleE;
        break;
      case TaxFormType.SCHEDULE_F:
        newForm = { ...base, businessName: '', principalProduct: '', businessCode: '', ein: '', grossIncome: 0, totalExpenses: 0, netFarmProfit: 0 } as ScheduleF;
        break;
      case TaxFormType.W2G:
        newForm = { ...base, payer: '', payerEin: '', payerAddress: { street: '', city: '', state: '', zip: '' }, recipient: '', grossWinnings: 0, dateWon: '', typeOfWager: '', federalWithheld: 0, stateWithheld: 0, localWinnings: 0, localWithheld: 0, locality: '' } as W2GForm;
        break;
      case TaxFormType.FORM_1099_NEC:
      case TaxFormType.FORM_1099_MISC:
        newForm = { ...base, payer: '', recipient: '', incomeAmount: 0, federalWithheld: 0, stateWithheld: 0, localWithheld: 0, locality: '' } as Form1099;
        break;
      case TaxFormType.LOCAL_1040:
      case TaxFormType.LOCAL_1040_EZ:
      case TaxFormType.FORM_R:
        newForm = { ...base, reportedTaxableIncome: 0, reportedTaxDue: 0 } as LocalTaxForm;
        break;
      default:
        newForm = { ...base } as TaxFormData;
    }

    const updated = [...individualForms, newForm];
    setIndividualForms(updated);
    if (currentSession) {
      const s = { ...currentSession, forms: updated };
      saveSession(s);
      setCurrentSession(s);
    }
  };

  const handleCalculate = async () => {
    try {
      // Call Java Backend
      if (!currentSession) {
        console.error("No active session to calculate taxes for.");
        alert("No active session. Please start or load a session.");
        return;
      }

      const result = await api.taxEngine.calculateIndividual(
        currentSession.forms, // Use currentSession.forms for consistency with the diff's intent
        currentSession.profile as TaxPayerProfile,
        currentSession.settings,
        DEFAULT_TAX_RULES
      );

      setCurrentSession(prev => prev ? {
        ...prev,
        lastCalculationResult: result
      } : null);
      setCalculationResult(result); // Also update local state for immediate display
      setStep(AppStep.RESULTS);

      // Save results immediately
      if (currentSession) {
        const updated = {
          ...currentSession,
          profile: currentSession.type === 'INDIVIDUAL' ? taxPayerProfile : currentSession.profile,
          settings: returnSettings,
          forms: individualForms,
          lastCalculationResult: result,
          lastModifiedDate: new Date().toISOString()
        };
        saveSession(updated);
        setCurrentSession(updated);
      }
    } catch (error) {
      console.error("Calculation failed", error);
      alert("Failed to calculate taxes. Please try again.");
    }
  };

  const handleBusinessFilingComplete = (data: WithholdingReturnData) => {
    if (currentSession?.type === 'BUSINESS') {
      const updated = { ...currentSession, businessFilings: [...(currentSession.businessFilings || []), data] };
      saveSession(updated);
      setCurrentSession(updated);
      setStep(AppStep.BUSINESS_DASHBOARD);
    }
  };

  const handleNetProfitComplete = async (data: NetProfitReturnData) => {
    if (currentSession?.type === 'BUSINESS') {
      try {
        const result = await api.taxEngine.calculateBusiness(
          data.taxYear,
          data.estimatedPayments,
          data.priorYearCredit,
          data.reconciliation,
          data.allocation,
          data.nolAvailable,
          businessRules
        );

        const updated = { ...currentSession, netProfitFilings: [...(currentSession.netProfitFilings || []), result] };
        saveSession(updated);
        setCurrentSession(updated);
        setStep(AppStep.BUSINESS_DASHBOARD);
      } catch (error) {
        console.error("Business Calculation failed", error);
        alert("Failed to calculate business taxes.");
      }
    }
  };

  const handleReconciliationComplete = (data: ReconciliationReturnData) => {
    if (currentSession?.type === 'BUSINESS') {
      const updated = { ...currentSession, reconciliations: [...(currentSession.reconciliations || []), data] };
      saveSession(updated);
      setCurrentSession(updated);
      setStep(AppStep.BUSINESS_DASHBOARD);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-900 pb-20">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => setStep(AppStep.DASHBOARD)} className="p-2 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors">
              <Home className="w-5 h-5" />
            </button>
            <div className="h-6 w-px bg-slate-200"></div>
            <h1 className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-700 to-blue-600">
              MuniTax <span className="font-light text-slate-400">| Dublin</span>
            </h1>
          </div>

          <div className="flex items-center gap-3">
            {currentSession && (
              <button onClick={handleSave} className="flex items-center gap-2 px-3 py-1.5 bg-indigo-50 text-indigo-700 rounded-lg text-sm font-bold hover:bg-indigo-100 transition-colors">
                {savedNotify ? <CheckCircle className="w-4 h-4" /> : <Save className="w-4 h-4" />}
                {savedNotify ? "Saved!" : "Save"}
              </button>
            )}

            {currentSession?.type === 'INDIVIDUAL' && step !== AppStep.DASHBOARD && (
              <button
                onClick={() => setShowRulesEngine(true)}
                className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                title="Tax Rule Engine"
              >
                <Settings className="w-5 h-5" />
              </button>
            )}
          </div>
        </div>
      </header>

      {showRulesEngine && (
        <RuleConfigurationScreen
          rules={taxRules}
          onUpdateRules={setTaxRules}
          onClose={() => setShowRulesEngine(false)}
        />
      )}

      {showBizRules && (
        <BusinessRuleConfigScreen
          rules={businessRules}
          onUpdate={setBusinessRules}
          onClose={() => setShowBizRules(false)}
        />
      )}

      <main className="max-w-7xl mx-auto px-4 py-8">

        {step === AppStep.DASHBOARD && (
          <Dashboard
            onSelectSession={setCurrentSession}
            onRegisterBusiness={() => setStep(AppStep.REGISTER_BUSINESS)}
          />
        )}

        {step === AppStep.REGISTER_BUSINESS && (
          <BusinessRegistration
            onRegister={(profile) => {
              const s = createNewSession(profile, undefined, 'BUSINESS');
              setCurrentSession(s);
              setStep(AppStep.BUSINESS_DASHBOARD);
            }}
            onCancel={() => setStep(AppStep.DASHBOARD)}
          />
        )}

        {/* INDIVIDUAL FLOW */}
        {step === AppStep.UPLOAD && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-center">Let's start your return</h2>
            <UploadSection onDataExtracted={handleExtraction} />
            <div className="text-center">
              <button onClick={() => setStep(AppStep.REVIEW)} className="text-indigo-600 font-medium hover:underline">Skip to Manual Entry</button>
            </div>
          </div>
        )}

        {step === AppStep.SUMMARY && (
          <ExtractionSummary
            forms={pendingForms}
            summary={pendingSummary}
            formProvenances={pendingFormProvenances}
            pdfData={pendingPdfData}
            onConfirm={handleConfirmExtraction}
            onCancel={() => { 
              setPendingForms([]); 
              setPendingPdfData(undefined);
              setPendingFormProvenances(undefined);
              setPendingSummary(undefined);
              setStep(AppStep.UPLOAD); 
            }}
          />
        )}

        {step === AppStep.REVIEW && currentSession && (
          <ReviewSection
            forms={individualForms}
            profile={taxPayerProfile}
            settings={returnSettings}
            onUpdateProfile={setTaxPayerProfile}
            onUpdateSettings={setReturnSettings}
            onUpdateForm={(updated) => {
              const newForms = individualForms.map(f => f.id === updated.id ? updated : f);
              setIndividualForms(newForms);
            }}
            onDeleteForm={(id) => setIndividualForms(individualForms.filter(f => f.id !== id))}
            onAddManualForm={handleAddManualForm}
            onCalculate={handleCalculate}
            onBack={() => setStep(AppStep.UPLOAD)}
            rules={taxRules}
            isSubmitted={currentSession.status === TaxReturnStatus.SUBMITTED}
          />
        )}

        {step === AppStep.RESULTS && calculationResult && (
          <ResultsSection
            result={calculationResult}
            onReset={() => { setStep(AppStep.UPLOAD); setIndividualForms([]); }}
            onBack={() => setStep(AppStep.REVIEW)}
            isSubmitted={currentSession?.status === TaxReturnStatus.SUBMITTED}
            onSubmit={async (submissionData) => {
              if (currentSession && user) {
                try {
                  await api.submission.submitReturn({
                    tenantId: user.tenantId || 'dublin',
                    userId: user.id,
                    taxYear: currentSession.settings.taxYear.toString(),
                    refundChoice: submissionData?.refundChoice,
                    // Add other fields as needed by Submission entity
                  });
                  const s = { ...currentSession, status: TaxReturnStatus.SUBMITTED };
                  saveSession(s);
                  setCurrentSession(s);
                  alert("Return submitted successfully!");
                } catch (e) {
                  console.error("Submission failed", e);
                  alert("Failed to submit return.");
                }
              }
            }}
            onAmend={() => {
              if (currentSession) {
                const s = { ...currentSession, status: TaxReturnStatus.AMENDED, settings: { ...currentSession.settings, isAmendment: true } };
                saveSession(s);
                setCurrentSession(s);
                setReturnSettings(s.settings);
                setStep(AppStep.REVIEW);
              }
            }}
          />
        )}

        {/* BUSINESS FLOW */}
        {step === AppStep.BUSINESS_DASHBOARD && currentSession && (
          <BusinessDashboard
            session={currentSession}
            onStartWithholding={() => setStep(AppStep.WITHHOLDING_WIZARD)}
            onStartNetProfits={() => setStep(AppStep.NET_PROFITS_WIZARD)}
            onStartReconciliation={() => setStep(AppStep.RECONCILIATION_WIZARD)}
            onViewHistory={() => setStep(AppStep.BUSINESS_HISTORY)}
            onOpenRules={() => setShowBizRules(true)}
            onUploadDocs={() => alert("Upload documents in wizard steps.")}
          />
        )}

        {step === AppStep.WITHHOLDING_WIZARD && currentSession && (
          <WithholdingWizard
            profile={currentSession.profile as BusinessProfile}
            onBack={() => setStep(AppStep.BUSINESS_DASHBOARD)}
            onComplete={handleBusinessFilingComplete}
          />
        )}

        {step === AppStep.NET_PROFITS_WIZARD && currentSession && (
          <NetProfitsWizard
            profile={currentSession.profile as BusinessProfile}
            onBack={() => setStep(AppStep.BUSINESS_DASHBOARD)}
            onComplete={handleNetProfitComplete}
          />
        )}

        {step === AppStep.RECONCILIATION_WIZARD && currentSession && (
          <ReconciliationWizard
            profile={currentSession.profile as BusinessProfile}
            filings={currentSession.businessFilings || []}
            onBack={() => setStep(AppStep.BUSINESS_DASHBOARD)}
            onComplete={handleReconciliationComplete}
          />
        )}

        {step === AppStep.BUSINESS_HISTORY && currentSession && (
          <BusinessHistory
            profile={currentSession.profile as BusinessProfile}
            history={currentSession.businessFilings}
            onBack={() => setStep(AppStep.BUSINESS_DASHBOARD)}
          />
        )}

      </main>
    </div>
  );
}
