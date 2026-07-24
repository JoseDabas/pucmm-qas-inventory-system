import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/session';

test.describe('Categorías - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', '/categorias');
  });

  test('Admin ve el botón Nueva Categoría', async ({ page }) => {
    await expect(page.getByTestId('create-category-button')).toBeVisible();
  });

  test('Admin crea una categoría y aparece en la tabla', async ({ page }) => {
    const name = `E2E-CAT-${Date.now()}`;
    await page.getByTestId('create-category-button').click();
    await page.getByTestId('category-name').fill(name);
    await page.getByTestId('category-description').fill('Categoría de prueba E2E');
    await page.getByTestId('category-submit').click();

    await page.getByTestId('category-search-input').fill(name);
    await expect(page.getByTestId('categories-table').getByText(name)).toBeVisible({ timeout: 10000 });
  });

  test('Bloquea crear categoría con nombre vacío', async ({ page }) => {
    await page.getByTestId('create-category-button').click();
    await page.getByTestId('category-submit').click();
    await expect(page.getByRole('heading', { name: 'Crear Categoría' })).toBeVisible();
    const valid = await page.getByTestId('category-name').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(valid).toBeFalsy();
  });

  test('Muestra error 409 al crear categoría duplicada', async ({ page }) => {
    const name = `E2E-DUP-${Date.now()}`;
    // Primera creación (éxito)
    await page.getByTestId('create-category-button').click();
    await page.getByTestId('category-name').fill(name);
    await page.getByTestId('category-submit').click();
    await page.getByTestId('category-search-input').fill(name);
    await expect(page.getByTestId('categories-table').getByText(name)).toBeVisible({ timeout: 10000 });
    // Segunda creación con el mismo nombre (409)
    await page.getByTestId('create-category-button').click();
    await page.getByTestId('category-name').fill(name);
    await page.getByTestId('category-submit').click();
    await expect(page.getByText(/ya existe/i)).toBeVisible({ timeout: 10000 });
  });

  test('Admin crea y elimina una categoría (sin productos)', async ({ page }) => {
    const name = `E2E-DEL-${Date.now()}`;
    await page.getByTestId('create-category-button').click();
    await page.getByTestId('category-name').fill(name);
    await page.getByTestId('category-submit').click();
    await page.getByTestId('category-search-input').fill(name);
    await expect(page.getByTestId('categories-table').getByText(name)).toBeVisible({ timeout: 10000 });

    // Con el filtro aplicado, la única fila visible es la recién creada.
    await page.getByTestId('delete-category-button').first().click();
    await page.getByTestId('confirm-delete-button').click();
    // Scopeamos a la tabla: el diálogo de confirmación también contiene el nombre.
    await expect(page.getByTestId('categories-table').getByText(name)).not.toBeVisible({ timeout: 10000 });
  });

  test('Snapshot de la página de categorías (filas enmascaradas)', async ({ page }) => {
    await expect(page.getByTestId('categories-table')).toBeVisible();
    await expect(page.getByTestId('main-content')).toHaveScreenshot('categories.png', {
      mask: [page.getByTestId('categories-table').locator('tbody')],
      maxDiffPixelRatio: 0.2,
    });
  });
});

test.describe('Categorías - Viewer (solo lectura)', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'viewer', '/categorias');
  });

  test('Viewer no ve botones de gestión pero sí la tabla', async ({ page }) => {
    await expect(page.getByTestId('categories-table')).toBeVisible();
    await expect(page.getByTestId('create-category-button')).not.toBeVisible();
    await expect(page.getByTestId('delete-category-button')).toHaveCount(0);
  });
});
