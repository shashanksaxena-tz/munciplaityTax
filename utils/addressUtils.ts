import { Address } from "../types";

// Simulated Local GIS Database
// In a real app, this would query the Dublin GIS or generic Maps API
const DUBLIN_OH_ZIPS = ['43016', '43017', '43235'];
const JEDD_ZIPS = ['43064']; // Plain City / Nearby JEDDs often overlap

// Partial matching for demo purposes
const DUBLIN_STREET_PATTERNS = [
    'RIVERSIDE', 'DUBLIN', 'SHIER RINGS', 'TUTTLE', 'FRANTZ', 'MUIRFIELD',
    'POST', 'COFFMAN', 'EMERALD', 'PERIMETER', 'AVERY'
];

export const verifyLocalAddress = (address: Address): Address => {
    if (!address || !address.zip) return { ...address, verificationStatus: 'UNVERIFIED' };

    const cleanZip = address.zip.substring(0, 5);
    const cleanCity = address.city.toUpperCase().trim();
    const cleanStreet = address.street.toUpperCase().trim();

    // 1. Direct City Match
    if (cleanCity === 'DUBLIN') {
        // Check Zip
        if (DUBLIN_OH_ZIPS.includes(cleanZip)) {
            return { ...address, verificationStatus: 'VERIFIED_IN_DISTRICT' };
        }
    }

    // 2. JEDD Check
    if (JEDD_ZIPS.includes(cleanZip) || cleanStreet.includes('INDUSTRIAL')) {
        return { ...address, verificationStatus: 'JEDD' };
    }

    // 3. Street Pattern Match (Simulating checking if it's in corporate limits even if city says Columbus)
    // Many Dublin addresses technically have Columbus as city but pay Dublin tax
    if (cleanCity === 'COLUMBUS' && DUBLIN_OH_ZIPS.includes(cleanZip)) {
        const isCommonDublinStreet = DUBLIN_STREET_PATTERNS.some(s => cleanStreet.includes(s));
        if (isCommonDublinStreet) {
            return { ...address, verificationStatus: 'VERIFIED_IN_DISTRICT' };
        }
    }

    // 4. Default Out
    return { ...address, verificationStatus: 'VERIFIED_OUT_DISTRICT' };
};
