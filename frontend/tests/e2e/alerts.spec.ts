import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/session';

test.describe('Alertas - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/alertas');
  });

  test('Admin ve la página de alertas (tabla o estado vacío)', async ({ page }) => {
    const table = page.getByTestId('alerts-table');
    const emptyState = page.getByTestId('alerts-empty-state');
    // La página muestra uno u otro según si hay productos críticos en ese momento
    await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 });
  });

  test('Snapshot de la página de alertas', async ({ page }) => {
    const table = page.getByTestId('alerts-table');
    const emptyState = page.getByTestId('alerts-empty-state');
    await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('main-content')).toHaveScreenshot('alerts.png', {
      mask: [table.locator('tbody')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Alertas - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/alertas');
  });

  test('Viewer puede acceder a la página de alertas', async ({ page }) => {
    const table = page.getByTestId('alerts-table');
    const emptyState = page.getByTestId('alerts-empty-state');
    await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 });
  });
});
