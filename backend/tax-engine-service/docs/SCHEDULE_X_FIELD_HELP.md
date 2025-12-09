# Schedule X Field Help Documentation

This document provides help text for all Schedule X (book-tax reconciliation) fields.

## Add-Back Fields (22 fields)

### FR-020A: Club Dues
**Field Name:** `clubDues`  
**Type:** Add-back (increases taxable income)  
**Description:** Non-deductible club dues and membership fees.

**When to Use:**
- Country club memberships
- Golf club dues  
- Social or athletic club memberships
- Any club primarily for business entertainment, recreation, or social purposes

**Tax Treatment:**
- **Federal:** Non-deductible under IRC Section 274(a)(3) - already disallowed on federal return
- **Municipal:** Follows federal treatment - typically no additional add-back needed unless incorrectly deducted federally

**Common Scenarios:**
- If club dues were incorrectly deducted on federal return, add back here
- Business-purpose clubs (e.g., business/trade associations) ARE deductible - don't add back
- Distinguish between non-deductible social clubs vs. deductible professional associations

**Validation Rules:**
- Must be non-negative
- If > $0, consider whether already disallowed federally (avoid double add-back)

**Related Fields:**
- `mealsAndEntertainment` - Related but separate category
- `otherAddBacks` - Use clubDues for specificity instead

**Example:**
Company paid $3,000 in country club dues. These are non-deductible federally, so likely already added back on Form 1120. Only enter here if they were incorrectly deducted federally.

---

### FR-020B: Pension/Profit-Sharing Contribution Limits
**Field Name:** `pensionProfitSharingLimits`  
**Type:** Add-back (increases taxable income)  
**Description:** Excess pension or profit-sharing contributions exceeding federal/municipal limits.

**When to Use:**
- Contributions exceed IRC Section 404 limits
- Defined benefit plan contributions exceed deductible limits
- Profit-sharing contributions exceed 25% of covered compensation
- 401(k) deferrals exceed annual limits ($22,500 in 2023, $23,000 in 2024)
- Excess contributions to officers/owner-employees

**Tax Treatment:**
- **Federal:** Limited under IRC Section 404 - excess is non-deductible
- **Municipal:** Follows federal limits - add back any excess contributions

**Common Scenarios:**
1. **Overfunded Pension Plans:** Employer contributed $100,000 to defined benefit plan, but only $80,000 is deductible under full funding limits → Add back $20,000
2. **Profit-Sharing Excess:** Company contributed 30% of payroll to profit-sharing plan, but 25% limit applies → Add back excess 5%
3. **Owner-Employee Limits:** S-Corp owner deducted $70,000 SEP-IRA contribution, but limit is $66,000 → Add back $4,000

**Validation Rules:**
- Must be non-negative
- Verify against federal limits for current tax year
- Consider carryforward of excess contributions to future years

**Related Fields:**
- `officerLifeInsurance` - Separate category for life insurance premiums
- `otherAddBacks` - Use pensionProfitSharingLimits for specificity

**Calculation Tips:**
- Review Form 1120 Schedule M-1 or Form 1120-S Schedule M-1 for federal treatment
- Check pension/profit-sharing plan documents for contribution limits
- Verify with actuary for defined benefit plan limits

**Example:**
Company contributed $300,000 to profit-sharing plan. Covered compensation is $1,000,000. Federal limit is 25% × $1,000,000 = $250,000. Excess of $50,000 must be added back.

---

## References

- **IRS Publication 535 (Business Expenses)** - Club dues and entertainment
- **IRC Section 274** - Disallowance of certain entertainment expenses
- **IRC Section 404** - Deduction for contributions to pension plans
- **IRS Publication 560 (Retirement Plans)** - Pension contribution limits

## Version History

- **2024-12-09:** Added clubDues and pensionProfitSharingLimits fields (FR-020A, FR-020B)
- **Previous:** Original 27-field implementation
