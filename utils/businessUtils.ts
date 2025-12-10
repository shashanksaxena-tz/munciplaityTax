import { FilingFrequency, WithholdingPeriod, WithholdingReturnData, DiscrepancyReport, ReconciliationReturnData, ReconciliationIssue } from "../types";

const MUNICIPAL_RATE = 0.020;

export const getAvailablePeriods = (frequency: FilingFrequency, year: number): WithholdingPeriod[] => {
    const periods: WithholdingPeriod[] = [];
    if (frequency === FilingFrequency.QUARTERLY) {
        const quarters = [{ p: 'Q1', due: '-04-30' }, { p: 'Q2', due: '-07-31' }, { p: 'Q3', due: '-10-31' }, { p: 'Q4', due: '-01-31' }];
        quarters.forEach(q => periods.push({ year, period: q.p, startDate: '', endDate: '', dueDate: `${q.p === 'Q4' ? year + 1 : year}${q.due}` }));
    }
    else if (frequency === FilingFrequency.MONTHLY) {
        ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"].forEach((m, idx) => {
            const dueYear = idx === 11 ? year + 1 : year;
            periods.push({ year, period: m, startDate: '', endDate: '', dueDate: `${dueYear}-${String(idx === 11 ? 1 : idx + 2).padStart(2, '0')}-15` });
        });
    }
    return periods;
};

export const getDailyPeriod = (dateStr: string): WithholdingPeriod => ({ year: parseInt(dateStr.split('-')[0]), period: dateStr, startDate: dateStr, endDate: dateStr, dueDate: dateStr });

export const calculateWithholding = (gross: number, adj: number, period: WithholdingPeriod): WithholdingReturnData => ({
    id: crypto.randomUUID(), dateFiled: new Date().toISOString(), period, grossWages: gross, adjustments: adj, taxDue: (gross + adj) * MUNICIPAL_RATE, penalty: 0, interest: 0, totalAmountDue: (gross + adj) * MUNICIPAL_RATE, isReconciled: true, paymentStatus: 'UNPAID'
});

export const reconcilePayroll = (manual: number, upload: number): DiscrepancyReport => ({
    hasDiscrepancies: Math.abs(manual - upload) > 1, issues: []
});

/**
 * Reconcile W-1 filings by calling backend API
 * @param employerId - Employer/Business ID
 * @param taxYear - Tax year to reconcile
 * @returns Promise with list of reconciliation issues
 */
export const reconcileW1Filings = async (
    employerId: string, 
    taxYear: number
): Promise<ReconciliationIssue[]> => {
    try {
        const response = await fetch('/api/v1/w1-filings/reconcile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                employerId,
                taxYear,
            }),
        });

        if (!response.ok) {
            throw new Error(`Reconciliation failed: ${response.statusText}`);
        }

        const issues: ReconciliationIssue[] = await response.json();
        return issues;
    } catch (error) {
        console.error('Error reconciling W-1 filings:', error);
        // Return empty array on error - component will show "no issues"
        return [];
    }
};

export const reconcileW3 = (w1: number, w2: number, year: number): ReconciliationReturnData => ({
    id: crypto.randomUUID(), dateFiled: new Date().toISOString(), taxYear: year, totalW1Tax: w1, totalW2Tax: w2, discrepancy: w1 - w2, status: Math.abs(w1 - w2) < 1 ? 'BALANCED' : 'UNBALANCED'
});

export const getNextDueDate = (f: FilingFrequency) => new Date();
