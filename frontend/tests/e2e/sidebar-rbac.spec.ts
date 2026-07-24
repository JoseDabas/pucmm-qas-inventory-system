import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/session';

const ALL_LINKS = ['Dashboard', 'Inventario', 'Historial', 'Categorías', 'Reportes', 'Usuarios'];

test.describe('Sidebar RBAC - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/dashboard');
  });

  test('Admin ve todos los enlaces del menú, incluido Usuarios', async ({ page }) => {
    for (const label of ALL_LINKS) {
      await expect(page.getByRole('link', { name: label, exact: true })).toBeVisible();
    }
  });

  test('Snapshot del layout (sidebar admin)', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Usuarios', exact: true })).toBeVisible();
    // Enmascaramos el contenido para comparar solo sidebar + header (chrome estable).
    await expect(page).toHaveScreenshot('layout-admin.png', {
      mask: [page.getByTestId('main-content')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Sidebar RBAC - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/dashboard');
  });

  test('Viewer ve los enlaces permitidos pero NO Usuarios', async ({ page }) => {
    for (const label of ['Dashboard', 'Inventario', 'Historial', 'Categorías', 'Reportes']) {
      await expect(page.getByRole('link', { name: label, exact: true })).toBeVisible();
    }
    await expect(page.getByRole('link', { name: 'Usuarios', exact: true })).not.toBeVisible();
  });

  test('Viewer navegando a /usuarios es redirigido al Dashboard', async ({ page }) => {
    await page.goto('/usuarios');
    // La guarda de ruta redirige al dashboard: verificamos por URL (hay dos
    // headings "Dashboard" — Header y página — que romperían el strict mode).
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByTestId('users-table')).not.toBeVisible();
  });

  test('Snapshot del layout (sidebar viewer)', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Dashboard', exact: true })).toBeVisible();
    await expect(page).toHaveScreenshot('layout-viewer.png', {
      mask: [page.getByTestId('main-content')],
      maxDiffPixelRatio: 0.2,
    });
  });
});
