import { test, expect, type Page } from '@playwright/test';
import { loginAs } from './helpers/session';

const METRIC_LABELS = [
  'Productos',
  'Categorías',
  'Movimientos',
  'Productos críticos',
  'Unidades en inventario',
  'Valor del inventario',
  '% en estado crítico',
];

async function assertDashboardMetrics(page: Page): Promise<void> {
  // Scopeamos al contenido principal: el Header y el Sidebar repiten textos
  // ("Dashboard" como título, "Categorías" como enlace) que romperían el strict mode.
  const content = page.getByTestId('main-content');
  await expect(content.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  for (const label of METRIC_LABELS) {
    await expect(content.getByText(label, { exact: true })).toBeVisible();
  }
}

test.describe('Dashboard - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/dashboard');
  });

  test('Admin ve las 7 métricas del tablero', async ({ page }) => {
    await assertDashboardMetrics(page);
  });

  test('Snapshot del dashboard (valores enmascarados)', async ({ page }) => {
    await expect(page.getByTestId('main-content').getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    // Los valores numéricos cambian con los datos reales: se enmascaran.
    await expect(page.getByTestId('main-content')).toHaveScreenshot('dashboard.png', {
      mask: [page.getByTestId('metric-value')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Dashboard - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/dashboard');
  });

  test('Viewer también ve el tablero (métricas libres para todo rol)', async ({ page }) => {
    await assertDashboardMetrics(page);
  });
});
