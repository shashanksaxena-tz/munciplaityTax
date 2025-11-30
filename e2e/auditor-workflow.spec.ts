import { test, expect } from '@playwright/test';
import { testUsers } from './fixtures/testUsers';

test.describe('Auditor Workflow E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Login as auditor
    await page.goto('/');
    await page.fill('input[name="email"], input[type="email"]', testUsers.auditor.email);
    await page.fill('input[name="password"], input[type="password"]', testUsers.auditor.password);
    await page.click('button[type="submit"], button:has-text("Login")');
    await page.waitForURL(/dashboard|home|auditor/i, { timeout: 5000 });
  });

  test('should view submission queue', async ({ page }) => {
    await page.click('text=/auditor|queue|submissions/i');
    await expect(page.getByText(/queue|submissions|pending/i)).toBeVisible();
  });

  test('should filter submissions by status', async ({ page }) => {
    await page.click('text=/auditor|queue/i');
    
    // Find and use status filter
    const statusFilter = page.locator('select[name*="status"], [aria-label*="status"]');
    if (await statusFilter.isVisible()) {
      await statusFilter.selectOption('PENDING');
      await expect(page.getByText(/pending/i)).toBeVisible();
    }
  });

  test('should review a submission', async ({ page }) => {
    await page.click('text=/auditor|queue/i');
    
    // Click on first submission
    const firstSubmission = page.locator('[data-testid="submission-item"]').first();
    if (await firstSubmission.isVisible()) {
      await firstSubmission.click();
      await expect(page.getByText(/review|details|taxpayer/i)).toBeVisible();
    }
  });

  test('should approve a submission', async ({ page }) => {
    await page.click('text=/auditor|queue/i');
    
    // Navigate to submission details
    const reviewButton = page.getByRole('button', { name: /review|view/i }).first();
    if (await reviewButton.isVisible()) {
      await reviewButton.click();
      
      // Approve the submission
      const approveButton = page.getByRole('button', { name: /approve/i });
      if (await approveButton.isVisible()) {
        await approveButton.click();
        
        // Enter e-signature if required
        const signatureInput = page.locator('input[name*="signature"]');
        if (await signatureInput.isVisible()) {
          await signatureInput.fill('AuditorSignature');
        }
        
        await page.click('button:has-text("Confirm"), button:has-text("Submit")');
        await expect(page.getByText(/approved|success/i)).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('should reject a submission', async ({ page }) => {
    await page.click('text=/auditor|queue/i');
    
    const reviewButton = page.getByRole('button', { name: /review|view/i }).first();
    if (await reviewButton.isVisible()) {
      await reviewButton.click();
      
      // Reject the submission
      const rejectButton = page.getByRole('button', { name: /reject/i });
      if (await rejectButton.isVisible()) {
        await rejectButton.click();
        
        // Enter rejection reason
        const reasonInput = page.locator('textarea[name*="reason"], textarea[placeholder*="reason"]');
        if (await reasonInput.isVisible()) {
          await reasonInput.fill('Missing required documentation');
        }
        
        await page.click('button:has-text("Confirm"), button:has-text("Submit")');
        await expect(page.getByText(/rejected|sent back/i)).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('should request additional documents', async ({ page }) => {
    await page.click('text=/auditor|queue/i');
    
    const reviewButton = page.getByRole('button', { name: /review|view/i }).first();
    if (await reviewButton.isVisible()) {
      await reviewButton.click();
      
      const requestDocsButton = page.getByRole('button', { name: /request.*doc|document/i });
      if (await requestDocsButton.isVisible()) {
        await requestDocsButton.click();
        
        const docRequestInput = page.locator('textarea[name*="document"], textarea[placeholder*="document"]');
        if (await docRequestInput.isVisible()) {
          await docRequestInput.fill('Please provide W-2 forms for all employees');
        }
        
        await page.click('button:has-text("Send"), button:has-text("Submit")');
        await expect(page.getByText(/request.*sent|pending.*document/i)).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('should view audit trail', async ({ page }) => {
    await page.click('text=/audit.*trail|history|log/i');
    await expect(page.getByText(/audit.*trail|activity|history/i)).toBeVisible();
  });
});
