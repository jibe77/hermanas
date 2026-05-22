import { test, expect } from '@playwright/test';

test.describe('Hermanas Application', () => {
    test('should redirect to login when not authenticated', async ({ page }) => {
        // Navigate to the application
        await page.goto('/');

        // Wait for redirect to login page
        await page.waitForURL('**/auth/login');

        // Verify we're on the login page
        expect(page.url()).toContain('/auth/login');
    });

    test('should not have console errors', async ({ page }) => {
        const consoleErrors: string[] = [];

        // Listen for console errors
        page.on('console', msg => {
            if (msg.type() === 'error') {
                consoleErrors.push(msg.text());
            }
        });

        // Navigate to the application
        await page.goto('/');

        // Wait for the page to be stable
        await page.waitForLoadState('networkidle');

        // Assert there are no console errors
        expect(consoleErrors).toEqual([]);
    });

    test('should have proper meta tags', async ({ page }) => {
        await page.goto('/');

        // Check that the page has a title
        await expect(page).toHaveTitle(/Hermanas/);
    });
});

test.describe('Login Page', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/');
        await page.waitForURL('**/auth/login');
    });

    test('should display login form elements', async ({ page }) => {
        // Check for AWS Amplify Authenticator component
        const authenticator = page.locator('amplify-authenticator');
        await expect(authenticator).toBeAttached();
    });

    test('should have proper page structure', async ({ page }) => {
        // Verify page title
        await expect(page).toHaveTitle(/Hermanas/);

        // Verify login page is rendered
        expect(page.url()).toContain('/auth/login');
    });

    test('should load without JavaScript errors', async ({ page }) => {
        const jsErrors: Error[] = [];

        page.on('pageerror', error => {
            jsErrors.push(error);
        });

        await page.waitForLoadState('networkidle');

        expect(jsErrors).toEqual([]);
    });

    test('should be responsive on mobile', async ({ page }) => {
        // Set mobile viewport
        await page.setViewportSize({ width: 375, height: 667 });

        await page.waitForLoadState('networkidle');

        // Check authenticator is still attached on mobile
        const authenticator = page.locator('amplify-authenticator');
        await expect(authenticator).toBeAttached();
    });

    test('should be responsive on tablet', async ({ page }) => {
        // Set tablet viewport
        await page.setViewportSize({ width: 768, height: 1024 });

        await page.waitForLoadState('networkidle');

        // Check authenticator is still attached on tablet
        const authenticator = page.locator('amplify-authenticator');
        await expect(authenticator).toBeAttached();
    });
});

test.describe('Application Navigation', () => {
    test('should prevent direct access to protected routes', async ({ page }) => {
        // Try to access dashboard directly
        await page.goto('/dashboard');

        // Should redirect to login
        await page.waitForURL('**/auth/login', { timeout: 5000 });
        expect(page.url()).toContain('/auth/login');
    });

    test('should handle navigation to various routes', async ({ page }) => {
        // Test that application doesn't crash when navigating to different routes
        const routes = ['/dashboard', '/camera'];

        for (const route of routes) {
            await page.goto(route);
            await page.waitForLoadState('networkidle');

            // Verify the page doesn't crash - body should be visible
            const body = page.locator('body');
            await expect(body).toBeVisible();
        }
    });
});

test.describe('Performance', () => {
    test('should load page within reasonable time', async ({ page }) => {
        const startTime = Date.now();
        await page.goto('/');
        await page.waitForLoadState('domcontentloaded');
        const loadTime = Date.now() - startTime;

        // Page should load within 5 seconds
        expect(loadTime).toBeLessThan(5000);
    });

    test('should not have excessive bundle size', async ({ page }) => {
        const resources: { size: number; type: string }[] = [];

        page.on('response', async response => {
            const url = response.url();
            if (url.endsWith('.js') || url.endsWith('.css')) {
                const headers = response.headers();
                const contentLength = headers['content-length'];
                if (contentLength) {
                    resources.push({
                        size: parseInt(contentLength, 10),
                        type: url.endsWith('.js') ? 'js' : 'css',
                    });
                }
            }
        });

        await page.goto('/');
        await page.waitForLoadState('networkidle');

        // Individual JS files should not exceed 5MB
        const largeFiles = resources.filter(r => r.size > 5 * 1024 * 1024);
        expect(largeFiles).toEqual([]);
    });
});

test.describe('Accessibility', () => {
    test('should have proper HTML structure', async ({ page }) => {
        await page.goto('/');
        await page.waitForLoadState('networkidle');

        // Check for essential HTML elements
        const html = page.locator('html');
        await expect(html).toHaveAttribute('lang');

        const body = page.locator('body');
        await expect(body).toBeVisible();
    });

    test('should have app root element', async ({ page }) => {
        await page.goto('/');
        await page.waitForLoadState('networkidle');

        const appRoot = page.locator('sb-root, app-root');
        await expect(appRoot).toBeVisible();
    });

    test('should have viewport meta tag', async ({ page }) => {
        await page.goto('/');

        const viewport = page.locator('meta[name="viewport"]');
        await expect(viewport).toHaveAttribute('content', /width=device-width/);
    });
});

test.describe('SEO and Meta Information', () => {
    test('should have proper document title', async ({ page }) => {
        await page.goto('/');

        const title = await page.title();
        expect(title).toBeTruthy();
        expect(title.length).toBeGreaterThan(0);
    });

    test('should have meta charset', async ({ page }) => {
        await page.goto('/');

        const charset = page.locator('meta[charset]');
        const charsetCount = await charset.count();
        expect(charsetCount).toBeGreaterThan(0);
    });
});

test.describe('Error Handling', () => {
    test('should handle 404 routes gracefully', async ({ page }) => {
        await page.goto('/non-existent-route');

        // Should either redirect to login or show error page
        // Wait for navigation to complete
        await page.waitForLoadState('networkidle');

        // Check that page doesn't crash
        const body = page.locator('body');
        await expect(body).toBeVisible();
    });

    test('should not expose sensitive information in errors', async ({ page }) => {
        const consoleMessages: string[] = [];

        page.on('console', msg => {
            consoleMessages.push(msg.text().toLowerCase());
        });

        await page.goto('/');
        await page.waitForLoadState('networkidle');

        // Check console messages don't contain sensitive keywords
        const sensitiveKeywords = ['password', 'token', 'secret', 'api_key'];
        const exposedSecrets = consoleMessages.filter(msg =>
            sensitiveKeywords.some(keyword => msg.includes(keyword))
        );

        // Allow AWS Amplify token-related logs but not actual token values
        const realExposures = exposedSecrets.filter(
            msg => !msg.includes('token:') && !msg.includes('fetching token')
        );

        expect(realExposures.length).toBe(0);
    });
});
