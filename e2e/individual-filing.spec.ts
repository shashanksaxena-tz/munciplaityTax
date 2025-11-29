import { test, expect } from '@playwright/test';

test.describe('Individual Tax Filing E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Login as individual taxpayer
    await page.goto('/');
    await page.fill('input[name="email"], input[type="email"]', 'individual@example.com');
    await page.fill('input[name="password"], input[type="password"]', 'password123');
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForURL(/dashboard|home/i, { timeout: 5000 });
  });

  test('should navigate to tax filing page', async ({ page }) => {
    await page.click('text=/file.*return|new.*filing/i');
    await expect(page).toHaveURL(/filing|return|upload/i);
  });

  test('should upload W-2 form', async ({ page }) => {
    await page.click('text=/file.*return|new.*filing/i');
    
    // Wait for file upload section
    await page.waitForSelector('input[type="file"]', { timeout: 5000 });
    
    // Create a test file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles({
      name: 'w2-test.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('Test W-2 PDF content')
    });
    
    // Wait for upload confirmation or progress
    await expect(page.getByText(/uploaded|processing|success/i)).toBeVisible({ timeout: 10000 });
  });

  test('should complete tax calculation', async ({ page }) => {
    await page.click('text=/file.*return|new.*filing/i');
    
    // Fill in tax information
    await page.fill('input[name*="income"], input[placeholder*="income"]', '50000');
    await page.fill('input[name*="withholding"], input[placeholder*="withholding"]', '1250');
    
    // Submit for calculation
    await page.click('button:has-text("Calculate"), button:has-text("Submit")');
    
    // Wait for results
    await expect(page.getByText(/tax.*due|refund|owe/i)).toBeVisible({ timeout: 10000 });
  });

  test('should review and submit return', async ({ page }) => {
    await page.click('text=/file.*return|new.*filing/i');
    
    // Navigate through filing steps
    await page.fill('input[name*="income"]', '50000');
    await page.click('button:has-text("Next"), button:has-text("Continue")');
    
    // Review page
    await expect(page.getByText(/review|summary/i)).toBeVisible({ timeout: 5000 });
    
    // Submit return
    const submitButton = page.getByRole('button', { name: /submit|file.*return/i });
    if (await submitButton.isVisible()) {
      await submitButton.click();
      await expect(page.getByText(/success|submitted|confirmation/i)).toBeVisible({ timeout: 10000 });
    }
  });

  test('should view filing history', async ({ page }) => {
    await page.click('text=/history|past.*returns|submissions/i');
    await expect(page.getByText(/filing.*history|returns|submissions/i)).toBeVisible();
  });
});
