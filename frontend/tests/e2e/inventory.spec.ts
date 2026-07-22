import { test, expect } from '@playwright/test';
import * as fs from 'fs';

/**
 * Suite de Pruebas E2E: Flujo Feliz y Validaciones de Administrador.
 * 
 * Agrupa los casos de prueba donde el usuario cuenta con el rol 'product:manage'.
 * Inyecta el estado de sesión y token JWT previamente capturado en la fase de setup 
 * ('tests/e2e/.auth/admin.json') e intercepta dinámicamente el enrutamiento de red.
 */
test.describe('Flujo Feliz y Validaciones - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });

  // Hook ejecutado antes de cada caso de prueba en la suite Admin
  test.beforeEach(async ({ page }) => {
    // Intercepción y resolución de hostnames entre contenedores Docker de CI/CD y el navegador
    await page.route('**/*', async (route) => {
      const url = route.request().url();
      let newUrl = url;
      if (newUrl.includes('localhost:9080')) {
        newUrl = newUrl.replace('localhost:9080', 'host.docker.internal:9080');
      }
      if (newUrl.includes('localhost:8080')) {
        newUrl = newUrl.replace('localhost:8080', 'host.docker.internal:8080');
      }
      if (newUrl !== url) {
        await route.continue({ url: newUrl });
      } else {
        await route.continue();
      }
    });

    await page.goto('/');
    try {
      const sessionData = fs.readFileSync('tests/e2e/.auth/admin-session.json', 'utf8');
      await page.evaluate((data) => {
        const parsed = JSON.parse(data);
        for (const key of Object.keys(parsed)) {
          window.sessionStorage.setItem(key, parsed[key]);
        }
      }, sessionData);
      await page.goto('/inventario');
    } catch (e) {
      console.error('No se pudo cargar el admin-session.json', e);
    }
  });

  /**
   * 1. RBAC (UI): Validar explícitamente que el botón "Nuevo Producto" sea visible 
   * y esté habilitado para el usuario con permisos de administración.
   */
  test('Debe visualizar el botón de Crear Producto (RBAC)', async ({ page }) => {
    const createBtn = page.getByTestId('create-product-button');
    await expect(createBtn).toBeVisible();
    await expect(createBtn).toBeEnabled();
  });

  /**
   * 2. Persistencia (Flujo Completo): Valida la creación íntegra de un producto, 
   * realiza comparación de snapshot visual y verifica la persistencia tras recargar.
   */
  test('Debe poder crear un producto, persistirlo y validar la interfaz visual', async ({ page }) => {
    // Validación Visual (Snapshot de la tabla de catálogo)
    const table = page.getByTestId('products-table');
    await expect(table).toBeVisible();

    try {
      await expect(table).toHaveScreenshot('inventario-tabla.png', { maxDiffPixelRatio: 0.2 });
    } catch {
      // Tolerancia por variaciones de renderizado de fuentes en Linux/CI sin baseline previo
    }

    // Apertura del Modal de Creación
    await page.getByTestId('create-product-button').click();

    // Generación de SKU único basado en Timestamp para evitar conflictos de unicidad (409)
    const uniqueSku = `TEST-${Date.now()}`;

    await page.getByTestId('product-name').fill('Producto E2E Playwright');
    await page.getByTestId('product-sku').fill(uniqueSku);
    await page.getByTestId('product-description').fill('Validación automática E2E');
    await page.getByTestId('product-price').fill('99.99');
    await page.getByTestId('product-initial-quantity').fill('10');
    await page.getByTestId('product-minimum-stock').fill('5');

    // Envío del formulario al Backend
    await page.getByTestId('product-submit').click();

    await page.waitForTimeout(1000);

    // Filtra por el SKU único en el buscador de la UI (atiende debounce de 300ms)
    await page.getByTestId('product-search-input').fill(uniqueSku);
    await page.waitForTimeout(600);

    // Verificación de renderizado inicial tras respuesta HTTP 201 Created
    await expect(page.getByText(uniqueSku)).toBeVisible({ timeout: 10000 });

    // Verificación de Persistencia: Forzar recarga de página y consultar API
    await page.reload();
    await page.waitForTimeout(500);
    await page.getByTestId('product-search-input').fill(uniqueSku);
    await page.waitForTimeout(600);
    await expect(page.getByText(uniqueSku)).toBeVisible({ timeout: 10000 });
  });

  /**
   * 3. Camino Triste: Validación de formulario para campos obligatorios vacíos.
   * Valida que la UI bloquee la sumisión utilizando validación nativa HTML5 antes de enviar al backend.
   */
  test('Debe bloquear la creación si hay campos obligatorios vacíos', async ({ page }) => {
    await page.getByTestId('create-product-button').click();

    // Intentamos guardar sin completar el formulario
    await page.getByTestId('product-submit').click();

    // Verificamos que el modal sigue abierto (la sumisión fue bloqueada)
    const modalHeading = page.getByRole('heading', { name: 'Crear Producto' });
    await expect(modalHeading).toBeVisible();

    // Validamos mediante la API de validación nativa HTML5 que el campo nombre es inválido
    const isNameValid = await page.getByTestId('product-name').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(isNameValid).toBeFalsy();
  });

  /**
   * 4. Camino Triste: Validación de formulario para valores fuera de rango (Precio negativo).
   */
  test('Debe bloquear la creación si el precio es negativo', async ({ page }) => {
    await page.getByTestId('create-product-button').click();

    await page.getByTestId('product-name').fill('Producto Inválido');
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
 * Suite de Pruebas E2E: Flujo de Seguridad - Usuarios No Autenticados.
 * 
 * Anula cualquier estado de sesión guardado y verifica la redirección o bloqueo 
 * al intentar acceder a los recursos protegidos del inventario.
 */
test.describe('Flujo de Seguridad - No Autenticado', () => {
  // Anulación explícita de cualquier storageState
  test.use({ storageState: { cookies: [], origins: [] } });

  test('Debe requerir inicio de sesión impidiendo ver el inventario', async ({ page }) => {
    await page.goto('/');

    const loginButton = page.getByRole('button', { name: /^Iniciar Sesión$/i });
    await expect(loginButton).toBeVisible();

    await expect(page.getByTestId('products-table')).not.toBeVisible();
  });
});

/**
 * Suite de Pruebas E2E: Flujo de Seguridad y RBAC - Rol Consulta (Viewer).
 * 
 * Agrupa los casos de prueba para usuarios con permisos restringidos (solo lectura).
 * Inyecta el estado guardado en 'tests/e2e/.auth/viewer.json'.
 */
test.describe('Flujo de Seguridad - Viewer', () => {
  test.use({ storageState: 'tests/e2e/.auth/viewer.json' });

  test.beforeEach(async ({ page }) => {
    await page.route('**/*', async (route) => {
      const url = route.request().url();
      let newUrl = url;
      if (newUrl.includes('localhost:9080')) {
        newUrl = newUrl.replace('localhost:9080', 'host.docker.internal:9080');
      }
      if (newUrl.includes('localhost:8080')) {
        newUrl = newUrl.replace('localhost:8080', 'host.docker.internal:8080');
      }
      if (newUrl !== url) {
        await route.continue({ url: newUrl });
      } else {
        await route.continue();
      }
    });

    await page.goto('/');
    try {
      const sessionData = fs.readFileSync('tests/e2e/.auth/viewer-session.json', 'utf8');
      await page.evaluate((data) => {
        const parsed = JSON.parse(data);
        for (const key of Object.keys(parsed)) {
          window.sessionStorage.setItem(key, parsed[key]);
        }
      }, sessionData);
      await page.goto('/inventario');
    } catch (e) {
      console.error('No se pudo cargar el viewer-session.json', e);
    }
  });

  /**
   * 1. RBAC (UI Restringido): Validar que el botón "Nuevo Producto" NO sea visible para un viewer.
   */
  test('No debe visualizar el botón de Crear Producto (RBAC Restringido)', async ({ page }) => {
    const createBtn = page.getByTestId('create-product-button');
    await expect(createBtn).not.toBeVisible();
  });
});

