import { test, expect } from '@playwright/test';

/**
 * Suite de Pruebas: Flujo Feliz - Administrador.
 * 
 * Agrupa todos los casos de prueba donde el actor principal cuenta con un nivel
 * de acceso administrativo irrestricto. Inyecta automáticamente el estado de sesión
 * previamente generado en el paso de `setup`, simulando un usuario logueado en Keycloak.
 */
test.describe('Flujo Feliz - Admin', () => {
  // Inyección del contexto de seguridad (cookies/tokens) para el rol de Admin
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });

  /**
   * Valida la creación íntegra de un producto y garantiza la fidelidad visual.
   * 
   * Paso a paso:
   * 1. Carga la página principal donde se asume el listado (al estar autenticado).
   * 2. Captura un Snapshot visual estricto de la tabla de inventario para detectar regresiones CSS.
   * 3. Interactúa con el Modal de "Nuevo Producto" e introduce datos simulados (SKU único).
   * 4. Envía el formulario y valida de forma asíncrona que el backend acepta la creación
   *    y el frontend renderiza dinámicamente el nuevo ítem en pantalla.
   */
  test('Debe poder crear un producto y validar la interfaz visual', async ({ page }) => {
    // 1. Navegación a la vista principal
    await page.goto('/');

    // 2. Validación Visual (Snapshot)
    const table = page.getByTestId('products-table');
    await expect(table).toBeVisible();

    // Captura visual comparando contra una imagen de referencia, tolerando una diferencia del 10%
    await expect(table).toHaveScreenshot('inventario-tabla.png', { maxDiffPixelRatio: 0.1 });

    // 3. Apertura del Modal de Creación
    await page.getByTestId('create-product-button').click();

    // Generación de un SKU único basado en Timestamp para prevenir conflictos en la Base de Datos
    const uniqueSku = `TEST-${Date.now()}`;

    // Llenado automatizado de todos los campos requeridos y opcionales
    await page.getByTestId('product-name').fill('Producto E2E Playwright');
    await page.getByTestId('product-sku').fill(uniqueSku);
    await page.getByTestId('product-description').fill('Validación automática E2E');
    await page.getByTestId('product-category').fill('QA');
    await page.getByTestId('product-price').fill('99.99');
    await page.getByTestId('product-initial-quantity').fill('10');
    await page.getByTestId('product-minimum-stock').fill('5');

    // 4. Sumisión del formulario al Backend
    await page.getByTestId('product-submit').click();

    // 5. Verificación de renderizado en el listado tras la respuesta HTTP 201 Created
    await expect(page.getByText(uniqueSku)).toBeVisible({ timeout: 10000 });
  });
});

/**
 * Suite de Pruebas: Flujo de Seguridad - Usuario Sin Privilegios.
 * 
 * Agrupa los casos de prueba destinados a validar que las restricciones RBAC
 * (Role-Based Access Control) aplicadas en el Backend están operando correctamente,
 * impidiendo acciones destructivas o de escritura a roles con un JWT limitado.
 */
test.describe('Flujo de Seguridad - User (Sin privilegios)', () => {
  // Inyección del contexto de seguridad limitado
  test.use({ storageState: 'tests/e2e/.auth/user.json' });

  /**
   * Verifica la denegación estricta al crear un producto (Simulación de Inyección/Hack).
   * 
   * Dado que la UI de esta aplicación no utiliza rutas protegidas separadas (como `/products/new`),
   * sino que la creación se maneja en un Modal, el usuario sin privilegios puede visualizar
   * el botón, pero al enviar el payload al backend, el servicio API debe responder
   * con un código HTTP 403 Forbidden, el cual será renderizado como error.
   */
  test('Debe denegar la creación de producto por falta de privilegios (RBAC)', async ({ page }) => {
    await page.goto('/');

    // Apertura del modal de creación (Intento de vulneración)
    await page.getByTestId('create-product-button').click();

    const uniqueSku = `TEST-USER-${Date.now()}`;
    await page.getByTestId('product-name').fill('Intento Hack');
    await page.getByTestId('product-sku').fill(uniqueSku);
    await page.getByTestId('product-price').fill('1');
    await page.getByTestId('product-initial-quantity').fill('1');
    await page.getByTestId('product-minimum-stock').fill('1');

    // Sumisión forzada del formulario
    await page.getByTestId('product-submit').click();

    // Verificación de la contención: El contenedor genérico de errores (Tailwind: bg-red-500/10) debe aparecer
    const errorAlert = page.locator('.bg-red-500\\/10');
    await expect(errorAlert).toBeVisible();

    // Verificación semántica de la alerta para evitar falsos positivos
    await expect(errorAlert).toContainText(/error/i);
  });
});

/**
 * Suite de Pruebas: Flujo de Seguridad - Sesión Inexistente o Expirada.
 * 
 * Agrupa los casos de prueba que interactúan con la aplicación web
 * partiendo de un estado limpio y anónimo (Sin JWT, Cookies ni LocalStorage).
 */
test.describe('Flujo de Seguridad - No Autenticado', () => {
  // Anulación explícita de cualquier storageState heredado, asegurando virginidad en la sesión
  test.use({ storageState: { cookies: [], origins: [] } });

  /**
   * Garantiza que el sistema impone autenticación mandatoria antes de exponer data sensible.
   * 
   * Paso a paso:
   * 1. Ingresa a la raíz de la aplicación sin contexto de sesión.
   * 2. Confirma la carga de la vista de "Bienvenida/SSO" instigada por `react-oidc-context`.
   * 3. Garantiza que la tabla de inventario permanezca oculta y protegida.
   * 4. Valida el correcto encadenamiento hacia el Identity Provider (Keycloak) al interactuar.
   */
  test('Debe requerir inicio de sesión impidiendo ver el inventario', async ({ page }) => {
    await page.goto('/');

    // 1. Verificación de renderizado de la UI de bienvenida
    const loginButton = page.getByRole('button', { name: /Iniciar Sesión con SSO/i });
    await expect(loginButton).toBeVisible();

    // 2. Comprobación de seguridad: La tabla (Data sensitiva) está estrictamente inaccesible
    await expect(page.getByTestId('products-table')).not.toBeVisible();

    // 3. Ejecución del redireccionamiento OAuth2/OIDC
    await loginButton.click();

    // 4. Verificación de la URL destino (Regex validando los dominios típicos de Keycloak)
    await expect(page).toHaveURL(/.*(keycloak|auth|realms).*/i, { timeout: 10000 });
  });
});
