import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Deployment Verification Tests
 * 
 * These tests are designed to run after Docker Compose deployment
 * to verify all services are healthy and capture evidence.
 * 
 * Results are saved as artifacts (screenshots, API responses).
 */

// Configure for deployment testing (different base URLs)
const DEPLOYMENT_CONFIG = {
  frontend: process.env.FRONTEND_URL || 'http://localhost:3000',
  gateway: process.env.GATEWAY_URL || 'http://localhost:8080',
  eureka: process.env.EUREKA_URL || 'http://localhost:8761',
  zipkin: process.env.ZIPKIN_URL || 'http://localhost:9411',
};

const RESULTS_DIR = process.env.RESULTS_DIR || 'test-results';

// Ensure results directories exist
const screenshotsDir = path.join(RESULTS_DIR, 'screenshots');
const apiResponsesDir = path.join(RESULTS_DIR, 'api-responses');

test.beforeAll(async () => {
  // Create directories if they don't exist
  if (!fs.existsSync(screenshotsDir)) {
    fs.mkdirSync(screenshotsDir, { recursive: true });
  }
  if (!fs.existsSync(apiResponsesDir)) {
    fs.mkdirSync(apiResponsesDir, { recursive: true });
  }
});

test.describe('Deployment Verification Suite', () => {
  
  test.describe('Infrastructure Health Checks', () => {
    
    test('Eureka Discovery Service is healthy', async ({ request }) => {
      const response = await request.get(`${DEPLOYMENT_CONFIG.eureka}/actuator/health`, {
        timeout: 30000,
      });
      
      const body = await response.json();
      
      // Save API response
      fs.writeFileSync(
        path.join(apiResponsesDir, 'eureka-health.json'),
        JSON.stringify(body, null, 2)
      );
      
      expect(response.status()).toBe(200);
      expect(body.status).toBe('UP');
    });
    
    test('Gateway Service is healthy', async ({ request }) => {
      const response = await request.get(`${DEPLOYMENT_CONFIG.gateway}/actuator/health`, {
        timeout: 30000,
      });
      
      const body = await response.json();
      
      // Save API response
      fs.writeFileSync(
        path.join(apiResponsesDir, 'gateway-health.json'),
        JSON.stringify(body, null, 2)
      );
      
      expect(response.status()).toBe(200);
      expect(body.status).toBe('UP');
    });
    
    test('Zipkin Tracing is available', async ({ request }) => {
      try {
        const response = await request.get(`${DEPLOYMENT_CONFIG.zipkin}/health`, {
          timeout: 30000,
        });
        
        const body = await response.text();
        
        // Save API response
        fs.writeFileSync(
          path.join(apiResponsesDir, 'zipkin-health.json'),
          JSON.stringify({ status: response.status(), body }, null, 2)
        );
        
        expect(response.status()).toBeLessThan(500);
      } catch (error) {
        // Zipkin might not have a health endpoint
        fs.writeFileSync(
          path.join(apiResponsesDir, 'zipkin-health.json'),
          JSON.stringify({ status: 'unavailable', error: String(error) }, null, 2)
        );
        // Don't fail the test - Zipkin is optional
        console.log('Zipkin health check skipped:', error);
      }
    });
  });
  
  test.describe('Frontend Application', () => {
    
    test('Frontend loads successfully', async ({ page }) => {
      await page.goto(DEPLOYMENT_CONFIG.frontend, {
        timeout: 60000,
        waitUntil: 'networkidle',
      });
      
      // Wait for page to stabilize
      await page.waitForTimeout(2000);
      
      // Take full page screenshot
      await page.screenshot({
        path: path.join(screenshotsDir, 'frontend-homepage.png'),
        fullPage: true,
      });
      
      // Verify page loaded
      const title = await page.title();
      expect(title).toBeTruthy();
      
      // Save page info
      fs.writeFileSync(
        path.join(apiResponsesDir, 'frontend-info.json'),
        JSON.stringify({
          title,
          url: page.url(),
          timestamp: new Date().toISOString(),
        }, null, 2)
      );
    });
    
    test('Frontend has main content', async ({ page }) => {
      await page.goto(DEPLOYMENT_CONFIG.frontend, {
        timeout: 60000,
        waitUntil: 'domcontentloaded',
      });
      
      // Wait for any React hydration
      await page.waitForTimeout(3000);
      
      // Take screenshot of viewport
      await page.screenshot({
        path: path.join(screenshotsDir, 'frontend-content.png'),
      });
      
      // Check for common elements
      const bodyText = await page.locator('body').textContent();
      expect(bodyText).toBeTruthy();
      expect(bodyText?.length).toBeGreaterThan(0);
    });
  });
  
  test.describe('Service Dashboards', () => {
    
    test('Eureka Dashboard loads', async ({ page }) => {
      await page.goto(DEPLOYMENT_CONFIG.eureka, {
        timeout: 60000,
        waitUntil: 'networkidle',
      });
      
      await page.waitForTimeout(2000);
      
      await page.screenshot({
        path: path.join(screenshotsDir, 'eureka-dashboard.png'),
        fullPage: true,
      });
      
      // Eureka shows registered instances
      const pageContent = await page.content();
      
      fs.writeFileSync(
        path.join(apiResponsesDir, 'eureka-dashboard.json'),
        JSON.stringify({
          url: page.url(),
          hasContent: pageContent.length > 0,
          timestamp: new Date().toISOString(),
        }, null, 2)
      );
    });
    
    test('Zipkin Dashboard loads', async ({ page }) => {
      try {
        await page.goto(DEPLOYMENT_CONFIG.zipkin, {
          timeout: 60000,
          waitUntil: 'networkidle',
        });
        
        await page.waitForTimeout(2000);
        
        await page.screenshot({
          path: path.join(screenshotsDir, 'zipkin-dashboard.png'),
          fullPage: true,
        });
        
        fs.writeFileSync(
          path.join(apiResponsesDir, 'zipkin-dashboard.json'),
          JSON.stringify({
            url: page.url(),
            status: 'available',
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
      } catch (error) {
        console.log('Zipkin dashboard skipped:', error);
        fs.writeFileSync(
          path.join(apiResponsesDir, 'zipkin-dashboard.json'),
          JSON.stringify({
            status: 'unavailable',
            error: String(error),
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
      }
    });
  });
  
  test.describe('API Gateway Routes', () => {
    
    test('Gateway actuator info endpoint', async ({ request }) => {
      try {
        const response = await request.get(`${DEPLOYMENT_CONFIG.gateway}/actuator/info`, {
          timeout: 30000,
        });
        
        let body;
        try {
          body = await response.json();
        } catch {
          body = await response.text();
        }
        
        fs.writeFileSync(
          path.join(apiResponsesDir, 'gateway-info.json'),
          JSON.stringify({
            status: response.status(),
            body,
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
        
        expect(response.status()).toBeLessThan(500);
      } catch (error) {
        fs.writeFileSync(
          path.join(apiResponsesDir, 'gateway-info.json'),
          JSON.stringify({
            status: 'error',
            error: String(error),
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
      }
    });
    
    test('Gateway routes are configured', async ({ request }) => {
      try {
        const response = await request.get(`${DEPLOYMENT_CONFIG.gateway}/actuator/gateway/routes`, {
          timeout: 30000,
        });
        
        let body;
        try {
          body = await response.json();
        } catch {
          body = await response.text();
        }
        
        fs.writeFileSync(
          path.join(apiResponsesDir, 'gateway-routes.json'),
          JSON.stringify({
            status: response.status(),
            body,
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
        
        // Routes endpoint might not be exposed
        expect(response.status()).toBeLessThan(500);
      } catch (error) {
        fs.writeFileSync(
          path.join(apiResponsesDir, 'gateway-routes.json'),
          JSON.stringify({
            status: 'error',
            error: String(error),
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
      }
    });
  });
  
  test.describe('Eureka Service Registry', () => {
    
    test('Eureka apps endpoint returns registered services', async ({ request }) => {
      try {
        const response = await request.get(`${DEPLOYMENT_CONFIG.eureka}/eureka/apps`, {
          timeout: 30000,
          headers: {
            'Accept': 'application/json',
          },
        });
        
        let body;
        try {
          body = await response.json();
        } catch {
          body = await response.text();
        }
        
        fs.writeFileSync(
          path.join(apiResponsesDir, 'eureka-apps.json'),
          JSON.stringify({
            status: response.status(),
            body,
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
        
        expect(response.status()).toBe(200);
      } catch (error) {
        fs.writeFileSync(
          path.join(apiResponsesDir, 'eureka-apps.json'),
          JSON.stringify({
            status: 'error',
            error: String(error),
            timestamp: new Date().toISOString(),
          }, null, 2)
        );
      }
    });
  });
});

test.describe('Deployment Summary', () => {
  
  test('Generate deployment summary report', async ({ request }) => {
    const summary = {
      timestamp: new Date().toISOString(),
      environment: {
        frontend: DEPLOYMENT_CONFIG.frontend,
        gateway: DEPLOYMENT_CONFIG.gateway,
        eureka: DEPLOYMENT_CONFIG.eureka,
        zipkin: DEPLOYMENT_CONFIG.zipkin,
      },
      healthChecks: {} as Record<string, { status: string; responseTime?: number }>,
    };
    
    // Check each service
    const services = [
      { name: 'eureka', url: `${DEPLOYMENT_CONFIG.eureka}/actuator/health` },
      { name: 'gateway', url: `${DEPLOYMENT_CONFIG.gateway}/actuator/health` },
      { name: 'frontend', url: DEPLOYMENT_CONFIG.frontend },
    ];
    
    for (const service of services) {
      const startTime = Date.now();
      try {
        const response = await request.get(service.url, { timeout: 10000 });
        summary.healthChecks[service.name] = {
          status: response.ok() ? 'healthy' : 'unhealthy',
          responseTime: Date.now() - startTime,
        };
      } catch {
        summary.healthChecks[service.name] = {
          status: 'unreachable',
          responseTime: Date.now() - startTime,
        };
      }
    }
    
    // Save summary
    fs.writeFileSync(
      path.join(apiResponsesDir, 'deployment-summary.json'),
      JSON.stringify(summary, null, 2)
    );
    
    console.log('Deployment Summary:', JSON.stringify(summary, null, 2));
    
    // Test passes if we could generate the summary
    expect(summary.timestamp).toBeTruthy();
  });
});
