import { TaxFormData, TaxFormType, W2Form, Form1099, W2GForm, ScheduleC, ScheduleE, ScheduleF, LocalTaxForm, BusinessFederalForm, FederalTaxForm } from "../types";

export const mapExtractionResultToForms = (finalJson: any, fileName: string): { forms: TaxFormData[], extractedProfile?: any, extractedSettings?: any } => {
    const rawForms = Array.isArray(finalJson) ? finalJson : (finalJson.forms || []);

    console.log('[ExtractionMapper] Mapping raw forms:', rawForms);

    const extractedForms: TaxFormData[] = rawForms.map((f: any) => {
        console.log('[ExtractionMapper] Processing form:', f.formType, f);
        
        const base = {
            id: crypto.randomUUID(),
            fileName: fileName,
            taxYear: new Date().getFullYear() - 1,
            confidenceScore: f.confidenceScore || 0.8,
            fieldConfidence: f.fieldConfidence || {},
            sourcePage: f.pageNumber || 1,
            extractionReason: f.extractionReason || "AI Identified Form",
            formType: f.formType,
            owner: f.owner || 'PRIMARY',
            isAiExtracted: true
        };

        if (f.formType === TaxFormType.W2) {
            const w2Form = { 
                ...f, // Spread extracted data FIRST
                ...base, // Then override with base (id, fileName, etc.)
                // Ensure required fields exist (use extracted values or defaults)
                employer: f.employer || '',
                employerEin: f.employerEin || '',
                employerAddress: f.employerAddress || { street: '', city: '', state: '', zip: '' },
                employee: f.employee || '',
                federalWages: f.federalWages ?? 0,
                medicareWages: f.medicareWages ?? 0,
                socialSecurityWages: f.socialSecurityWages ?? 0,
                socialSecurityTaxWithheld: f.socialSecurityTaxWithheld ?? 0,
                medicareTaxWithheld: f.medicareTaxWithheld ?? 0,
                federalWithheld: f.federalWithheld ?? 0,
                localWages: f.localWages ?? 0,
                localWithheld: f.localWithheld ?? 0,
                stateWages: f.stateWages ?? 0,
                stateIncomeTax: f.stateIncomeTax ?? 0,
                locality: f.locality || ''
            } as W2Form;
            console.log('[ExtractionMapper] Mapped W2:', w2Form);
            return w2Form;
        }
        if (f.formType === TaxFormType.SCHEDULE_E) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                rentals: (f.rentals || []).map((r: any) => ({ ...r, id: crypto.randomUUID(), calculatedNetIncome: (r.line21_FairRentalDays_or_Income || 0) + (r.line22_DeductibleLoss || 0) })),
                partnerships: (f.partnerships || []).map((p: any) => ({ ...p, id: crypto.randomUUID() })),
                totalNetIncome: 0
            } as ScheduleE;
        }
        if (f.formType === TaxFormType.SCHEDULE_C) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                businessName: f.businessName || '',
                businessEin: f.businessEin || '',
                principalBusiness: f.principalBusiness || '',
                businessCode: f.businessCode || '',
                businessAddress: f.businessAddress || { street: '', city: '', state: '', zip: '' },
                grossReceipts: f.grossReceipts ?? 0,
                totalExpenses: f.totalExpenses ?? 0,
                netProfit: f.netProfit ?? 0
            } as ScheduleC;
        }
        if (f.formType === TaxFormType.SCHEDULE_F) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                businessName: f.businessName || '',
                principalProduct: f.principalProduct || '',
                businessCode: f.businessCode || '',
                ein: f.ein || '',
                grossIncome: f.grossIncome ?? 0,
                totalExpenses: f.totalExpenses ?? 0,
                netFarmProfit: f.netFarmProfit ?? 0
            } as ScheduleF;
        }
        if (f.formType === TaxFormType.W2G) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                payer: f.payer || '',
                payerEin: f.payerEin || '',
                payerAddress: f.payerAddress || { street: '', city: '', state: '', zip: '' },
                recipient: f.recipient || '',
                grossWinnings: f.grossWinnings ?? 0,
                dateWon: f.dateWon || '',
                typeOfWager: f.typeOfWager || '',
                federalWithheld: f.federalWithheld ?? 0,
                stateWithheld: f.stateWithheld ?? 0,
                localWinnings: f.localWinnings ?? 0,
                localWithheld: f.localWithheld ?? 0,
                locality: f.locality || ''
            } as W2GForm;
        }
        if (f.formType === TaxFormType.FORM_1099_NEC || f.formType === TaxFormType.FORM_1099_MISC) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                payer: f.payer || '',
                recipient: f.recipient || '',
                incomeAmount: f.incomeAmount ?? 0,
                federalWithheld: f.federalWithheld ?? 0,
                stateWithheld: f.stateWithheld ?? 0,
                localWithheld: f.localWithheld ?? 0,
                locality: f.locality || ''
            } as Form1099;
        }
        if (f.formType === TaxFormType.LOCAL_1040 || f.formType === TaxFormType.LOCAL_1040_EZ || f.formType === TaxFormType.FORM_R) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                qualifyingWages: f.qualifyingWages ?? 0,
                otherIncome: f.otherIncome ?? 0,
                totalIncome: f.totalIncomeLocal ?? 0,
                taxDue: f.taxDue ?? 0,
                credits: f.credits ?? 0,
                overpayment: f.overpayment ?? 0,
                reportedTaxableIncome: f.totalIncomeLocal ?? 0,
                reportedTaxDue: f.taxDue ?? 0
            } as LocalTaxForm;
        }
        if (f.formType === TaxFormType.FEDERAL_1040) {
            return { ...f, ...base } as FederalTaxForm;
        }
        if ([TaxFormType.FORM_1120, TaxFormType.FORM_1065, TaxFormType.FORM_27].includes(f.formType)) {
            return {
                ...f, // Spread extracted data FIRST
                ...base, // Then base overrides
                businessName: f.businessName || "Unknown Business",
                ein: f.businessEin || "",
                fedTaxableIncome: f.fedTaxableIncome ?? 0,
                reconciliation: f.reconciliation,
                allocation: f.allocation
            } as BusinessFederalForm;
        }
        return { ...f, ...base } as TaxFormData;
    });

    return {
        forms: extractedForms,
        extractedProfile: finalJson.taxPayerProfile,
        extractedSettings: finalJson.returnSettings
    };
};
