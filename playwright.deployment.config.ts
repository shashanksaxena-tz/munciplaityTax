import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for deployment verification tests
 * 
 * This configuration is specifically for testing Docker Compose deployments
 * where services run on different ports than local development.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: 'deployment-verification.spec.ts',
  fullyParallel: false, // Run serially for deployment tests
  forbidOnly: !!process.env.CI,
  retries: 1, // One retry for flaky network issues
  workers: 1, // Single worker for deployment tests
  reporter: [
    ['html', { outputFolder: 'test-results/html-report' }],
    ['json', { outputFile: 'test-results/test-results.json' }],
    ['list'],
  ],
  
  timeout: 60000, // Longer timeout for deployment tests
  
  use: {
    // Default to Docker Compose URLs
    baseURL: process.env.FRONTEND_URL || 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'on', // Always capture screenshots
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'deployment-chromium',
      use: { 
        ...devices['Desktop Chrome'],
        viewport: { width: 1920, height: 1080 },
      },
    },
  ],

  // No webServer - we're testing already running Docker services
});
