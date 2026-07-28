import { test, expect } from '@playwright/test';
import { installNetworkRewrite } from './helpers/session';

test.describe('Login', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test.beforeEach(async ({ page }) => {
    await installNetworkRewrite(page);
    await page.goto('/');
  });

  test('Muestra el formulario de login al no estar autenticado', async ({ page }) => {
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.getByRole('button', { name: /Iniciar Sesión/i })).toBeVisible();
  });

  test('Bloquea el envío si los campos están vacíos', async ({ page }) => {
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    // Los campos son required (HTML5) — el formulario no llega al servidor
    const usernameValid = await page.locator('#username').evaluate((el: HTMLInputElement) => el.checkValidity());
    expect(usernameValid).toBeFalsy();
    // El formulario sigue visible (no navegó)
    await expect(page.locator('#username')).toBeVisible();
  });

  test('Muestra error con credenciales incorrectas', async ({ page }) => {
    await page.locator('#username').fill('usuario-inexistente');
    await page.locator('#password').fill('contraseña-incorrecta');
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    await expect(page.getByText(/usuario o contraseña incorrectos/i)).toBeVisible({ timeout: 10000 });
    // El formulario permanece visible — no redirigió al dashboard
    await expect(page.locator('#username')).toBeVisible();
  });

  test('Snapshot del formulario de login', async ({ page }) => {
    await expect(page.locator('#username')).toBeVisible();
    await expect(page).toHaveScreenshot('login.png', { maxDiffPixelRatio: 0.2 });
  });
});
