import { test, expect } from '@playwright/test';
import { testUsers } from './fixtures/testUsers';

test.describe('Business Tax Filing E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Login as business user
    await page.goto('/');
    await page.fill('input[name="email"], input[type="email"]', testUsers.business.email);
    await page.fill('input[name="password"], input[type="password"]', testUsers.business.password);
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForURL(/dashboard|home/i, { timeout: 5000 });
  });

  test('should register a new business', async ({ page }) => {
    await page.click('text=/register.*business|new.*business/i');
    
    // Fill business registration form
    await page.fill('input[name*="name"], input[placeholder*="business name"]', 'Test Corporation');
    await page.fill('input[name*="ein"], input[placeholder*="EIN"]', '12-3456789');
    await page.fill('input[name*="address"]', '123 Business St');
    await page.fill('input[name*="city"]', 'Dublin');
    await page.fill('input[name*="zip"], input[name*="postal"]', '43016');
    
    await page.click('button:has-text("Register"), button:has-text("Submit")');
    
    await expect(page.getByText(/success|registered|complete/i)).toBeVisible({ timeout: 10000 });
  });

  test('should file W-1 withholding return', async ({ page }) => {
    // Navigate to withholding filing
    await page.click('text=/file.*withholding|W-1|withholding.*return/i');
    
    // Select tax period
    await page.selectOption('select[name*="quarter"], select[name*="period"]', 'Q1');
    await page.selectOption('select[name*="year"]', '2024');
    
    // Enter wage information
    await page.fill('input[name*="wage"], input[placeholder*="wage"]', '100000');
    await page.fill('input[name*="withheld"], input[placeholder*="withheld"]', '2500');
    await page.fill('input[name*="employees"]', '10');
    
    // Continue to review
    await page.click('button:has-text("Continue"), button:has-text("Next")');
    
    // Review and submit
    await expect(page.getByText(/review|summary/i)).toBeVisible({ timeout: 5000 });
    await page.click('button:has-text("Submit"), button:has-text("File Return")');
    
    await expect(page.getByText(/success|submitted|filed/i)).toBeVisible({ timeout: 10000 });
  });

  test('should file reconciliation W-3 return', async ({ page }) => {
    await page.click('text=/reconciliation|W-3/i');
    
    // Enter reconciliation data
    await page.fill('input[name*="total.*wage"]', '400000');
    await page.fill('input[name*="total.*withheld"]', '10000');
    await page.fill('input[name*="quarterly.*reported"]', '400000');
    
    // Upload W-2 forms
    const fileInput = page.locator('input[type="file"]');
    if (await fileInput.isVisible()) {
      await fileInput.setInputFiles({
        name: 'w2-forms.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.from('Mock W-2 PDF content')
      });
    }
    
    await page.click('button:has-text("Submit"), button:has-text("File")');
    
    await expect(page.getByText(/success|submitted/i)).toBeVisible({ timeout: 10000 });
  });

  test('should view business filing history', async ({ page }) => {
    await page.click('text=/history|past.*filing|submission.*history/i');
    
    await expect(page.getByText(/filing.*history|returns|submissions/i)).toBeVisible();
    
    // Should display list of filings
    const filings = page.locator('[data-testid*="filing"], tr, .filing-item');
    await expect(filings.first()).toBeVisible({ timeout: 5000 });
  });

  test('should amend a previous return', async ({ page }) => {
    await page.click('text=/history|past.*filing/i');
    
    // Select a return to amend
    const amendButton = page.getByRole('button', { name: /amend|edit/i }).first();
    if (await amendButton.isVisible()) {
      await amendButton.click();
      
      // Modify return data
      await page.fill('input[name*="wage"]', '105000');
      
      // Explain amendment
      await page.fill('textarea[name*="reason"], textarea[placeholder*="reason"]', 'Correcting wage amount');
      
      await page.click('button:has-text("Submit Amendment")');
      
      await expect(page.getByText(/amendment.*submitted|success/i)).toBeVisible({ timeout: 10000 });
    }
  });
});
