import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/session';

test.describe('Auditoría - Viewer (acceso denegado)', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/dashboard');
  });

  test('Viewer navegando a /auditoria es redirigido al Dashboard', async ({ page }) => {
    await page.goto('/auditoria');
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByTestId('audit-products-table')).not.toBeVisible();
  });
});

test.describe('Auditoría - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/auditoria');
  });

  test('Admin ve las dos pestañas de auditoría', async ({ page }) => {
    await expect(page.getByTestId('audit-products-tab')).toBeVisible();
    await expect(page.getByTestId('audit-movements-tab')).toBeVisible();
  });

  test('La pestaña Productos muestra su tabla por defecto', async ({ page }) => {
    await expect(page.getByTestId('audit-products-table')).toBeVisible();
    await expect(page.getByTestId('audit-movements-table')).not.toBeVisible();
  });

  test('Al cambiar a Movimientos se muestra la tabla de movimientos', async ({ page }) => {
    await page.getByTestId('audit-movements-tab').click();
    await expect(page.getByTestId('audit-movements-table')).toBeVisible();
    await expect(page.getByTestId('audit-products-table')).not.toBeVisible();
  });

  test('Snapshot de la página de auditoría (tbody enmascarado)', async ({ page }) => {
    await expect(page.getByTestId('audit-products-table')).toBeVisible();
    await expect(page.getByTestId('main-content')).toHaveScreenshot('audit.png', {
      mask: [page.getByTestId('audit-products-table').locator('tbody')],
      maxDiffPixelRatio: 0.2,
    });
  });
});
