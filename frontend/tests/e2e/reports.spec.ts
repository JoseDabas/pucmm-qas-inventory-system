import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/session';

test.describe('Reportes - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/reportes');
  });

  test('Admin ve el botón de descarga de reportes', async ({ page }) => {
    await expect(page.getByTestId('reports-download-button')).toBeVisible();
    await expect(page.getByTestId('reports-download-button')).toBeEnabled();
  });

  test('Muestra error si se intenta descargar sin seleccionar fechas', async ({ page }) => {
    await page.getByTestId('reports-download-button').click();
    await expect(page.getByTestId('reports-error-message')).toBeVisible();
    await expect(page.getByTestId('reports-error-message')).toContainText(/fecha/i);
  });

  test('Muestra error si la fecha de inicio es posterior a la de fin', async ({ page }) => {
    await page.getByTestId('reports-start-date').fill('2025-12-31T23:59');
    await page.getByTestId('reports-end-date').fill('2025-01-01T00:00');
    await page.getByTestId('reports-download-button').click();
    await expect(page.getByTestId('reports-error-message')).toBeVisible();
    await expect(page.getByTestId('reports-error-message')).toContainText(/inicio/i);
  });

  test('Snapshot de la página de reportes', async ({ page }) => {
    await expect(page.getByTestId('reports-download-button')).toBeVisible();
    await expect(page.getByTestId('main-content')).toHaveScreenshot('reports.png', {
      mask: [page.getByTestId('reports-category-select')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Reportes - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/reportes');
  });

  test('Viewer puede acceder a la página de reportes', async ({ page }) => {
    await expect(page.getByTestId('reports-download-button')).toBeVisible();
  });
});
