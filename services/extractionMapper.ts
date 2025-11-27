import { TaxFormData, TaxFormType, W2Form, Form1099, W2GForm, ScheduleC, ScheduleE, ScheduleF, LocalTaxForm, BusinessFederalForm, FederalTaxForm } from "../types";

export const mapExtractionResultToForms = (finalJson: any, fileName: string): { forms: TaxFormData[], extractedProfile?: any, extractedSettings?: any } => {
    const rawForms = Array.isArray(finalJson) ? finalJson : (finalJson.forms || []);

    const extractedForms: TaxFormData[] = rawForms.map((f: any) => {
        const base = {
            id: crypto.randomUUID(),
            fileName: fileName,
            taxYear: new Date().getFullYear() - 1,
            confidenceScore: f.confidenceScore || 0.8,
            fieldConfidence: f.fieldConfidence || {},
            sourcePage: f.pageNumber || 1,
            extractionReason: f.extractionReason || "AI Identified Form",
            formType: f.formType,
            owner: f.owner || 'PRIMARY'
        };

        if (f.formType === TaxFormType.W2) {
            return { ...base, employer: f.employer, employerEin: f.employerEin, federalWages: f.federalWages, medicareWages: f.medicareWages, localWages: f.localWages, localWithheld: f.localWithheld, locality: f.locality } as W2Form;
        }
        if (f.formType === TaxFormType.SCHEDULE_E) {
            return {
                ...base,
                rentals: (f.rentals || []).map((r: any) => ({ ...r, id: crypto.randomUUID(), calculatedNetIncome: (r.line21_FairRentalDays_or_Income || 0) + (r.line22_DeductibleLoss || 0) })),
                partnerships: (f.partnerships || []).map((p: any) => ({ ...p, id: crypto.randomUUID() })),
                totalNetIncome: 0
            } as ScheduleE;
        }
        if (f.formType === TaxFormType.LOCAL_1040 || f.formType === TaxFormType.LOCAL_1040_EZ || f.formType === TaxFormType.FORM_R) {
            return {
                ...base,
                qualifyingWages: f.qualifyingWages || 0,
                otherIncome: f.otherIncome || 0,
                totalIncome: f.totalIncomeLocal || 0,
                taxDue: f.taxDue || 0,
                credits: f.credits || 0,
                overpayment: f.overpayment || 0,
                reportedTaxableIncome: f.totalIncomeLocal || 0,
                reportedTaxDue: f.taxDue || 0
            } as LocalTaxForm;
        }
        if (f.formType === TaxFormType.FEDERAL_1040) {
            return { ...base, ...f } as FederalTaxForm;
        }
        if ([TaxFormType.FORM_1120, TaxFormType.FORM_1065, TaxFormType.FORM_27].includes(f.formType)) {
            return {
                ...base,
                businessName: f.businessName || "Unknown Business",
                ein: f.businessEin || "",
                fedTaxableIncome: f.fedTaxableIncome || 0,
                reconciliation: f.reconciliation,
                allocation: f.allocation
            } as BusinessFederalForm;
        }
        return { ...base, ...f } as TaxFormData;
    });

    return {
        forms: extractedForms,
        extractedProfile: finalJson.taxPayerProfile,
        extractedSettings: finalJson.returnSettings
    };
};
