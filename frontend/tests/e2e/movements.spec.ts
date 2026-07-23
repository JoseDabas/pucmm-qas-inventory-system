import { test, expect } from '@playwright/test';
import { loginAs, getAccessToken, seedProduct } from './helpers/session';

test.describe('Movimientos - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/historial');
  });

  test('Admin ve el botón Registrar Movimiento', async ({ page }) => {
    await expect(page.getByTestId('create-movement-button')).toBeVisible();
  });

  test('Admin registra un movimiento y aparece en el historial', async ({ page, request }) => {
    // Semilla: producto vía API para tener algo que seleccionar en el combo.
    const product = await seedProduct(request, getAccessToken('admin'));
    await loginAs(page, 'admin', '/historial');

    await page.getByTestId('create-movement-button').click();
    // El combo muestra "<name> (<sku>)"; seleccionamos por label.
    await page.getByTestId('movement-product').selectOption({ label: `${product.name} (${product.skuCode})` });
    await page.getByTestId('movement-type').selectOption('IN');
    await page.getByTestId('movement-quantity').fill('7');
    await page.getByTestId('movement-observations').fill('Movimiento E2E');
    await page.getByTestId('movement-submit').click();

    await page.getByTestId('movement-search-input').fill(product.name);
    await expect(page.getByText(product.name).first()).toBeVisible({ timeout: 10000 });
  });

  test('Bloquea registrar movimiento sin producto ni cantidad', async ({ page }) => {
    await page.getByTestId('create-movement-button').click();
    await page.getByTestId('movement-submit').click();
    await expect(page.getByRole('heading', { name: 'Registrar Movimiento' })).toBeVisible();
    const productValid = await page.getByTestId('movement-product').evaluate((el: HTMLSelectElement) => el.checkValidity());
    expect(productValid).toBeFalsy();
  });

  test('Snapshot del historial (filas enmascaradas)', async ({ page }) => {
    await expect(page.getByTestId('movements-table')).toBeVisible();
    await expect(page.getByTestId('main-content')).toHaveScreenshot('movements.png', {
      mask: [page.getByTestId('movements-table').locator('tbody')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Movimientos - Viewer (solo lectura)', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/historial');
  });

  test('Viewer no ve el botón de registrar pero sí la tabla', async ({ page }) => {
    await expect(page.getByTestId('movements-table')).toBeVisible();
    await expect(page.getByTestId('create-movement-button')).not.toBeVisible();
  });
});
