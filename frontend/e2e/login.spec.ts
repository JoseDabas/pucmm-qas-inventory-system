import { test, expect } from '@playwright/test';

test('muestra la pantalla de login cuando no hay sesión', async ({ page }) => {
  await page.goto('/'); // Navega a la página principal sin sesión activa

  await expect(page.getByRole('heading', { name: 'Bienvenido' })).toBeVisible(); // Asegura que el título de bienvenida se muestra
  await expect(page.getByRole('button', { name: /Iniciar Sesión con SSO/ })).toBeVisible(); // Asegura que el botón de SSO se muestra
});
