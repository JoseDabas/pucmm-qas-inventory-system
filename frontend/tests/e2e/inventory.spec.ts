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
    // NOTA: Se ha deshabilitado la validación visual (Snapshot) con 'toHaveScreenshot'.
    // Dado que la base de datos no se limpia entre pruebas, la tabla crecerá cada vez 
    // que se cree un producto E2E nuevo, haciendo que su tamaño en píxeles no coincida 
    // nunca con la imagen original de referencia. Para datos dinámicos, es mejor 
    // validar semánticamente (ej: verificar que el texto exista en la tabla).
    // await expect(table).toHaveScreenshot('inventario-tabla.png', { maxDiffPixelRatio: 0.1 });

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
 * Suite de Pruebas: Flujo de Seguridad - Usuario Sin Privilegios.
 * 
 * Agrupa los casos de prueba destinados a validar que las restricciones RBAC
 * aplicadas en la Interfaz y en el Backend operan correctamente.
 */
test.describe('Flujo de Seguridad - User (Sin privilegios)', () => {
  test.use({ storageState: 'tests/e2e/.auth/user.json' });

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    try {
      const sessionData = fs.readFileSync('tests/e2e/.auth/user-session.json', 'utf8');
      await page.evaluate((data) => {
        const parsed = JSON.parse(data);
        for (const key of Object.keys(parsed)) {
          window.sessionStorage.setItem(key, parsed[key]);
        }
      }, sessionData);
      await page.reload();
    } catch (e) {
      console.error('No se pudo cargar el user-session.json', e);
    }
  });

  /**
   * 1. RBAC (UI): Validar explícitamente que el botón "Crear Producto" NO exista.
   * (Nota QA: Este test revelará un defecto si el Frontend no oculta condicionalmente el botón).
   */
  test('No debe visualizar el botón de Crear Producto (RBAC)', async ({ page }) => {
    const createBtn = page.getByTestId('create-product-button');
    // Aseguramos que no esté visible ni renderizado en el DOM para usuarios normales
    await expect(createBtn).toBeHidden();
  });

  /**
   * 2. Bypass Routing: Pruebas de Seguridad Forzando URLs.
   * Simulamos la inyección directa de una ruta de creación en el navegador.
   */
  test('Debe interceptar el acceso directo a rutas de creación (Bypass Routing)', async ({ page }) => {
    // Navegación directa a una ruta no autorizada/inexistente en el router
    await page.goto('/productos/nuevo');

    // Verificamos que el sistema asegure el enrutamiento mostrando el dashboard base
    // o bloqueando la renderización del formulario modal.
    const table = page.getByTestId('products-table');
    await expect(table).toBeVisible();

    // Confirmamos explícitamente que la UI de creación nunca se expuso
    const modalHeading = page.getByRole('heading', { name: 'Crear Producto' });
    await expect(modalHeading).toBeHidden();
  });

  /**
   * Seguridad Adicional (Profundidad de Defensa): 
   * Intento de vulneración forzado directamente hacia el Backend.
   * Si el usuario lograra manipular el DOM para habilitar el botón, la API debe rechazarlo con 403.
   */
  test('Debe denegar la creación de producto por falta de privilegios (Backend API 403)', async ({ page }) => {
    // Forzamos el click en Playwright (ignorando si el elemento está oculto o deshabilitado)
    await page.getByTestId('create-product-button').click({ force: true });

    const uniqueSku = `TEST-USER-${Date.now()}`;
    await page.getByTestId('product-name').fill('Intento Hack');
    await page.getByTestId('product-sku').fill(uniqueSku);
    await page.getByTestId('product-price').fill('1');
    await page.getByTestId('product-initial-quantity').fill('1');
    await page.getByTestId('product-minimum-stock').fill('1');

    await page.getByTestId('product-submit').click({ force: true });

    // Verificamos que el contenedor de error genérico retornado por la red aparezca visible
    const errorAlert = page.locator('.bg-red-500\\/10');
    await expect(errorAlert).toBeVisible();
    await expect(errorAlert).toContainText(/error/i);
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
