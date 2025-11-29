import { test, expect } from '@playwright/test';

test.describe('Payment Processing E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('input[name="email"], input[type="email"]', 'taxpayer@example.com');
    await page.fill('input[name="password"], input[type="password"]', 'password123');
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForURL(/dashboard|home/i, { timeout: 5000 });
  });

  test('should process tax payment with credit card', async ({ page }) => {
    // Navigate to payment
    await page.click('text=/pay|payment|make.*payment/i');
    
    // Select payment method
    await page.click('text=/credit.*card|card/i');
    
    // Enter card details
    await page.fill('input[name*="card"], input[placeholder*="card number"]', '4111111111111111');
    await page.fill('input[name*="expiry"], input[placeholder*="expiry"]', '12/25');
    await page.fill('input[name*="cvv"], input[placeholder*="CVV"]', '123');
    await page.fill('input[name*="name"], input[placeholder*="cardholder"]', 'John Doe');
    
    // Enter billing address
    await page.fill('input[name*="address"]', '123 Main St');
    await page.fill('input[name*="city"]', 'Dublin');
    await page.fill('input[name*="zip"]', '43016');
    
    // Review payment amount
    await expect(page.getByText(/\$|amount/i)).toBeVisible();
    
    // Submit payment
    await page.click('button:has-text("Pay Now"), button:has-text("Submit Payment")');
    
    // Wait for success
    await expect(page.getByText(/success|completed|paid/i)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/confirmation|receipt|transaction/i)).toBeVisible();
  });

  test('should process ACH payment', async ({ page }) => {
    await page.click('text=/pay|payment/i');
    
    // Select ACH/Bank transfer
    await page.click('text=/bank.*transfer|ACH|direct.*debit/i');
    
    // Enter bank details
    await page.fill('input[name*="routing"]', '123456789');
    await page.fill('input[name*="account"]', '987654321');
    await page.selectOption('select[name*="account.*type"]', 'CHECKING');
    
    await page.click('button:has-text("Submit"), button:has-text("Pay")');
    
    await expect(page.getByText(/submitted|processing|pending/i)).toBeVisible({ timeout: 10000 });
  });

  test('should set up payment plan', async ({ page }) => {
    await page.click('text=/payment.*plan|installment/i');
    
    // View total amount due
    await expect(page.getByText(/\$|total.*due/i)).toBeVisible();
    
    // Select number of installments
    await page.selectOption('select[name*="installment"], select[name*="months"]', '12');
    
    // Review monthly payment amount
    await expect(page.getByText(/monthly.*payment|\$.*month/i)).toBeVisible();
    
    // Agree to terms
    const termsCheckbox = page.locator('input[type="checkbox"][name*="terms"]');
    if (await termsCheckbox.isVisible()) {
      await termsCheckbox.check();
    }
    
    // Submit plan request
    await page.click('button:has-text("Create Plan"), button:has-text("Submit")');
    
    await expect(page.getByText(/approved|created|success/i)).toBeVisible({ timeout: 10000 });
  });

  test('should download payment receipt', async ({ page }) => {
    await page.click('text=/payment.*history|transaction.*history/i');
    
    // Wait for history to load
    await expect(page.getByText(/payment.*history|transactions/i)).toBeVisible();
    
    // Click on download receipt
    const downloadButton = page.getByRole('button', { name: /download|receipt|pdf/i }).first();
    
    if (await downloadButton.isVisible()) {
      const downloadPromise = page.waitForEvent('download');
      await downloadButton.click();
      const download = await downloadPromise;
      
      expect(download.suggestedFilename()).toMatch(/receipt|payment/i);
    }
  });

  test('should handle payment errors gracefully', async ({ page }) => {
    await page.click('text=/pay|payment/i');
    
    // Select credit card
    await page.click('text=/credit.*card/i');
    
    // Enter invalid card details
    await page.fill('input[name*="card"]', '4111111111111112'); // Invalid card
    await page.fill('input[name*="expiry"]', '12/25');
    await page.fill('input[name*="cvv"]', '123');
    
    await page.click('button:has-text("Pay"), button:has-text("Submit")');
    
    // Should show error message
    await expect(page.getByText(/error|declined|invalid|failed/i)).toBeVisible({ timeout: 10000 });
  });

  test('should refund overpayment', async ({ page }) => {
    await page.click('text=/refund|overpayment/i');
    
    // View refund amount
    await expect(page.getByText(/\$|refund.*amount/i)).toBeVisible();
    
    // Select refund method
    await page.click('text=/direct.*deposit|check/i');
    
    if (await page.getByText(/direct.*deposit/i).isVisible()) {
      await page.fill('input[name*="routing"]', '123456789');
      await page.fill('input[name*="account"]', '987654321');
    }
    
    await page.click('button:has-text("Request Refund"), button:has-text("Submit")');
    
    await expect(page.getByText(/requested|processing|submitted/i)).toBeVisible({ timeout: 10000 });
  });
});
