import React, { useState, useEffect } from 'react';
import { BusinessProfile, ReconciliationReturnData, WithholdingReturnData, W3ReconciliationData, W3DiscrepancyData, W3FormData } from '../types';
import { api } from '../services/api';
import { ArrowRight, AlertOctagon, CheckCircle, ChevronLeft, ChevronRight, FileText, AlertTriangle, Loader, PenLine, Check, X } from 'lucide-react';

interface ReconciliationWizardProps {
  profile: BusinessProfile;
  filings: WithholdingReturnData[];
  onBack: () => void;
  onComplete: (data: ReconciliationReturnData) => void;
}

type WizardStep = 'summary' | 'w3-form' | 'discrepancies' | 'review' | 'submit';

export const ReconciliationWizard: React.FC<ReconciliationWizardProps> = ({ profile, filings, onBack, onComplete }) => {
  const [step, setStep] = useState<WizardStep>('summary');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reconciliation, setReconciliation] = useState<W3ReconciliationData | null>(null);
  const [discrepancyData, setDiscrepancyData] = useState<W3DiscrepancyData | null>(null);
  const [formData, setFormData] = useState<W3FormData>({
    totalW2Tax: 0,
    w2FormCount: 0,
    totalEmployees: 0,
    notes: '',
    eSignature: '',
    eSignatureDate: ''
  });

  const currentYear = new Date().getFullYear() - 1;
  const yearFilings = filings.filter(f => f.period.year === currentYear);
  const totalW1 = yearFilings.reduce((acc, f) => acc + f.taxDue, 0);

  // Fetch or create W-3 reconciliation
  useEffect(() => {
    const fetchReconciliation = async () => {
      setLoading(true);
      setError(null);
      try {
        // Try to fetch existing reconciliation
        const data = await api.w3Reconciliation.getByYear(profile.fein, currentYear);
        setReconciliation(data);
        setFormData({
          totalW2Tax: data.totalW2Tax,
          w2FormCount: data.w2FormCount,
          totalEmployees: data.totalEmployees || 0,
          notes: data.notes || '',
          eSignature: '',
          eSignatureDate: ''
        });
      } catch (err) {
        // If not found, create new one
        try {
          const newData = await api.w3Reconciliation.create({
            businessId: profile.fein,
            taxYear: currentYear,
            totalW2Tax: 0,
            w2FormCount: 0,
            totalEmployees: 0,
            notes: ''
          });
          setReconciliation(newData);
        } catch (createErr) {
          setError('Failed to initialize W-3 reconciliation');
          console.error('Error:', createErr);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchReconciliation();
  }, [profile.fein, currentYear]);

  // Fetch discrepancy data when needed
  const fetchDiscrepancies = async () => {
    if (!reconciliation) return;
    
    setLoading(true);
    try {
      const data = await api.w3Reconciliation.getDiscrepancies(reconciliation.id);
      setDiscrepancyData(data);
    } catch (err) {
      setError('Failed to fetch discrepancy details');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!reconciliation) return;
    
    setLoading(true);
    setError(null);
    try {
      const response = await api.w3Reconciliation.submit(reconciliation.id);
      
      // Convert to legacy format for onComplete
      const result: ReconciliationReturnData = {
        id: response.id,
        dateFiled: response.filingDate || new Date().toISOString(),
        taxYear: response.taxYear,
        totalW1Tax: response.totalW1Tax,
        totalW2Tax: response.totalW2Tax,
        discrepancy: response.discrepancy,
        status: response.status,
        confirmationNumber: response.confirmationNumber
      };
      
      onComplete(result);
    } catch (err) {
      setError('Failed to submit W-3 reconciliation');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const renderStepIndicator = () => {
    const steps = [
      { id: 'summary', label: 'Summary' },
      { id: 'w3-form', label: 'W-3 Form' },
      { id: 'discrepancies', label: 'Discrepancies' },
      { id: 'review', label: 'Review' }
    ];
    
    const currentIndex = steps.findIndex(s => s.id === step);
    
    return (
      <div className="flex items-center justify-between mb-8">
        {steps.map((s, idx) => (
          <React.Fragment key={s.id}>
            <div className="flex items-center">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                idx <= currentIndex ? 'bg-[#970bed] text-white' : 'bg-[#dcdede] text-[#5d6567]'
              }`}>
                {idx < currentIndex ? <Check className="w-4 h-4" /> : idx + 1}
              </div>
              <div className="ml-2 text-sm font-medium text-[#0f1012]">{s.label}</div>
            </div>
            {idx < steps.length - 1 && (
              <div className={`flex-1 h-1 mx-4 rounded ${idx < currentIndex ? 'bg-[#970bed]' : 'bg-[#dcdede]'}`} />
            )}
          </React.Fragment>
        ))}
      </div>
    );
  };

  const renderSummaryStep = () => (
    <div className="space-y-6">
      <div className="bg-[#ebf4ff] border border-[#469fe8]/30 p-6 rounded-xl">
        <div className="text-sm font-bold text-[#469fe8] uppercase mb-2">W-1 Filings Summary</div>
        <div className="text-3xl font-bold text-[#0f1012] mb-2">
          ${totalW1.toLocaleString(undefined, {minimumFractionDigits: 2})}
        </div>
        <div className="text-sm text-[#5d6567]">
          Total tax from {yearFilings.length} W-1 filings for {currentYear}
        </div>
        <div className="mt-4 pt-4 border-t border-[#469fe8]/20 space-y-2">
          {yearFilings.map(filing => (
            <div key={filing.id} className="flex justify-between text-sm">
              <span className="text-[#5d6567]">{filing.period.period}</span>
              <span className="font-mono font-medium">${filing.taxDue.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
            </div>
          ))}
        </div>
      </div>

      {reconciliation && (
        <>
          <div className="bg-white border border-[#dcdede] p-6 rounded-xl">
            <div className="text-sm font-bold text-[#0f1012] uppercase mb-2">W-2 Totals</div>
            <div className="text-3xl font-bold text-[#0f1012] mb-2">
              ${reconciliation.totalW2Tax.toLocaleString(undefined, {minimumFractionDigits: 2})}
            </div>
            <div className="text-sm text-[#5d6567]">
              Total from {reconciliation.w2FormCount} W-2 forms • {reconciliation.totalEmployees || 0} employees
            </div>
          </div>

          <div className={`p-6 rounded-xl border ${
            reconciliation.status === 'BALANCED' 
              ? 'bg-[#d5faeb] border-[#10b981]/30' 
              : 'bg-[#fef2f2] border-[#ef4444]/30'
          }`}>
            <div className="flex items-start gap-4">
              {reconciliation.status === 'BALANCED' ? (
                <CheckCircle className="w-6 h-6 text-[#10b981] flex-shrink-0" />
              ) : (
                <AlertOctagon className="w-6 h-6 text-[#ef4444] flex-shrink-0" />
              )}
              <div className="flex-1">
                <div className="font-bold text-[#0f1012] mb-1">
                  {reconciliation.status === 'BALANCED' ? 'Reconciliation Balanced' : 'Discrepancy Detected'}
                </div>
                <div className="text-sm text-[#5d6567] mb-3">
                  Difference: <span className="font-mono font-bold text-[#0f1012]">
                    ${Math.abs(reconciliation.discrepancy).toLocaleString(undefined, {minimumFractionDigits: 2})}
                  </span>
                  {reconciliation.status === 'UNBALANCED' && ' - Review and resolve before submission'}
                </div>
                {reconciliation.totalPenalties > 0 && (
                  <div className="text-sm">
                    <span className="font-medium text-[#ef4444]">Penalties: </span>
                    <span className="font-mono">${reconciliation.totalPenalties.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      <div className="flex gap-3">
        <button
          onClick={onBack}
          className="px-6 py-3 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#0f1012] rounded-xl font-bold"
        >
          <ChevronLeft className="w-4 h-4 inline mr-2" />
          Back
        </button>
        <button
          onClick={() => setStep('w3-form')}
          disabled={loading}
          className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold disabled:opacity-50"
        >
          Continue to W-3 Form
          <ChevronRight className="w-4 h-4 inline ml-2" />
        </button>
      </div>
    </div>
  );

  const renderW3FormStep = () => (
    <div className="space-y-6">
      <div className="bg-[#fbfbfb] border border-[#dcdede] p-4 rounded-xl">
        <div className="flex items-center gap-2 text-sm text-[#5d6567]">
          <FileText className="w-4 h-4" />
          <span>Complete the W-3 form with W-2 totals. Fields marked with * are required.</span>
        </div>
      </div>

      <div className="bg-white border border-[#dcdede] p-6 rounded-xl space-y-4">
        <div>
          <label className="block text-sm font-bold text-[#0f1012] mb-2">
            Total Local Tax Withheld (Box 19 from W-2s) *
          </label>
          <input
            type="number"
            step="0.01"
            value={formData.totalW2Tax}
            onChange={e => setFormData({...formData, totalW2Tax: parseFloat(e.target.value) || 0})}
            className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none"
            placeholder="0.00"
          />
          <p className="text-xs text-[#5d6567] mt-1">Sum of all Box 19 amounts from employee W-2 forms</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-bold text-[#0f1012] mb-2">
              Number of W-2 Forms *
            </label>
            <input
              type="number"
              value={formData.w2FormCount}
              onChange={e => setFormData({...formData, w2FormCount: parseInt(e.target.value) || 0})}
              className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none"
              placeholder="0"
            />
          </div>
          
          <div>
            <label className="block text-sm font-bold text-[#0f1012] mb-2">
              Total Employees
            </label>
            <input
              type="number"
              value={formData.totalEmployees}
              onChange={e => setFormData({...formData, totalEmployees: parseInt(e.target.value) || 0})}
              className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none"
              placeholder="0"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-bold text-[#0f1012] mb-2">
            Notes / Explanation
          </label>
          <textarea
            value={formData.notes}
            onChange={e => setFormData({...formData, notes: e.target.value})}
            className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none resize-none"
            rows={4}
            placeholder="Optional notes about this reconciliation..."
          />
        </div>
      </div>

      <div className="flex gap-3">
        <button
          onClick={() => setStep('summary')}
          className="px-6 py-3 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#0f1012] rounded-xl font-bold"
        >
          <ChevronLeft className="w-4 h-4 inline mr-2" />
          Back
        </button>
        <button
          onClick={() => {
            fetchDiscrepancies();
            setStep('discrepancies');
          }}
          disabled={!formData.totalW2Tax || !formData.w2FormCount}
          className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold disabled:opacity-50"
        >
          Review Discrepancies
          <ChevronRight className="w-4 h-4 inline ml-2" />
        </button>
      </div>
    </div>
  );

  const renderDiscrepanciesStep = () => (
    <div className="space-y-6">
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader className="w-6 h-6 animate-spin text-[#970bed]" />
          <span className="ml-3 text-[#5d6567]">Analyzing discrepancies...</span>
        </div>
      ) : discrepancyData ? (
        <>
          <div className={`p-6 rounded-xl border ${
            discrepancyData.status === 'BALANCED'
              ? 'bg-[#d5faeb] border-[#10b981]/30'
              : 'bg-[#fef2f2] border-[#ef4444]/30'
          }`}>
            <div className="flex items-start gap-4">
              {discrepancyData.status === 'BALANCED' ? (
                <CheckCircle className="w-6 h-6 text-[#10b981]" />
              ) : (
                <AlertTriangle className="w-6 h-6 text-[#ef4444]" />
              )}
              <div className="flex-1">
                <div className="font-bold text-[#0f1012] mb-2">{discrepancyData.description}</div>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-[#5d6567]">W-1 Tax Remitted:</span>
                    <span className="font-mono font-bold">${discrepancyData.totalW1Tax.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-[#5d6567]">W-2 Tax Reported:</span>
                    <span className="font-mono font-bold">${discrepancyData.totalW2Tax.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
                  </div>
                  <div className="flex justify-between pt-2 border-t border-current/20">
                    <span className="font-medium">Discrepancy:</span>
                    <span className="font-mono font-bold">
                      ${Math.abs(discrepancyData.discrepancy).toLocaleString(undefined, {minimumFractionDigits: 2})}
                      {discrepancyData.discrepancyPercentage > 0 && ` (${discrepancyData.discrepancyPercentage.toFixed(2)}%)`}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {discrepancyData.missingW1Filings > 0 && (
            <div className="bg-[#fef2f2] border border-[#ef4444]/30 p-4 rounded-xl">
              <div className="flex items-center gap-2 text-sm">
                <AlertTriangle className="w-4 h-4 text-[#ef4444]" />
                <div>
                  <div className="font-bold text-[#ef4444]">Missing W-1 Filings</div>
                  <div className="text-[#5d6567] mt-1">
                    Expected {discrepancyData.expectedW1Filings} filings, found {discrepancyData.w1FilingCount}
                  </div>
                </div>
              </div>
            </div>
          )}

          {discrepancyData.totalPenalties > 0 && (
            <div className="bg-white border border-[#dcdede] p-6 rounded-xl">
              <div className="text-sm font-bold text-[#0f1012] mb-4">Penalties</div>
              <div className="space-y-2">
                {discrepancyData.lateFilingPenalty > 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-[#5d6567]">Late Filing Penalty:</span>
                    <span className="font-mono font-bold text-[#ef4444]">
                      ${discrepancyData.lateFilingPenalty.toLocaleString(undefined, {minimumFractionDigits: 2})}
                    </span>
                  </div>
                )}
                {discrepancyData.missingFilingPenalty > 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-[#5d6567]">Missing Filing Penalty:</span>
                    <span className="font-mono font-bold text-[#ef4444]">
                      ${discrepancyData.missingFilingPenalty.toLocaleString(undefined, {minimumFractionDigits: 2})}
                    </span>
                  </div>
                )}
                <div className="flex justify-between pt-2 border-t border-[#dcdede]">
                  <span className="font-bold">Total Penalties:</span>
                  <span className="font-mono font-bold text-[#ef4444]">
                    ${discrepancyData.totalPenalties.toLocaleString(undefined, {minimumFractionDigits: 2})}
                  </span>
                </div>
              </div>
            </div>
          )}

          {discrepancyData.recommendedAction && (
            <div className="bg-[#ebf4ff] border border-[#469fe8]/30 p-4 rounded-xl">
              <div className="text-sm font-bold text-[#469fe8] mb-2">Recommended Action</div>
              <div className="text-sm text-[#0f1012]">{discrepancyData.recommendedAction}</div>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-12 text-[#5d6567]">
          No discrepancy data available
        </div>
      )}

      <div className="flex gap-3">
        <button
          onClick={() => setStep('w3-form')}
          className="px-6 py-3 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#0f1012] rounded-xl font-bold"
        >
          <ChevronLeft className="w-4 h-4 inline mr-2" />
          Back
        </button>
        <button
          onClick={() => setStep('review')}
          className="flex-1 py-3 bg-gradient-to-r from-[#970bed] to-[#469fe8] hover:from-[#7f09c5] hover:to-[#3a8bd4] text-white rounded-xl font-bold"
        >
          Continue to Review
          <ChevronRight className="w-4 h-4 inline ml-2" />
        </button>
      </div>
    </div>
  );

  const renderReviewStep = () => (
    <div className="space-y-6">
      <div className="bg-[#fbfbfb] border border-[#dcdede] p-4 rounded-xl">
        <div className="flex items-center gap-2 text-sm text-[#5d6567]">
          <AlertTriangle className="w-4 h-4" />
          <span>Review all information carefully before submitting. Submission is final.</span>
        </div>
      </div>

      {reconciliation && (
        <>
          <div className="bg-white border border-[#dcdede] p-6 rounded-xl">
            <div className="text-sm font-bold text-[#0f1012] mb-4">Reconciliation Summary</div>
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-[#5d6567]">Tax Year:</span>
                <span className="font-bold">{reconciliation.taxYear}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-[#5d6567]">Business:</span>
                <span className="font-bold">{profile.businessName}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-[#5d6567]">W-1 Filings:</span>
                <span className="font-mono font-bold">${reconciliation.totalW1Tax.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-[#5d6567]">W-2 Totals:</span>
                <span className="font-mono font-bold">${reconciliation.totalW2Tax.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
              </div>
              <div className="flex justify-between text-sm pt-3 border-t border-[#dcdede]">
                <span className="font-bold">Status:</span>
                <span className={`font-bold ${reconciliation.status === 'BALANCED' ? 'text-[#10b981]' : 'text-[#ef4444]'}`}>
                  {reconciliation.status}
                </span>
              </div>
            </div>
          </div>

          <div className="bg-white border border-[#dcdede] p-6 rounded-xl">
            <div className="text-sm font-bold text-[#0f1012] mb-4">Electronic Signature</div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-bold text-[#0f1012] mb-2">
                  Type your full name to sign *
                </label>
                <input
                  type="text"
                  value={formData.eSignature}
                  onChange={e => setFormData({...formData, eSignature: e.target.value, eSignatureDate: new Date().toISOString()})}
                  className="w-full px-4 py-3 border border-[#dcdede] focus:border-[#970bed] focus:ring-[#970bed]/20 focus:ring-2 rounded-xl outline-none"
                  placeholder="Full Name"
                />
              </div>
              {formData.eSignature && (
                <div className="text-sm text-[#5d6567]">
                  By signing, you certify that the information provided is accurate and complete.
                  <div className="mt-2 font-medium">
                    Date: {new Date().toLocaleString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>
              )}
            </div>
          </div>
        </>
      )}

      <div className="flex gap-3">
        <button
          onClick={() => setStep('discrepancies')}
          className="px-6 py-3 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#0f1012] rounded-xl font-bold"
        >
          <ChevronLeft className="w-4 h-4 inline mr-2" />
          Back
        </button>
        <button
          onClick={handleSubmit}
          disabled={loading || !formData.eSignature}
          className="flex-1 py-3 bg-[#10b981] hover:bg-[#059669] text-white rounded-xl font-bold disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? (
            <>
              <Loader className="w-4 h-4 inline animate-spin mr-2" />
              Submitting...
            </>
          ) : (
            <>
              <Check className="w-4 h-4 inline mr-2" />
              Submit W-3 Reconciliation
            </>
          )}
        </button>
      </div>
    </div>
  );

  if (loading && !reconciliation) {
    return (
      <div className="max-w-3xl mx-auto py-8 animate-fadeIn">
        <div className="flex items-center justify-center py-12">
          <Loader className="w-6 h-6 animate-spin text-[#970bed]" />
          <span className="ml-3 text-[#5d6567]">Loading reconciliation data...</span>
        </div>
      </div>
    );
  }

  if (error && !reconciliation) {
    return (
      <div className="max-w-3xl mx-auto py-8 animate-fadeIn">
        <div className="bg-[#fef2f2] border border-[#ef4444]/30 p-6 rounded-xl">
          <div className="flex items-center gap-3">
            <AlertOctagon className="w-6 h-6 text-[#ef4444]" />
            <div>
              <div className="font-bold text-[#ef4444]">Error</div>
              <div className="text-sm text-[#5d6567] mt-1">{error}</div>
            </div>
          </div>
          <button
            onClick={onBack}
            className="mt-4 px-4 py-2 border border-[#dcdede] hover:bg-[#fbfbfb] text-[#0f1012] rounded-lg"
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-8 animate-fadeIn">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={onBack} className="p-2 hover:bg-[#fbfbfb] rounded-full">
          <ChevronLeft className="w-5 h-5 text-[#5d6567]" />
        </button>
        <div>
          <h2 className="text-xl font-bold text-[#0f1012]">Form W-3: Year-End Reconciliation</h2>
          <p className="text-sm text-[#5d6567]">{profile.businessName} • Tax Year {currentYear}</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-[#dcdede] p-8">
        {renderStepIndicator()}
        
        {error && step !== 'summary' && (
          <div className="mb-6 bg-[#fef2f2] border border-[#ef4444]/30 p-4 rounded-xl">
            <div className="flex items-center gap-2 text-sm text-[#ef4444]">
              <AlertOctagon className="w-4 h-4" />
              <span>{error}</span>
            </div>
          </div>
        )}

        {step === 'summary' && renderSummaryStep()}
        {step === 'w3-form' && renderW3FormStep()}
        {step === 'discrepancies' && renderDiscrepanciesStep()}
        {step === 'review' && renderReviewStep()}
      </div>
    </div>
  );
};
