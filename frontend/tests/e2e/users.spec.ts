import { test, expect } from '@playwright/test';
import { loginAs, deleteKeycloakUserByUsername } from './helpers/session';

test.describe('Usuarios - Admin', () => {
  test.use({ storageState: 'tests/e2e/.auth/admin.json' });

  // Usuario creado por el happy-path; se limpia en afterEach.
  let createdUsername: string | null = null;

  test.beforeEach(async ({ page }) => {
    createdUsername = null;
    await loginAs(page, 'admin', '/usuarios');
  });

  test.afterEach(async ({ request }) => {
    if (createdUsername) {
      await deleteKeycloakUserByUsername(request, createdUsername);
      createdUsername = null;
    }
  });

  test('Admin ve el botón Crear Cuenta y la tabla de usuarios', async ({ page }) => {
    await expect(page.getByTestId('users-table')).toBeVisible();
    await expect(page.getByTestId('create-user-button')).toBeVisible();
  });

  test('Admin crea una cuenta y aparece en la tabla', async ({ page }) => {
    const username = `e2e-user-${Date.now()}`;
    createdUsername = username; // registrar para limpieza aun si falla una aserción
    await page.getByTestId('create-user-button').click();
    await page.getByTestId('user-username').fill(username);
    await page.getByTestId('user-email').fill(`${username}@inventario.local`);
    await page.getByTestId('user-firstname').fill('Prueba');
    await page.getByTestId('user-lastname').fill('E2E');
    await page.getByTestId('user-password').fill('Password123');
    // El rol ya viene preseleccionado (primer rol del catálogo); no lo cambiamos.
    await page.getByTestId('user-submit').click();

    // exact:true para no chocar con la celda de correo (username@inventario.local).
    await expect(page.getByText(username, { exact: true })).toBeVisible({ timeout: 10000 });
  });

  test('Bloquea crear cuenta con contraseña menor a 8 caracteres', async ({ page }) => {
    await page.getByTestId('create-user-button').click();
    await page.getByTestId('user-username').fill(`e2e-short-${Date.now()}`);
    await page.getByTestId('user-email').fill('short@inventario.local');
    await page.getByTestId('user-password').fill('123');
    await page.getByTestId('user-submit').click();
    await expect(page.getByRole('heading', { name: 'Crear Cuenta' })).toBeVisible();
    const valid = await page.getByTestId('user-password').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(valid).toBeFalsy();
  });

  test('Muestra error 409 al crear una cuenta con username existente', async ({ page }) => {
    // 'admin-user' ya existe en Keycloak -> conflicto. No crea nada nuevo (sin limpieza).
    await page.getByTestId('create-user-button').click();
    await page.getByTestId('user-username').fill('admin-user');
    await page.getByTestId('user-email').fill('dup@inventario.local');
    await page.getByTestId('user-password').fill('Password123');
    await page.getByTestId('user-submit').click();
    await expect(page.getByText(/ya existe una cuenta/i)).toBeVisible({ timeout: 10000 });
  });

  test('Snapshot de la página de usuarios (filas enmascaradas)', async ({ page }) => {
    await expect(page.getByTestId('users-table')).toBeVisible();
    await expect(page.getByTestId('main-content')).toHaveScreenshot('users.png', {
      mask: [page.getByTestId('users-table').locator('tbody')],
      maxDiffPixelRatio: 0.2,
    });
  });
});
