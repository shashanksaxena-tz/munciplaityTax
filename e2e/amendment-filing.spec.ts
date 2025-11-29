import { test, expect } from '@playwright/test';

test.describe('Amendment Filing E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.fill('input[name="email"], input[type="email"]', 'taxpayer@example.com');
    await page.fill('input[name="password"], input[type="password"]', 'password123');
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForURL(/dashboard|home/i, { timeout: 5000 });
  });

  test('should file an amended individual return', async ({ page }) => {
    // Go to filing history
    await page.click('text=/history|past.*returns/i');
    
    // Select a return to amend
    const amendButton = page.getByRole('button', { name: /amend|edit/i }).first();
    await amendButton.click();
    
    // Wait for amendment form
    await expect(page.getByText(/amend|amendment/i)).toBeVisible({ timeout: 5000 });
    
    // Modify income information
    const incomeInput = page.locator('input[name*="income"], input[placeholder*="income"]').first();
    await incomeInput.clear();
    await incomeInput.fill('55000'); // Changed from original
    
    // Provide reason for amendment
    await page.fill('textarea[name*="reason"], textarea[placeholder*="explain"]', 
      'Correcting income amount due to additional W-2 received');
    
    // Attach supporting documents
    const fileInput = page.locator('input[type="file"]');
    if (await fileInput.isVisible()) {
      await fileInput.setInputFiles({
        name: 'additional-w2.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.from('Additional W-2 document')
      });
    }
    
    // Review changes
    await page.click('button:has-text("Review"), button:has-text("Continue")');
    
    // Verify amendment summary
    await expect(page.getByText(/original.*amount|amended.*amount/i)).toBeVisible();
    
    // Submit amendment
    await page.click('button:has-text("Submit Amendment")');
    
    await expect(page.getByText(/submitted|received|processed/i)).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(/confirmation/i)).toBeVisible();
  });

  test('should file amended business return', async ({ page }) => {
    await page.click('text=/business/i');
    await page.click('text=/history/i');
    
    const amendButton = page.getByRole('button', { name: /amend/i }).first();
    if (await amendButton.isVisible()) {
      await amendButton.click();
      
      // Modify withholding amount
      await page.fill('input[name*="withheld"]', '2750');
      
      // Add explanation
      await page.fill('textarea[name*="reason"]', 'Correcting withholding calculation error');
      
      await page.click('button:has-text("Submit")');
      
      await expect(page.getByText(/success|submitted/i)).toBeVisible({ timeout: 10000 });
    }
  });

  test('should track amendment status', async ({ page }) => {
    await page.click('text=/amendments|amended.*returns/i');
    
    // Should display amendment tracking
    await expect(page.getByText(/amendment.*status|tracking/i)).toBeVisible();
    
    // Check status indicators
    const statusElements = page.locator('[data-status], .status, .badge');
    await expect(statusElements.first()).toBeVisible({ timeout: 5000 });
  });

  test('should view amendment history', async ({ page }) => {
    await page.click('text=/history/i');
    
    // Filter for amended returns
    const filterButton = page.getByRole('button', { name: /filter/i });
    if (await filterButton.isVisible()) {
      await filterButton.click();
      await page.click('text=/amended/i');
    }
    
    // Should show amended returns
    await expect(page.getByText(/amended|amendment/i)).toBeVisible();
  });

  test('should handle amendment validation errors', async ({ page }) => {
    await page.click('text=/history/i');
    const amendButton = page.getByRole('button', { name: /amend/i }).first();
    await amendButton.click();
    
    // Try to submit without providing reason
    const reasonField = page.locator('textarea[name*="reason"]');
    if (await reasonField.isVisible()) {
      await reasonField.clear();
    }
    
    await page.click('button:has-text("Submit")');
    
    // Should show validation error
    await expect(page.getByText(/required|must.*provide|reason.*required/i)).toBeVisible({ timeout: 5000 });
  });
});
