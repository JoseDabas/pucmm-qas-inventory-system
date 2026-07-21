import { test, expect } from '@playwright/test';
import * as fs from 'fs';
/**
 * Suite de Pruebas: Flujo Feliz y Validaciones - Administrador.
 * 
 * Agrupa todos los casos de prueba donde el actor principal cuenta con un nivel
 * de acceso administrativo irrestricto. Inyecta automáticamente el estado de sesión
 * previamente generado en el paso de `setup`.
 */
test.describe('Flujo Feliz y Validaciones - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });

  // Hook que se ejecuta antes de cada prueba de esta suite
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    try {
      const sessionData = fs.readFileSync('tests/e2e/.auth/admin-session.json', 'utf8');
      await page.evaluate((data) => {
        const parsed = JSON.parse(data);
        for (const key of Object.keys(parsed)) {
          window.sessionStorage.setItem(key, parsed[key]);
        }
      }, sessionData);
      await page.reload();
    } catch (e) {
      console.error('No se pudo cargar el admin-session.json', e);
    }
  });

  /**
   * 1. RBAC (UI): Validar explícitamente que el botón "Crear Producto" sea visible 
   * y esté habilitado para el administrador.
   */
  test('Debe visualizar el botón de Crear Producto (RBAC)', async ({ page }) => {
    const createBtn = page.getByTestId('create-product-button');
    await expect(createBtn).toBeVisible();
    await expect(createBtn).toBeEnabled();
  });

  /**
   * 4. Persistencia (Flujo Completo): Valida la creación íntegra de un producto, 
   * garantiza fidelidad visual y verifica la persistencia tras recargar la página.
   */
  test('Debe poder crear un producto, persistirlo y validar la interfaz visual', async ({ page }) => {
    // Validación Visual (Snapshot)
    const table = page.getByTestId('products-table');
    await expect(table).toBeVisible();
    await expect(table).toHaveScreenshot('inventario-tabla.png', { maxDiffPixelRatio: 0.1 });

    // Apertura del Modal
    await page.getByTestId('create-product-button').click();

    // Generación de SKU único basado en Timestamp
    const uniqueSku = `TEST-${Date.now()}`;

    await page.getByTestId('product-name').fill('Producto E2E Playwright');
    await page.getByTestId('product-sku').fill(uniqueSku);
    await page.getByTestId('product-description').fill('Validación automática E2E');
    await page.getByTestId('product-category').fill('QA');
    await page.getByTestId('product-price').fill('99.99');
    await page.getByTestId('product-initial-quantity').fill('10');
    await page.getByTestId('product-minimum-stock').fill('5');

    // Sumisión del formulario al Backend
    await page.getByTestId('product-submit').click();

    // Verificación de renderizado inicial tras respuesta HTTP
    await expect(page.getByText(uniqueSku)).toBeVisible({ timeout: 10000 });

    // Verificación de Persistencia: Forzamos recarga de la página para consultar el Backend
    await page.reload();
    await expect(page.getByText(uniqueSku)).toBeVisible({ timeout: 10000 });
  });

  /**
   * 3. Camino Triste: Validación de formulario para campos obligatorios vacíos.
   * Valida que la UI bloquee la acción antes de enviarla al backend.
   */
  test('Debe bloquear la creación si hay campos obligatorios vacíos', async ({ page }) => {
    await page.getByTestId('create-product-button').click();

    // Intentamos guardar sin llenar nada
    await page.getByTestId('product-submit').click();

    // Verificamos que el modal sigue abierto (la sumisión fue bloqueada)
    const modalHeading = page.getByRole('heading', { name: 'Crear Producto' });
    await expect(modalHeading).toBeVisible();

    // Validamos mediante la API de validación nativa HTML5 que el campo name es inválido
    const isNameValid = await page.getByTestId('product-name').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(isNameValid).toBeFalsy();
  });

  /**
   * 3. Camino Triste: Validación de formulario para valores ilógicos (Precio negativo).
   */
  test('Debe bloquear la creación si el precio es negativo', async ({ page }) => {
    await page.getByTestId('create-product-button').click();

    await page.getByTestId('product-name').fill('Producto Invalido');
    await page.getByTestId('product-sku').fill(`INV-${Date.now()}`);
    // Ingresamos un valor negativo intencionalmente
    await page.getByTestId('product-price').fill('-50.00');
    await page.getByTestId('product-initial-quantity').fill('5');
    await page.getByTestId('product-minimum-stock').fill('1');

    await page.getByTestId('product-submit').click();

    // El formulario debe bloquear la acción usando el atributo min="0" nativo
    const modalHeading = page.getByRole('heading', { name: 'Crear Producto' });
    await expect(modalHeading).toBeVisible();

    // Evaluamos la propiedad validity del input de precio
    const isPriceValid = await page.getByTestId('product-price').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(isPriceValid).toBeFalsy();
  });
});



/**
 * Suite de Pruebas: Flujo de Seguridad - Sesión Inexistente o Expirada.
 */
test.describe('Flujo de Seguridad - No Autenticado', () => {
  // Anulación explícita de cualquier storageState
  test.use({ storageState: { cookies: [], origins: [] } });

  test('Debe requerir inicio de sesión impidiendo ver el inventario', async ({ page }) => {
    await page.goto('/');

    const loginButton = page.getByRole('button', { name: /Iniciar Sesión con SSO/i });
    await expect(loginButton).toBeVisible();

    await expect(page.getByTestId('products-table')).not.toBeVisible();

    await loginButton.click();
    await expect(page).toHaveURL(/.*(keycloak|auth|realms).*/i, { timeout: 10000 });
  });
});

/**
 * Suite de Pruebas: Flujo de Seguridad - Usuario Viewer.
 *
 * Agrupa los casos de prueba para usuarios con permisos limitados (ej. solo vista).
 */
test.describe('Flujo de Seguridad - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    try {
      const sessionData = fs.readFileSync('tests/e2e/.auth/viewer-session.json', 'utf8');
      await page.evaluate((data) => {
        const parsed = JSON.parse(data);
        for (const key of Object.keys(parsed)) {
          window.sessionStorage.setItem(key, parsed[key]);
        }
      }, sessionData);
      await page.reload();
    } catch (e) {
      console.error('No se pudo cargar el viewer-session.json', e);
    }
  });

  /**
   * 1. RBAC (UI): Validar que el botón "Crear Producto" NO sea visible para un viewer.
   */
  test('No debe visualizar el botón de Crear Producto (RBAC Restringido)', async ({ page }) => {
    const createBtn = page.getByTestId('create-product-button');
    // El botón debería estar oculto o deshabilitado si el frontend implementa RBAC en UI
    await expect(createBtn).not.toBeVisible();
  });
});
