import { test as setup, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// Rutas locales donde se guardarán las cookies y el almacenamiento local capturado
const authFileAdmin = 'tests/e2e/.auth/admin.json';

/**
 * Hook global que se ejecuta antes de todas las pruebas de setup.
 * 
 * Se encarga de verificar que la carpeta de almacenamiento de estado (.auth)
 * exista en el sistema de archivos, creándola de manera recursiva si no es el caso.
 * Esto evita errores de escritura al momento de guardar los storageStates.
 */
setup.beforeAll(() => {
  const authDirAdmin = path.dirname(authFileAdmin);
  if (!fs.existsSync(authDirAdmin)) {
    fs.mkdirSync(authDirAdmin, { recursive: true });
  }
});

/**
 * Script de configuración de estado: Autenticación como Administrador.
 * 
 * Navega a la aplicación, interactúa con el botón de SSO para ser redirigido
 * al servidor de Keycloak, inyecta las credenciales del usuario Administrador,
 * y luego de un inicio de sesión exitoso, captura y guarda el estado (cookies,
 * localStorage) en un archivo JSON.
 * 
 * Este archivo JSON será posteriormente inyectado en las pruebas que requieran
 * el rol de Administrador.
 */
setup('authenticate as admin', async ({ page }) => {
  await page.goto('/');

  // Inicia el flujo OIDC mediante la interfaz de usuario de la aplicación
  await page.getByRole('button', { name: /Iniciar Sesión con SSO/i }).click();

  // Resolución de credenciales mediante variables de entorno para CI o desarrollo
  const adminUser = process.env.KEYCLOAK_ADMIN_USERNAME || 'admin_placeholder';
  const adminPass = process.env.KEYCLOAK_ADMIN_PASSWORD || 'admin_placeholder';

  // Inserción de credenciales en los campos estándar de Keycloak
  await page.fill('input[name="username"]', adminUser);
  await page.fill('input[name="password"]', adminPass);

  // Dispara el envío del formulario de inicio de sesión de Keycloak
  await page.click('input[name="login"], button[name="login"]');

  // Asegura que el redireccionamiento OIDC se completó esperando ver la UI del Inventario
  await expect(page.getByText('Inventario')).toBeVisible({ timeout: 15000 });

  // Espera explícita estática (2 segundos) para garantizar que oidc-client-ts termine 
  // de persistir el token en el SessionStorage, independientemente del nombre de la llave.
  await page.waitForTimeout(2000);

  // Extraemos el SessionStorage y lo guardamos manualmente en un archivo
  const sessionStorageAdmin = await page.evaluate(() => JSON.stringify(window.sessionStorage));
  fs.writeFileSync('tests/e2e/.auth/admin-session.json', sessionStorageAdmin);

  // Consolida y exporta el contexto de seguridad capturado (Cookies)
  await page.context().storageState({ path: authFileAdmin });
});

